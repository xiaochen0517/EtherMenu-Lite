package EtherMenu.utils;

import java.util.Properties;
import zombie.core.Color;

public class ConfigUtils {
   public static boolean getBooleanFromConfig(Properties var0, String var1, boolean var2) {
      String var3 = var0.getProperty(var1);
      return var3 != null ? Boolean.parseBoolean(var3) : var2;
   }

   public static Color getColorFromConfig(Properties var0, String var1, Color var2) {
      String var3 = var0.getProperty(var1);
      return var3 != null ? ColorUtils.stringToColor(var3) : var2;
   }

   public static int getIntFromConfig(Properties var0, String var1, int var2) {
      String var3 = var0.getProperty(var1);
      if (var3 != null) {
         try {
            return Integer.parseInt(var3);
         } catch (NumberFormatException ignored) {
         }
      }
      return var2;
   }
}
