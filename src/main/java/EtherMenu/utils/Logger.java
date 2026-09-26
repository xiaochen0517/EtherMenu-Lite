package EtherMenu.utils;

import zombie.debug.DebugLog;
import zombie.debug.DebugType;

public class Logger {
   public static void print(String var0) {
      System.out.println("[EtherMenu]: " + var0);
   }

   public static void printLog(String var0) {
      // Defensive: this method sits on the GameWindow.init injection path
      // (GameWindow.init -> EtherLuaCompiler.init -> here), so ANY drift in the
      // game logging API must never crash game startup. LinkageErrors such as
      // NoSuchFieldError / NoSuchMethodError / NoClassDefFoundError are NOT
      // caught by catch(Exception), hence catch(Throwable) with a System.out
      // fallback.
      try {
         DebugLog.log(DebugType.General, "[EtherMenu]: " + var0);
      } catch (Throwable t) {
         System.out.println("[EtherMenu]: " + var0);
      }
   }

   public static void printCredits() {
      System.out.println();
      System.out.println();
      System.out.println(" _____ _   _               __  __                  ");
      System.out.println("| ____| |_| |__   ___ _ __|  \\/  | ___ _ __  _   _ ");
      System.out.println("|  _| | __| '_ \\ / _ \\ '__| |\\/| |/ _ \\ '_ \\| | | |");
      System.out.println("| |___| |_| | | |  __/ |  | |  | |  __/ | | | |_| |");
      System.out.println("|_____|\\__|_| |_|\\___|_|  |_|  |_|\\___|_| |_|\\__,_|");
      System.out.println();
      System.out.println("                    by DRKM43");
      System.out.println();
   }
}
