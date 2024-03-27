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
    inputCounterFile = string.format("AI/AI_%d_InputCounter.txt", id);
    outputCounterFile = string.format("AI/AI_%d_OutputCounter.txt", id);
    outputFile = string.format("AI/AI_%d_Output.txt", id);
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

function file_exists(file)
  local f = io.open(file, "rb")
  if f then f:close() end
  return f ~= nil
end

function waitForClient() 
	while true do
        local updated = false;

		if file_exists(inputCounterFile) then
			local fileLines = io.lines(inputCounterFile);
			
			for line in fileLines do 
				if (line ~= nil) then
					n = tonumber(line);
					if (n > count and n~= nil) then
						count = n;
						updated = true;
					end 
				end
			end
		end -- if file exists
        
        if (updated) then break end
    end
end

function onScriptUpdate()
    local title = "\n\n\n\n";
    
    while true do
		if pcall(waitForClient) then
		   break;
		end
	end
    
    local x = 1;
    for line in io.lines(inputFile) do 
        if (x == 1) then if (line ~= "/") then if (line == "true") then PressButton("A"); else ReleaseButton("A"); end end end
        if (x == 2) then if (line ~= "/") then if (line == "true") then PressButton("B"); else ReleaseButton("B"); end end end
        if (x == 3) then if (line ~= "/") then if (line == "true") then PressButton("D-Up"); else ReleaseButton("D-Up"); end end end
        if (x == 4) then if (line ~= "/") then if (line == "true") then PressButton("Start"); else ReleaseButton("Start"); end end end
        if (x == 5) then if (line ~= "/") then SetMainStickX(tonumber(line)); end end
        if (x == 6) then if (line ~= "/") then SetMainStickY(tonumber(line)); end end
        if (x == 7) then if (line ~= "/") then LoadState(false, line); end end
        if (x == 8) then if (line ~= "/") then if (line == "true") then EnableSkipRendering(); else DisableSkipRendering(); end end end
        if (x >= 9) then title = title .. "\n" .. line; end
        
        x = x + 1;
    end
    
    local pos = core.getPos();
    local rot = core.calculateEuler();
    local a = GetPointerNormal(Pointers.getRaceData2Pointer());
    local checkpoint = 0;
    if (ReadValue32(a) >= 0x80000000) then
        checkpoint = ReadValue16(a + 0xA);
    end
    
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
    
    SetScreenText(title);
    
    file = io.open(outputFile, "w");
    io.output(file);
    io.write(string.format("Position=(%f | %f |%f)\n", pos.X, pos.Y, pos.Z));
    io.write(string.format("Rotation=(%f | %f |%f)\n", rot.X, rot.Y, rot.Z));
    io.write(string.format("Checkpoint=%d\n", checkpoint));
    io.write(string.format("Lap=%d\n", lap));
    io.write(string.format("Ground=%d\n", ReadValue32(0x809BF0D0)));
    io.write(string.format("Frame=%d\n", core.getFrameOfInput()));
    
    local buttons = core.getInput()
    
    local aButton = buttons.ABLR % 2
    local bButton = (buttons.ABLR >> 1) % 2
    io.write(string.format("A=%d\n", aButton));
    io.write(string.format("B=%d\n", bButton));
    io.write(string.format("X=%d\n", stickPos(buttons.X)));
    io.write(string.format("Y=%d\n", stickPos(buttons.Y)));
    io.write(string.format("DPAD=%d\n", buttons.DPAD));
    
    
    if file then
        file:close();
        file = nil;
    end
	
	sleep(0.002) -- Sleep 2 ms befor writing counter
    
    file = io.open(outputCounterFile, "w");
    io.output(file);
    io.write(string.format("%d", count));
    if file then
        file:close();
        file = nil;
    end
    
end

