-- are we ready??

NOTE("Server application initialised")

socket = cumulus:createTCPClient()
writers = {}
publishers = {}

	
function onStart(path)

	NOTE("Server application onStart '"..path.."' started")
	error = socket:connect("localhost", 5555)	

	if error then 
		ERROR(error) 
	end		
end

function onStop(path)

	NOTE("Server application onStop '"..path.."' stopped")

	if socket.connected then 	-- useless if already disconnected
		socket:disconnect()	
	end	
end


function socket:onReception(data)

	--NOTE("Reception from "..self.peerAddress.." to "..self.address .. " " .. #data)	
	--NOTE(data)

	local pointer = 1

	while #data > 0 do
		
		local msgType = data:sub(1, 2)
		local msgLen = tonumber(data:sub(3, 6))
				
		if msgType == "01" then

			local time = tonumber(data:sub(7, 12))
			local stream = trim(data:sub(13, 48))
			local audio = data:sub(49, 209)

			local publisher = publishers[stream]

			if publisher == nil then

				NOTE('startPublisher ' .. stream)			
				publisher = cumulus:publish(stream)
				publishers[stream] = publisher
			end
			
			
			--NOTE('handleAudioData ' .. stream .. '  ' .. msgLen .. '  ' .. #audio)			
			publisher:pushAudioPacket(time, audio)	
			publisher:flush()				
			
			pointer = pointer + 209			

		end

		if msgType == "02" then

			local stream = trim(data:sub(7, 42))
			--NOTE('startPublisher ' .. stream)
			
			pointer = pointer + 42				

		end

		if msgType == "03" then

			local stream = trim(data:sub(7, 42))
			--NOTE('stopPublisher ' .. stream)

			pointer = pointer + 42	
		end
		
		data = data:sub(pointer)
		
	end
	
	
	return 0 
end

function socket:onDisconnection()

	if self.error then 		-- error? or normal disconnection?
		ERROR(self.error)
	end
	
	NOTE("TCP disconnection")
	
	writers = {}
	publishers = {}
		
end


function onConnection(client,response,...)

	NOTE("Client connection "..client.id.." address "..client.address.." path "..client.path.." pageUrl "..client.pageUrl.." swfUrl "..client.swfUrl)
	writers[client.id] = client.writer:newFlowWriter()
		
end

function onDisconnection(client)

	NOTE("Client disconnection "..client.id)
	writers[client.id] = nil
end 


function onAudioPacket(client,publication,time,packet)

	--NOTE("onAudioPacket client id "..client.id)
	--NOTE("onAudioPacket publication name "..publication.name )
	--NOTE("onAudioPacket publication publisherId "..publication.publisherId )	
	--NOTE("onAudioPacket time "..time )
	
	length = 46 + string.len(packet)
	socket:send("01" .. string.format("%04d", length) .. string.format("%06d", time) .. pad(publication.name, 36) .. packet) 
	
end

function onManage()

	
end


function pad(text, width)

	return text .. string.rep(" ", (width - string.len(text)))
end

function trim (s)

	return (string.gsub(s, "^%s*(.-)%s*$", "%1"))
end