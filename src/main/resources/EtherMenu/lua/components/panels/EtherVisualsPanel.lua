require "ISUI/ISPanel"

--*********************************************************
--* Глобальные установки UI
--*********************************************************
EtherVisualsPanel = ISPanel:derive("EtherVisualsPanel"); -- Наследование от ISPanel

--*********************************************************
--* Обработка prerender
--*********************************************************
function EtherVisualsPanel:prerender()
    self:setStencilRect(0,10,self:getWidth(),self:getHeight() - 20);
    ISPanel.prerender(self);
end

--*********************************************************
--* Обработка render
--*********************************************************
function EtherVisualsPanel:render()
    ISPanel.render(self);
    self:clearStencilRect();
end

--*********************************************************
--* Обработка событий колесика мыши
--*********************************************************
function EtherVisualsPanel:onMouseWheel(del)
	self:setYScroll(self:getYScroll() - (del * 40));
	return true;
end

--*********************************************************
--* Добавление чекбоксов
--*********************************************************
function EtherVisualsPanel:addCheckBox(title, method, isSelected)
    local yOffset = 5;
    if #self.uiElements == 0 then
        yOffset = 0;
    end

    local checkbox = UICheckbox:new(10, self.yRowPosition + yOffset, title, isSelected, method);
    checkbox:initialise();
    checkbox:instantiate();
    checkbox:setAnchorLeft(true);
    checkbox:setAnchorRight(false);
    checkbox:setAnchorTop(false);
    checkbox:setAnchorBottom(true);
    self:addChild(checkbox);

    self:setScrollHeight(self:getScrollHeight() + checkbox.height + 5);

    self.yRowPosition = self.yRowPosition + checkbox.height + yOffset;

    table.insert(self.uiElements, checkbox);
end

--*********************************************************
--* Создание метки
--*********************************************************
function EtherVisualsPanel:addLabel(posX, posY, title)
    local label = ISLabel:new(posX, posY + 3, getTextManager():getFontHeight(UIFont.Small), title, 1, 1, 1, 1, UIFont.Small, true)
	self:addChild(label)
    return label
end

--*********************************************************
--* Создание слайдера
--*********************************************************
function EtherVisualsPanel:addSlider(posX, posY, width, height, value, minValue, maxValue, method)
    local slider = UISlider:new(posX, posY, width, height, value, minValue, maxValue, method)
    slider:initialise();
    slider:instantiate();
    self:addChild(slider);
    return slider
end

--*********************************************************
--* Создание слайдера
--*********************************************************
function EtherVisualsPanel:addSliderWithLabel(title, value, minValue, maxValue, method)
    local yOffset = 10;
    if #self.uiElements == 0 then
        yOffset = 10;
    end

    local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small);
    local sliderHeight = 10;
    local sliderWidth = 100;

    local labelY = self.yRowPosition + yOffset;
    self:addLabel(10, labelY, title);
    -- Слайдер располагается под подписью, а под ним ещё значение текущего параметра
    local slider = self:addSlider(self.width - sliderWidth - 50, labelY + fontHeightSmall + 4, sliderWidth, sliderHeight, value, minValue, maxValue, method)
    
    local rowHeight = fontHeightSmall + 4 + sliderHeight + 2 + fontHeightSmall + 5;
    self:setScrollHeight(self:getScrollHeight() + rowHeight);
    
    self.yRowPosition = labelY + rowHeight;

    table.insert(self.uiElements, slider);

    return slider;
end

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function EtherVisualsPanel:createChildren()
    ISPanel.createChildren(self);

    self:setScrollChildren(true);
    self:setScrollHeight(0);
    self:addScrollBars();

    self:addCheckBox(getTranslate("UI_VisualsPanel_IsVisualsEnable"), function(isChecked)
        toggleVisualsEnable(isChecked);
    end, isVisualsEnable());

    self:addCheckBox(getTranslate("UI_VisualsPanel_360Vision"), function(isChecked)
        toggleVisualEnable360Vision(isChecked);
    end, isVisualEnable360Vision());



    self:addCheckBox(getTranslate("UI_VisualsPanel_IsVisualsVehiclesEnable"), function(isChecked)
        toggleVisualsVehiclesEnable(isChecked);
    end, isVisualsVehiclesEnable());

    self:addCheckBox(getTranslate("UI_VisualsPanel_DrawLineToVehicles"), function(isChecked)
        toggleVisualDrawLineToVehicle(isChecked);
    end, isVisualDrawLineToVehicle());



    self:addCheckBox(getTranslate("UI_VisualsPanel_IsVisualsZombiesEnable"), function(isChecked)
        toggleVisualsZombiesEnable(isChecked);
    end, isVisualsZombiesEnable());



    self:addCheckBox(getTranslate("UI_VisualsPanel_IsVisualsPlayersEnable"), function(isChecked)
        toggleVisualsPlayersEnable(isChecked);
    end, isVisualsPlayersEnable());

    self:addCheckBox(getTranslate("UI_VisualsPanel_DrawToLocalPlayer"), function(isChecked)
        toggleVisualDrawToLocalPlayer(isChecked);
    end, isVisualDrawToLocalPlayer());

    self:addCheckBox(getTranslate("UI_VisualsPanel_DrawLineToPlayers"), function(isChecked)
         toggleVisualDrawLineToPlayers(isChecked);
    end, isVisualDrawLineToPlayers());

    self:addCheckBox(getTranslate("UI_VisualsPanel_DrawPlayerName"), function(isChecked)
        toggleVisualDrawPlayerNickname(isChecked) ;
    end, isVisualDrawPlayerNickname());

    self:addCheckBox(getTranslate("UI_VisualsPanel_DrawPlayerInfo"), function(isChecked)
        toggleVisualDrawPlayerInfo(isChecked) ;
    end, isVisualDrawPlayerInfo());
end
--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherVisualsPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.yRowPosition = 10;
    self.__index = self;

    self.uiElements = {}; -- Список всех элементов

    return menuTableData;
end