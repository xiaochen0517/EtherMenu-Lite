package EtherMenu;

import EtherMenu.utils.Info;
import EtherMenu.utils.Logger;
import EtherMenu.utils.Patch;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.jar.JarFile;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
/**
 * Класс, отвечающий за установку и удаление чита из кодов игры
 */
public class GamePatcher {

   /**
    * Список всех файлов, подлежащих инъекции
    */
   private final String[] patchFiles = new String[]{
           "GameWindow.class", "inventory/ItemContainer.class", "Lua/LuaEventManager.class", "Lua/LuaManager.class", "characters/Stats.class",
           "characters/BodyDamage/Nutrition.class", "characters/BodyDamage/BodyDamage.class", "characters/IsoGameCharacter.class",
           "network/packets/PlayerXpPacket.class", "characters/PlayerCheats.class",
           "core/Core.class", "Lua/LuaManager$GlobalObject.class",
           "network/anticheats/AbstractAntiCheat.class", "network/anticheats/SuspiciousActivity.class",
           "network/GameServer.class"
   };

   /**
    * Nombre del archivo jar del juego (PZ Build 42+)
    */
   private final String gameJarFile = "projectzomboid.jar";

   /**
    * Название игровой папки с .class файлами
    */
   private final String gameClassFolder = "zombie";

   /**
    * Папки и файлы, которые нужно экспортировать в корневую директорию игры
    */
   private final String[] whiteListPathEtherFiles = new String[]{"EtherMenu"};

   /**
    * Экспортирование файлов EtherMenu в корневую директорию игры
    */
   public void extractEtherMenu() {
      try {
         String jarFilePath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
         Path currentDirectory = Paths.get(System.getProperty("user.dir"));

         try (JarFile jarFile = new JarFile(jarFilePath)) {
            jarFile.stream().filter((entry) -> Arrays.stream(whiteListPathEtherFiles).anyMatch(entry.getName()::startsWith))
                    .forEach((entry) -> {
                       try {
                          Path extractPath = currentDirectory.resolve(entry.getName());

                          if (entry.isDirectory()) {
                             Files.createDirectories(extractPath);
                          } else {
                             Files.createDirectories(extractPath.getParent());

                             try (InputStream inputStream = jarFile.getInputStream(entry)) {
                                Files.copy(inputStream, extractPath, StandardCopyOption.REPLACE_EXISTING);
                             }
                          }
                       } catch (IOException e) {
                          e.printStackTrace();
                       }
                    });
            Logger.print("Extraction completed successfully");
         }
      } catch (URISyntaxException | IOException e) {
         e.printStackTrace();
      }
   }


   /**
    *  Удаление всех экспортированных файлов EtherMenu из директории игры
    */
   public void uninstallEtherMenuFiles() {
      Logger.print("Deleting all EtherMenu files...");

      try {
         Path currentDirectory = Paths.get(System.getProperty("user.dir"));
         for (String pathPrefix : whiteListPathEtherFiles) {
            Path targetPath = currentDirectory.resolve(pathPrefix);
            if (Files.exists(targetPath)) {
               Files.walk(targetPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
         }
         Logger.print("Deletion EtherMenu files completed successfully");
      } catch (IOException except) {
         except.printStackTrace();
      }

   }

   /**
    * PZ Build 42+: No loose class files to back up pre-install.
    * Patched classes are written as new loose files that override jar entries via classpath order.
    * This method is kept for logging purposes.
    */
   public void backupGameFiles() {
      Logger.print("PZ Build 42+: Classes are inside " + gameJarFile + ". Patched classes will be written as loose files.");
      Logger.print("To uninstall, run with '--uninstall' flag to remove the loose class files.");
   }

   /**
    * Внедрение в файл игрового окна
    */
   public void patchGameWindow() {
      Patch.injectIntoClass("zombie/GameWindow", "init", true, (method) -> {
         AbstractInsnNode insertionPoint = null;

         // Find the point of injection
         for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof MethodInsnNode) {
               MethodInsnNode methodInsn = (MethodInsnNode) insn;
               if (methodInsn.getOpcode() == Opcodes.INVOKESTATIC
                       && methodInsn.owner.equals("zombie/Lua/LuaManager")
                       && methodInsn.name.equals("init")) {
                  insertionPoint = insn;
                  break;
               }
            }
         }

         if (insertionPoint != null) {
            InsnList initEtherLuaInstructions = new InsnList();
            initEtherLuaInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherMenu/Ether/EtherLuaCompiler", "getInstance", "()LEtherMenu/Ether/EtherLuaCompiler;", false));
            initEtherLuaInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "EtherMenu/Ether/EtherLuaCompiler", "init", "()V", false));
            method.instructions.insert(insertionPoint, initEtherLuaInstructions);
         } else {
            throw new IllegalStateException("Cannot find LuaManager.init() invocation in the method when patching the Game window");
         }
         // Find the last RETURN instruction in the normal flow (not in exception handlers)
         AbstractInsnNode returnInsn = null;
         for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
               returnInsn = insn;
            }
         }
         if (returnInsn != null){
            InsnList initLogoInstructions = new InsnList();
            initLogoInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherMenu/Ether/EtherLogo", "getInstance", "()LEtherMenu/Ether/EtherLogo;", false));
            initLogoInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "EtherMenu/Ether/EtherLogo", "init", "()V", false));
            method.instructions.insertBefore(returnInsn, initLogoInstructions);

            InsnList initEtherInstructions = new InsnList();
            initEtherInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherMenu/Ether/EtherMain", "getInstance", "()LEtherMenu/Ether/EtherMain;", false));
            initEtherInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "EtherMenu/Ether/EtherMain", "init", "()V", false));
            method.instructions.insertBefore(returnInsn, initEtherInstructions);
         } else {
            throw new IllegalStateException("Could not find RETURN instruction when patching the Game window");
         }
      });

   }

   /**
    * Внедрение в файлы игровых предметов
    */
   public void patchItemContainer() {
      // getWeight() removed in PZ Build 42 - only patching getCapacityWeight and getContentsWeight
      Patch.injectIntoClass("zombie/inventory/ItemContainer", "getCapacityWeight", false, (method) -> {
         InsnList newInstructions = new InsnList();
         LabelNode carryOnLabel = new LabelNode();
         newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherMenu/Ether/EtherMain", "getInstance", "()LEtherMenu/Ether/EtherMain;", false));
         newInstructions.add(new JumpInsnNode(Opcodes.IFNULL, carryOnLabel));
         newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherMenu/Ether/EtherMain", "getInstance", "()LEtherMenu/Ether/EtherMain;", false));
         newInstructions.add(new FieldInsnNode(Opcodes.GETFIELD, "EtherMenu/Ether/EtherMain", "etherAPI", "LEtherMenu/Ether/EtherAPI;"));
         newInstructions.add(new JumpInsnNode(Opcodes.IFNULL, carryOnLabel));
         newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherMenu/Ether/EtherMain", "getInstance", "()LEtherMenu/Ether/EtherMain;", false));
         newInstructions.add(new FieldInsnNode(Opcodes.GETFIELD, "EtherMenu/Ether/EtherMain", "etherAPI", "LEtherMenu/Ether/EtherAPI;"));
         newInstructions.add(new FieldInsnNode(Opcodes.GETFIELD, "EtherMenu/Ether/EtherAPI", "isUnlimitedCarry", "Z"));
         newInstructions.add(new JumpInsnNode(Opcodes.IFEQ, carryOnLabel));
         newInstructions.add(new InsnNode(Opcodes.FCONST_0));
         newInstructions.add(new InsnNode(Opcodes.FRETURN));
         newInstructions.add(carryOnLabel);
         method.instructions.insert(newInstructions);
      });
      Patch.injectIntoClass("zombie/inventory/ItemContainer", "getContentsWeight", false, (method) -> {
         InsnList newInstructions = new InsnList();
         LabelNode carryOnLabel = new LabelNode();
         newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherMenu/Ether/EtherMain", "getInstance", "()LEtherMenu/Ether/EtherMain;", false));
         newInstructions.add(new JumpInsnNode(Opcodes.IFNULL, carryOnLabel));
         newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherMenu/Ether/EtherMain", "getInstance", "()LEtherMenu/Ether/EtherMain;", false));
         newInstructions.add(new FieldInsnNode(Opcodes.GETFIELD, "EtherMenu/Ether/EtherMain", "etherAPI", "LEtherMenu/Ether/EtherAPI;"));
         newInstructions.add(new JumpInsnNode(Opcodes.IFNULL, carryOnLabel));
         newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherMenu/Ether/EtherMain", "getInstance", "()LEtherMenu/Ether/EtherMain;", false));
         newInstructions.add(new FieldInsnNode(Opcodes.GETFIELD, "EtherMenu/Ether/EtherMain", "etherAPI", "LEtherMenu/Ether/EtherAPI;"));
         newInstructions.add(new FieldInsnNode(Opcodes.GETFIELD, "EtherMenu/Ether/EtherAPI", "isUnlimitedCarry", "Z"));
         newInstructions.add(new JumpInsnNode(Opcodes.IFEQ, carryOnLabel));
         newInstructions.add(new InsnNode(Opcodes.FCONST_0));
         newInstructions.add(new InsnNode(Opcodes.FRETURN));
         newInstructions.add(carryOnLabel);
         method.instructions.insert(newInstructions);
      });
   }

   /**
    * Внедрение в файл LuaEventManager
    */
   public void patchLuaEventManager() {
      Patch.injectIntoClass("zombie/Lua/LuaEventManager", "triggerEvent", true, (method) -> {
         InsnList toInject = new InsnList();
         toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
         toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherMenu/utils/EventSubscriber", "invokeSubscriber", "(Ljava/lang/String;)V", false));
         method.instructions.insertBefore(method.instructions.get(0), toInject);
      });
   }

   /**
    * Внедрение в файл LuaManager
    */
   public void patchLuaManager() {
      Patch.injectIntoClass("zombie/Lua/LuaManager", "RunLua", true, (method) -> {
         if (!method.desc.equals("(Ljava/lang/String;Z)Ljava/lang/Object;")) {
            return;
         }

         InsnList newInstructions = new InsnList();
         LabelNode endOfMethodLabel = new LabelNode();

         newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "EtherMenu/Ether/EtherLuaCompiler", "getInstance", "()LEtherMenu/Ether/EtherLuaCompiler;", false));
         newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
         newInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "EtherMenu/Ether/EtherLuaCompiler", "isShouldLuaCompile", "(Ljava/lang/String;)Z", false));

         newInstructions.add(new JumpInsnNode(Opcodes.IFNE, endOfMethodLabel));

         newInstructions.add(new InsnNode(Opcodes.ACONST_NULL));
         newInstructions.add(new InsnNode(Opcodes.ARETURN));

         newInstructions.add(endOfMethodLabel);

         method.instructions.insert(newInstructions);
      });
   }

   /**
    * Inject into Stats.set(CharacterStat, float) to intercept ALL stat changes
    * (including server-authoritative updates) and enforce our overrides.
    */
   public void patchStats() {
      Patch.injectIntoClass("zombie/characters/Stats", "set", false, (method) -> {
         // Only patch set(CharacterStat, float) -> boolean
         if (!method.desc.equals("(Lzombie/characters/CharacterStat;F)Z")) {
            return;
         }
         // At the beginning of set(), intercept the float value:
         //   fstore_2 = EtherAPI.interceptStatSet(aload_1, fload_2)
         InsnList hook = new InsnList();
         hook.add(new VarInsnNode(Opcodes.ALOAD, 1));   // CharacterStat param
         hook.add(new VarInsnNode(Opcodes.FLOAD, 2));   // float value param
         hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                 "EtherMenu/Ether/EtherAPI", "interceptStatSet",
                 "(Lzombie/characters/CharacterStat;F)F", false));
         hook.add(new VarInsnNode(Opcodes.FSTORE, 2));  // overwrite float param
         method.instructions.insertBefore(method.instructions.get(0), hook);
      });
   }

   /**
    * Inject into Nutrition.setCalories(float) and Nutrition.setWeight(double)
    * to intercept server-authoritative nutrition updates.
    */
   public void patchNutrition() {
      // Hook setCalories(float) -> intercept and override float param
      Patch.injectIntoClass("zombie/characters/BodyDamage/Nutrition", "setCalories", false, (method) -> {
         if (!method.desc.equals("(F)V")) return;
         InsnList hook = new InsnList();
         hook.add(new VarInsnNode(Opcodes.FLOAD, 1));   // float value param
         hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                 "EtherMenu/Ether/EtherAPI", "interceptNutritionSetCalories",
                 "(F)F", false));
         hook.add(new VarInsnNode(Opcodes.FSTORE, 1));   // overwrite float param
         method.instructions.insertBefore(method.instructions.get(0), hook);
      });

      // Hook setWeight(double) -> intercept and override double param
      Patch.injectIntoClass("zombie/characters/BodyDamage/Nutrition", "setWeight", false, (method) -> {
         if (!method.desc.equals("(D)V")) return;
         InsnList hook = new InsnList();
         hook.add(new VarInsnNode(Opcodes.DLOAD, 1));   // double value param
         hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                 "EtherMenu/Ether/EtherAPI", "interceptNutritionSetWeight",
                 "(D)D", false));
         hook.add(new VarInsnNode(Opcodes.DSTORE, 1));   // overwrite double param
         method.instructions.insertBefore(method.instructions.get(0), hook);
      });
   }

   /**
    * Inject into BodyDamage.setOverallBodyHealth(float) to intercept
    * server-authoritative body health updates for stealth god mode.
    */
   public void patchBodyDamage() {
      Patch.injectIntoClass("zombie/characters/BodyDamage/BodyDamage", "setOverallBodyHealth", false, (method) -> {
         if (!method.desc.equals("(F)V")) return;
         InsnList hook = new InsnList();
         hook.add(new VarInsnNode(Opcodes.FLOAD, 1));   // float value param
         hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                 "EtherMenu/Ether/EtherAPI", "interceptSetOverallBodyHealth",
                 "(F)F", false));
         hook.add(new VarInsnNode(Opcodes.FSTORE, 1));   // overwrite float param
         method.instructions.insertBefore(method.instructions.get(0), hook);
      });
   }

   /**
    * Inject into IsoGameCharacter.setHealth(float) to intercept health changes.
    * Passes 'this' to the interceptor so it can check for local player only.
    */
   public void patchIsoGameCharacter() {
      Patch.injectIntoClass("zombie/characters/IsoGameCharacter", "setHealth", false, (method) -> {
         if (!method.desc.equals("(F)V")) return;
         InsnList hook = new InsnList();
         hook.add(new VarInsnNode(Opcodes.ALOAD, 0));   // this (IsoGameCharacter)
         hook.add(new VarInsnNode(Opcodes.FLOAD, 1));   // float value param
         hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                 "EtherMenu/Ether/EtherAPI", "interceptSetHealth",
                 "(Lzombie/characters/IsoGameCharacter;F)F", false));
         hook.add(new VarInsnNode(Opcodes.FSTORE, 1));   // overwrite float param
         method.instructions.insertBefore(method.instructions.get(0), hook);
      });
   }

   /**
    * Inject into PlayerXpPacket.parse() to intercept server XP sync.
    * BEFORE XP.load(), checks shouldSkipXpLoad() — if true, skips the load entirely
    * so server cannot overwrite locally-set skill levels. No notifications, no spam.
    */
   public void patchPlayerXpPacket() {
      Patch.injectIntoClass("zombie/network/packets/PlayerXpPacket", "parse", false, (method) -> {
         if (!method.desc.equals("(Lzombie/core/network/ByteBufferReader;Lzombie/network/IConnection;)V")) {
            return;
         }

         // Find the XP.load() invocation and the ALOAD_0 that starts its block
         MethodInsnNode loadInsn = null;
         String getPlayerOwner = null;
         for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof MethodInsnNode) {
               MethodInsnNode m = (MethodInsnNode) insn;
               if (m.name.equals("getPlayer") && m.desc.equals("()Lzombie/characters/IsoPlayer;")) {
                  getPlayerOwner = m.owner;
               }
               if (m.name.equals("load") && m.owner.equals("zombie/characters/IsoGameCharacter$XP")) {
                  loadInsn = m;
                  break;
               }
            }
         }

         if (loadInsn == null || getPlayerOwner == null) {
            throw new IllegalStateException("Cannot find XP.load() or getPlayer() in PlayerXpPacket.parse()");
         }

         // Find the ALOAD_0 that starts the XP.load() block:
         //   aload_0 -> getPlayer() -> getXp() -> ... -> XP.load()
         // Walk backwards from loadInsn to find the matching getXp() then getPlayer() then ALOAD_0
         AbstractInsnNode blockStart = loadInsn;
         while (blockStart != null) {
            blockStart = blockStart.getPrevious();
            if (blockStart != null && blockStart.getOpcode() == Opcodes.ALOAD
                    && ((VarInsnNode) blockStart).var == 0) {
               // Verify this ALOAD_0 leads to getPlayer -> getXp -> ... -> load
               AbstractInsnNode next = blockStart.getNext();
               while (next instanceof LabelNode || next instanceof LineNumberNode || next instanceof FrameNode) {
                  next = next.getNext();
               }
               if (next instanceof MethodInsnNode && ((MethodInsnNode) next).name.equals("getPlayer")) {
                  break; // Found the start of the load block
               }
            }
         }

         if (blockStart == null) {
            throw new IllegalStateException("Cannot find ALOAD_0 block start before XP.load()");
         }

         // Find the jump target: the GOTO after XP.load() jumps to RETURN
         // We'll create our own label to jump past the load block
         LabelNode skipLabel = new LabelNode();

         // Insert skip label after XP.load()
         method.instructions.insert(loadInsn, skipLabel);

         // Insert guard before the load block:
         //   if (EtherAPI.shouldSkipXpLoad(this.getPlayer())) goto skipLabel
         InsnList guard = new InsnList();
         guard.add(new VarInsnNode(Opcodes.ALOAD, 0));   // this (PlayerXpPacket)
         guard.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                 getPlayerOwner, "getPlayer", "()Lzombie/characters/IsoPlayer;", false));
         guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                 "EtherMenu/Ether/EtherAPI", "shouldSkipXpLoad",
                 "(Lzombie/characters/IsoPlayer;)Z", false));
         guard.add(new JumpInsnNode(Opcodes.IFNE, skipLabel));
         method.instructions.insertBefore(blockStart, guard);
      });
   }

   /**
    * Patch Core.getDebug() and Core.isInDebug() to return DebugBridge.debugOverride
    * instead of reading Core.debug directly. This keeps Core.debug=false (invisible
    * to network serialization) while debug features work via the bridge.
    */
   public void patchCoreDebug() {
      // Patch getDebug(): return Core.debug || DebugBridge.debugOverride
      Patch.injectIntoClass("zombie/core/Core", "getDebug", false, (method) -> {
         method.instructions.clear();
         method.tryCatchBlocks.clear();
         method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "zombie/core/Core", "debug", "Z"));
         method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "EtherMenu/DebugBridge", "debugOverride", "Z"));
         method.instructions.add(new InsnNode(Opcodes.IOR));
         method.instructions.add(new InsnNode(Opcodes.IRETURN));
      });

      // Patch isInDebug(): same logic
      Patch.injectIntoClass("zombie/core/Core", "isInDebug", false, (method) -> {
         method.instructions.clear();
         method.tryCatchBlocks.clear();
         method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "zombie/core/Core", "debug", "Z"));
         method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "EtherMenu/DebugBridge", "debugOverride", "Z"));
         method.instructions.add(new InsnNode(Opcodes.IOR));
         method.instructions.add(new InsnNode(Opcodes.IRETURN));
      });
   }

   /**
    * Patch LuaManager.GlobalObject.isDebugEnabled() to return DebugBridge.debugOverride
    * instead of Core.debug. This is the Lua global function isDebugEnabled().
    */
   public void patchIsDebugEnabled() {
      Patch.injectIntoClass("zombie/Lua/LuaManager$GlobalObject", "isDebugEnabled", true, (method) -> {
         method.instructions.clear();
         method.tryCatchBlocks.clear();
         method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "zombie/core/Core", "debug", "Z"));
         method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "EtherMenu/DebugBridge", "debugOverride", "Z"));
         method.instructions.add(new InsnNode(Opcodes.IOR));
         method.instructions.add(new InsnNode(Opcodes.IRETURN));
      });
   }

   /**
    * Patch PlayerCheats.isCheatAllowed() to always return true.
    * This ensures PlayerCheats.set() and isSet() work regardless of
    * Core.debug / GameClient.client / GameServer.server state.
    */
   public void patchPlayerCheats() {
      Patch.injectIntoClass("zombie/characters/PlayerCheats", "isCheatAllowed", false, (method) -> {
         // Replace entire method body with: return true
         method.instructions.clear();
         method.instructions.add(new InsnNode(Opcodes.ICONST_1));
         method.instructions.add(new InsnNode(Opcodes.IRETURN));
         // Clear exception handlers since we replaced the body
         method.tryCatchBlocks.clear();
      });
   }

   /**
    * Patch PlayerCheats.save() to always write an empty cheat set to the network buffer.
    * This prevents cheat flags (GOD_MODE, NO_CLIP, etc.) from being serialized and
    * sent to the server in PlayerUpdateReliable packets, which would trigger kicks.
    * The local EnumSet remains intact so isSet() still works client-side.
    */
   public void patchPlayerCheatsSave() {
      Patch.injectIntoClass("zombie/characters/PlayerCheats", "save", false, (method) -> {
         // Replace entire method body with: buffer.putInt(0); return;
         // Signature: void save(ByteBuffer buffer, boolean flag)
         // this=aload_0, buffer=aload_1, flag=iload_2
         method.instructions.clear();
         method.tryCatchBlocks.clear();
         method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));   // push ByteBuffer
         method.instructions.add(new InsnNode(Opcodes.ICONST_0));       // push 0
         method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                 "java/nio/ByteBuffer", "putInt",
                 "(I)Ljava/nio/ByteBuffer;", false));
         method.instructions.add(new InsnNode(Opcodes.POP));            // discard returned ByteBuffer
         method.instructions.add(new InsnNode(Opcodes.RETURN));         // return void
      });
   }

   /**
    * Patch AbstractAntiCheat.validate() to call ServerAntiCheatBypass.hookValidation().
    * If hook returns true, the original validation is skipped (return null).
    * Actual signature: String validate(UdpConnection, INetworkPacket) — instance method.
    */
   public void patchAntiCheatValidation() {
      Patch.injectIntoClass("zombie/network/anticheats/AbstractAntiCheat", "validate", false, (method) -> {
         InsnList hook = new InsnList();
         LabelNode continueLabel = new LabelNode();

         // hookValidation(this) — this=aload_0 (AbstractAntiCheat instance)
         hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
         hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                 "EtherMenu/Ether/ServerAntiCheatBypass", "hookValidation",
                 "(Ljava/lang/Object;)Z", false));
         hook.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
         // validate returns String — return null to indicate "no violation"
         hook.add(new InsnNode(Opcodes.ACONST_NULL));
         hook.add(new InsnNode(Opcodes.ARETURN));
         hook.add(continueLabel);

         method.instructions.insertBefore(method.instructions.get(0), hook);
      });
   }

   /**
    * Patch SuspiciousActivity.report() to call ServerAntiCheatBypass.hookSuspiciousActivity().
    * If hook returns true, return 0 (no suspicious activity increment).
    * Actual signature: int report(AntiCheat) — instance method.
    */
   public void patchSuspiciousActivityReport() {
      Patch.injectIntoClass("zombie/network/anticheats/SuspiciousActivity", "report", false, (method) -> {
         InsnList hook = new InsnList();
         LabelNode continueLabel = new LabelNode();

         // hookSuspiciousActivity(this) — this=aload_0 (SuspiciousActivity instance)
         hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
         hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                 "EtherMenu/Ether/ServerAntiCheatBypass", "hookSuspiciousActivity",
                 "(Ljava/lang/Object;)Z", false));
         hook.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
         // report returns int — return 0 to indicate "no suspicious activity"
         hook.add(new InsnNode(Opcodes.ICONST_0));
         hook.add(new InsnNode(Opcodes.IRETURN));
         hook.add(continueLabel);

         method.instructions.insertBefore(method.instructions.get(0), hook);
      });
   }

   /**
    * Patch GameServer.kick() to call ServerAntiCheatBypass.hookKickAction().
    * If hook returns true (anti-cheat related kick), the kick is blocked.
    * Actual signature: static void kick(IConnection, String, String).
    */
   public void patchKickPlayer() {
      Patch.injectIntoClass("zombie/network/GameServer", "kick", true, (method) -> {
         InsnList hook = new InsnList();
         LabelNode continueLabel = new LabelNode();

         // static kick(IConnection conn, String msg1, String msg2)
         // aload_0=IConnection, aload_1=String, aload_2=String
         hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
         hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
         hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                 "EtherMenu/Ether/ServerAntiCheatBypass", "hookKickAction",
                 "(Ljava/lang/String;Ljava/lang/String;)Z", false));
         hook.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
         hook.add(new InsnNode(Opcodes.RETURN));
         hook.add(continueLabel);

         method.instructions.insertBefore(method.instructions.get(0), hook);
      });
   }

   /**
    * Проверяет, содержит ли хотя бы один из заданных файлов аннотацию @Injected.
    * @return true, если аннотация @Injected найдена хотя бы в одном файле. false в противном случае.
    */
   public boolean checkInjectedAnnotations() {
      return Arrays.stream(patchFiles)
              .anyMatch(filePath -> Patch.isInjectedAnnotationPresent(filePath, gameClassFolder));
   }

   /**
    * Проверяет наличие игровой папки и определенных файлов внутри.
    * @return true, если игровой jar присутствует. false в противном случае.
    */
   public boolean isGameFolder() {
      // PZ Build 42+: classes are inside projectzomboid.jar, not loose files
      Path jarPath = Paths.get(gameJarFile);
      return Files.exists(jarPath) && Files.isRegularFile(jarPath);
   }

   /**
    * Патчинг игровых bytecode файлов игры
    * для реализации собственного фунционала
    */
   public void patchGame() {
      Logger.printCredits();

      Logger.print("Preparing to install the EtherMenu...");

      if (!isGameFolder()) {
         Logger.print("No game files were found in this directory. Place the cheat in the root folder of the game");
         return;
      }

      Logger.print("Checking for injections in game files");

      if (checkInjectedAnnotations()) {
         Logger.print("Signs of interference were found in the game files. If you have installed this cheat before, run it with the '--uninstall' flag. Otherwise, check the integrity of the game files via Steam");
         return;
      }
      Logger.print("No signs of injections were found. Preparing for backup...");
      backupGameFiles();
      Logger.print("Preparation for injection into game file...");

      patchGameWindow();
      patchItemContainer();
      patchLuaEventManager();
      patchLuaManager();
      patchStats();
      patchNutrition();
      patchBodyDamage();
      patchIsoGameCharacter();
      patchPlayerXpPacket();
      patchPlayerCheats();
      patchPlayerCheatsSave();
      patchCoreDebug();
      patchIsDebugEnabled();
      patchAntiCheatValidation();
      patchSuspiciousActivityReport();
      patchKickPlayer();
      //GameClientPatcher.applyPatches();

      Patch.saveModifiedClasses();

      Logger.print("The injections were completed!");

      Logger.print("Extracting EtherMenu files to the current directory...");

      extractEtherMenu();

      Logger.print("The cheat installation is complete, you can enter the game!");
   }

   /**
    * PZ Build 42+: Remove patched loose class files to restore original game behavior.
    * The original classes remain untouched inside projectzomboid.jar.
    */
   public void restoreFiles() {
      Logger.printCredits();
      Logger.print("Restoring files...");
      Path currentPath = Paths.get("").toAbsolutePath();

      for(int i = 0; i < patchFiles.length; ++i) {
         String fileName = patchFiles[i];
         String iteration = "[" + (i + 1) + "/" + patchFiles.length + "]";
         Logger.print("Removing patched file '" + fileName + "' " + iteration);
         Path patchedFilePath = Paths.get(currentPath.toString(), gameClassFolder, patchFiles[i]);
         if (Files.exists(patchedFilePath)) {
            try {
               Files.delete(patchedFilePath);
            } catch (IOException e) {
               Logger.print("Error when removing patched file '" + fileName + "': " + e.getMessage());
            }
         } else {
            Logger.print("Patched file '" + fileName + "' not found. Skipping.");
         }
      }

      // Clean up empty directories left behind
      try {
         Path zombieDir = currentPath.resolve(gameClassFolder);
         if (Files.exists(zombieDir)) {
            // Delete subdirectories if empty (inventory/, Lua/)
            Files.walk(zombieDir)
                 .sorted(java.util.Comparator.reverseOrder())
                 .filter(Files::isDirectory)
                 .forEach(dir -> {
                    try {
                       String[] contents = dir.toFile().list();
                       if (contents != null && contents.length == 0) {
                          Files.delete(dir);
                       }
                    } catch (IOException ignored) {}
                 });
         }
      } catch (IOException e) {
         Logger.print("Error cleaning up directories: " + e.getMessage());
      }

      Logger.print("Files restoration completed!");
      uninstallEtherMenuFiles();
   }
}