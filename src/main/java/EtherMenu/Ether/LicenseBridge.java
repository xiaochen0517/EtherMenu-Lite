package EtherMenu.Ether;

import java.lang.reflect.Method;

// Reflectively bridges to the closed-source EtherMenu.Ether.LicenseManager, which
// only exists in the full-version build. In this Lite source tree the class is
// absent entirely, so all lookups must go through reflection to keep compilation
// independent of it; when the class can't be loaded every call is a no-op.
public final class LicenseBridge {
   private static final Class<?> CLASS;
   private static final Object INSTANCE;

   static {
      Class<?> clazz = null;
      Object instance = null;
      try {
         clazz = Class.forName("EtherMenu.Ether.LicenseManager");
         instance = clazz.getMethod("getInstance").invoke(null);
      } catch (Throwable ignored) {
      }
      CLASS = clazz;
      INSTANCE = instance;
   }

   private LicenseBridge() {
   }

   public static boolean isAvailable() {
      return INSTANCE != null;
   }

   public static void init() {
      invoke("init", new Class<?>[0]);
   }

   public static boolean isLicensed() {
      return Boolean.TRUE.equals(invoke("isLicensed", new Class<?>[0]));
   }

   public static boolean activate(String key) {
      return Boolean.TRUE.equals(invoke("activate", new Class<?>[]{String.class}, key));
   }

   public static String getLicenseKey() {
      Object result = invoke("getLicenseKey", new Class<?>[0]);
      return result instanceof String ? (String) result : "";
   }

   public static String getLicenseType() {
      Object result = invoke("getLicenseType", new Class<?>[0]);
      return result instanceof String ? (String) result : "";
   }

   public static String getExpiresDate() {
      Object result = invoke("getExpiresDate", new Class<?>[0]);
      return result instanceof String ? (String) result : "";
   }

   private static Object invoke(String methodName, Class<?>[] paramTypes, Object... args) {
      if (INSTANCE == null) {
         return null;
      }

      try {
         Method method = CLASS.getMethod(methodName, paramTypes);
         return method.invoke(INSTANCE, args);
      } catch (Throwable ignored) {
         return null;
      }
   }
}
