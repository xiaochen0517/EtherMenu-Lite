require "ISUI/ISPanel"

--*********************************************************
--* Подключение модулей
--*********************************************************
local etherModules = {
    "EtherMenu/lua/components/override/EtherAdminMenu.lua",
    "EtherMenu/lua/components/override/EtherDebugMenu.lua",
    "EtherMenu/lua/components/override/EtherEditInventoryItem.lua",
    "EtherMenu/lua/components/override/EtherEditWorldObjects.lua",
    "EtherMenu/lua/components/ui/UIButtonsPanel.lua",
    "EtherMenu/lua/components/ui/UICheckbox.lua",
    "EtherMenu/lua/components/ui/UIButton.lua",
    "EtherMenu/lua/components/ui/UISlider.lua",
    "EtherMenu/lua/components/ui/UIMechanics.lua",
    "EtherMenu/lua/components/ui/UIModalAddXP.lua",
    "EtherMenu/lua/components/ui/UIMovableMiniMap.lua",
    "EtherMenu/lua/components/ui/UIModalAddTrait.lua",
    "EtherMenu/lua/components/ui/UIHealth.lua",
    "EtherMenu/lua/components/ui/UIItemTables.lua",
    "EtherMenu/lua/components/ui/UIMap.lua",
    "EtherMenu/lua/components/ui/UISkillTable.lua",
    "EtherMenu/lua/components/ui/UITraitsTable.lua",
    "EtherMenu/lua/components/panels/EtherInfoPanel.lua",
    "EtherMenu/lua/components/panels/EtherCharacterPanel.lua",
    "EtherMenu/lua/components/panels/EtherPlayerEditor.lua",
    "EtherMenu/lua/components/panels/EtherVisualsPanel.lua",
    "EtherMenu/lua/components/panels/EtherMapPanel.lua",
    "EtherMenu/lua/components/panels/EtherSettingsPanel.lua"
}

for _, module in ipairs(etherModules) do
    requireExtra(module);
end

--*********************************************************
--* Глобальные установки UI
--*********************************************************
EtherMain                   = ISPanel:derive("EtherMain"); -- Наследование от ISPanel
EtherMain.instance          = nil; --Экземпляр окна
EtherMain.menuKeyID         = getMenuKeyID(); -- Клавиша открытия окна - F1 (59)
EtherMain.defaultWidth      = 765; -- Стандартная ширина окна (+50% для высоких разрешений)
EtherMain.defaultHeight     = 750; -- Стандартная высота окна (+50% для высоких разрешений)
EtherMain.currentTabID      = 1; -- Последняя открытая вкладка
EtherMain.accentColor       = {r = getAccentUIColor():getR(), g = getAccentUIColor():getG(), b = getAccentUIColor():getB(), a = 1.0}; -- Акцентный цвет

--*********************************************************
--* Закрытие окна по нажатию кнопки UI
--*********************************************************
function EtherMain:close()
	EtherMain.instance:setVisible(false);
    EtherMain.instance:removeFromUIManager();
    EtherMain.instance = nil;
end

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function EtherMain:createChildren()
    ISPanel.createChildren(self);

    self.buttonsPanel = UIButtonsPanel:new(0, 0, 50, self.height, self, EtherMain.accentColor);
    self.buttonsPanel:initialise();
    self.buttonsPanel:instantiate();
    self.buttonsPanel:setVisible(true);
    self:addChild(self.buttonsPanel);

    self.buttonsPanel:addButton("EtherMenu/media/ui/info.png", EtherInfoPanel);
    self.buttonsPanel:addButton("EtherMenu/media/ui/character.png", EtherCharacterPanel);
    self.buttonsPanel:addButton("EtherMenu/media/ui/playerEditor.png", EtherPlayerEditor);
    self.buttonsPanel:addButton("EtherMenu/media/ui/visuals.png", EtherVisualsPanel);
    self.buttonsPanel:addButton("EtherMenu/media/ui/teleport.png", EtherMapPanel);
    self.buttonsPanel:addButton("EtherMenu/media/ui/settings.png", EtherSettingsPanel);

    self.buttonsPanel:openPanel(EtherMain.currentTabID);
end

--*********************************************************
--* Логика открытия и закрытия меню по нажатию клавиши
--*********************************************************
function EtherMain.OnOpenPanel(key)
    if key == EtherMain.menuKeyID then
        -- Если панель уже существует, закрываем окно
        if EtherMain.instance ~= nil then
            EtherMain.instance:setVisible(false);
            EtherMain.instance:removeFromUIManager();
            EtherMain.instance = nil;
            return
        end

        -- Создаем новую панель
        EtherMain.instance  = EtherMain:new();
        EtherMain.instance:initialise();
        EtherMain.instance:instantiate();
        EtherMain.instance:addToUIManager();
        EtherMain.instance:setVisible(true);
        EtherMain.instance:setAlwaysOnTop(false);
    end
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherMain:new()
    local menuTableData = {};

    local positionX = getCore():getScreenWidth() / 2 - EtherMain.defaultWidth / 2;
    local positionY = getCore():getScreenHeight() / 2 - EtherMain.defaultHeight / 2;

    menuTableData = ISPanel:new(positionX, positionY, EtherMain.defaultWidth, EtherMain.defaultHeight);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.05, g=0.05, b=0.05, a=1};
	menuTableData.borderColor = {r=0, g=0, b=0, a=0};
	menuTableData.moveWithMouse = true;
    self.__index = self;

    return menuTableData;
end

Events.OnKeyPressed.Add(EtherMain.OnOpenPanel);
