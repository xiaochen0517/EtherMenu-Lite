--***********************************************************
--** EtherMenu: Override B42 build system for cheat mode
--**
--** ROOT CAUSE: In B42 MP, the build system is server-authoritative.
--** ISBuildAction:start() -> createBuildAction() sends to server.
--** Server runs create() but lacks our cheat flags, so it fails.
--** Client perform() returns early without calling create().
--**
--** Additionally, setInfo() always calls transmitCompleteItemToClients()
--** which is a server-to-client method. From a client we need
--** transmitCompleteItemToServer() instead (like ISMoveableSpriteProps does).
--**
--** FIX: Three overrides applied via OnGameStart event:
--** 1. ISBuildPanel - force blockBuild=false for cursor display
--** 2. ISBuildIsoEntity:create - client-side placement with correct transmit
--** 3. ISBuildAction:perform - trigger client-side create() after action
--***********************************************************

local _buildOverridesApplied = false;

local function applyBuildOverrides()
    if _buildOverridesApplied then return end
    if not ISBuildPanel or not ISBuildIsoEntity or not ISBuildAction then return end
    _buildOverridesApplied = true;

    -------------------------------------------------------
    -- 1. ISBuildPanel: force blockBuild=false when cheat
    -------------------------------------------------------
    local _origCreateBuildIsoEntity = ISBuildPanel.createBuildIsoEntity;
    function ISBuildPanel:createBuildIsoEntity(dontSetDrag)
        _origCreateBuildIsoEntity(self, dontSetDrag);
        if self.buildEntity and ISBuildMenu and ISBuildMenu.cheat then
            self.buildEntity.blockBuild = false;
        end
    end

    -------------------------------------------------------
    -- 2. ISBuildIsoEntity:create - client cheat placement
    --    Uses transmitCompleteItemToServer() for proper sync
    -------------------------------------------------------
    local _origCreate = ISBuildIsoEntity.create;
    function ISBuildIsoEntity:create(x, y, z, north, sprite)
        -- Non-cheat builds or server-side: use original
        if not (ISBuildMenu and ISBuildMenu.cheat) then
            return _origCreate(self, x, y, z, north, sprite);
        end

        -- Server-side cheat builds: use original (server uses transmitToClients)
        if isServer() and not isClient() then
            return _origCreate(self, x, y, z, north, sprite);
        end

        -- CLIENT-SIDE cheat build: full create with correct network transmit
        local playerObj = getSpecificPlayer(self.player);
        if not playerObj then return end

        local cell = getWorld():getCell();
        self.sq = cell:getGridSquare(x, y, z);
        if not self.sq then return end

        -- Ensure face is computed
        local face = self:getFace();
        if not face then
            pcall(function() self:getSprite() end);
            face = self:getFace();
            if not face then return end
        end
        local openFace = self:getOpenFace(north);

        if openFace and (openFace:getWidth() ~= face:getWidth() or openFace:getHeight() ~= face:getHeight()) then
            openFace = nil;
        end

        pcall(function() self:updateModData() end);

        -- Place each tile of the object
        for zz = 0, face:getzLayers() - 1 do
            for xx = 0, face:getWidth() - 1 do
                for yy = 0, face:getHeight() - 1 do
                    local tileInfo = face:getTileInfo(xx, yy, zz);
                    local openTileInfo = openFace and openFace:getTileInfo(xx, yy, zz);
                    local sq = cell:getGridSquare(x + xx, y + yy, z + zz);
                    if tileInfo and tileInfo:getSpriteName() and sq then
                        local tileSprite = tileInfo:getSpriteName();
                        local openSprite = openTileInfo and openTileInfo:getSpriteName() or false;

                        -- Inline setInfo logic with correct client transmit
                        local ok, err = pcall(function()
                            -- Handle prop type
                            if self.objectInfo and self.objectInfo:getScript() and self.objectInfo:getScript():isProp() then
                                local props = ISMoveableSpriteProps.new(IsoObject.new(sq, tileSprite):getSprite());
                                props.rawWeight = 10;
                                props:placeMoveableInternal(sq, instanceItem(ItemKey.Weapon.PLANK), tileSprite);
                                return;
                            end

                            -- Create IsoThumpable
                            local thumpable;
                            if openSprite then
                                thumpable = IsoThumpable.new(getCell(), sq, tileSprite, openSprite, north, self);
                            else
                                thumpable = IsoThumpable.new(getCell(), sq, tileSprite, north, self);
                            end

                            -- Set properties
                            buildUtil.setInfo(thumpable, self);

                            -- Set health
                            local craftRecipe = self.objectInfo:getRecipe():getCraftRecipe();
                            local baseHealth = math.max(self.objectInfo:getScript():getHealth(), 0);
                            local bonusHealth = self.objectInfo:getScript():getBonusHealth();
                            local skillBonus = craftRecipe:getHighestRelevantSkillLevel(playerObj) * self.objectInfo:getScript():getSkillBaseHealth();
                            local bonusHealthMultiplier = getSandboxOptions():getOptionByName("ConstructionBonusPoints"):getValue();
                            if bonusHealthMultiplier == 1 then bonusHealth = bonusHealth * 0.5 end
                            if bonusHealthMultiplier == 2 then bonusHealth = bonusHealth * 0.7 end
                            if bonusHealthMultiplier == 4 then bonusHealth = bonusHealth * 1.3 end
                            if bonusHealthMultiplier == 5 then bonusHealth = bonusHealth * 1.5 end
                            local totalHealth = baseHealth + bonusHealth + skillBonus;
                            thumpable:setMaxHealth(totalHealth);
                            thumpable:setHealth(totalHealth);

                            -- Break sound
                            pcall(function() thumpable:setBreakSound(self.objectInfo:getScript():getBreakSound()) end);

                            -- Game entity
                            if self.objectInfo:getScript():getParent() then
                                local gameEntityScript = self.objectInfo:getScript():getParent();
                                GameEntityFactory.CreateIsoObjectEntity(thumpable, gameEntityScript, true);
                            end

                            -- Add to square
                            sq:AddSpecialObject(thumpable);
                            buildUtil.checkCorner(sq:getX(), sq:getY(), sq:getZ(), north, thumpable, self);
                            thumpable:setExplored(true);

                            -- OnCreate callback
                            local result = nil;
                            if self.objectInfo:getScript():getOnCreate() then
                                local facing = self:getFace():getFaceName();
                                local func = self.objectInfo:getScript():getOnCreate();
                                local recipeData = nil;
                                pcall(function() recipeData = self.buildPanelLogic:getRecipeData() end);
                                result = BaseCraftingLogic.callLuaObject(func, {thumpable = thumpable, craftRecipeData = recipeData, character = playerObj, facing = facing});
                            end

                            sq:RecalcAllWithNeighbours(true);

                            -- CRITICAL: Use transmitCompleteItemToServer on client
                            if result and result.objectAlreadyTransmitted then
                                return;
                            end
                            if result and result.replaceObject and result.object then
                                if isClient() then
                                    result.object:transmitCompleteItemToServer();
                                else
                                    result.object:transmitCompleteItemToClients();
                                end
                                return;
                            end

                            if isClient() then
                                thumpable:transmitCompleteItemToServer();
                            else
                                thumpable:transmitCompleteItemToClients();
                            end
                        end);

                        if not ok then
                            print("EtherMenu: setInfo error for tile ("..xx..","..yy..","..zz.."): "..tostring(err));
                        end
                    end
                end
            end
        end

        pcall(function() self.sq:RecalcAllWithNeighbours(true) end);
    end

    -------------------------------------------------------
    -- 3. ISBuildAction:perform - trigger client-side build
    -------------------------------------------------------
    local _origPerform = ISBuildAction.perform;
    function ISBuildAction:perform()
        _origPerform(self);

        -- After the original perform (which returns early on client),
        -- call our patched create() that uses transmitCompleteItemToServer
        if isClient() and ISBuildMenu and ISBuildMenu.cheat then
            self.item.character = self.character;
            self.item.blockBuild = false;
            local ok, err = pcall(function()
                self.item:create(self.x, self.y, self.z, self.north, self.spriteName);
            end)
            if not ok then
                print("EtherMenu: perform->create error: "..tostring(err));
            end
            pcall(function()
                if self.square then
                    self.square:RecalcAllWithNeighbours(true);
                    buildUtil.setHaveConstruction(self.square, true);
                end
            end)
        end
    end
end

Events.OnGameStart.Add(applyBuildOverrides);
