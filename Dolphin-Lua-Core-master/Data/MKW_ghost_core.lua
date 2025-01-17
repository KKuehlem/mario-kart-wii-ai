local core = require "MKW_core"

local ghost_core = {}

local function getGhostAddressPointer()
	local gameID = core.getGameID()
	local baseAddress = 0x0

	if gameID == "RMCE01" then baseAddress = 0x9B8F70
	elseif gameID == "RMCP01" then baseAddress = 0x9BD730
	elseif gameID == "RMCJ01" then baseAddress = 0x9BC790
	elseif gameID == "RMCK01" then baseAddress = 0x9ABD70
	end


	local offset1 = 0x1FC
	local offset2 = 0x4
	local address1 = GetPointerNormal(baseAddress)
	local address2 = GetPointerNormal(address1 + offset1)
	local address3 = GetPointerNormal(address2 + offset2)

	local offsetFaceButton = 0x94
	local addressF1 = GetPointerNormal(address3 + offsetFaceButton)
	local offsetDirectionInput = 0x98
	local addressD1 = GetPointerNormal(address3 + offsetDirectionInput)
	local offsetTrickInput = 0x9C
	local addressT1 = GetPointerNormal(address3 + offsetTrickInput)

	local offset3 = 0x4

	return addressF1+offset3, addressD1+offset3, addressT1+offset3
end
ghost_core.getGhostAddressPointer = getGhostAddressPointer

local function getGhostAddresses()
	local addressF1, addressD1, addressT1 = getGhostAddressPointer()

	local addressF2 = GetPointerNormal(addressF1)
	local addressD2 = GetPointerNormal(addressD1)
	local addressT2 = GetPointerNormal(addressT1)

	return addressF2, addressD2, addressT2
end
ghost_core.getGhostAddresses = getGhostAddresses


-- ###############################

local function maskFaceButton(aButton, bButton, lButton, prevMask)
	local x8Mask = 0x0
	if bButton == 1 and (prevMask == 0x1 or prevMask == 0x5 or prevMask == 0xB or prevMask == 0xF) then
		x8Mask = 0x8
	end
	return aButton * 0x1 + bButton * 0x2 + lButton * 0x4 + x8Mask
end
ghost_core.maskFaceButton = maskFaceButton

function maskDirectionInput(horizontalInput, verticalInput)
	return (horizontalInput << 4) + verticalInput
end
ghost_core.maskDirectionInput = maskDirectionInput

function maskTrickInput(trickInput)
	return trickInput << 4
end
ghost_core.maskTrickInput = maskTrickInput

-- ###############################

local function writeRKGData(writeAddress, inputData, durationData)
	WriteValue8(writeAddress, inputData)
	WriteValue8(writeAddress + 0x1, durationData)

	return writeAddress + 0x2
end

-- ###############################

function writeInputsIntoRKG(input_ghost)
	local currentAddress = 0x0
	
	local addressFaceButton, addressDirectionInput, addressTrickInput = getGhostAddresses()
	local addressFaceButtonPointer, addressDirectionInputPointer, addressTrickInputPointer = getGhostAddressPointer()
	
	local prevInput = maskFaceButton(input_ghost[1][1], input_ghost[1][2], input_ghost[1][3], 0x0)
	local amountCurrentFrames = 0x0
	
	currentAddress = addressFaceButton
	
	for _, inputs in ipairs(input_ghost) do
		local currentInput = maskFaceButton(inputs[1], inputs[2], inputs[3], prevInput)
		if prevInput ~= currentInput then
			currentAddress = writeRKGData(currentAddress, prevInput, amountCurrentFrames)
			
			prevInput = currentInput
			amountCurrentFrames = 0x1
		else
			if amountCurrentFrames >= 0xFF then
				currentAddress = writeRKGData(currentAddress, prevInput, amountCurrentFrames)
				
				prevInput = currentInput
				amountCurrentFrames = 0x1
			else
				amountCurrentFrames = amountCurrentFrames + 1
			end
		end
	end
	
	currentAddress = writeRKGData(currentAddress, prevInput, amountCurrentFrames)
	
	WriteValue32(addressDirectionInputPointer, currentAddress + 0x80000000)
	
	prevInput = maskDirectionInput(input_ghost[1][4], input_ghost[1][5])
	amountCurrentFrames = 0x0
	
	for _, inputs in ipairs(input_ghost) do
		local currentInput = maskDirectionInput(inputs[4], inputs[5])
		if prevInput ~= currentInput then
			currentAddress = writeRKGData(currentAddress, prevInput, amountCurrentFrames)
			
			prevInput = currentInput
			amountCurrentFrames = 0x1
		else
			if amountCurrentFrames >= 0xFF then
				currentAddress = writeRKGData(currentAddress, prevInput, amountCurrentFrames)
				
				prevInput = currentInput
				amountCurrentFrames = 0x1
			else
				amountCurrentFrames = amountCurrentFrames + 1
			end
		end
	end
	
	currentAddress = writeRKGData(currentAddress, prevInput, amountCurrentFrames)
	
	WriteValue32(addressTrickInputPointer, currentAddress + 0x80000000)
	
	prevInput = maskTrickInput(input_ghost[1][6])
	amountCurrentFrames = 0x0
	
	for _, inputs in ipairs(input_ghost) do
		local currentInput = maskTrickInput(inputs[6])
		if prevInput ~= currentInput then
			local inputData = prevInput + math.floor(amountCurrentFrames / 0x100)
			
			currentAddress = writeRKGData(currentAddress, inputData, amountCurrentFrames % 0x100)
			
			prevInput = currentInput
			amountCurrentFrames = 0x1
		else
			if amountCurrentFrames >= 0xFFF then
				local inputData = prevInput + math.floor(amountCurrentFrames / 0x100)
				
				currentAddress = writeRKGData(currentAddress, inputData, amountCurrentFrames % 0x100)
				
				prevInput = currentInput
				amountCurrentFrames = 0x1
			else
				amountCurrentFrames = amountCurrentFrames + 1
			end
		end
	end
	
	local inputData = prevInput + math.floor(amountCurrentFrames / 0x100)
	currentAddress = writeRKGData(currentAddress, inputData, amountCurrentFrames % 0x100)
	
	endOfFile = addressFaceButton + 0x2774
	while currentAddress < endOfFile do
		WriteValue16(currentAddress, 0x0)
		currentAddress = currentAddress + 0x2
	end
end
ghost_core.writeInputsIntoRKG = writeInputsIntoRKG

-- ###############################

return ghost_core
