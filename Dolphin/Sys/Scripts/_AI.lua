local core = require "MKW_core"
local Pointers = require "MKW_Pointers"

--package.path = package.path..';./libs/lua/?.lua'
--package.cpath = package.cpath..';./libs/socket/?.dll;./libs/mime/?.dll'
--local socket = require 'socket'


local count = -1;
local lastCp = 0;
local lap = 1;
local inputFile = "";
local inputCounterFile = "";
local outputCounterFile = "";
local outputFile = "";
local id = "?";

local clock = os.clock
function sleep(n)  -- seconds
    local t0 = clock()
    while clock() - t0 <= n do end
end

function abs(n)
    if n > 0 then return n else return -n end
end

function onScriptStart()
    id = GetClientID();
    MsgBox(string.format("Launching Lua Script. Client ID = %d",id));
    inputFile = string.format("AI/AI_%d_Input.txt", id);
	
	SocketInit();
	SocketWriteInt(id);
end

function onScriptCancel()
    
end

function stickPos(x)
    if (x == nil) then return 128; end
    
    if x == 0 then return 59;
    elseif x == 1 then return 68;
    elseif x == 2 then return 77;
    elseif x == 3 then return 86;
    elseif x == 4 then return 95;
    elseif x == 5 then return 104;
    elseif x == 6 then return 112;
    elseif x == 7 then return 128;
    elseif x == 8 then return 152;
    elseif x == 9 then return 161;
    elseif x == 10 then return 170;
    elseif x == 11 then return 179;
    elseif x == 12 then return 188;
    elseif x == 13 then return 197;
    elseif x == 14 then return 205;
    else return 128;
    end
end

function onStateLoaded()
    -- Needs to exist to prevent errors
    lastCp = 0;
    lap = 1;
end

function onStateSaved()
    
end

function writeOutput()
	-- Do some math
    local pos = core.getPos();
    local rot = core.calculateEuler();
	
	-- To do: Function
	local a = GetPointerNormal(Pointers.getRaceData2Pointer());
    local checkpoint = 0;
    if (ReadValue32(a) >= 0x80000000) then
        checkpoint = ReadValue16(a + 0xA);
    end
    
    
	-- Calc CP
	if (lastCp == 0 and lap == 1 and checkpoint > 10) then -- Beginning of race
        checkpoint = 0;
    end
    if (lastCp > 10 and checkpoint == 0) then -- Next Lap
        lap = lap + 1;
    end
    if (lastCp < checkpoint and lastCp < 10 and checkpoint > 10 and lap > 1) then -- Backwards
        lap = lap - 1;
    end
	lastCp = checkpoint;

   
    
	-- Write Output
	SocketWriteDouble(pos.X);
	SocketWriteDouble(pos.Y);
	SocketWriteDouble(pos.Z);
	SocketWriteDouble(rot.X);
	SocketWriteDouble(rot.Y);
	SocketWriteDouble(rot.Z);
	
	SocketWriteInt(checkpoint);
	SocketWriteInt(ReadValue32(0x809BF0D0));
	SocketWriteInt(core.getFrameOfInput());
	SocketWriteInt(lap);
	
	local buttons = core.getInput()
    
    local aButton = buttons.ABLR % 2
    local bButton = (buttons.ABLR >> 1) % 2
	SocketWriteInt(aButton);
	SocketWriteInt(bButton);
	SocketWriteInt(buttons.DPAD);
	SocketWriteInt(stickPos(buttons.X));
	SocketWriteInt(stickPos(buttons.Y));
	
	-- Bitfields
	local playerHolder = GetPointerNormal(0x809BD110)
	local players = GetPointerNormal(playerHolder + 0x20)
	local player0 = GetPointerNormal(players + 0)
	local playerSub1c = GetPointerNormal(player0 + 0x20)
	SocketWriteInt(ReadValue32(playerSub1c + 0x4)); -- Bitfield 1
	SocketWriteInt(ReadValue32(playerSub1c + 0x8)); -- Bitfield 2
end

local a = 0;
local b = 0;
local dUp = 0;
local start = 0;
local sx = -1;
local sy = -1;
local skip = 0;
local loadState = "/";

function processInput()
	if (a ~= 0) then if (a == 1) then PressButton("A"); else ReleaseButton("A"); end end
	if (b ~= 0) then if (b == 1) then PressButton("B"); else ReleaseButton("B"); end end
	if (dUp ~= 0) then if (dUp == 1) then PressButton("D-Up"); else ReleaseButton("D-Up"); end end
	if (start ~= -0) then if (start == 1) then PressButton("Start"); else ReleaseButton("Start"); end end
	if (sx ~= -1) then SetMainStickX(sx) end
	if (sy ~= -1) then SetMainStickY(sy) end
	if (skip == 1) then EnableSkipRendering(); else DisableSkipRendering(); end 
	if (loadState ~= "/") then LoadState(false, loadState); end
end

function onScriptUpdate()
    local title = "\n\n\n\n";
	
	-- Read from SocketInit
	count = SocketReadInt();
	-- Send back input frame
	SocketWriteInt(core.getFrameOfInput());
	-- Request action
	local action = SocketReadInt();
	
	if (action == 0) then -- No an onput frame, just get load
		local loadState = SocketReadString();
		if (loadState ~= "/") then LoadState(false, loadState); end
		
		processInput();
	else -- Input frame
		writeOutput();
		a = SocketReadInt();
		b = SocketReadInt();
		dUp = SocketReadInt();
		start = SocketReadInt();
		sx = SocketReadInt();
		sy = SocketReadInt();
		skip = SocketReadInt();
		loadState = SocketReadString();
		
		-- Read other things from file which do not need syncing
		for line in io.lines(inputFile) do 
			title = title .. "\n" .. line;
		end
		SetScreenText(title);
	end
	
	SocketWriteInt(count); -- Mirror back count
	
	
    
end

