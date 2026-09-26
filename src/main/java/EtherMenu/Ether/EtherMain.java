package EtherMenu.Ether;

import EtherMenu.utils.Logger;

public class EtherMain {
   private static EtherMain instance;
   public EtherTranslator etherTranslator;
   public EtherLuaManager etherLuaManager;
   public EtherAPI etherAPI;

   private EtherMain() {
   }

   public void init() {
      Logger.printLog("Initializing EtherMenu...");
      LicenseBridge.init();
      this.etherTranslator = new EtherTranslator();
      this.etherTranslator.loadTranslations();
      this.etherAPI = new EtherAPI();
      this.etherAPI.loadAPI();
      this.etherLuaManager = new EtherLuaManager();
      this.etherLuaManager.loadLua();
      Logger.printLog("Initialization EtherMenu was completed!");
      Logger.printLog("Edition: " + (LicenseBridge.isLicensed() ? "LICENSED" : "COMMUNITY"));
   }

   public static EtherMain getInstance() {
      if (instance == null) {
         instance = new EtherMain();
      }

      return instance;
   }
}
