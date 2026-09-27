require "ISUI/ISPanel"

--*********************************************************
--* Глобальные установки UI
--*********************************************************
EtherPlayerEditor = ISPanel:derive("EtherPlayerEditor"); -- Наследование от ISPanel

--*********************************************************
--* Обработка prerender
--*********************************************************
function EtherPlayerEditor:prerender()
    self:setStencilRect(0,10,self:getWidth(),self:getHeight() - 20);
    ISPanel.prerender(self);

    if self.localPlayer == nil then return end
    local x, y, w, h = self.avatarPanel.x, self.avatarPanel.y, self.avatarPanel.width, self.avatarPanel.height
    self:drawRectBorder(x - 2, y - 2, w + 4, h + 4, 1, 0.3, 0.3, 0.3);
	self:drawTextureScaled(self.avatarBackgroundTexture, x, y, w, h, 1, 1, 1, 1);
end

--*********************************************************
--* Обработка событий колесика мыши
--*********************************************************
function EtherPlayerEditor:onMouseWheel(del)
	self:setYScroll(self:getYScroll() - (del * 40));
	return true;
end

--*********************************************************
--* Обработка render
--*********************************************************
function EtherPlayerEditor:render()
    ISPanel.render(self);

    if self.localPlayer == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
    end;

    self:clearStencilRect();
end

--*********************************************************
--* Добавление текстовой строки
--*********************************************************
function EtherPlayerEditor:addLabel(text, x, y, font, color)
    if font == nil then
        font = UIFont.Small;
    end

    if color == nil then
        color = {r = 1, g = 1, b = 1, a = 1}
    end

    local height = getTextManager():getFontHeight(font)

    local label = ISLabel:new(x, y, height, text, color.r, color.g, color.b, color.a, font, true)
    label:initialise()
    label:instantiate()
    self:addChild(label)
end

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function EtherPlayerEditor:createChildren()
    ISPanel.createChildren(self);

    if self.localPlayer == nil then return end;

    -- Позиции строк считаются от реальной высоты шрифта (B42: высота динамическая)
    local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small);
    local fontHeightMedium = getTextManager():getFontHeight(UIFont.Medium);
    local rowPitch = math.max(20, fontHeightSmall + 4);
    local editButtonHeight = math.max(18, fontHeightSmall + 4);

	self.avatarPanel = ISUI3DModel:new(10, 10, 64, 135)
	self.avatarPanel:setVisible(true)
	self.avatarPanel:setOutfitName("Foreman", false, false)
	self.avatarPanel:setState("idle")
	self.avatarPanel:setDirection(IsoDirections.S)
	self.avatarPanel:setIsometric(false)
	self:addChild(self.avatarPanel)

    local labelRow = 0;
    local function nextLabelY()
        local y = 10 + labelRow * rowPitch;
        labelRow = labelRow + 1;
        return y;
    end

    self:addLabel(getText("IGUI_PlayerStats_Username") .. " ".. self.localPlayer:getUsername(), 90, nextLabelY());
    self:addLabel(getText("IGUI_PlayerStats_DisplayName").. " ".. self.localPlayer:getDisplayName(), 90, nextLabelY());
    self:addLabel(getText("UI_characreation_forename").. ": " .. self.localPlayer:getDescriptor():getForename(), 90, nextLabelY());
    self:addLabel(getText("UI_characreation_surname").. ": " .. self.localPlayer:getDescriptor():getSurname(), 90, nextLabelY());
    local charProf = self.localPlayer:getDescriptor():getCharacterProfession()
    local profDef = CharacterProfessionDefinition.getCharacterProfessionDefinition(charProf)
    local profName = profDef and profDef:getUIName() or ""
    self:addLabel(getText("IGUI_PlayerStats_Profession").. " ".. profName, 90, nextLabelY());
    -- self:addLabel(getText("IGUI_char_Survived_For").. ": " .. self.localPlayer:getTimeSurvived(), 90, nextLabelY());
    local survivedY = nextLabelY();
    self:addLabel(getText("IGUI_char_Survived_For").. ": " .. self.localPlayer:getTimeSurvived(), 90, survivedY);
    local editTimeBtn = ISButton:new(250, survivedY - 2, 60, editButtonHeight, getTranslate("UI_PlayerEditor_EditStats"), self, self.onEditTimeButton)
    editTimeBtn:initialise()
    editTimeBtn:instantiate()
    self:addChild(editTimeBtn)
    -- self:addLabel(getText("IGUI_char_Zombies_Killed").. ": " .. tostring(self.localPlayer:getZombieKills()), 90, nextLabelY());
    local killsY = nextLabelY();
    self:addLabel(getText("IGUI_char_Zombies_Killed").. ": " .. tostring(self.localPlayer:getZombieKills()), 90, killsY);
    local editKillsBtn = ISButton:new(250, killsY - 2, 60, editButtonHeight, getTranslate("UI_PlayerEditor_EditStats"), self, self.onEditKillsButton)
    editKillsBtn:initialise()
    editKillsBtn:instantiate()
    self:addChild(editKillsBtn)

    local chatMuted = getText("Sandbox_ThumpNoChasing_option1");
    if not self.localPlayer:isAllChatMuted() then
        chatMuted = getText("Sandbox_ThumpNoChasing_option2")
    end

    labelRow = 0;
    self:addLabel(getText("IGUI_PlayerStats_AccessLevel") .. " ".. self.localPlayer:getRole():getName(), 300, nextLabelY());
    self:addLabel(getText("IGUI_PlayerStats_ChatMuted").. " ".. chatMuted, 300, nextLabelY());
    self:addLabel(getText("IGUI_char_Weight").. ": ".. tostring(math.floor(self.localPlayer:getNutrition():getWeight())), 300, nextLabelY());
    self:addLabel(getTranslate("UI_PlayerEditor_PlayerInfo_Calories").. ": ".. tostring(math.floor(self.localPlayer:getNutrition():getCalories())), 300, nextLabelY());

    -- Раздел «Черты» располагается под моделью персонажа
    local traitsTitleY = self.avatarPanel.y + self.avatarPanel.height + 5;
    self:addLabel(getTranslate("UI_PlayerEditor_PlayerTraits_Title"), 10, traitsTitleY, UIFont.Medium )

    self.traitsPanel = UITraitsTable:new(10, traitsTitleY + fontHeightMedium + 5, self.width - 10 * 2, 180);
    self.traitsPanel:initialise();
    self.traitsPanel.parent = self;
    self:addChild(self.traitsPanel);

    -- Раздел «Навыки» располагается под таблицей черт (а не поверх неё)
    local skillsTitleY = self.traitsPanel.y + self.traitsPanel.height + 5;
    self:addLabel(getTranslate("UI_PlayerEditor_PlayerSkills_Title"), 10, skillsTitleY, UIFont.Medium )

    self.skillPanel = UISkillTable:new(10, skillsTitleY + fontHeightMedium + 5, self.width - 10 * 2, 180);
    self.skillPanel:initialise();
    self.skillPanel.parent = self;
    self:addChild(self.skillPanel);
end

function EtherPlayerEditor:updateLabels()
    -- Remove all existing labels and buttons
    self:removeChild(self.avatarPanel)
    for _,child in pairs(self:getChildren()) do
        self:removeChild(child)
    end
    -- Recreate all elements
    self:createChildren()
end

function EtherPlayerEditor:onEditTimeButton()
    local modal = ISTextBox:new(0, 0, 280, 180, getTranslate("UI_PlayerEditor_EditHoursTitle"),
        tostring(getHoursAlive()),
        self,
        function(target, button)
            if button.internal == "OK" then
                local value = tonumber(button.parent.entry:getText())
                if value then
                    setHoursAlive(value)
                    self:updateLabels()
                end
            end
        end)
    modal:initialise()
    modal:addToUIManager()
end

function EtherPlayerEditor:onEditKillsButton()
    local modal = ISTextBox:new(0, 0, 280, 180, getTranslate("UI_PlayerEditor_EditKillsTitle"),
        tostring(getZombieKills()),
        self,
        function(target, button)
            if button.internal == "OK" then
                local value = tonumber(button.parent.entry:getText())
                if value then
                    setZombieKills(value)
                    self:updateLabels()
                end
            end
        end)
    modal:initialise()
    modal:addToUIManager()
end

--*********************************************************
--* Обновление панели
--*********************************************************
function EtherPlayerEditor:update()
    ISPanel.update(self);

    if self.localPlayer == nil then return end

    self.avatarPanel:setCharacter(self.localPlayer)
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherPlayerEditor:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.avatarBackgroundTexture = getTexture("media/ui/avatarBackground.png")
    menuTableData.workInGameText = getTranslate("UI_PlayerEditor_PanelWorkOnlyInGame");
    menuTableData.localPlayer = getPlayer();
    EtherPlayerEditor.instance = self;
    self.__index = self;

    return menuTableData;
end
