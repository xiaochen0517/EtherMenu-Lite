package EtherMenu.utils;

import java.io.IOException;
import java.util.Properties;

public class Info {
   private static final String CHEAT_VERSION;
   public static final String CHEAT_GUI_TITLE;
   public static final String CHEAT_WINDOW_TITLE_SUFFIX;
   public static final String CHEAT_NAME = "EtherMenu";
   public static final String CHEAT_AUTHOR = "DRKM43";
   public static final String CHEAT_TAG = "[EtherMenu]: ";

   static {
      Properties var0 = new Properties();

      try {
         var0.load(Info.class.getClassLoader().getResourceAsStream("EtherMenu/EtherMenu.properties"));
         CHEAT_VERSION = var0.getProperty("version").replace("'", "");
      } catch (IOException var1) {
         throw new ExceptionInInitializerError("Unable to load version from EtherMenu.properties");
      }

      CHEAT_GUI_TITLE = "EtherMenu (" + CHEAT_VERSION + ")";
      CHEAT_WINDOW_TITLE_SUFFIX = " by EtherMenu (" + CHEAT_VERSION + ")";
   }
}
