
/**
 * The openlink manager manages Openlink commands on an XMPP Connection.
 *
 * @param {XMPP.Connection} connection the connection which this service discovery manager will
 * handle disco for.
 */
 
org.xmpp = {

}


org.xmpp.openlink = {

}


org.xmpp.openlink.Profile = function() {
    this.id =  null;     
    this.label =  null;  
    this.actions = new Object();    
}

org.xmpp.openlink.Profile.prototype = {

}

org.xmpp.openlink.Interest = function() {
    this.id =  null;  
    this.label =  null; 
    this.lineType = null;
    this.isDefault = null;
}

org.xmpp.openlink.Interest.prototype = {

}

org.xmpp.openlink.Feature = function() {
    this.id =  null;  
    this.type =  null;    
    this.label =  null;    
    this.voicemessages = new Array();
}

org.xmpp.openlink.Feature.prototype = {

}

org.xmpp.openlink.Action = function() {
    this.id =  null;     
    this.label =  null;    
}

org.xmpp.openlink.Action.prototype = {

}

org.xmpp.openlink.Keypage = function() {
    this.id =  null;     
    this.label =  null;
    this.keys = new Array();

}

org.xmpp.openlink.Keypage.prototype = {

}

org.xmpp.openlink.Key = function() {
    this.id =  null;     
    this.label =  null;
    this.keyFunction = null;
    this.qualifier = null;
    this.modifier = null;
    this.color = null;
    this.interest = null;   
}

org.xmpp.openlink.Key.prototype = {

}

org.xmpp.openlink.VoiceMessage = function() {
    this.msglen =  null;  
    this.status =  null;    
    this.statusdescriptor =  null;    
    this.exten = null;
}

org.xmpp.openlink.VoiceMessage.prototype = {

}

org.xmpp.openlink.Bridge = function() {
    this.jid =  null;  
    this.eventType =  null;    
    this.dtmf =  null;    
    this.participants = null;
    this.callState =  null;  
    this.conference =  null;    
    this.participant =  null;    
    this.callInfo = null;    
    this.eventInfo = null;    
}

org.xmpp.openlink.Bridge.prototype = {

}
					    
org.xmpp.openlink.Participant = function() {
    this.jid =  null;
    this.direction = null;
    this.type = null;   
}

org.xmpp.openlink.Participant.prototype = {

}

org.xmpp.openlink.Call = function() {
    this.callId = "";
    this.direction = "";
    this.caller = "";
    this.called = "";  
    this.callerName = "";
    this.calledName = "";    
    this.state = "";  
    this.interest = "";
    this.profile = "";
    this.tsc = "";
    this.duration = "";
    this.timestamp = "";
    this.participants = new Array();
    this.actions = new Array();     
    this.manager = "";
    this.defaultAction = "";
}

org.xmpp.openlink.Call.prototype = {

    doAction: function(olCallback, action, destination) 
    {
	this.manager.doAction(olCallback, action, this.callId, this.interest, destination);
 
    }
}

org.xmpp.openlink.Command = function() {
    this.noteType = null;
    this.note = null;  
    this.node = null;     
    this.profiles = new Array();  
    this.interests = new Array();  
    this.features = new Array(); 
    this.keypages = new Array();
    this.callHistory = new Array();    
    this.itemsTotal = null;
    this.itemsStart = null;
    this.itemsCount = null;    
    this.manager = null;
}

org.xmpp.openlink.Command.prototype = {
        
}
 
org.xmpp.openlink.Manager = function(connection, jid, callback) {
    this._connection = connection;
    this._jid = jid;
    this._callback = callback;
    this.olIQ = null;
    this.olIn = null;
    this.olCommand = null;
    this.olOpenlink = null;
    this.olIn = null;     

    this.openlinkFilter = new org.jive.spank.PacketFilter(this._createOpenlinkHandler(), this._createOpenlinkFilter);
    this._connection.addPacketFilter(this.openlinkFilter);    
}

org.xmpp.openlink.Manager.prototype = {

    _createOpenlinkHandler: function() {
        var openlink = this;
        return function(packet) {
            openlink._openlinkHandler(packet);
        }
    },
    
    _createOpenlinkFilter: function(packet) {

	return packet.getPacketType() == "message" && packet.getExtension("event", "http://jabber.org/protocol/pubsub#event");
    },

    _openlinkHandler: function(packet) 
    {
	var calls = new Array();
	var bridge = null;
	
	var olEvent = packet.getExtension("event", "http://jabber.org/protocol/pubsub#event");
	var olNodes = olEvent.childNodes;
	var interest = "";

	for (var i = 0; i < olNodes.length; i++) 
	{
	    
	    if(olNodes[i].tagName == "items") 
	    {
		    var olOpenlink = olNodes[i].childNodes;
		    interest = olNodes[i].getAttribute("node")
		    
		    for (var o = 0; o < olOpenlink.length; o++) 
		    {
			    if (olOpenlink[o].tagName == "item") 
			    {
				var olEvents = olOpenlink[o].childNodes;

				for (var j = 0; j < olEvents.length; j++) 
				{
				    if (olEvents[j].tagName == "voicebridge") 
				    {				    
					var olBridge = olEvents[j].childNodes;
					
					bridge = new org.xmpp.openlink.Bridge();

					for (var z = 0; z < olBridge.length; z++) 
					{
					    if (olBridge[z].tagName == "jid" && olBridge[z].firstChild != null)
					    {
						bridge.jid = olBridge[z].firstChild.nodeValue;							    	
					    }
					    
					    if (olBridge[z].tagName == "eventtype" && olBridge[z].firstChild != null)
					    {
						bridge.eventType = olBridge[z].firstChild.nodeValue;							    	
					    }
					    
					    if (olBridge[z].tagName == "dtmf" && olBridge[z].firstChild != null)
					    {
						bridge.dtmf = olBridge[z].firstChild.nodeValue;							    	
					    }	
					    
					    if (olBridge[z].tagName == "participants" && olBridge[z].firstChild != null)
					    {
						bridge.participants = olBridge[z].firstChild.nodeValue;							    	
					    }	
					    
					    if (olBridge[z].tagName == "callstate" && olBridge[z].firstChild != null)
					    {
						bridge.callState = olBridge[z].firstChild.nodeValue;							    	
					    }
					    
					    if (olBridge[z].tagName == "conference" && olBridge[z].firstChild != null)
					    {
						bridge.conference = olBridge[z].firstChild.nodeValue;							    	
					    }	
					    
					    if (olBridge[z].tagName == "participant" && olBridge[z].firstChild != null)
					    {
						bridge.participant = olBridge[z].firstChild.nodeValue;							    	
					    }	

					    if (olBridge[z].tagName == "callinfo" && olBridge[z].firstChild != null)
					    {
						bridge.callInfo = olBridge[z].firstChild.nodeValue;							    	
					    }
					    
					    if (olBridge[z].tagName == "eventinfo" && olBridge[z].firstChild != null)
					    {
						bridge.eventInfo = olBridge[z].firstChild.nodeValue;							    	
					    }	
					    
					}
					
				    }
					
				    if (olEvents[j].tagName == "callstatus") 
				    {				    
					var olCalls = olEvents[j].childNodes;

					for (var z = 0; z < olCalls.length; z++) 
					{
					    if (olCalls[z].tagName == "call") 
					    {
						var olCall = olCalls[z].childNodes;

						var call = new org.xmpp.openlink.Call();
						call.interest = interest;
						call.manager = this;

						for (var n = 0; n < olCall.length; n++) 
						{
						    if (olCall[n].tagName == "id" && olCall[n].firstChild != null)
						    {
							call.callId = olCall[n].firstChild.nodeValue;							    	
						    }

						    if (olCall[n].tagName == "state" && olCall[n].firstChild != null)
						    {
							call.state = olCall[n].firstChild.nodeValue;
						    }

						    if (olCall[n].tagName == "direction" && olCall[n].firstChild != null)
						    {
							call.direction = olCall[n].firstChild.nodeValue;
						    }
						    
						    if (olCall[n].tagName == "actions") 
						    {
							var olActions = olCall[n].childNodes;

							call.actions = new Array();

							for (var x = 0; x < olActions.length; x++) 
							{
								call.actions.push(olActions[x].tagName);
							}
						    }

						    if (olCall[n].tagName == "caller") 
						    {
							var olCaller = olCall[n].childNodes;

							for (var x = 0; x < olCaller.length; x++) 
							{
								if (olCaller[x].tagName == "number" && olCaller[x].firstChild != null) 
								{								
									call.caller = olCaller[x].firstChild.nodeValue;
								}

								if (olCaller[x].tagName == "name" && olCaller[x].firstChild != null) 
								{								
									call.callerName = olCaller[x].firstChild.nodeValue;									
								}
							}

						    }

						    if (olCall[n].tagName == "called") 
						    {
							var olCalled = olCall[n].childNodes;

							for (var x = 0; x < olCalled.length; x++) 
							{
								if (olCalled[x].tagName == "number" && olCalled[x].firstChild != null) 
								{								
									call.called = olCalled[x].firstChild.nodeValue;
								}

								if (olCalled[x].tagName == "name" && olCalled[x].firstChild != null) 
								{								
									call.calledName = olCalled[x].firstChild.nodeValue;									
								}
							}

						    }

						    if (olCall[n].tagName == "participants") 
						    {
							var olParticipants = olCall[n].childNodes;

							call.participants = new Array();

							for (var p = 0; p < olParticipants.length; p++) 
							{
								if (olParticipants[p].tagName == "participant") 
								{												    
									var olParticipant = olParticipants[p].childNodes;

									var participant = new org.xmpp.openlink.Participant();

									participant.jid = olParticipants[p].getAttribute("jid")
									participant.direction = olParticipants[p].getAttribute("direction")
									participant.type = olParticipants[p].getAttribute("type")

									call.participants.push(participant);
								}


							}
						    }
						}

						calls.push(call);								
					    }

					}

					
				    }

				}

			    }
		    }
	    }
	}
	
	if (this._callback != null) this._callback(calls, bridge);	
    },
    
    _sendCommand: function(olCallback, olIQ) {
        var id = olIQ.getID();
        
        this._connection.sendPacket(olIQ, new org.jive.spank.PacketFilter(
                function(packet) {

                    if (olCallback) {
                    	this._processPacket(olCallback, packet);
                    }
                }.bind(this), function(packet) {
            return packet.getID() == id;
        }));
    },

    _processPacket: function(olCallback, packet) {
    
    	var command = new org.xmpp.openlink.Command();

	var olCommand = packet.getExtension("command", "http://jabber.org/protocol/commands");
	var olNodes = olCommand.childNodes;

	for (var i = 0; i < olNodes.length; i++) 
	{
		if (olNodes[i].tagName == "note") 
		{
			command.note = olNodes[i].firstChild.nodeValue;
			command.noteType = olNodes[i].getAttribute("type");
		}
	    
		if (olNodes[i].tagName == "iodata") 
		{
			var olOpenlink = olNodes[i].childNodes;

			for (var j = 0; j < olOpenlink.length; j++) 
			{
				if (olOpenlink[j].tagName == "out") 
				{
					var olOut = olOpenlink[j].childNodes;

					for (var z = 0; z < olOut.length; z++) 
					{
						if (olOut[z].tagName == "profiles") 
						{
							var olProfiles = olOut[z].childNodes;
							command.profiles = new Array(); 

							for (var p = 0; p < olProfiles.length; p++) 
							{
								if (olProfiles[p].tagName == "profile") 
								{
									var profile 	= new org.xmpp.openlink.Profile();
									
									profile.id   	= olProfiles[p].getAttribute("id");
									profile.label   = olProfiles[p].getAttribute("label");
									
									profile.actions = new Object(); 									
									
									var olProfile = olProfiles[p].childNodes;

									for (var n = 0; n < olProfile.length;n++) 
									{
										if (olProfile[n].tagName == "actions") 
										{
											var olActions = olProfile[n].childNodes;

											for (var m = 0; m < olActions.length; m++) 
											{
												if (olActions[m].tagName == "action") 
												{
													var action 	= new org.xmpp.openlink.Action();

													action.id    	= olActions[m].getAttribute("id");
													action.label 	= olActions[m].getAttribute("label");										

													profile.actions[action.id] = action;	
												}
											}

								
										}
									}									
									
									command.profiles.push(profile);									
								}
							}
						}

						if (olOut[z].tagName == "profile") 
						{
							var olProfile = olOut[z].childNodes;

							for (var q = 0; q < olProfile.length; q++) 
							{
								if (olProfile[q].tagName == "keypages") 
								{
									var olKeypages = olProfile[q].childNodes;
									command.keypages = new Array(); 

									for (var p = 0; p < olKeypages.length; p++) 
									{
										if (olKeypages[p].tagName == "keypage") 
										{
											var keypage 	= new org.xmpp.openlink.Keypage();
											
											keypage.id    	= olKeypages[p].getAttribute("id");
											keypage.label  	= olKeypages[p].getAttribute("label");

											var olKeypage = olKeypages[p].childNodes;
											keypage.keys = new Array(); 

											for (var r = 0; r < olKeypage.length; r++) 
											{
												if (olKeypage[r].tagName == "key") 
												{
													var key = new org.xmpp.openlink.Key();
													
													key.id 		= olKeypage[r].getAttribute("id");
													key.label 	= olKeypage[r].getAttribute("label");
													key.keyFunction = olKeypage[r].getAttribute("function");
													key.qualifier 	= olKeypage[r].getAttribute("qualifier");
													key.modifier 	= olKeypage[r].getAttribute("modifier");													
													key.color 	= olKeypage[r].getAttribute("color");
													key.interest 	= olKeypage[r].getAttribute("interest");																									

													keypage.keys.push(key);	
												}
											}
											command.keypages.push(keypage);	
										}
									}
								}
							}

						}
						
						if (olOut[z].tagName == "interests") 
						{
							var olInterests = olOut[z].childNodes;
							command.interests = new Array(); 

							for (var p = 0; p < olInterests.length; p++) 
							{
								if (olInterests[p].tagName == "interest") 
								{
									var interest 	= new org.xmpp.openlink.Interest();
									
									interest.id    	= olInterests[p].getAttribute("id");
									interest.label 	= olInterests[p].getAttribute("label");	
									interest.lineType = olInterests[p].getAttribute("type");
									interest.isDefault = olInterests[p].getAttribute("default");									
									
									command.interests.push(interest);	
								}
							}

						}

						if (olOut[z].tagName == "interest") 
						{
							var interest 	= new org.xmpp.openlink.Interest();

							interest.id    	= olOut[z].getAttribute("id");
							interest.label 	= olOut[z].getAttribute("label");
							interest.lineType = olOut[z].getAttribute("type");
    							interest.isDefault = olOut[z].getAttribute("default");

							command.interests.push(interest);	
						}
						
						if (olOut[z].tagName == "features") 
						{
							var olFeatures = olOut[z].childNodes;
							command.features = new Array(); 

							for (var p = 0; p < olFeatures.length; p++) 
							{
								if (olFeatures[p].tagName == "feature") 
								{
									var feature 	= new org.xmpp.openlink.Feature();
									
									feature.id    	= olFeatures[p].getAttribute("id");
									feature.type   	= olFeatures[p].getAttribute("type");									
									feature.label 	= olFeatures[p].getAttribute("label");	
									
									command.features.push(feature);	
								}
							}

						}

						if (olOut[z].tagName == "callhistory") 
						{							
							command.itemsTotal = olOut[z].getAttribute("total");
							command.itemsStart = olOut[z].getAttribute("start");
							command.itemsCount = olOut[z].getAttribute("count");
							
							var olCallHistory = olOut[z].childNodes;
							command.callHistory = new Array(); 

							for (var p = 0; p < olCallHistory.length; p++) 
							{
							    if (olCallHistory[p].tagName == "call") 
							    {
								var call = new org.xmpp.openlink.Call();
								    
								var olCall = olCallHistory[p].childNodes;

								for (var n = 0; n < olCall.length; n++) 
								{
								    if (olCall[n].tagName == "direction" && olCall[n].firstChild != null)
								    {
									call.direction = olCall[n].firstChild.nodeValue;
								    }

								    if (olCall[n].tagName == "id" && olCall[n].firstChild != null)
								    {
									call.callId = olCall[n].firstChild.nodeValue;							    	
								    }

								    if (olCall[n].tagName == "state" && olCall[n].firstChild != null)
								    {
									call.state = olCall[n].firstChild.nodeValue;
								    }

								    if (olCall[n].tagName == "profile" && olCall[n].firstChild != null)
								    {
									call.profile = olCall[n].firstChild.nodeValue;
								    }
								    
								    if (olCall[n].tagName == "interest" && olCall[n].firstChild != null)
								    {
									call.interest = olCall[n].firstChild.nodeValue;
								    }
								    
								    if (olCall[n].tagName == "caller" && olCall[n].firstChild != null) 
								    {								
									call.caller = olCall[n].firstChild.nodeValue;
								    }

								    if (olCall[n].tagName == "callername" && olCall[n].firstChild != null) 
								    {								
									call.callerName = olCall[n].firstChild.nodeValue;									
								    }


								    if (olCall[n].tagName == "called" && olCall[n].firstChild != null) 
								    {								
									call.called = olCall[n].firstChild.nodeValue;
								    }

								    if (olCall[n].tagName == "calledname" && olCall[n].firstChild != null) 
								    {								
									call.calledName = olCall[n].firstChild.nodeValue;									
								    }

								    if (olCall[n].tagName == "timestamp" && olCall[n].firstChild != null)
								    {
									call.timestamp = olCall[n].firstChild.nodeValue;
								    }
								    
								    if (olCall[n].tagName == "duration" && olCall[n].firstChild != null)
								    {
									call.duration = olCall[n].firstChild.nodeValue;
								    }
								    
								    if (olCall[n].tagName == "tsc" && olCall[n].firstChild != null)
								    {
									call.tsc = olCall[n].firstChild.nodeValue;
								    }
								    
								}
							    
							    	command.callHistory.push(call);									
							    }
							}
						}
						
						if (olOut[z].tagName == "devicestatus") 
						{
							var olDeviceStatus = olOut[z].childNodes;

							for (var q = 0; q < olDeviceStatus.length; q++) 
							{
								if (olDeviceStatus[q].tagName == "features") 
								{
									var olFeatures = olDeviceStatus[q].childNodes;
									command.features = new Array(); 

									for (var p = 0; p < olFeatures.length; p++) 
									{
										if (olFeatures[p].tagName == "feature") 
										{
											var feature 	= new org.xmpp.openlink.Feature();
											feature.id    	= olFeatures[p].getAttribute("id");

											var olFeature = olFeatures[p].childNodes;
											feature.voicemessages = new Array(); 

											for (var r = 0; r < olFeature.length; r++) 
											{
												if (olFeature[r].tagName == "voicemessage") 
												{
													var voicemessage = new org.xmpp.openlink.VoiceMessage();
													
													var olVoicemsg = olFeature[r].childNodes;

													for (var s = 0; s < olVoicemsg.length; s++) 
													{
													    if (olVoicemsg[s].tagName == "msglen" && olVoicemsg[s].firstChild != null)
													    {
														voicemessage.msglen = olVoicemsg[s].firstChild.nodeValue;
													    }	
													    
													    if (olVoicemsg[s].tagName == "status" && olVoicemsg[s].firstChild != null)
													    {
														voicemessage.status = olVoicemsg[s].firstChild.nodeValue;
													    }
													    
													    if (olVoicemsg[s].tagName == "statusdescriptor" && olVoicemsg[s].firstChild != null)
													    {
														voicemessage.statusdescriptor = olVoicemsg[s].firstChild.nodeValue;
													    }	
													    
													    if (olVoicemsg[s].tagName == "exten" && olVoicemsg[s].firstChild != null)
													    {
														voicemessage.exten = olVoicemsg[s].firstChild.nodeValue;
													    }													    
													}

													feature.voicemessages.push(voicemessage);	
												}
											}
											command.features.push(feature);	
										}
									}
								}
							}

						}						
					}

				}
			}
		}

	}
    	
	olCallback(command);    
    },
    
    _command: function(node)  {
        this.olIQ = new XMPP.IQ("set", this._connection._jid, this._jid.toString());
        this.olCommand = this.olIQ.addExtension("command", "http://jabber.org/protocol/commands");
        this.olCommand.setAttribute("action", "execute");
        this.olCommand.setAttribute("node", node);
        this.olOpenlink = this.olCommand.appendChild(this.olIQ.doc.createElement("iodata"));
        this.olOpenlink.setAttribute("xmlns", 'urn:xmpp:tmp:io-data');
        this.olOpenlink.setAttribute("type", 'input');        
        this.olIn = this.olOpenlink.appendChild(this.olIQ.doc.createElement("in"));    
    }, 
    
    getProfiles: function(olCallback, userJID) 
    {
	this._command('http://xmpp.org/protocol/openlink:01:00:00#get-profiles');
	
        var olUser = this.olIn.appendChild(this.olIQ.doc.createElement("jid"));
        olUser.appendChild(this.olIQ.doc.createTextNode(userJID)); 
        
	this._sendCommand(olCallback, this.olIQ)    
    },

    getProfile: function(olCallback, profile) 
    {
	this._command('http://xmpp.org/protocol/openlink:01:00:00#get-profile');
	
        var olProfile = this.olIn.appendChild(this.olIQ.doc.createElement("profile"));
        olProfile.appendChild(this.olIQ.doc.createTextNode(profile)); 
                
	this._sendCommand(olCallback, this.olIQ)    
    },
    
    getInterests: function(olCallback, profile) 
    {
	this._command('http://xmpp.org/protocol/openlink:01:00:00#get-interests');
	
        var olProfile = this.olIn.appendChild(this.olIQ.doc.createElement("profile"));
        olProfile.appendChild(this.olIQ.doc.createTextNode(profile)); 
                
	this._sendCommand(olCallback, this.olIQ)    
    },

    getInterest: function(olCallback, interest) 
    {
	this._command('http://xmpp.org/protocol/openlink:01:00:00#get-interest'); 
        
        var olInterest = this.olIn.appendChild(this.olIQ.doc.createElement("interest"));
       	olInterest.appendChild(this.olIQ.doc.createTextNode(interest));        
                
	this._sendCommand(olCallback, this.olIQ)    
    },
    
    getFeatures: function(olCallback, profile) 
    {
	this._command('http://xmpp.org/protocol/openlink:01:00:00#get-features');
	
        var olProfile = this.olIn.appendChild(this.olIQ.doc.createElement("profile"));
        olProfile.appendChild(this.olIQ.doc.createTextNode(profile)); 
                
	this._sendCommand(olCallback, this.olIQ)    
    },

    setFeature: function(olCallback, profile, feature, value1, value2, value3) 
    {
	this._command('http://xmpp.org/protocol/openlink:01:00:00#set-features');
	
        var olProfile = this.olIn.appendChild(this.olIQ.doc.createElement("profile"));
        olProfile.appendChild(this.olIQ.doc.createTextNode(profile)); 

        var olFeature = this.olIn.appendChild(this.olIQ.doc.createElement("feature"));
        olFeature.appendChild(this.olIQ.doc.createTextNode(feature)); 
        
        if (value1 != null)
        {
	        var olValue1 = this.olIn.appendChild(this.olIQ.doc.createElement("value1"));
	        olValue1.appendChild(this.olIQ.doc.createTextNode(value1));         
        }

        if (value2 != null)
        {
	        var olValue2 = this.olIn.appendChild(this.olIQ.doc.createElement("value2"));
	        olValue2.appendChild(this.olIQ.doc.createTextNode(value2));         
        }

        if (value3 != null)
        {
	        var olValue3 = this.olIn.appendChild(this.olIQ.doc.createElement("value3"));
	        olValue3.appendChild(this.olIQ.doc.createTextNode(value3));         
        }
        
	this._sendCommand(olCallback, this.olIQ)    
    },
    
    makeCall: function(olCallback, userJID, interest, destination) 
    {
	this._command('http://xmpp.org/protocol/openlink:01:00:00#make-call');
	
        var olUser = this.olIn.appendChild(this.olIQ.doc.createElement("jid"));
        olUser.appendChild(this.olIQ.doc.createTextNode(userJID));
	        
        var olInterest = this.olIn.appendChild(this.olIQ.doc.createElement("interest"));
       	olInterest.appendChild(this.olIQ.doc.createTextNode(interest)); 

        var olDestination = this.olIn.appendChild(this.olIQ.doc.createElement("destination"));
        olDestination.appendChild(this.olIQ.doc.createTextNode(destination)); 
        
	this._sendCommand(olCallback, this.olIQ)    
    }, 

    manageVoiceBridge: function(olCallback, userJID, actions) 
    {
	this._command('http://xmpp.org/protocol/openlink:01:00:00#manage-voice-bridge');
	
        var olUser = this.olIn.appendChild(this.olIQ.doc.createElement("jid"));
        olUser.appendChild(this.olIQ.doc.createTextNode(userJID)); 
			
        if (actions != null)
        {
                var olActions = this.olIn.appendChild(this.olIQ.doc.createElement("actions"));
                
		for (var i = 0; i < actions.length; i++) 
		{
			var olAction = olActions.appendChild(this.olIQ.doc.createElement("action"));
			
			var olName = olAction.appendChild(this.olIQ.doc.createElement("name"));
			olName.appendChild(this.olIQ.doc.createTextNode(actions[i][0]));
			
			var olValue1 = olAction.appendChild(this.olIQ.doc.createElement("value1"));
			olValue1.appendChild(this.olIQ.doc.createTextNode(actions[i][1]));

			if (actions[i].length == 3 && actions[i][2] != null)
			{			
				var olValue2 = olAction.appendChild(this.olIQ.doc.createElement("value2"));
				olValue2.appendChild(this.olIQ.doc.createTextNode(actions[i][2]));
			}
		}
	}
        
	this._sendCommand(olCallback, this.olIQ)    
    }, 

    manageVoiceMessage: function(olCallback, profile, features, action, label) 
    {
	this._command('http://xmpp.org/protocol/openlink:01:00:00#manage-voice-message');
	
        var olProfile = this.olIn.appendChild(this.olIQ.doc.createElement("profile"));
        olProfile.appendChild(this.olIQ.doc.createTextNode(profile)); 
			
        if (features != null)
        {
                var olFeatures = this.olIn.appendChild(this.olIQ.doc.createElement("features"));
                
		for (var i = 0; i < features.length; i++) 
		{
			var olFeature = olFeatures.appendChild(this.olIQ.doc.createElement("feature"));
			var olID = olFeature.appendChild(this.olIQ.doc.createElement("id"));
			olID.appendChild(this.olIQ.doc.createTextNode(features[i]));		
		}
	}

        var olAction = this.olIn.appendChild(this.olIQ.doc.createElement("action"));
        olAction.appendChild(this.olIQ.doc.createTextNode(action)); 

        if (label != null)
        {
	        var olLabel = this.olIn.appendChild(this.olIQ.doc.createElement("label"));
	        olLabel.appendChild(this.olIQ.doc.createTextNode(label));         
        }
        
	this._sendCommand(olCallback, this.olIQ)    
    },

    makeFeatureCall: function(olCallback, userJID, interest, destination, feature, value1, value2) 
    {
	this._command('http://xmpp.org/protocol/openlink:01:00:00#make-call');
	
        var olUser = this.olIn.appendChild(this.olIQ.doc.createElement("jid"));
        olUser.appendChild(this.olIQ.doc.createTextNode(userJID));
	        
        var olInterest = this.olIn.appendChild(this.olIQ.doc.createElement("interest"));
       	olInterest.appendChild(this.olIQ.doc.createTextNode(interest)); 

        var olDestination = this.olIn.appendChild(this.olIQ.doc.createElement("destination"));
        olDestination.appendChild(this.olIQ.doc.createTextNode(destination)); 
        
        var olFeatures = this.olIn.appendChild(this.olIQ.doc.createElement("features"));
        var olFeature = olFeatures.appendChild(this.olIQ.doc.createElement("feature")); 
        var olFeatureId = olFeature.appendChild(this.olIQ.doc.createElement("id"));
 	olFeatureId.appendChild(this.olIQ.doc.createTextNode(feature));                 
        var olFeatureValue1 = olFeature.appendChild(this.olIQ.doc.createElement("value1"));         
 	olFeatureValue1.appendChild(this.olIQ.doc.createTextNode(value1));         
        var olFeatureValue2 = olFeature.appendChild(this.olIQ.doc.createElement("value2"));         
 	olFeatureValue2.appendChild(this.olIQ.doc.createTextNode(value2));        
	this._sendCommand(olCallback, this.olIQ)    
    },
    
    doAction: function(olCallback, action, callId, interest, value1) 
    {
	this._command('http://xmpp.org/protocol/openlink:01:00:00#request-action');
	
	var olInterest = this.olIn.appendChild(this.olIQ.doc.createElement("interest"));
        olInterest.appendChild(this.olIQ.doc.createTextNode(interest));
	
	var olAction = this.olIn.appendChild(this.olIQ.doc.createElement("action"));
        olAction.appendChild(this.olIQ.doc.createTextNode(action));

	var olCall = this.olIn.appendChild(this.olIQ.doc.createElement("call"));
        olCall.appendChild(this.olIQ.doc.createTextNode(callId));

	if (value1 != null)
	{
		var olValue1 = this.olIn.appendChild(this.olIQ.doc.createElement("value1"));
	        olValue1.appendChild(this.olIQ.doc.createTextNode(value1));	
	}
        
	this._sendCommand(olCallback, this.olIQ);  
    },     

    getCallHistory: function(olCallback, userJID, caller, called, callType, fromDate, uptoDate, start, count) 
    {
	this._command('http://xmpp.org/protocol/openlink:01:00:00#get-call-history');
	
        var olUser = this.olIn.appendChild(this.olIQ.doc.createElement("jid"));
        olUser.appendChild(this.olIQ.doc.createTextNode(userJID));
	        
        var olCaller = this.olIn.appendChild(this.olIQ.doc.createElement("caller"));
       	olCaller.appendChild(this.olIQ.doc.createTextNode(caller)); 

        var olCalled = this.olIn.appendChild(this.olIQ.doc.createElement("called"));
       	olCalled.appendChild(this.olIQ.doc.createTextNode(called)); 
       	
        var olType = this.olIn.appendChild(this.olIQ.doc.createElement("calltype"));
        olType.appendChild(this.olIQ.doc.createTextNode(callType)); 

        var olFromDate = this.olIn.appendChild(this.olIQ.doc.createElement("fromdate"));
       	olFromDate.appendChild(this.olIQ.doc.createTextNode(fromDate));         

        var olUptoDate = this.olIn.appendChild(this.olIQ.doc.createElement("uptodate"));
       	olUptoDate.appendChild(this.olIQ.doc.createTextNode(uptoDate)); 
       	
        var olStart = this.olIn.appendChild(this.olIQ.doc.createElement("start"));
       	olStart.appendChild(this.olIQ.doc.createTextNode(start));        	

        var olCount = this.olIn.appendChild(this.olIQ.doc.createElement("count"));
       	olCount.appendChild(this.olIQ.doc.createTextNode(count));  
       	
	this._sendCommand(olCallback, this.olIQ)    
    },
    
    monitorInterest: function(olCallback, pubsubJID, userJID, interest) 
    {
        this.olIQ = new XMPP.IQ("set", this._connection._jid, pubsubJID);
        var olPubsub = this.olIQ.addExtension("pubsub", "http://jabber.org/protocol/pubsub");
        var olSubscribe = olPubsub.appendChild(this.olIQ.doc.createElement("subscribe"));
        olSubscribe.setAttribute("node", interest);
        olSubscribe.setAttribute("jid", this._connection._jid.toString());        
                
	this._sendCommand(olCallback, this.olIQ)    
    },    
    
    unmonitorInterest: function(olCallback, pubsubJID, userJID, interest) 
    {
        this.olIQ = new XMPP.IQ("set", this._connection._jid, pubsubJID);
        var olPubsub = this.olIQ.addExtension("pubsub", "http://jabber.org/protocol/pubsub");
        var olSubscribe = olPubsub.appendChild(this.olIQ.doc.createElement("unsubscribe"));
        olSubscribe.setAttribute("node", interest);
        olSubscribe.setAttribute("jid", this._connection._jid.toString());        
                
	this._sendCommand(olCallback, this.olIQ)    
    }     
}


