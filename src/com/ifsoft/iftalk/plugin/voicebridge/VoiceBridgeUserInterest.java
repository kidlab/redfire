
package com.ifsoft.iftalk.plugin.voicebridge;

import java.io.Serializable;
import java.util.*;
import org.xmpp.packet.JID;
import org.jivesoftware.util.Log;

import com.ifsoft.iftalk.plugin.tsc.*;
import com.ifsoft.iftalk.plugin.tsc.calllog.*;

public class VoiceBridgeUserInterest extends AbstractUserInterest
{

    private VoiceBridgeUser voicebridgeUser;
    private VoiceBridgeInterest voicebridgeInterest;
    private String defaultInterest;
	private String callFWD = "false";
	private String callFWDDigits = "";
    private Map<String, VoiceBridgeCall> voicebridgeCalls;
	private Map<String, VoiceBridgeSubscriber> voicebridgeSubscribers;
	private int maxNumCalls = 0;

    public VoiceBridgeUserInterest()
    {
        defaultInterest = "false";
        voicebridgeCalls = Collections.synchronizedMap( new HashMap<String, VoiceBridgeCall>());
        voicebridgeSubscribers = Collections.synchronizedMap( new HashMap<String, VoiceBridgeSubscriber>());
    }

	public String getInterestName() {
		return voicebridgeInterest.getInterestId() + voicebridgeUser.getUserNo();
	}

    public void handleCallInfo(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sVDISNewLineState, String speakerCount, String handsetCount, String direction, String sPrivacyOn, String sRealVDISDDI, String lineType, String sELC)
    {
        VoiceBridgeCall voicebridgeCall = createCallByNo(sCallNo);
        voicebridgeCall.start();
        voicebridgeCall.line = sVDISVoiceBridgeLineNo;
        voicebridgeCall.label = sVDISVoiceBridgeLineName;
		voicebridgeCall.console = getUser().getDeviceNo();
		voicebridgeCall.handset = getUser().getHandsetNo();
		voicebridgeCall.direction = "I".equals(direction) ? "Incoming" : "Outgoing";
		voicebridgeCall.setPrivacy(sPrivacyOn);

		if ("I".equals(sVDISNewLineState))
		{
			voicebridgeCall.setState("ConnectionCleared");
		}

		if ("R".equals(sVDISNewLineState))
		{
			voicebridgeCall.setState("CallDelivered");
        	voicebridgeCall.direction = "Incoming";
		}

		if ("C".equals(sVDISNewLineState) || "A".equals(sVDISNewLineState))
		{
			voicebridgeCall.setState("CallEstablished");
		}

		if ("H".equals(sVDISNewLineState))
		{
			voicebridgeCall.setState("CallHeld");
		}

		if ("F".equals(sVDISNewLineState))
		{
			voicebridgeCall.setState("CallConferenced");
		}

		voicebridgeCall.setValidActions();
	}

    public void handleCallELC(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISHandsetNo, String sELC, String sConnectOrDisconnect)
    {
        VoiceBridgeCall voicebridgeCall = createCallByNo(sCallNo);
        voicebridgeCall.line = sVDISVoiceBridgeLineNo;

		Log.info("handleCallELC " + voicebridgeCall.callid + " "  + sConnectOrDisconnect);

        if("C".equals(sConnectOrDisconnect))
        {
            voicebridgeCall.localConferenced = true;
        	voicebridgeCall.setState("CallConferenced");

        } else {
            voicebridgeCall.localConferenced = false;
 			voicebridgeCall.setState("CallEstablished");
		}

		voicebridgeCall.setValidActions();

    }

    public void handleBusyLine(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISOldLineState, String sVDISNewLineState,
            String sVDISHandsetOrSpeaker, String sVDISSpeakerNo, String sVDISHandsetNo, String sVDISConnectOrDisconnect)
	{

    }

    public void handleConnectionCleared(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISHandsetNo, String sVDISSpeakerNo)
    {
        VoiceBridgeCall voicebridgeCall = createCallByNo(sCallNo);
        voicebridgeCall.line = sVDISVoiceBridgeLineNo;
        voicebridgeCall.setState("ConnectionCleared");
        voicebridgeCall.setValidActions();
        voicebridgeCall.participation = "Inactive";
        voicebridgeCall.endDuration();

        voicebridgeCall.clear();

        getUser().setIntercom(false);

        if ("1".equals(sVDISHandsetNo))
        	getUser().setCurrentHS1Call(null);

 		else if ("2".equals(sVDISHandsetNo))
        	getUser().setCurrentHS2Call(null);

 		else if ("65".equals(sVDISSpeakerNo))
	        getUser().setCurrentICMCall(null);
    }

    public void handleCallOutgoing(String sCallNo, String sVDISVoiceBridgeLineNo, String sRealVDISDDI, String sVDISDDI, String sVDISVoiceBridgeLineName)
    {
        VoiceBridgeCall voicebridgeCall = createCallByNo(sCallNo);
        voicebridgeCall.clear();
        voicebridgeCall.line = sVDISVoiceBridgeLineNo;
        voicebridgeCall.ddi = sRealVDISDDI;
        voicebridgeCall.ddiLabel = sVDISVoiceBridgeLineName;
        voicebridgeCall.phantomDDI = sVDISDDI;
        voicebridgeCall.label = sVDISVoiceBridgeLineName;
        voicebridgeCall.direction = "Outgoing";
        //voicebridgeCall.proceedingDigits = "";
    }

    public void handleCallPrivate(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISHandsetNo, String sPrivacyOn)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.setPrivacy(sPrivacyOn);
			voicebridgeCall.setValidActions();
		}
    }


    public void handleCallPrivateElsewhere(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISHandsetNo, String sPrivacyOn)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.setPrivacy(sPrivacyOn);
			voicebridgeCall.setValidActions();
		}
    }

    public void handleCallIncoming(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sRealVDISDDI, String sVDISDDI)
    {
        VoiceBridgeCall voicebridgeCall = createCallByNo(sCallNo);
        voicebridgeCall.start();

        voicebridgeCall.line = sVDISVoiceBridgeLineNo;

        if("D".equals(getInterest().getInterestType()))
        {
            voicebridgeCall.ddi = sRealVDISDDI;
            voicebridgeCall.ddiLabel = sVDISVoiceBridgeLineName;
        }
        voicebridgeCall.phantomDDI = sVDISDDI;
        voicebridgeCall.label = sVDISVoiceBridgeLineName;

        voicebridgeCall.direction = "Incoming";
        voicebridgeCall.setState("CallDelivered");
		voicebridgeCall.setValidActions();
        voicebridgeCall.participation = "Inactive";
    }

    public void handleCallAbandoned(String sCallNo, String sVDISVoiceBridgeLineNo)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.setState("CallMissed");
			voicebridgeCall.setValidActions();

			voicebridgeCall.clear();
		}
    }

    public void handleCallConferenced(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISOldLineState, String sVDISNewLineState,
            String sVDISHandsetOrSpeaker, String sVDISSpeakerNo, String sVDISHandsetNo, String sVDISConnectOrDisconnect)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.handset = sVDISHandsetNo;
			voicebridgeCall.speaker = sVDISSpeakerNo;

			setCurrentCall(voicebridgeCall);

			voicebridgeCall.setState("CallConferenced");
			voicebridgeCall.setValidActions();
			voicebridgeCall.startDuration();

			voicebridgeCall.participation = "Active";
			voicebridgeCall.startParticipation();
		}
    }

    public void handleCallConnected(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISOldLineState, String sVDISNewLineState,
                                    String sVDISHandsetOrSpeaker, String sVDISSpeakerNo, String sVDISHandsetNo, String sVDISConnectOrDisconnect, boolean isCallbackAvailable, boolean isVoiceDropAvailable)
    {

		Log.debug("setCurrentCall " + sCallNo + " " + sVDISHandsetNo + " " + sVDISOldLineState + " " + sVDISNewLineState);

        VoiceBridgeCall voicebridgeCall = createCallByNo(sCallNo);
        voicebridgeCall.start();
        voicebridgeCall.setCallbackActive(getUser().getCallbackActive());
        voicebridgeCall.setCallbackAvailable(isCallbackAvailable);
        voicebridgeCall.setVoiceDropActive(isVoiceDropAvailable);

        voicebridgeCall.line = sVDISVoiceBridgeLineNo;
        voicebridgeCall.label = sVDISVoiceBridgeLineName;

		voicebridgeCall.handset = sVDISHandsetNo;
		voicebridgeCall.speaker = sVDISSpeakerNo;

		setCurrentCall(voicebridgeCall);

        voicebridgeCall.setState("CallEstablished");

        if("I".equals(sVDISOldLineState))
        {
            voicebridgeCall.setState("CallOriginated");
        }
        else

        if("H".equals(sVDISOldLineState) || "F".equals(sVDISOldLineState))
        {
            if (voicebridgeCall.getCallProgress() != null)
            {
				if ("Outgoing".equals(voicebridgeCall.direction))
				{
					if("0".equals(voicebridgeCall.getCallProgress()))
					{
						if("D".equals(getInterest().getInterestType()))
						{
							voicebridgeCall.setState("CallDelivered");

						} else
							voicebridgeCall.setState("CallEstablished");

					} else if("4".equals(voicebridgeCall.getCallProgress()) || "1".equals(voicebridgeCall.getCallProgress()) || "2".equals(voicebridgeCall.getCallProgress())) {

						voicebridgeCall.setState("CallEstablished");

					} else
						voicebridgeCall.setState("CallFailed");
				}

			} else {

				if ("CallHeld".equals(voicebridgeCall.getState()) && "Outgoing".equals(voicebridgeCall.direction))
				{
				 voicebridgeCall.setState("CallOriginated");
			 	}
			}
        }

		if (voicebridgeCall.localConferenced)
		{
        	voicebridgeCall.setState("CallConferenced");
		}

        voicebridgeCall.setValidActions();

		if("CallEstablished".equals(voicebridgeCall.getState()))
		{
        	voicebridgeCall.startDuration();
		}

        voicebridgeCall.participation = "Active";
		voicebridgeCall.startParticipation();
    }

	public VoiceBridgeCall getCurrentCall(String handset)
	{
		VoiceBridgeCall voicebridgeCall = null;

		VoiceBridgeCall intercomCall = getUser().getCurrentICMCall();
		VoiceBridgeCall voicebridgeCall1 = getUser().getCurrentHS1Call();
		VoiceBridgeCall voicebridgeCall2 = getUser().getCurrentHS2Call();

		if ("1".equals(handset))
			voicebridgeCall = voicebridgeCall1;

		else if ("2".equals(handset))
			voicebridgeCall = voicebridgeCall2;

		else if ("0".equals(handset))
			voicebridgeCall = intercomCall;

		else if ("3".equals(handset))
			voicebridgeCall = voicebridgeCall1;

        return voicebridgeCall;
	}

	private void setCurrentCall(VoiceBridgeCall voicebridgeCall)
	{
		Log.debug("setCurrentCall " + voicebridgeCall.getCallID() + " " + voicebridgeCall.handset + " " + voicebridgeCall.speaker);

        voicebridgeCall.console = getUser().getDeviceNo();

        if ("1".equals(voicebridgeCall.handset))
        	getUser().setCurrentHS1Call(voicebridgeCall);

        else if ("2".equals(voicebridgeCall.handset))
        	getUser().setCurrentHS2Call(voicebridgeCall);

        else if ("65".equals(voicebridgeCall.speaker))
        	getUser().setCurrentICMCall(voicebridgeCall);

        else if ("3".equals(voicebridgeCall.handset))
        	getUser().setCurrentHS1Call(voicebridgeCall);
	}

    public void handleCallOutgoingBusy(String sCallNo, String sVDISVoiceBridgeLineNo)
    {
		// call busy, but previously busy

        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.start();
			voicebridgeCall.line = sVDISVoiceBridgeLineNo;
			voicebridgeCall.setState("CallBusy");
			voicebridgeCall.delivered = true;
			voicebridgeCall.setValidActions();

			voicebridgeCall.participation = "Inactive";
			voicebridgeCall.setStatus("Actions");
		}
    }

    public void handleCallIncomingBusy(String sCallNo, String sVDISVoiceBridgeLineNo)
    {
		// call busy, but previously ringing

        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.start();
			voicebridgeCall.line = sVDISVoiceBridgeLineNo;
			voicebridgeCall.setState("CallBusy");
			voicebridgeCall.delivered = true;
			voicebridgeCall.setValidActions();
			voicebridgeCall.participation = "Inactive";
		}
    }


    public void handleConnectionBusy(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISOldLineState, String sVDISNewLineState,
            String sVDISHandsetOrSpeaker, String sVDISSpeakerNo, String sVDISHandsetNo, String sVDISConnectOrDisconnect)
    {
		// call busy, but previously idle

        VoiceBridgeCall voicebridgeCall = createCallByNo(sCallNo);
        voicebridgeCall.start();
        voicebridgeCall.line = sVDISVoiceBridgeLineNo;
        voicebridgeCall.setState("ConnectionBusy");
        voicebridgeCall.delivered = false;
        voicebridgeCall.setValidActions();
        voicebridgeCall.participation = "Inactive";
    }


    public void handleCallInactive(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISOldLineState, String sVDISNewLineState,
            String sVDISHandsetOrSpeaker, String sVDISSpeakerNo, String sVDISHandsetNo, String sVDISConnectOrDisconnect)
    {
		// call busy, but previously conferenced

        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.start();

			voicebridgeCall.line = sVDISVoiceBridgeLineNo;
			voicebridgeCall.label = sVDISVoiceBridgeLineName;
			voicebridgeCall.setState("CallBusy");
			voicebridgeCall.delivered = true;
			voicebridgeCall.setValidActions();
			voicebridgeCall.participation = "Inactive";

			voicebridgeCall.endDuration();	// create new duration record
		}
    }


    public void handleCallFailed(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISOldLineState, String sVDISNewLineState,
            String sVDISHandsetOrSpeaker, String sVDISSpeakerNo, String sVDISHandsetNo, String sVDISConnectOrDisconnect)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.clear();

			voicebridgeCall.line = sVDISVoiceBridgeLineNo;
			voicebridgeCall.label = sVDISVoiceBridgeLineName;
			voicebridgeCall.setState("CallFailed");
			voicebridgeCall.setValidActions();
			voicebridgeCall.participation = "Inactive";
		}
    }

    public void handleCallHeld(String sCallNo, String sVDISVoiceBridgeLineNo)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			if(voicebridgeCall.voicebridgeTransferFlag)			// internal transfer puts call on hold
			{
				voicebridgeCall.setState("CallDelivered");
				voicebridgeCall.voicebridgeTransferFlag = false;

			} else {

				voicebridgeCall.setState("CallHeld");
			}
			voicebridgeCall.setValidActions();
			voicebridgeCall.participation = "Inactive";
			voicebridgeCall.endDuration();
		}
    }

    public void handleCallHeldElsewhere(String sCallNo, String sVDISVoiceBridgeLineNo)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.setState("CallHeldElsewhere");
			voicebridgeCall.setValidActions();
			voicebridgeCall.participation = "Inactive";
			voicebridgeCall.endDuration();
		}
    }

    public void handleTransfer(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sTransferUserNo)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.voicebridgeTransferFlag = true;
		}
    }

    public void handleIntercom(String sCallNo)
    {
        VoiceBridgeCall voicebridgeCall = createCallByNo(sCallNo);
        voicebridgeCall.platformIntercom = true;
        voicebridgeCall.setState("CallEstablished");
        voicebridgeCall.setValidActions();

        getUser().setIntercom(true);
    }

    public void handleCallProgress(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISChannelNo, String sVDISVoiceBridgeFlag)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.setCallProgress(sVDISVoiceBridgeFlag);

			if ("Outgoing".equals(voicebridgeCall.direction))
			{
				if("CallOriginated".equals(voicebridgeCall.getState()))
				{
					if("0".equals(sVDISVoiceBridgeFlag))
					{
						if("D".equals(getInterest().getInterestType()))
						{
							voicebridgeCall.setState("CallDelivered");

						} else
							voicebridgeCall.setState("CallEstablished");

					} else if("4".equals(sVDISVoiceBridgeFlag) || "1".equals(sVDISVoiceBridgeFlag)) {

						voicebridgeCall.setState("CallEstablished");

					} else
						voicebridgeCall.setState("CallFailed");
				} else

				if("CallDelivered".equals(voicebridgeCall.getState()))
				{
					if("2".equals(sVDISVoiceBridgeFlag))
					{
						voicebridgeCall.setState("CallEstablished");

					} else if("4".equals(sVDISVoiceBridgeFlag) || "1".equals(sVDISVoiceBridgeFlag)) {

						voicebridgeCall.setState("CallEstablished");

					} else
						voicebridgeCall.setState("CallFailed");
				}

				voicebridgeCall.setValidActions();
			}

			if("CallEstablished".equals(voicebridgeCall.getState()))
			{
				voicebridgeCall.startDuration();
			}
		}
    }

    public void handleCallProceeding(VoiceBridgeComponent component, String sCallNo, String sVDISVoiceBridgeLineNo, String sDigits, String sEndFlag)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.proceedingDigitsBuffer = (new StringBuilder()).append(voicebridgeCall.proceedingDigitsBuffer).append(sDigits.trim()).toString();

			if("Y".equals(sEndFlag) && !"".equals(voicebridgeCall.proceedingDigitsBuffer))
			{
				voicebridgeCall.proceedingDigits = voicebridgeCall.proceedingDigitsBuffer;
				voicebridgeCall.proceedingDigitsBuffer = "";
				voicebridgeCall.proceedingDigitsLabel = voicebridgeCall.proceedingDigits;

				String cononicalNumber = voicebridgeCall.proceedingDigits;

				try
				{
					cononicalNumber = component.formatCanonicalNumber(voicebridgeCall.proceedingDigits);
				}
				catch(Exception e) { }

				if(component.voicebridgeLdapService.cliLookupTable.containsKey(cononicalNumber))
					voicebridgeCall.proceedingDigitsLabel = (String)component.voicebridgeLdapService.cliLookupTable.get(cononicalNumber);
			}
		}
    }

    public void handleCallCLI(String sCallNo, String sVDISVoiceBridgeLineNo, String cliDigits, String cliLabel)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			voicebridgeCall.setCLI(cliDigits);
			voicebridgeCall.setCLILabel(cliLabel);
		}
    }

    public void handleCallMoved(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineNo2)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
        	voicebridgeCall.line = sVDISVoiceBridgeLineNo2;
		}
    }

    public void handleRecallTransfer(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineNo2, String transferStatusFlag)
    {
        VoiceBridgeCall voicebridgeCall = getCallByNo(sCallNo);

        if (voicebridgeCall != null)
        {
			if("0".equals(transferStatusFlag))
			{
				voicebridgeCall.setState("CallTransferring");
				voicebridgeCall.transferFlag = false;

			} else if("1".equals(transferStatusFlag)) {

				voicebridgeCall.setState("CallEstablished");
				voicebridgeCall.transferFlag = false;

				if (voicebridgeCall.previousCalledNumber != null)
				{
					Iterator<VoiceBridgeUserInterest> it3 = getInterest().getUserInterests().values().iterator();

					while( it3.hasNext() )
					{
						VoiceBridgeUserInterest theUserInterest = (VoiceBridgeUserInterest)it3.next();
						VoiceBridgeCall theCall = theUserInterest.getCallByLine(voicebridgeCall.getLine());

						if (theCall != null)
						{
							theCall.proceedingDigits = voicebridgeCall.previousCalledNumber;
							theCall.proceedingDigitsLabel = voicebridgeCall.previousCalledLabel;
						}
					}
				}

			} else if("2".equals(transferStatusFlag)) {

				voicebridgeCall.setState("CallTransferred");

			}

			voicebridgeCall.setValidActions();
		}
    }

    public String getDefault()
    {
        return defaultInterest;
    }

    public void setDefault(String defaultInterest)
    {
        this.defaultInterest = defaultInterest;
    }

    public VoiceBridgeUser getUser()
    {
        return voicebridgeUser;
    }

    public void setUser(VoiceBridgeUser voicebridgeUser)
    {
        this.voicebridgeUser = voicebridgeUser;
    }

    public Map<String, VoiceBridgeSubscriber> getSubscribers()
    {
        return voicebridgeSubscribers;
    }

    public void setSubscribers(Map<String, VoiceBridgeSubscriber> voicebridgeSubscribers)
    {
        this.voicebridgeSubscribers = voicebridgeSubscribers;
    }

    public boolean isSubscribed(JID subscriber)
    {
        return voicebridgeSubscribers.containsKey(subscriber.getNode());
    }


    public VoiceBridgeSubscriber getSubscriber(JID subscriber)
    {
        VoiceBridgeSubscriber voicebridgeSubscriber = null;

        if (voicebridgeSubscribers.containsKey(subscriber.getNode()))
        {
            voicebridgeSubscriber = (VoiceBridgeSubscriber)voicebridgeSubscribers.get(subscriber.getNode());
        } else
        {
            voicebridgeSubscriber = new VoiceBridgeSubscriber();
            voicebridgeSubscriber.setJID(subscriber);
            voicebridgeSubscribers.put(subscriber.getNode(), voicebridgeSubscriber);
        }
        return voicebridgeSubscriber;
    }

    public void removeSubscriber(JID subscriber)
    {
		voicebridgeSubscribers.remove(subscriber.getNode());
	}

    public boolean canPublish(VoiceBridgeComponent component)
    {
		if (voicebridgeSubscribers.size() == 0)
		{
			return false;
		}

		boolean anySubscriberOnline = false;

		Iterator<VoiceBridgeSubscriber> iter = voicebridgeSubscribers.values().iterator();

		while( iter.hasNext() )
		{
			VoiceBridgeSubscriber subscriber = (VoiceBridgeSubscriber)iter.next();

			if (subscriber.getOnline() || component.isComponent(subscriber.getJID()))
			{
				anySubscriberOnline = true;
				break;
			}
		}

		return anySubscriberOnline;
    }

    public VoiceBridgeInterest getInterest()
    {
        return voicebridgeInterest;
    }

    public void setInterest(VoiceBridgeInterest voicebridgeInterest)
    {
        this.voicebridgeInterest = voicebridgeInterest;
    }

	public synchronized VoiceBridgeCall createCallByNo(String callNo)
	{
        if(voicebridgeCalls.containsKey(callNo))
        {
			return (VoiceBridgeCall)voicebridgeCalls.get(callNo);

		} else {

			String callID = callNo + getInterestName();
        	return createCallById(callID);
		}
	}


    public synchronized VoiceBridgeCall createCallById(String callID)
    {
		Log.debug("createCallById " + callID);

        VoiceBridgeCall voicebridgeCall = null;

        if(voicebridgeCalls.containsKey(callID))
        {
            voicebridgeCall = (VoiceBridgeCall)voicebridgeCalls.get(callID);

        } else {

            voicebridgeCall = new VoiceBridgeCall();
            voicebridgeCall.callid = callID;

			if("D".equals(getInterest().getInterestType()))	// set default caller ID for directory numbers
			{
				voicebridgeCall.ddi = getInterest().getInterestValue();
				voicebridgeCall.ddiLabel = getInterest().getInterestLabel();
			}

            voicebridgeCall.initialiseDuration();

            voicebridgeCalls.put(callID, voicebridgeCall);
        }
        return voicebridgeCall;
    }


    public VoiceBridgeCall getCallByNo(String callNo)
    {
		String callID = callNo + getInterestName();
        return getCallById(callID);
	}

    public VoiceBridgeCall getCallById(String callID)
    {
        VoiceBridgeCall voicebridgeCall = null;

        if(voicebridgeCalls.containsKey(callID))
        {
            voicebridgeCall = (VoiceBridgeCall)voicebridgeCalls.get(callID);
		}
        return voicebridgeCall;
	}

	public VoiceBridgeCall getCallByLine(String line)
	{
        VoiceBridgeCall lineCall = null;

		Iterator it2 = voicebridgeCalls.values().iterator();

		while( it2.hasNext() )
		{
			VoiceBridgeCall voicebridgeCall = (VoiceBridgeCall)it2.next();

			if (line.equals(voicebridgeCall.line))
			{
				lineCall = voicebridgeCall;
				break;
			}
		}

		return  lineCall;
	}

    public void removeCallByNo(String callNo)
    {
		String callID = callNo + getInterestName();
		removeCallById(callID);
    }

    public void removeCallById(String callID)
    {
        if(voicebridgeCalls.containsKey(callID))
        {
            VoiceBridgeCall voicebridgeCall = (VoiceBridgeCall)voicebridgeCalls.get(callID);
            voicebridgeCall = null;
            voicebridgeCalls.remove(callID);
        }
	}

    public Map<String, VoiceBridgeCall> getCalls()
    {
        return voicebridgeCalls;
    }


	public boolean isLineActive(String line)
	{
		VoiceBridgeCall voicebridgeCall1 = getUser().getCurrentHS1Call();
		VoiceBridgeCall voicebridgeCall2 = getUser().getCurrentHS2Call();

		boolean active = false;

		if (voicebridgeCall1 != null && !"".equals(voicebridgeCall1.line))
		{
			if (line.equals(String.valueOf(Long.parseLong(voicebridgeCall1.line))))
				active = true;
		}

		if (voicebridgeCall2 != null && !"".equals(voicebridgeCall2.line))
		{
			if (line.equals(String.valueOf(Long.parseLong(voicebridgeCall2.line))))
				active = true;
		}

		return active;
	}

	public boolean getHandsetBusyStatus()
	{
		VoiceBridgeCall voicebridgeCall1 = getUser().getCurrentHS1Call();
		VoiceBridgeCall voicebridgeCall2 = getUser().getCurrentHS2Call();

		if (voicebridgeCall1 == null && voicebridgeCall2 == null)
			return false;

		Iterator it2 = voicebridgeCalls.values().iterator();
		boolean busy1 = false;
		boolean busy2 = false;

		while( it2.hasNext() )
		{
			VoiceBridgeCall voicebridgeCall = (VoiceBridgeCall)it2.next();

			if (voicebridgeCall1 != null && voicebridgeCall.getCallID().equals(voicebridgeCall1.getCallID()))
			{
				busy1 = true;
			}

			if (voicebridgeCall2 != null && voicebridgeCall.getCallID().equals(voicebridgeCall2.getCallID()))
			{
				busy2 = true;
			}
		}

		return  busy1 && busy2;
	}

	public int getActiveCalls()
	{
		Iterator it2 = voicebridgeCalls.values().iterator();
		int calls = 0;

		while( it2.hasNext() )
		{
			VoiceBridgeCall voicebridgeCall = (VoiceBridgeCall)it2.next();

			if (! "ConnectionCleared".equals(voicebridgeCall.getState()))
			{
				calls++;
			}
		}

		return calls;
	}

	public boolean getBusyStatus()
	{
		return (getActiveCalls() >= getMaxNumCalls());
	}

	public String getCallFWD() {
		return callFWD;
	}

	public void setCallFWD(String callFWD) {
		this.callFWD = callFWD;
	}

	public String getCallFWDDigits() {
		return callFWDDigits;
	}

	public void setCallFWDDigits(String callFWDDigits) {
		this.callFWDDigits = callFWDDigits;
	}

	public void setMaxNumCalls(int maxNumCalls) {
		this.maxNumCalls = maxNumCalls;
	}

	public int getMaxNumCalls() {
		return maxNumCalls;
	}

    public void logCall(VoiceBridgeCall voicebridgeCall, String domain, long site)
    {
		String callId =  voicebridgeCall.getCallID();
		String tscId = "voicebridge" + site + "." + domain;

		if ("CallMissed".equals(voicebridgeCall.getState()) || "ConnectionCleared".equals(voicebridgeCall.getState()))
		{
			Log.debug("writing call record " + callId);

			//CallLogger.getLogger().logCall(tscId, callId, getUser().getProfileName(), getInterestName(), voicebridgeCall.getState(), voicebridgeCall.direction, voicebridgeCall.creationTimeStamp, voicebridgeCall.getDuration(), voicebridgeCall.getCallerNumber(getInterest().getInterestType()), voicebridgeCall.getCallerName(getInterest().getInterestType()), voicebridgeCall.getCalledNumber(getInterest().getInterestType()), voicebridgeCall.getCalledName(getInterest().getInterestType()));

			Iterator it3 = getInterest().getUserInterests().values().iterator();

			while( it3.hasNext() )
			{
				VoiceBridgeUserInterest voicebridgeParticipant = (VoiceBridgeUserInterest)it3.next();

				VoiceBridgeCall participantCall = voicebridgeParticipant.getCallByLine(voicebridgeCall.getLine());

				if (participantCall != null)
				{
					Log.debug("writing call participant record " + callId + " " + voicebridgeParticipant.getUser().getUserId());

					if (("Active".equals(participantCall.firstParticipation) && "ConnectionCleared".equals(participantCall.getState())))
					{
						//CallLogger.getLogger().logParticipant(tscId, callId, voicebridgeParticipant.getUser().getUserId() + "@" + voicebridgeParticipant.getUser().getUserNo() + "." + domain, participantCall.direction, participantCall.firstParticipation, participantCall.firstTimeStamp, participantCall.getDuration());
					}

					if ("CallMissed".equals(participantCall.getState()))
					{
						//CallLogger.getLogger().logParticipant(tscId, callId, voicebridgeParticipant.getUser().getUserId() + "@" + voicebridgeParticipant.getUser().getUserNo() + "." + domain, participantCall.direction, participantCall.firstParticipation, participantCall.creationTimeStamp, participantCall.getRingDuration());
					}

				}
			}
		}
    }
}
