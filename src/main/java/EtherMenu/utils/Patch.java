package EtherMenu.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class Patch {
   private static final Map<String, ClassNode> classNodeMap = new HashMap<>();
   private static final String GAME_JAR = "projectzomboid.jar";

   private static byte[] readClassFromGameJar(String className) throws IOException {
      try (JarFile jar = new JarFile(GAME_JAR)) {
         ZipEntry entry = jar.getEntry(className + ".class");
         if (entry == null) {
            throw new IOException("Class not found in " + GAME_JAR + ": " + className);
         }
         try (InputStream is = jar.getInputStream(entry)) {
            return is.readAllBytes();
         }
      }
   }

   public static void injectIntoClass(String className, String methodName, boolean isStatic, Consumer<MethodNode> injector) {
      Logger.print("Injection into a game file '" + className + "' in method: '" + methodName + "'");

      ClassNode classNode = classNodeMap.computeIfAbsent(className, key -> {
         ClassNode node = new ClassNode();
         try {
            byte[] classBytes = readClassFromGameJar(key);
            ClassReader reader = new ClassReader(classBytes);
            reader.accept(node, 0);
            return node;
         } catch (IOException e) {
            Logger.print("Failed to read class: " + e.getMessage());
            return null;
         }
      });

      if (classNode == null) {
         throw new RuntimeException("Failed to load class " + className);
      }

      for (MethodNode methodNode : classNode.methods) {
         if (methodNode.name.equals(methodName) && Modifier.isStatic(methodNode.access) == isStatic) {
            if (!hasInjectedAnnotation(methodNode)) {
               addInjectAnnotation(classNode, methodName);
            }
            injector.accept(methodNode);
         }
      }

      classNodeMap.put(className, classNode);
   }

   public static boolean isInjectedAnnotationPresent(String file, String baseDir) {
      Path filePath = Paths.get(baseDir, file);

      if (!Files.exists(filePath)) {
         return false;
      }

      try (FileInputStream fis = new FileInputStream(filePath.toString())) {
         ClassReader reader = new ClassReader(fis);
         boolean[] found = new boolean[]{false};

         reader.accept(new ClassVisitor(589824) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
               MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
               return new MethodVisitor(589824, mv) {
                  @Override
                  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                     if (descriptor.equals("LEtherMenu/annotations/Injected;")) {
                        found[0] = true;
                     }
                     return super.visitAnnotation(descriptor, visible);
                  }
               };
            }
         }, 0);

         return found[0];
      } catch (IOException e) {
         Logger.print("Error checking for injected annotations: " + e.getMessage());
         return false;
      }
   }

   private static void addInjectAnnotation(ClassNode classNode, String methodName) {
      for (MethodNode method : classNode.methods) {
         if (method.name.equals(methodName)) {
            if (method.visibleAnnotations == null) {
               method.visibleAnnotations = new LinkedList<>();
            }

            // Check if annotation already exists
            boolean hasAnnotation = method.visibleAnnotations.stream()
                    .anyMatch(anno -> anno.desc.equals("LEtherMenu/annotations/Injected;"));

            if (!hasAnnotation) {
               method.visibleAnnotations.add(new AnnotationNode("LEtherMenu/annotations/Injected;"));
            }

            return;
         }
      }
   }

   private static boolean hasInjectedAnnotation(MethodNode method) {
      if (method.visibleAnnotations == null) {
         return false;
      }
      return method.visibleAnnotations.stream()
              .anyMatch(anno -> anno.desc.equals("LEtherMenu/annotations/Injected;"));
   }

   public static void saveModifiedClasses() {
      URLClassLoader gameClassLoader;
      try {
         gameClassLoader = new URLClassLoader(
                 new URL[]{Paths.get(GAME_JAR).toUri().toURL()},
                 ClassLoader.getPlatformClassLoader()
         );
      } catch (Exception e) {
         Logger.print("Failed to create game classloader: " + e.getMessage());
         return;
      }

      for (Map.Entry<String, ClassNode> entry : classNodeMap.entrySet()) {
         String className = entry.getKey();
         ClassNode classNode = entry.getValue();

         try {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
               @Override
               protected String getCommonSuperClass(String type1, String type2) {
                  try {
                     Class<?> c1 = Class.forName(type1.replace('/', '.'), false, gameClassLoader);
                     Class<?> c2 = Class.forName(type2.replace('/', '.'), false, gameClassLoader);
                     if (c1.isAssignableFrom(c2)) return type1;
                     if (c2.isAssignableFrom(c1)) return type2;
                     if (c1.isInterface() || c2.isInterface()) return "java/lang/Object";
                     do { c1 = c1.getSuperclass(); } while (!c1.isAssignableFrom(c2));
                     return c1.getName().replace('.', '/');
                  } catch (ClassNotFoundException e) {
                     return "java/lang/Object";
                  }
               }
            };
            classNode.accept(writer);
            byte[] bytes = writer.toByteArray();

            Path outputPath = Paths.get(className + ".class");
            Files.createDirectories(outputPath.getParent());

            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
               fos.write(bytes);
            }
         } catch (IOException e) {
            Logger.print("Error saving modified class '" + className + "': " + e.getMessage());
         }
      }

      try {
         gameClassLoader.close();
      } catch (IOException ignored) {}
   }
}