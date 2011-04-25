package com.ifsoft.iftalk.plugin.voicebridge;

import java.util.*;
import org.dom4j.*;

import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.http.HttpBindManager;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.JiveGlobals;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.jivesoftware.openfire.cluster.ClusterManager;

import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import com.ifsoft.iftalk.plugin.tsc.*;
import com.ifsoft.iftalk.plugin.tsc.voicemessage.message.VMessage;

import org.red5.server.webapp.voicebridge.*;

public class VoiceBridgeLinkService extends AbstractLinkService
{
	private long siteID;
	private String siteName;
	private String theVoiceBridgelinkCOS;

	private Map<String, VoiceBridgeInterest> lineInterests;
	private Map<String, VoiceBridgeUserInterest> userInterests;
	private Map<String, VoiceBridgeCallback> callbacks;
	private Map<String, VoiceBridgeUser> externalCalls;
	private Map<String, VoiceBridgeUser> callbackUserTable;
	private Map<String, String> voicebridgeCallIds;
	private Map<String, String> voicebridgeCallLines;
	private Map<String, TimerCheck> voicebridgeTimeOuts;

	private boolean voicebridgeConnected = false;
	private String voicebridgeLinkVersion = null;
    private ComponentManager componentManager;

    public Site site;
	public VoiceBridgeComponent component;

    private Random randomNumberGenerator;

    private Timer timer = new Timer();
    private TimerCheck timerCheck = null;

    private List<String> sipPeers;


	public VoiceBridgeLinkService(VoiceBridgeComponent component, Site site)
	{
		Log.info( "["+ site.getName() + "] VoiceBridgeLinkService");

		this.component 			= component;
		this.site 				= site;
        this.siteID    			= site.getSiteID();
        this.siteName  			= site.getName();

		lineInterests 			= new HashMap<String, VoiceBridgeInterest>();
		userInterests 			= new HashMap<String, VoiceBridgeUserInterest>();
		callbacks 				= new HashMap<String, VoiceBridgeCallback>();
		externalCalls			= new HashMap<String, VoiceBridgeUser>();
		callbackUserTable		= new HashMap<String, VoiceBridgeUser>();
		voicebridgeCallIds 		= new HashMap<String, String>();
		voicebridgeCallLines	= new HashMap<String, String>();
		voicebridgeTimeOuts		= new HashMap<String, TimerCheck>();

		sipPeers 				= new ArrayList<String>();
		randomNumberGenerator 	= new Random();

 		componentManager = ComponentManagerFactory.getComponentManager();
	}

	public void startup()
	{
        try {
			Log.info("["+ siteName + "] startup");

        }
        catch(Exception e) {
        	Log.error(e.getMessage());
        }
	}



	public void logoutFromVoiceBridgeLink()
	{
		Log.info( "["+ siteName + "] closing connection to VoiceBridgeLink...");

		try {

		}
		catch(Exception e) {
        	Log.error(e.getMessage());
        }
	}


//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


    public String manageCallParticipant(JID userJID, String uid, String parameter, String value)
    {
		//return Application.getInstance().manageCallParticipant(userJID, uid, parameter, value);

		return null;
	}

    public void handleMessage(Message received)
    {
		//Application.getInstance().handleMessage(received);
    }

	public void interceptMessage(Message received)
	{
		//Application.getInstance().interceptMessage(received);

	}

	public void handlePostBridge(List<String> uids)
	{
		//Application.getInstance().handlePostBridge(uids);
	}


//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	public void restoreCallState(String sVDISVoiceBridgeLineNo, String sCallNo)
	{
		voicebridgeCallIds.put(sVDISVoiceBridgeLineNo, sCallNo);
		voicebridgeCallLines.put(sCallNo, sVDISVoiceBridgeLineNo);

		requestLineInfo(sVDISVoiceBridgeLineNo);
	}


	public boolean isLinkAlive()
	{
		return voicebridgeConnected;
	}

	public String getLinkVersion()
	{
		return voicebridgeLinkVersion;
	}



	public void reconnect(String hostName)
	{
		Log.info("["+ siteName + "]  reconnect connection to VoiceBridgeLink...");

		try {
			voicebridgeConnected = true;


			if (!component.voicebridgeLdapService.isLdapAlive())
			{
				Log.info(siteName + " doing re-connection to VoiceBridgeLdap as well");
				component.voicebridgeLdapService.connect();
			}
		}
		catch(Exception e) {
			Log.error("["+ siteName + "]  reconnect " + e);
			voicebridgeConnected = false;
        }
	}

	public void logMessage(String msg) {
		Log.debug("["+ siteName + "] " + msg);
	}

	public void logError(String msg) {
		voicebridgeConnected = false;
		Log.error("["+ siteName + "] " + msg);
	}

	public void allocateCallbacks()
	{
		Log.info("["+ siteName + "]  allocateCallbacks ");

		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "]  allocateCallbacks " + e);
        }
	}

	public void freeCallbacks()
	{
		Log.info("["+ siteName + "]  freeCallbacks ");

		try {
			Iterator<VoiceBridgeCallback> it = callbacks.values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeCallback voicebridgeCallback = (VoiceBridgeCallback)it.next();

				if (voicebridgeCallback.getVoiceBridgeUser() != null && voicebridgeCallback.getVoiceBridgeUser().getCallbackActive())
				{
					clearCall(voicebridgeCallback.getVirtualDeviceId(), voicebridgeCallback.getRemoteHandset());
					clearCall(voicebridgeCallback.getVirtualDeviceId(), voicebridgeCallback.getLocalHandset());
				}
			}


		}
		catch(Exception e) {
			Log.error("["+ siteName + "]  freeCallbacks " + e);
        }
	}

	private void createCallback(String start, String count)
	{
		Log.debug("["+ siteName + "]  createCallback " + start + " " + count);

		try {

			if (!"".equals(start) && !"".equals(count))
			{
				int vscStart = Integer.parseInt(start);
				int vscCount = Integer.parseInt(count);

				for (int i = 0; i < vscCount; i++)
				{
					VoiceBridgeCallback voicebridgeCallback = new VoiceBridgeCallback();
					voicebridgeCallback.setRemoteHandset("2");
					voicebridgeCallback.setLocalHandset("1");
					voicebridgeCallback.setVirtualDeviceId(format10digits(String.valueOf(vscStart + i)));
					voicebridgeCallback.setVirtualUserId(format12digits(String.valueOf(vscStart + i + 1000)));

					String callbackKey = voicebridgeCallback.getVirtualDeviceId() + voicebridgeCallback.getRemoteHandset();

					if (!callbacks.containsKey(callbackKey))
					{
						callbacks.put(callbackKey, voicebridgeCallback);
						Log.debug("["+ siteName + "]  createCallback " + callbackKey);
					}
				}
			}

		}
		catch(Exception e) {
			Log.error("["+ siteName + "]  createCallback " + e);
        }
	}

	public boolean isCallbackAvailable()
	{
		return callbacks.size() > 0;
	}


	private boolean isVoiceDropAvailable()
	{
		return component.voicebridgeVmsService.isTelephonyServerConnected();
	}

	public Map<String, VoiceBridgeCallback> getCallbacks()
	{
		return callbacks;
	}

	public VoiceBridgeCallback getCallback(VoiceBridgeUser voicebridgeUser)
	{
		Log.debug("["+ siteName + "]  getCallback " + voicebridgeUser.getUserNo());

		VoiceBridgeCallback theCallback = null;

		try {

			Iterator<VoiceBridgeCallback> it = callbacks.values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeCallback voicebridgeCallback = (VoiceBridgeCallback)it.next();

				if (voicebridgeCallback.getVoiceBridgeUser() != null && voicebridgeUser.getUserNo().equals(voicebridgeCallback.getVoiceBridgeUser().getUserNo()))
				{
					theCallback = voicebridgeCallback;
					break;
				}
			}
		}
		catch(Exception e) {
			Log.error("["+ siteName + "]  getCallback " + e);
        }

		return theCallback;
	}


	public VoiceBridgeCallback allocateCallback(VoiceBridgeUser voicebridgeUser)
	{
		Log.debug("["+ siteName + "]  allocateCallback " + voicebridgeUser.getUserNo());

		VoiceBridgeCallback freeCallback = null;

		try {

			Iterator<VoiceBridgeCallback> it = callbacks.values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeCallback voicebridgeCallback = (VoiceBridgeCallback)it.next();

				if (voicebridgeCallback.getVoiceBridgeUser() == null || voicebridgeUser.getUserNo().equals(voicebridgeCallback.getVoiceBridgeUser().getUserNo()))
				{
					voicebridgeCallback.setVoiceBridgeUser(voicebridgeUser);
					voicebridgeCallback.setTimestamp(new Date());
					voicebridgeUser.setPhoneCallback(voicebridgeCallback);
					activateCallback(voicebridgeCallback);

					if (voicebridgeUser.getUserType().equals("VoiceBridge"))			// real user
					{
						callbackUserTable.put(voicebridgeCallback.getVirtualUserId(), voicebridgeUser);
						//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_PHANTOM_LOGIN + format10digits(voicebridgeCallback.getVirtualDeviceId()) + format12digits(voicebridgeCallback.getVoiceBridgeUser().getUserNo()) + voicebridgeCallback.getLocalHandset() + "002");
					}

					freeCallback = voicebridgeCallback;
					break;
				}
			}
		}
		catch(Exception e) {
			Log.error("["+ siteName + "]  allocateCallback " + e);
        }

		return freeCallback;
	}


	public String addExternalCall(String line, String destination)
	{
		Log.debug("["+ siteName + "]  addExternalCall " + line + " " + destination);

		String message = null;

		VoiceBridgeUser voicebridgeUser = new VoiceBridgeUser();
		voicebridgeUser.setUserType("VSC");
		voicebridgeUser.setUserName(destination);
		voicebridgeUser.setUserId("");
		voicebridgeUser.setUserNo(String.valueOf(System.currentTimeMillis()));
		voicebridgeUser.setSiteName(siteName);
		voicebridgeUser.setSiteID(siteID);
		voicebridgeUser.setHandsetNo("0");
		voicebridgeUser.setCallback(destination);
		voicebridgeUser.setVSCLine(line);

		if (!externalCalls.containsKey(destination+line))
		{
			VoiceBridgeCallback voicebridgeCallback = allocateCallback(voicebridgeUser);

			if (voicebridgeCallback != null)
			{
				selectLine(line, voicebridgeCallback.getVirtualDeviceId(), voicebridgeCallback.getLocalHandset());
				externalCalls.put(destination+line, voicebridgeUser);

			} else message = "unable to allocate a virtual turret";

		} else message = "destination in use";

		return message;
	}


	public String removeExternalCall(String line, String destination)
	{
		Log.debug("["+ siteName + "]  removeExternalCall " + line + " " + destination);

		String message = null;

		if (externalCalls.containsKey(destination+line))
		{
			VoiceBridgeUser voicebridgeUser = externalCalls.get(destination+line);
			freeCallback(voicebridgeUser.getUserNo());
			voicebridgeUser = null;
			externalCalls.remove(destination+line);

		} else message = "call removed or cleared";

		return message;
	}


	public void activateCallback(VoiceBridgeCallback voicebridgeCallback)
	{
		if (voicebridgeCallback.getVoiceBridgeUser() != null)
		{

		}
	}


	public void freeCallback(String userNo)
	{
		Log.debug("["+ siteName + "]  freeCallback " + userNo);

		try {

			Iterator<VoiceBridgeCallback> it = callbacks.values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeCallback voicebridgeCallback = (VoiceBridgeCallback)it.next();

				if (voicebridgeCallback.getVoiceBridgeUser() != null && userNo.equals(voicebridgeCallback.getVoiceBridgeUser().getUserNo()))
				{
					if (voicebridgeCallback.getVoiceBridgeUser().getUserType().equals("VoiceBridge"))			// real user
					{
						callbackUserTable.remove(voicebridgeCallback.getVirtualUserId());
						//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_PHANTOM_LOGOFF + format10digits(voicebridgeCallback.getVirtualDeviceId()) + format12digits(voicebridgeCallback.getVoiceBridgeUser().getUserNo()) + voicebridgeCallback.getLocalHandset() + "002");
					}

					voicebridgeCallback.getVoiceBridgeUser().setCallbackActive(false);
					voicebridgeCallback.getVoiceBridgeUser().setPhoneCallback(null);
					voicebridgeCallback.setVoiceBridgeUser(null);
					voicebridgeCallback.setTimestamp(null);

					clearCall(voicebridgeCallback.getVirtualDeviceId(), voicebridgeCallback.getRemoteHandset());
					clearCall(voicebridgeCallback.getVirtualDeviceId(), voicebridgeCallback.getLocalHandset());

					break;
				}
			}
		}
		catch(Exception e) {
			Log.error("["+ siteName + "]  freeCallback " + e);
        }
	}


	private void handleVoiceConnectChange(VoiceBridgeCallback voicebridgeCallback, String sVDISVoiceBridgeLineNo, String sVDISOldLineState, String sVDISNewLineState,  String sVDISConnectOrDisconnect)
	{
		Log.debug("["+ siteName + "]  handleVoiceConnectChange " + voicebridgeCallback.getVoiceBridgeUser().getUserNo() + " " + sVDISVoiceBridgeLineNo + " " + sVDISOldLineState + " " + sVDISNewLineState  + " " + sVDISConnectOrDisconnect);

		try {

			if (("C".equals(sVDISNewLineState) || "A".equals(sVDISNewLineState)) && "C".equals(sVDISConnectOrDisconnect) && !"F".equals(sVDISOldLineState) && !voicebridgeCallback.getVoiceBridgeUser().getCallbackActive())
			{
				// got a line, now dial

				//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_DIAL_STRING + sVDISVoiceBridgeLineNo + pad(voicebridgeCallback.getVoiceBridgeUser().getCallback(), 30));
				voicebridgeCallback.getVoiceBridgeUser().setCallbackActive(true);
			}

			if ("I".equals(sVDISNewLineState) && "D".equals(sVDISConnectOrDisconnect))
			{
				// callback disconnected, termnate call

				VoiceBridgeUser voicebridgeUser = voicebridgeCallback.getVoiceBridgeUser();

				if (voicebridgeUser != null)
				{
					String destination = voicebridgeUser.getCallback();
					String line = voicebridgeUser.getVSCLine();

					if (line != null && destination != null && externalCalls.containsKey(destination+line))
					{
						freeCallback(voicebridgeUser.getUserNo());
						voicebridgeUser = null;
						externalCalls.remove(destination+line);

					} else {
						voicebridgeCallback.getVoiceBridgeUser().setCallbackActive(false);
					}

					clearCall(voicebridgeCallback.getVirtualDeviceId(), voicebridgeCallback.getLocalHandset());

				} else {

					Log.warn("["+ siteName + "]  handleVoiceConnectChange no user for VSC " + voicebridgeCallback.getVirtualDeviceId());
				}
			}
		}
		catch(Exception e) {
			Log.error("["+ siteName + "]  handleVoiceConnectChange " + e);
        }
	}



	public void handleVDISMessage(Object source, String sVDISEventMessage)
	{
/*
		String sCallNo 					= null;
		String sVDISVoiceBridgeLineNo 			= null;
		String sVDISVoiceBridgeLineNo2 			= null;
		String sVDISVoiceBridgeLineName 		= null;
		String sVDISVoiceBridgeConsoleNo 		= null;
		String sVDISVoiceBridgeUserNo 			= null;
		String sClientId 				= null;
		String sVDISHandsetOrSpeaker 	= null;
		String sVDISSpeakerNo 			= null;
		String sVDISHandsetNo 			= null;
		String sVDISVoiceBridgeFlag 			= null;
		String sVDISVoiceBridgeCallset 			= null;
		String sVDISVoiceBridgeFunction 		= null;
		String sVDISVoiceBridgeGroup 			= null;
		String sVDISVoiceBridgeKeystate 		= null;
		String sRealVDISDDI 			= null;
		String sVDISDDI 				= null;

		try {
			String sVDISMessageId = sVDISEventMessage.substring(1,4);
			int nVDISMessageId = Integer.parseInt(sVDISMessageId);
			int nVDISMessagePayloadLength = Integer.parseInt(sVDISEventMessage.substring(4,6));
			String sVDISMessagePayload = sVDISEventMessage.substring(6);

			if (nVDISMessagePayloadLength > 0 )
			{
				if (sVDISMessagePayload.indexOf("\\") > -1) {
					sVDISMessagePayload = oldReplace(sVDISMessagePayload, "\\", "\\\\");
				}
			}

			if (!voicebridgeConnected) {
				voicebridgeConnected = true;
				Log.info( "["+ siteName + "] Connected VoiceBridgelink for " + siteName);
			}

			if (component.voicebridgeLdapService == null)
			{
				return;
			}

			switch( nVDISMessageId )
			{
				case VDIS_SERVER_MSG_ID_HEARTBEAT:
						deleteExpiredCalls();
						break;

    			case VDIS_SERVER_MSG_ID_VERSION:
    					voicebridgeLinkVersion = String.valueOf(Integer.parseInt(sVDISMessagePayload.substring(0, 3))) + "." + String.valueOf(Integer.parseInt(sVDISMessagePayload.substring(3, 6))) + "." + String.valueOf(Integer.parseInt(sVDISMessagePayload.substring(6, 9))) + " " + sVDISMessagePayload.substring(9, 10);
    					break;

				case VDIS_SERVER_MSG_ID_INCOMING:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);
						sCallNo = makeCallNo(sVDISVoiceBridgeLineNo);
						voicebridgeCallIds.put(sVDISVoiceBridgeLineNo, sCallNo);
						voicebridgeCallLines.put(sCallNo, sVDISVoiceBridgeLineNo);

						sVDISVoiceBridgeLineName = sVDISMessagePayload.substring(20, 40).trim();
						sRealVDISDDI = sVDISMessagePayload.substring(40, 52);
						sVDISDDI = sVDISMessagePayload.substring(52, 57);
						handleCallIncoming(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sRealVDISDDI, sVDISDDI);
						break;

				case VDIS_SERVER_MSG_ID_ABANDONED:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

							handleCallAbandoned(sCallNo, sVDISVoiceBridgeLineNo);

						} else {

							Log.warn( "["+ siteName + "] Call abandoned. cannot find call id for line " + sVDISVoiceBridgeLineNo);
						}
						break;

				case VDIS_SERVER_MSG_ID_CONNECT_CHANGE:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

						} else {

							sCallNo = makeCallNo(sVDISVoiceBridgeLineNo);
							voicebridgeCallIds.put(sVDISVoiceBridgeLineNo, sCallNo);
							voicebridgeCallLines.put(sCallNo, sVDISVoiceBridgeLineNo);
						}

						sVDISVoiceBridgeLineName = sVDISMessagePayload.substring(20, 40).trim();
						sVDISVoiceBridgeConsoleNo = sVDISMessagePayload.substring(40, 50);
						sVDISVoiceBridgeUserNo = sVDISMessagePayload.substring(50 ,62);
						String sVDISOldLineState = sVDISMessagePayload.substring(62, 63);
						String sVDISNewLineState = sVDISMessagePayload.substring(63, 64);
						sVDISHandsetOrSpeaker = sVDISMessagePayload.substring(64, 65);
						sVDISSpeakerNo = sVDISMessagePayload.substring(65, 67);
						sVDISHandsetNo = sVDISMessagePayload.substring(67, 68);
						String sVDISConnectOrDisconnect = sVDISMessagePayload.substring(68, 69);

						boolean callbackEvent = false;

						if (callbacks.containsKey(sVDISVoiceBridgeConsoleNo+sVDISHandsetNo))
						{
							VoiceBridgeCallback voicebridgeCallback = callbacks.get(sVDISVoiceBridgeConsoleNo+sVDISHandsetNo);

							if (voicebridgeCallback.getVoiceBridgeUser() != null)
							{
								handleVoiceConnectChange(voicebridgeCallback, sVDISVoiceBridgeLineNo, sVDISOldLineState, sVDISNewLineState,  sVDISConnectOrDisconnect);
								callbackEvent = true;
							}
						}

						if (!callbackEvent)
						{
							handleCallConnected(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect);
						}
						break;

				case VDIS_SERVER_MSG_ID_CALL_PROGRESS:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

							String sVDISChannelNo = sVDISMessagePayload.substring(20, 22);
							sVDISVoiceBridgeFlag = sVDISMessagePayload.substring(22, 23);
							handleCallProgress(sCallNo, sVDISVoiceBridgeLineNo, sVDISChannelNo, sVDISVoiceBridgeFlag);

						} else {

							Log.warn( "["+ siteName + "] Call progress. cannot find call id for line " + sVDISVoiceBridgeLineNo);
						}

						break;

				case VDIS_SERVER_MSG_ID_CALL_PROCEEDING:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

							String sDigits = sVDISMessagePayload.substring(20, 28);
							String sEndFlag = sVDISMessagePayload.substring(28, 29);
							handleCallProceeding(sCallNo, sVDISVoiceBridgeLineNo, sDigits, sEndFlag);

						} else {

							Log.warn( "["+ siteName + "] Call proceeding. cannot find call id for line " + sVDISVoiceBridgeLineNo);
						}

						break;

				case VDIS_SERVER_MSG_ID_PRIVATE:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

							sVDISVoiceBridgeConsoleNo = sVDISMessagePayload.substring(20, 30);
							sVDISVoiceBridgeUserNo = sVDISMessagePayload.substring(30, 42);
							sVDISHandsetNo = sVDISMessagePayload.substring(42, 43);
							String sPrivacyOn = sVDISMessagePayload.substring(43, 44);
							handleCallPrivate(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISHandsetNo, sPrivacyOn);

						} else {

							Log.warn( "["+ siteName + "] Call private. cannot find call id for line " + sVDISVoiceBridgeLineNo);
						}

						break;

				case VDIS_SERVER_MSG_ID_DIALED_DIGVoiceBridge:
						break;

				case VDIS_SERVER_MSG_ID_INTERCOM:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

						} else {

							sCallNo = makeCallNo(sVDISVoiceBridgeLineNo);
							voicebridgeCallIds.put(sVDISVoiceBridgeLineNo, sCallNo);
							voicebridgeCallLines.put(sCallNo, sVDISVoiceBridgeLineNo);
						}

						sVDISVoiceBridgeConsoleNo = sVDISMessagePayload.substring(20, 30);
						sVDISVoiceBridgeUserNo = sVDISMessagePayload.substring(30, 42);
						String sIntercomUserNo = sVDISMessagePayload.substring(42, 54);
						handleIntercom(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sIntercomUserNo);
						break;

				case VDIS_SERVER_MSG_ID_PLATFORM_INTERCOM:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

						} else {

							sCallNo = makeCallNo(sVDISVoiceBridgeLineNo);
							voicebridgeCallIds.put(sVDISVoiceBridgeLineNo, sCallNo);
							voicebridgeCallLines.put(sCallNo, sVDISVoiceBridgeLineNo);
						}

						sVDISVoiceBridgeConsoleNo = sVDISMessagePayload.substring(20, 30);
						sVDISVoiceBridgeUserNo = sVDISMessagePayload.substring(30, 42);
						sIntercomUserNo = sVDISMessagePayload.substring(42, 54);
						handleIntercom(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sIntercomUserNo);

						break;

				case VDIS_SERVER_MSG_ID_TRANSFER:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

							sVDISVoiceBridgeConsoleNo = sVDISMessagePayload.substring(20, 30);
							sVDISVoiceBridgeUserNo = sVDISMessagePayload.substring(30, 42);
							String sTransferUserNo = sVDISMessagePayload.substring(42, 54);
							handleTransfer(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sTransferUserNo);

						} else {

							Log.warn( "["+ siteName + "] Call transfer. cannot find call id for line " + sVDISVoiceBridgeLineNo);
						}

						break;

				case VDIS_SERVER_MSG_ID_GROUP_TRANSFER:
						break;

				case VDIS_SERVER_MSG_ID_CLI_DETAILS:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

							String cliDigits = sVDISMessagePayload.substring(20, 50);
							handleCallCLI(sCallNo, sVDISVoiceBridgeLineNo, cliDigits);

						} else {

							Log.warn( "["+ siteName + "] Call cli. cannot find call id for line " + sVDISVoiceBridgeLineNo);
						}

						break;

				case VDIS_SERVER_MSG_ID_LINE_INFO:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

							sVDISVoiceBridgeLineName = sVDISMessagePayload.substring(20, 40).trim();
							sVDISNewLineState = sVDISMessagePayload.substring(40, 41);

							String speakerCount = sVDISMessagePayload.substring(41, 43);
							String handsetCount = sVDISMessagePayload.substring(43, 45);
							String direction = sVDISMessagePayload.substring(45, 46);
							String sPrivacyOn = sVDISMessagePayload.substring(46, 47);
							sRealVDISDDI = sVDISMessagePayload.substring(47, 59);
							String lineType = sVDISMessagePayload.substring(59, 62);
							String sELC = sVDISMessagePayload.substring(62, 66);

							handleCallInfo(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISNewLineState, speakerCount, handsetCount, direction, sPrivacyOn, sRealVDISDDI, lineType, sELC);

						} else {

							Log.warn( "["+ siteName + "] Call info. cannot find call id for line " + sVDISVoiceBridgeLineNo);
						}

						break;

				case VDIS_SERVER_MSG_ID_ELC:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

						} else {

							sCallNo = makeCallNo(sVDISVoiceBridgeLineNo);
							voicebridgeCallIds.put(sVDISVoiceBridgeLineNo, sCallNo);
							voicebridgeCallLines.put(sCallNo, sVDISVoiceBridgeLineNo);
						}

						sVDISVoiceBridgeConsoleNo = sVDISMessagePayload.substring(20, 30);
						sVDISVoiceBridgeUserNo = sVDISMessagePayload.substring(30, 42);
						sVDISHandsetNo = sVDISMessagePayload.substring(42, 43);
						String sELC = sVDISMessagePayload.substring(43, 47);
						String sConnectOrDisconnect = sVDISMessagePayload.substring(47, 48);
						handleCallELC(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISHandsetNo, sELC, sConnectOrDisconnect);
						break;

				case VDIS_SERVER_MSG_ID_CALL_MOVED:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

							sVDISVoiceBridgeLineNo2 = sVDISMessagePayload.substring(20, 30);
							handleCallMoved(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineNo2);

						} else {

							Log.warn( "["+ siteName + "] Call moved. cannot find call id for line " + sVDISVoiceBridgeLineNo);
						}

						break;

				case VDIS_SERVER_MSG_ID_CALL_OUTGOING:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);

						} else {

							sCallNo = makeCallNo(sVDISVoiceBridgeLineNo);
							voicebridgeCallIds.put(sVDISVoiceBridgeLineNo, sCallNo);
							voicebridgeCallLines.put(sCallNo, sVDISVoiceBridgeLineNo);
						}

						sRealVDISDDI = sVDISMessagePayload.substring(20, 32);
						sVDISDDI = sVDISMessagePayload.substring(32, 37);
						sVDISVoiceBridgeLineName = sVDISMessagePayload.substring(37, 57).trim();
						handleCallOutgoing(sCallNo, sVDISVoiceBridgeLineNo, sRealVDISDDI, sVDISDDI, sVDISVoiceBridgeLineName);
						break;

				case VDIS_SERVER_MSG_ID_CONSOLE_NO:
						sVDISVoiceBridgeUserNo = sVDISMessagePayload.substring(0, 12);
						sVDISVoiceBridgeConsoleNo = sVDISMessagePayload.substring(12, 22);
						String sVDISVoiceBridgeConsoleNoOld = sVDISMessagePayload.substring(22, 32);

						handleConsoleNo(sVDISVoiceBridgeUserNo, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeConsoleNoOld);
						break;

				case VDIS_SERVER_MSG_ID_ECKEY:
						sVDISVoiceBridgeConsoleNo = sVDISMessagePayload.substring(0, 10);
						sVDISVoiceBridgeUserNo = sVDISMessagePayload.substring(10, 22);
						sVDISVoiceBridgeGroup = sVDISMessagePayload.substring(22, 25);
						sVDISVoiceBridgeFunction = sVDISMessagePayload.substring(25, 30);
						sVDISHandsetNo = sVDISMessagePayload.substring(30, 31);
						sVDISVoiceBridgeKeystate = sVDISMessagePayload.substring(31, 34);
						handleConsoleECKey(sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISVoiceBridgeGroup, sVDISVoiceBridgeFunction, sVDISHandsetNo, sVDISVoiceBridgeKeystate);
						break;

				case VDIS_SERVER_MSG_ID_FREE_SEATING:
						sVDISVoiceBridgeUserNo = sVDISMessagePayload.substring(0, 12);
						String sVDISVoiceBridgeUserName = sVDISMessagePayload.substring(12, 32);
						sVDISVoiceBridgeConsoleNo = sVDISMessagePayload.substring(32, 42);
						String sVDISVoiceBridgeConsoleName = sVDISMessagePayload.substring(42, 62);
						String sVDISVoiceBridgeGroupNo = sVDISMessagePayload.substring(62, 67);
						String sVDISVoiceBridgeGroupName = sVDISMessagePayload.substring(67, 87);
						String sVDISVoiceBridgeConsoleType = sVDISMessagePayload.substring(87, 90);
						handleFreeSeating(sVDISVoiceBridgeUserNo, sVDISVoiceBridgeConsoleNo);
						break;

				case VDIS_SERVER_MSG_ID_RECALL_TRANSFER:
						sVDISVoiceBridgeLineNo = sVDISMessagePayload.substring(10, 20);

						if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
						{
							sCallNo = voicebridgeCallIds.get(sVDISVoiceBridgeLineNo);
							sVDISVoiceBridgeLineNo2 = sVDISMessagePayload.substring(20, 30);
							String transferStatus = sVDISMessagePayload.substring(52, 53);
							handleRecallTransfer(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineNo2, transferStatus);


						} else {

							Log.warn( "["+ siteName + "] Call recall transfer. cannot find call id for line " + sVDISVoiceBridgeLineNo);
						}

						break;

				default:
						break;
			}

		}
		catch(Exception e) {

        	Log.error("handleVDISMessage error " + e + "\n" + sVDISEventMessage);
        	e.printStackTrace();
        }
        */
	}

	private String makeCallNo(String sVDISVoiceBridgeLineNo)
	{
		try {
			return /*String.valueOf(Math.abs(randomNumberGenerator.nextInt())) +*/ String.valueOf(System.currentTimeMillis());
		}
		catch(Exception e) {

         	Log.error("["+ siteName + "] makeCallNo error " + e + " " + sVDISVoiceBridgeLineNo);
         	return sVDISVoiceBridgeLineNo + String.valueOf(System.currentTimeMillis());
        }
	}

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

    private void handleConsoleECKey(String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISVoiceBridgeGroup, String sVDISVoiceBridgeFunction, String sVDISHandsetNo, String sVDISVoiceBridgeKeystate)
    {
		Log.debug( "["+ siteName + "] handleConsoleECKey " + sVDISVoiceBridgeUserNo + " " + sVDISVoiceBridgeGroup + " " + sVDISVoiceBridgeFunction + " " + sVDISHandsetNo + " " + sVDISVoiceBridgeKeystate);

		String userKey = String.valueOf(Long.parseLong(sVDISVoiceBridgeUserNo));

		if (component.voicebridgeLdapService.voicebridgeUserTable.containsKey(userKey))
		{
			VoiceBridgeUser voicebridgeUser = component.voicebridgeLdapService.voicebridgeUserTable.get(userKey);

			String featureId = "vm_" + Integer.parseInt(sVDISVoiceBridgeFunction);
			VMessage message = component.voicebridgeVmsService.getVMessageFromName(voicebridgeUser, featureId);

			// validate that user has access to message

			if (message != null)
			{
				if ("020".equals(sVDISVoiceBridgeGroup) && isVoiceDropAvailable())
				{
					handleVMRecordECKey(voicebridgeUser, sVDISHandsetNo, message, sVDISVoiceBridgeFunction, sVDISVoiceBridgeKeystate);
				}
				else

				if ("021".equals(sVDISVoiceBridgeGroup) && isVoiceDropAvailable())
				{
					handleVMPlaybackECKey(voicebridgeUser, sVDISHandsetNo, message, sVDISVoiceBridgeFunction, sVDISVoiceBridgeKeystate);
				}

			} else Log.warn("["+ siteName + "] handleConsoleECKey cannot find message " + featureId);

		} else Log.warn("["+ siteName + "] handleConsoleECKey cannot find user " + userKey);
	}


	private void handleVMRecordECKey(VoiceBridgeUser voicebridgeUser, String sVDISHandsetNo, VMessage message, String sVDISVoiceBridgeFunction, String sVDISVoiceBridgeKeystate)
	{
		if ("103".equals(sVDISVoiceBridgeKeystate)) // ready, record
		{
			String exten = component.voicebridgeVmsService.recordMessage(voicebridgeUser, message.getName(), message.getComment(), message.getId());
			voicebridgeUser.selectCallset(component, voicebridgeUser.getCallset(), sVDISHandsetNo, null, null, exten);

			setECKey(voicebridgeUser.getDeviceNo(), "020", sVDISVoiceBridgeFunction, "105");
		}
		else

		if ("105".equals(sVDISVoiceBridgeKeystate)) // busy  cancel record
		{
			setECKey(voicebridgeUser.getDeviceNo(), "020", sVDISVoiceBridgeFunction, "103"); // also set when VMS event arrives
		}
	}

	private void handleVMPlaybackECKey(VoiceBridgeUser voicebridgeUser, String sVDISHandsetNo, VMessage message, String sVDISVoiceBridgeFunction, String sVDISVoiceBridgeKeystate)
	{
		VoiceBridgeCall voicebridgeCall1 = voicebridgeUser.getCurrentHS1Call();
		VoiceBridgeCall voicebridgeCall2 = voicebridgeUser.getCurrentHS2Call();

		if ("103".equals(sVDISVoiceBridgeKeystate)) // ready, record
		{
			// if we are on a call, drop otherwise playback

			if ("1".equals(sVDISHandsetNo) && voicebridgeCall1 != null) // drop
			{
				String exten = component.voicebridgeVmsService.getVMExtenToDial(voicebridgeUser, message.getId(), message.getName());
				addExternalCall(voicebridgeCall1.getLine(), component.makeDialableNumber(exten));

				//setECKey(voicebridgeUser.getDeviceNo(), "021", sVDISVoiceBridgeFunction, "105");

			} else if ("2".equals(sVDISHandsetNo) && voicebridgeCall2 != null) {

				String exten = component.voicebridgeVmsService.getVMExtenToDial(voicebridgeUser, message.getId(), message.getName());
				addExternalCall(voicebridgeCall2.getLine(), component.makeDialableNumber(exten));

				//setECKey(voicebridgeUser.getDeviceNo(), "021", sVDISVoiceBridgeFunction, "105");

			} else {	// playback

				String exten = component.voicebridgeVmsService.getVMExtenToDial(voicebridgeUser, message.getId(), message.getName());
				voicebridgeUser.selectCallset(component, voicebridgeUser.getCallset(), sVDISHandsetNo, null, null, exten);

				//setECKey(voicebridgeUser.getDeviceNo(), "021", sVDISVoiceBridgeFunction, "105");
			}

		}
		else

		if ("105".equals(sVDISVoiceBridgeKeystate)) // busy  cancel record
		{
			setECKey(voicebridgeUser.getDeviceNo(), "021", sVDISVoiceBridgeFunction, "103"); // also set when VMS event arrives
		}

	}


    private void handleCallInfo(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sVDISNewLineState, String speakerCount, String handsetCount, String direction, String sPrivacyOn, String sRealVDISDDI, String lineType, String sELC)
    {
		Log.debug( "["+ siteName + "] handleCallInfo " + sVDISVoiceBridgeLineNo);

		Iterator it = component.voicebridgeLdapService.voicebridgeInterests.values().iterator();

		while( it.hasNext())	// we must search all interests to find which call to be restored
		{
			VoiceBridgeInterest voicebridgeInterest = (VoiceBridgeInterest)it.next();

			Iterator it2 = voicebridgeInterest.getUserInterests().values().iterator();
			boolean interested = false;

			while( it2.hasNext() )
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it2.next();

        		if (voicebridgeUserInterest.getCallByNo(sCallNo) != null)
        		{
					Log.debug( "["+ siteName + "] handleCallInfo found call " + sCallNo);

					voicebridgeUserInterest.handleCallInfo(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISNewLineState, speakerCount, handsetCount, direction, sPrivacyOn, sRealVDISDDI, lineType, sELC);
					interested = true;
				}
			}

			if (interested)			// we are now interested in this line, so associate with interest
			{
				lineInterests.put(sVDISVoiceBridgeLineNo, voicebridgeInterest);
			}
		}
	}

	private void handleCallELC(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISHandsetNo, String sELC, String sVDISConnectOrDisconnect)
	{
		Log.debug( "["+ siteName + "] handleCallELC " + sVDISVoiceBridgeLineNo + " " + sVDISVoiceBridgeUserNo + " " + sVDISConnectOrDisconnect);

		VoiceBridgeInterest lineInterest = getLineInterest(sVDISVoiceBridgeLineNo, sVDISVoiceBridgeUserNo);

		if (lineInterest != null)
		{
			String userKey = String.valueOf(Long.parseLong(sVDISVoiceBridgeUserNo));

			if (lineInterest.getUserInterests().containsKey(userKey))
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = lineInterest.getUserInterests().get(userKey);
				voicebridgeUserInterest.handleCallELC(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISHandsetNo, sELC, sVDISConnectOrDisconnect);

				publishVoiceBridgeUserCallEvent(voicebridgeUserInterest);
			}
		}
	}

	private void handleCallOutgoing(String sCallNo, String sVDISVoiceBridgeLineNo, String sRealVDISDDI, String sVDISDDI, String sVDISVoiceBridgeLineName)
	{
		VoiceBridgeInterest lineInterest = getDDIInterest(sRealVDISDDI);

		if (lineInterest != null)
		{
			Iterator it = lineInterest.getUserInterests().values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it.next();
				voicebridgeUserInterest.handleCallOutgoing(sCallNo, sVDISVoiceBridgeLineNo, sRealVDISDDI, sVDISDDI, sVDISVoiceBridgeLineName);
			}
			lineInterests.put(sVDISVoiceBridgeLineNo, lineInterest);
		}
	}

	private void handleConsoleNo(String sVDISVoiceBridgeUserNo, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeConsoleNoOld)
	{
		String userKey = String.valueOf(Long.parseLong(sVDISVoiceBridgeUserNo));

		if (component.voicebridgeLdapService.voicebridgeUserTable.containsKey(userKey))
		{
			VoiceBridgeUser voicebridgeUser = component.voicebridgeLdapService.voicebridgeUserTable.get(userKey);
			voicebridgeUser.setDeviceNo(sVDISVoiceBridgeConsoleNo);
			voicebridgeUser.processConsoleNextSteps(component);

			if (!sVDISVoiceBridgeConsoleNo.equals("0000000000")) // login
			{
				if (isVoiceDropAvailable())
				{
					component.voicebridgeVmsService.setupUser(voicebridgeUser);
				}
			}

		}
	}


	private void handleFreeSeating(String sVDISVoiceBridgeUserNo, String sVDISVoiceBridgeConsoleNo)
	{
		String userKey = String.valueOf(Long.parseLong(sVDISVoiceBridgeUserNo));

		if (component.voicebridgeLdapService.voicebridgeUserTable.containsKey(userKey))
		{
			VoiceBridgeUser voicebridgeUser = component.voicebridgeLdapService.voicebridgeUserTable.get(userKey);
			voicebridgeUser.setDeviceNo(sVDISVoiceBridgeConsoleNo);
			voicebridgeUser.processConsoleNextSteps(component);
		}
	}

	private void handleCallPrivate(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISHandsetNo, String sPrivacyOn)
	{
		VoiceBridgeInterest lineInterest = getLineInterest(sVDISVoiceBridgeLineNo, sVDISVoiceBridgeUserNo);

		if (lineInterest!= null)
		{
			Iterator it = lineInterest.getUserInterests().values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it.next();

				Log.debug( "["+ siteName + "] handleCallPrivate - handling user interest " + voicebridgeUserInterest.getUser().getUserId() + " " + voicebridgeUserInterest.getInterest().getInterestId());

				if (sVDISVoiceBridgeConsoleNo.equals(voicebridgeUserInterest.getUser().getDeviceNo()))
					voicebridgeUserInterest.handleCallPrivate(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISHandsetNo, sPrivacyOn);
				else
					voicebridgeUserInterest.handleCallPrivateElsewhere(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISHandsetNo, sPrivacyOn);
			}
			publishVoiceBridgeCallEvent(lineInterest);
		}
	}

	private void handleTransfer(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sTransferUserNo)
	{
		VoiceBridgeInterest lineInterest = getLineInterest(sVDISVoiceBridgeLineNo, sVDISVoiceBridgeUserNo);

		if (lineInterest!= null)
		{
			String sUserNo = String.valueOf(Integer.parseInt(sTransferUserNo));

			if (lineInterest.getUserInterests().containsKey(sUserNo))
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = lineInterest.getUserInterests().get(sUserNo);
				voicebridgeUserInterest.handleTransfer(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sUserNo);
			}
		}
	}

	private void handleIntercom(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sIntercomUserNo)
	{
		String sUserNo1 = String.valueOf(Long.parseLong(sVDISVoiceBridgeUserNo));
		String sUserNo2 = String.valueOf(Integer.parseInt(sIntercomUserNo));

		VoiceBridgeInterest lineInterest = getLineInterest(sVDISVoiceBridgeLineNo, sVDISVoiceBridgeUserNo);

		if (lineInterest!= null)
		{
			if (lineInterest.getUserInterests().containsKey(sUserNo1))
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = lineInterest.getUserInterests().get(sUserNo1);
				voicebridgeUserInterest.handleIntercom(sCallNo);

				//publishVoiceBridgeUserDeviceEvent(voicebridgeUserInterest.getUser());
			}

			if (lineInterest.getUserInterests().containsKey(sUserNo2))
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = lineInterest.getUserInterests().get(sUserNo2);
				voicebridgeUserInterest.handleIntercom(sCallNo);

				//publishVoiceBridgeUserDeviceEvent(voicebridgeUserInterest.getUser());
			}

		}
	}



	private void handleCallIncoming(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sRealVDISDDI, String sVDISDDI)
	{
		VoiceBridgeInterest lineInterest = getDDIInterest(sRealVDISDDI);

		if (lineInterest!= null)
		{
			Iterator it = lineInterest.getUserInterests().values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it.next();
				voicebridgeUserInterest.handleCallIncoming(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sRealVDISDDI, sVDISDDI);
			}

			//theVoiceBridgelinkSession.sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_REQUEST_CLI + sVDISVoiceBridgeLineNo);
			lineInterests.put(sVDISVoiceBridgeLineNo, lineInterest);

			startWaitForEvent(sVDISVoiceBridgeLineNo, lineInterest, 500); // wait for CLI before publish

		} else {

			lineInterest = getLineInterest(sVDISVoiceBridgeLineNo, null);

			if (lineInterest!= null)
			{
				Iterator it = lineInterest.getUserInterests().values().iterator();

				while( it.hasNext() )
				{
					VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it.next();
					voicebridgeUserInterest.handleCallIncoming(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sRealVDISDDI, sVDISDDI);
				}

				publishVoiceBridgeCallEvent(lineInterest);
			}
		}
	}

	private void handleCallAbandoned(String sCallNo, String sVDISVoiceBridgeLineNo)
	{
		VoiceBridgeInterest lineInterest = getLineInterest(sVDISVoiceBridgeLineNo, null);

		if (lineInterest!= null)
		{
			cancelWaitForEvent(sVDISVoiceBridgeLineNo);	// stop waiting for CLI

			Iterator it = lineInterest.getUserInterests().values().iterator();

			while (it.hasNext())
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it.next();
				voicebridgeUserInterest.handleCallAbandoned(sCallNo, sVDISVoiceBridgeLineNo);
			}

			// first publish

			publishVoiceBridgeCallEvent(lineInterest);

			// next log call objects
			// then remove call objects

			Iterator<VoiceBridgeUserInterest> it2 = lineInterest.getUserInterests().values().iterator();
			boolean logged = false;

			while( it2.hasNext() )
			{
				VoiceBridgeUserInterest theUserInterest = (VoiceBridgeUserInterest)it2.next();

				if (theUserInterest.getUser().enabled())
				{
					VoiceBridgeCall voicebridgeCall = theUserInterest.getCallByNo(sCallNo);

					if (voicebridgeCall != null)
					{
						if ((ClusterManager.isClusteringEnabled() && ClusterManager.isSeniorClusterMember()) || !ClusterManager.isClusteringEnabled())
						{
							if (!logged)
							{
								theUserInterest.logCall(voicebridgeCall, component.getDomain(), siteID);
								logged = true;
							}

							component.voicebridgePlugin.removeCacheContent(lineInterest, theUserInterest, voicebridgeCall);
						}

						theUserInterest.removeCallByNo(sCallNo);
					}
				}
			}
		}
	}

	private void handleCallConnected(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineName, String sVDISVoiceBridgeConsoleNo, String sVDISVoiceBridgeUserNo, String sVDISOldLineState, String sVDISNewLineState, String sVDISHandsetOrSpeaker, String sVDISSpeakerNo, String sVDISHandsetNo, String sVDISConnectOrDisconnect)
	{
		Log.debug( "["+ siteName + "] handleCallConnected " + sVDISVoiceBridgeLineNo + " " + sVDISVoiceBridgeLineName);

		try {
			VoiceBridgeInterest lineInterest = getLineInterest(sVDISVoiceBridgeLineNo, sVDISVoiceBridgeUserNo);

			if (lineInterest!= null)
			{
				Iterator it = lineInterest.getUserInterests().values().iterator();

				while( it.hasNext() )
				{
					VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it.next();

					Log.debug( "["+ siteName + "] handleCallConnected - scan user interest " + voicebridgeUserInterest.getUser().getUserId() + " " + voicebridgeUserInterest.getInterest().getInterestId());


					if (sVDISVoiceBridgeConsoleNo.equals(voicebridgeUserInterest.getUser().getDeviceNo()) && "C".equals(sVDISConnectOrDisconnect) && voicebridgeUserInterest.getUser().enabled())
					{
						Log.debug( "["+ siteName + "] handleCallConnected - process NextSteps " + sVDISHandsetNo);

						voicebridgeUserInterest.getUser().setWaitingInterest(voicebridgeUserInterest);
						voicebridgeUserInterest.getUser().processConnectedNextSteps(component, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeConsoleNo, sVDISHandsetNo);
					}

					Log.debug( "["+ siteName + "] handleCallConnected - process user interest " + voicebridgeUserInterest.getUser().getUserId() + " " + voicebridgeUserInterest.getInterest().getInterestId());

					if ("A".equals(sVDISNewLineState))
					{
						if (sVDISVoiceBridgeConsoleNo.equals(voicebridgeUserInterest.getUser().getDeviceNo()))
						{
							voicebridgeUserInterest.handleCallConnected(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect, isCallbackAvailable(), isVoiceDropAvailable());
							userInterests.put(sVDISVoiceBridgeLineNo, voicebridgeUserInterest);

						} else {

							voicebridgeUserInterest.handleConnectionBusy(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect);
						}


					} else if ("B".equals(sVDISNewLineState)) {

							voicebridgeUserInterest.handleBusyLine(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect);


					} else if ("I".equals(sVDISNewLineState)) {

							voicebridgeUserInterest.handleConnectionCleared(sCallNo, sVDISVoiceBridgeLineNo, sVDISHandsetNo, sVDISSpeakerNo);


					} else if ("H".equals(sVDISNewLineState)) {


						if (sVDISVoiceBridgeConsoleNo.equals(voicebridgeUserInterest.getUser().getDeviceNo()))
						{
							voicebridgeUserInterest.handleCallHeld(sCallNo, sVDISVoiceBridgeLineNo);

						} else {

							voicebridgeUserInterest.handleCallHeldElsewhere(sCallNo, sVDISVoiceBridgeLineNo);							}

					} else if ("C".equals(sVDISNewLineState)) {

						if ("F".equals(sVDISOldLineState)) {

							if (sVDISVoiceBridgeConsoleNo.equals(voicebridgeUserInterest.getUser().getDeviceNo()) && "D".equals(sVDISConnectOrDisconnect))
							{
								// this user left conf
								voicebridgeUserInterest.handleCallInactive(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect);

							} else { // we must find last user connected to call

								VoiceBridgeCall voicebridgeCall1 = voicebridgeUserInterest.getUser().getCurrentHS1Call();
								VoiceBridgeCall voicebridgeCall2 = voicebridgeUserInterest.getUser().getCurrentHS2Call();

								if ((voicebridgeCall1 != null && sVDISVoiceBridgeLineNo.equals(voicebridgeCall1.getLine())) || (voicebridgeCall2 != null && sVDISVoiceBridgeLineNo.equals(voicebridgeCall2.getLine())))
								{
									voicebridgeUserInterest.handleCallConnected(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect, isCallbackAvailable(), isVoiceDropAvailable());
									userInterests.put(sVDISVoiceBridgeLineNo, voicebridgeUserInterest);
								}
							}

						} else if ("R".equals(sVDISOldLineState)) {

							cancelWaitForEvent(sVDISVoiceBridgeLineNo);	// stop waiting for CLI

							if (sVDISVoiceBridgeConsoleNo.equals(voicebridgeUserInterest.getUser().getDeviceNo()))
							{
								voicebridgeUserInterest.handleCallConnected(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect, isCallbackAvailable(), isVoiceDropAvailable());
								userInterests.put(sVDISVoiceBridgeLineNo, voicebridgeUserInterest);

							} else {

								voicebridgeUserInterest.handleCallIncomingBusy(sCallNo, sVDISVoiceBridgeLineNo);
							}

						} else {

							if (sVDISVoiceBridgeConsoleNo.equals(voicebridgeUserInterest.getUser().getDeviceNo()))
							{
								voicebridgeUserInterest.handleCallConnected(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect, isCallbackAvailable(), isVoiceDropAvailable());
								userInterests.put(sVDISVoiceBridgeLineNo, voicebridgeUserInterest);

							} else {

								voicebridgeUserInterest.handleConnectionBusy(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect);
							}
						}


					} else if ("F".equals(sVDISNewLineState)) {

						if ("F".equals(sVDISOldLineState)) {

							if (sVDISVoiceBridgeConsoleNo.equals(voicebridgeUserInterest.getUser().getDeviceNo()))
							{
								if ("D".equals(sVDISConnectOrDisconnect)) {

									// this user left conf now
									voicebridgeUserInterest.handleCallInactive(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect);

								} else {

									// this user joined conf now
									voicebridgeUserInterest.handleCallConferenced(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect);
								}

							} // no action for conferenced users.

						} else {

							if (sVDISVoiceBridgeConsoleNo.equals(voicebridgeUserInterest.getUser().getDeviceNo()))
							{
								// this user joined conf

								if ("C".equals(sVDISConnectOrDisconnect))
								{
									voicebridgeUserInterest.handleCallConferenced(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect);
								}

							} else { // we must find first user connected to call

								VoiceBridgeCall voicebridgeCall1 = voicebridgeUserInterest.getUser().getCurrentHS1Call();
								VoiceBridgeCall voicebridgeCall2 = voicebridgeUserInterest.getUser().getCurrentHS2Call();

								if ((voicebridgeCall1 != null && sVDISVoiceBridgeLineNo.equals(voicebridgeCall1.getLine())) || (voicebridgeCall2 != null && sVDISVoiceBridgeLineNo.equals(voicebridgeCall2.getLine())))
								{
									voicebridgeUserInterest.handleCallConferenced(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineName, sVDISVoiceBridgeConsoleNo, sVDISVoiceBridgeUserNo, sVDISOldLineState, sVDISNewLineState, sVDISHandsetOrSpeaker, sVDISSpeakerNo, sVDISHandsetNo, sVDISConnectOrDisconnect);
								}
							}
						}
					}

				}

				publishVoiceBridgeCallEvent(lineInterest);


				if ("I".equals(sVDISNewLineState)) // all line connect states except line goes idle
				{
					// mark for deletion or delete call objecs from user interests
					// remove call record from distributed cache
					// log call record for all user interest who where on the call: duration > 0

					Iterator<VoiceBridgeUserInterest> it3 = lineInterest.getUserInterests().values().iterator();

					while( it3.hasNext() )
					{
						VoiceBridgeUserInterest theUserInterest = (VoiceBridgeUserInterest)it3.next();

						if (theUserInterest.getUser().enabled())
						{
							VoiceBridgeCall voicebridgeCall = theUserInterest.getCallByNo(sCallNo);

							if (voicebridgeCall != null)
							{
								voicebridgeCall.deleted = true; // so we don't publish again

								if ((ClusterManager.isClusteringEnabled() && ClusterManager.isSeniorClusterMember()) || !ClusterManager.isClusteringEnabled())
								{
									component.voicebridgePlugin.removeCacheContent(lineInterest, theUserInterest, voicebridgeCall);

									if (sVDISVoiceBridgeConsoleNo.equals(theUserInterest.getUser().getDeviceNo()))
									{
										theUserInterest.logCall(voicebridgeCall, component.getDomain(), siteID);
									}
								}
							}
						}
					}

					// remove finished call line no and call id from tracking tables and user interest table

					lineInterests.remove(sVDISVoiceBridgeLineNo);
					userInterests.remove(sVDISVoiceBridgeLineNo);

					if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
					{
						voicebridgeCallLines.remove(voicebridgeCallIds.get(sVDISVoiceBridgeLineNo));
						voicebridgeCallIds.remove(sVDISVoiceBridgeLineNo);
					}
				}
			}

		}
		catch(Exception e) {

			Log.error( "["+ siteName + "] handleCallConnected " + e + " " + sVDISVoiceBridgeLineNo + " " + sVDISVoiceBridgeLineName);
			e.printStackTrace();
        }
	}


	private void handleCallProgress(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISChannelNo, String sVDISVoiceBridgeFlag)
	{
		Log.debug( "["+ siteName + "] handleCallProgress " + sCallNo + " " + sVDISVoiceBridgeLineNo + " " + sVDISVoiceBridgeFlag);

		if (userInterests.containsKey(sVDISVoiceBridgeLineNo))
		{
			VoiceBridgeUserInterest voicebridgeUserInterest = userInterests.get(sVDISVoiceBridgeLineNo);
			voicebridgeUserInterest.handleCallProgress(sCallNo, sVDISVoiceBridgeLineNo, sVDISChannelNo, sVDISVoiceBridgeFlag);

			publishVoiceBridgeUserCallEvent(voicebridgeUserInterest);

			if("0".equals(sVDISVoiceBridgeFlag))
			{
				VoiceBridgeInterest lineInterest = getLineInterest(sVDISVoiceBridgeLineNo, null);

				if (lineInterest!= null)
				{
					Iterator it = lineInterest.getUserInterests().values().iterator();

					while( it.hasNext() )
					{
						VoiceBridgeUserInterest theUserInterest = (VoiceBridgeUserInterest)it.next();

						if (!theUserInterest.getInterestName().equals(voicebridgeUserInterest.getInterestName()))
						{
							theUserInterest.handleCallOutgoingBusy(sCallNo, sVDISVoiceBridgeLineNo);
							publishVoiceBridgeUserCallEvent(theUserInterest);
						}
					}
				}
			}
		}
	}

    private void handleRecallTransfer(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineNo2, String transferStatusFlag)
    {
		Log.debug( "["+ siteName + "] handleRecallTransfer " + sVDISVoiceBridgeLineNo + " " + transferStatusFlag);

		if("0".equals(transferStatusFlag) || "1".equals(transferStatusFlag) || "2".equals(transferStatusFlag))
		{
			if (userInterests.containsKey(sVDISVoiceBridgeLineNo))
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = userInterests.get(sVDISVoiceBridgeLineNo);
				voicebridgeUserInterest.handleRecallTransfer(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineNo2, transferStatusFlag);

				publishVoiceBridgeUserCallEvent(voicebridgeUserInterest);
			}
		}

	}

	private void handleCallProceeding(String sCallNo, String sVDISVoiceBridgeLineNo, String sDigits, String sEndFlag)
	{
		Log.debug( "["+ siteName + "] handleCallProceeding " + sVDISVoiceBridgeLineNo + " " + sDigits + " " + sEndFlag);

		VoiceBridgeInterest lineInterest = getLineInterest(sVDISVoiceBridgeLineNo, null);

		if (lineInterest!= null)
		{
			Iterator it = lineInterest.getUserInterests().values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it.next();
				voicebridgeUserInterest.handleCallProceeding(component, sCallNo, sVDISVoiceBridgeLineNo, sDigits, sEndFlag);
			}
		}
	}


	private void handleCallCLI(String sCallNo, String sVDISVoiceBridgeLineNo, String cliDigits)
	{
		Log.debug( "["+ siteName + "] handleCallCLI " + sVDISVoiceBridgeLineNo + " " + cliDigits);

		VoiceBridgeInterest lineInterest = getLineInterest(sVDISVoiceBridgeLineNo, null);
		String cliCanonicalNumber = cliDigits;

		if (lineInterest!= null)
		{
			cancelWaitForEvent(sVDISVoiceBridgeLineNo);	// stop waiting for CLI

			try {
				String dialableNumber = component.formatDialableNumber(cliDigits);
				cliCanonicalNumber = component.formatCanonicalNumber(dialableNumber);
			}
			catch(Exception e) {}

			String cliName = cliDigits;

			if (component.voicebridgeLdapService.cliLookupTable.containsKey(cliCanonicalNumber))
			{
				cliName = component.voicebridgeLdapService.cliLookupTable.get(cliCanonicalNumber);

				Log.debug( "["+ siteName + "] handleCallCLI found " + cliName + " " + cliDigits);
			}

			Iterator it = lineInterest.getUserInterests().values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it.next();
				voicebridgeUserInterest.handleCallCLI(sCallNo, sVDISVoiceBridgeLineNo, cliDigits, cliName);
			}

			publishVoiceBridgeCallEvent(lineInterest);
		}

	}

	private void handleCallMoved(String sCallNo, String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeLineNo2)
	{
		VoiceBridgeInterest lineInterest = getLineInterest(sVDISVoiceBridgeLineNo, null);

		if (lineInterest!= null)
		{
			Iterator it = lineInterest.getUserInterests().values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it.next();
				voicebridgeUserInterest.handleCallMoved(sCallNo, sVDISVoiceBridgeLineNo, sVDISVoiceBridgeLineNo2);

				userInterests.remove(sVDISVoiceBridgeLineNo);
				userInterests.put(sVDISVoiceBridgeLineNo2, voicebridgeUserInterest);
			}

			lineInterests.put(sVDISVoiceBridgeLineNo2, lineInterest);
			lineInterests.remove(sVDISVoiceBridgeLineNo);

			if (voicebridgeCallIds.containsKey(sVDISVoiceBridgeLineNo))
			{
				voicebridgeCallLines.remove(voicebridgeCallIds.get(sVDISVoiceBridgeLineNo));
				voicebridgeCallIds.remove(sVDISVoiceBridgeLineNo);

				voicebridgeCallIds.put(sVDISVoiceBridgeLineNo2, sCallNo);
				voicebridgeCallLines.put(sCallNo, sVDISVoiceBridgeLineNo2);
			}

		}
	}

	private VoiceBridgeInterest getLineInterest(String sVDISVoiceBridgeLineNo, String sVDISVoiceBridgeUserNo)
	{
		VoiceBridgeInterest lineInterest = null;

		if (lineInterests.containsKey(sVDISVoiceBridgeLineNo))
			lineInterest = lineInterests.get(sVDISVoiceBridgeLineNo);

		else {
			String lineInterestId = "L" + String.valueOf(Long.parseLong(sVDISVoiceBridgeLineNo));

			if (component.voicebridgeLdapService.voicebridgeInterests.containsKey(lineInterestId))
			{
				lineInterest = component.voicebridgeLdapService.voicebridgeInterests.get(lineInterestId);

			} else {

				if (sVDISVoiceBridgeUserNo != null)
				{
					String userKey = String.valueOf(Long.parseLong(sVDISVoiceBridgeUserNo));
					VoiceBridgeUser voicebridgeUser = null;

					if (component.voicebridgeLdapService.voicebridgeUserTable.containsKey(userKey))
					{
						voicebridgeUser = component.voicebridgeLdapService.voicebridgeUserTable.get(userKey);

					} else if (callbackUserTable.containsKey(sVDISVoiceBridgeUserNo)) {

						voicebridgeUser = callbackUserTable.get(sVDISVoiceBridgeUserNo);
					}

					if (voicebridgeUser != null)
					{
						Log.debug( "["+ siteName + "] getLineInterest found user " + userKey);

						lineInterest = voicebridgeUser.getDefaultInterest();

						if (lineInterest != null)
						{
							Log.debug( "["+ siteName + "] getLineInterest found interest " + lineInterest.getInterestId());

							lineInterests.put(sVDISVoiceBridgeLineNo, lineInterest);
						}
					}
				}
			}
		}

		return lineInterest;
	}

	private VoiceBridgeInterest getDDIInterest(String sRealVDISDDI)
	{
		VoiceBridgeInterest lineInterest = null;
		String ddiInterestId = "D" + String.valueOf(Integer.parseInt(sRealVDISDDI));

		if (component.voicebridgeLdapService.voicebridgeInterests.containsKey(ddiInterestId))
		{
			lineInterest = component.voicebridgeLdapService.voicebridgeInterests.get(ddiInterestId);
		}
		return lineInterest;
	}


	private void deleteExpiredCalls()
	{
		Log.debug( "["+ site.getName() + "] deleteExpiredCalls");

		Iterator it = component.voicebridgeLdapService.voicebridgeInterests.values().iterator();

		while( it.hasNext())
		{
			VoiceBridgeInterest voicebridgeInterest = (VoiceBridgeInterest)it.next();

			Iterator it2 = voicebridgeInterest.getUserInterests().values().iterator();

			while( it2.hasNext() )
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it2.next();

				Iterator it3 = voicebridgeUserInterest.getCalls().values().iterator();

				while( it3.hasNext() )
				{
					try {
						VoiceBridgeCall voicebridgeCall = (VoiceBridgeCall)it3.next();

						if (("ConnectionCleared".equals(voicebridgeCall.getState()) || "Unknown".equals(voicebridgeCall.getState())) && (System.currentTimeMillis() - voicebridgeCall.completionTimeStamp) > 30000)
						{
							Log.debug( "["+ site.getName() + "] deleteExpiredCalls - " + voicebridgeCall.getCallID() + " " + voicebridgeCall.getCallerNumber(voicebridgeInterest.getInterestType()) + " " + voicebridgeCall.getCalledNumber(voicebridgeInterest.getInterestType()));
							voicebridgeUserInterest.removeCallById(voicebridgeCall.getCallID());
						}
					}
					catch(Exception e) {
						break;
					}
				}
			}
		}
	}


//-------------------------------------------------------
//
//
//
//-------------------------------------------------------



	public synchronized void publishVoiceBridgeCallEvent(VoiceBridgeInterest voicebridgeInterest)
	{
		if ((ClusterManager.isClusteringEnabled() && ClusterManager.isSeniorClusterMember()) || !ClusterManager.isClusteringEnabled())
		{
			Log.debug( "["+ siteName + "] publishVoiceBridgeCallEvent - interest " + voicebridgeInterest.getInterestId());

			Iterator it = voicebridgeInterest.getUserInterests().values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it.next();
				publishVoiceBridgeUserCallEvent(voicebridgeUserInterest);
			}
		}
	}

	public synchronized void publishVoiceBridgeUserCallEvent(VoiceBridgeUserInterest voicebridgeUserInterest)
	{
		if ((ClusterManager.isClusteringEnabled() && ClusterManager.isSeniorClusterMember()) || !ClusterManager.isClusteringEnabled())
		{
			Log.debug( "["+ siteName + "] publishVoiceBridgeUserEvent - user interest " + voicebridgeUserInterest.getInterestName() + " enabled: " + voicebridgeUserInterest.getUser().enabled());

			if (voicebridgeUserInterest.getUser().enabled())
			{
				if (voicebridgeUserInterest.canPublish(component))
				{
					publishVoiceBridgeUserInterestEvent(voicebridgeUserInterest.getInterest(), voicebridgeUserInterest);
				}

				updateCacheContent(voicebridgeUserInterest);

			}
		}
	}


	private void publishVoiceBridgeUserInterestEvent(VoiceBridgeInterest voicebridgeInterest, VoiceBridgeUserInterest voicebridgeUserInterest)
	{
		Log.debug( "["+ siteName + "] publishVoiceBridgeUserInterestEvent - scan user interest " + voicebridgeUserInterest.getUser().getUserId() + " " + voicebridgeUserInterest.getInterest().getInterestId());

		if (!"0000000000".equals(voicebridgeUserInterest.getUser().getDeviceNo()))
		{
			Log.debug( "["+ siteName + "] publishVoiceBridgeUserInterestEvent - publish user interest " + voicebridgeUserInterest.getUser().getUserId() + " " + voicebridgeUserInterest.getInterest().getInterestId());

			String interestNode = voicebridgeInterest.getInterestId() + voicebridgeUserInterest.getUser().getUserNo();

			IQ iq = new IQ(IQ.Type.set);
			iq.setFrom(component.getName() + "." + component.getDomain());
			iq.setTo("pubsub." + component.getDomain());
			Element pubsub = iq.setChildElement("pubsub", "http://jabber.org/protocol/pubsub");
			Element publish = pubsub.addElement("publish").addAttribute("node", interestNode);
			Element item = publish.addElement("item").addAttribute("id", interestNode);
			Element calls = item.addElement("callstatus", "http://xmpp.org/protocol/openlink:01:00:00#call-status");
			boolean busy = voicebridgeUserInterest.getBusyStatus();
			calls.addAttribute("busy", busy ? "true" : "false");

			if ("true".equals(voicebridgeUserInterest.getCallFWD()))
			{
				calls.addAttribute("fwd", voicebridgeUserInterest.getCallFWDDigits());
			}

			addVoiceBridgeCallsEvents(voicebridgeInterest, voicebridgeUserInterest, calls);

			if (calls.nodeCount() > 0)
			{
				component.sendPacket(iq);
			}
		}
	}


	private void updateCacheContent(VoiceBridgeUserInterest voicebridgeUserInterest)
	{
		Log.debug( "["+ siteName + "] updateCacheContent - user interest " + voicebridgeUserInterest.getInterestName());

		Iterator it2 = voicebridgeUserInterest.getCalls().values().iterator();

		while( it2.hasNext() )
		{
			VoiceBridgeCall voicebridgeCall = (VoiceBridgeCall)it2.next();
			voicebridgeCall.published = true;				// we can now send back response

			component.voicebridgePlugin.updateCacheContent(voicebridgeUserInterest.getInterest(), voicebridgeUserInterest, voicebridgeCall);
		}
	}


	private void addVoiceBridgeCallsEvents(VoiceBridgeInterest voicebridgeInterest, VoiceBridgeUserInterest voicebridgeUserInterest, Element calls)
	{
		Log.debug( "["+ siteName + "] addVoiceBridgeCallsEvents - user interest " + voicebridgeUserInterest.getInterestName());

		Iterator it2 = voicebridgeUserInterest.getCalls().values().iterator();

		while( it2.hasNext() )
		{
			VoiceBridgeCall voicebridgeCall = (VoiceBridgeCall)it2.next();

			if (!"Unknown".equals(voicebridgeCall.getState()) && !voicebridgeCall.deleted)
			{
				Element call = calls.addElement("call");
				addVoiceBridgeCallEvents(voicebridgeInterest, voicebridgeUserInterest, call, voicebridgeCall);
			}
		}
	}

	public synchronized void addVoiceBridgeCallEvents(VoiceBridgeInterest voicebridgeInterest, VoiceBridgeUserInterest voicebridgeUserInterest, Element call, VoiceBridgeCall voicebridgeCall)
	{
		Log.debug( "["+ siteName + "] addVoiceBridgeCallEvents - user interest " + voicebridgeUserInterest.getInterestName() + " " + voicebridgeCall.getCallID());

		call.addElement("id").setText(voicebridgeCall.getCallID());
		call.addElement("profile").setText(voicebridgeUserInterest.getUser().getProfileName());
		call.addElement("interest").setText(voicebridgeUserInterest.getInterestName());
		//call.addElement("changed").setText(voicebridgeCall.getStatus());
		call.addElement("state").setText(voicebridgeCall.getState());
		call.addElement("direction").setText(voicebridgeCall.getDirection());

		Element caller = call.addElement("caller");
		caller.addElement("number").setText(voicebridgeCall.getCallerNumber(voicebridgeInterest.getInterestType()));
		caller.addElement("name").setText(voicebridgeCall.getCallerName(voicebridgeInterest.getInterestType()));

		Element called = call.addElement("called");
		called.addElement("number").setText(voicebridgeCall.getCalledNumber(voicebridgeInterest.getInterestType()));
		called.addElement("name").setText(voicebridgeCall.getCalledName(voicebridgeInterest.getInterestType()));

		call.addElement("duration").setText(String.valueOf(voicebridgeCall.getDuration()));

		Element actions = call.addElement("actions");

		Iterator it4 = voicebridgeCall.getValidActions().iterator();

		while( it4.hasNext() )
		{
			String action = (String)it4.next();
			actions.addElement(action);
		}

		Element features  = call.addElement("features");
		addFeature(features, "priv_1", "Y".equals(voicebridgeCall.getPrivacy()) ? "true" : "false");
		addFeature(features, "hs_1",  "1".equals(voicebridgeCall.getHandset()) ? "true" : "false");
		addFeature(features, "hs_2",  "2".equals(voicebridgeCall.getHandset()) ? "true" : "false");

		if (voicebridgeUserInterest.getUser().getCallback() != null && isCallbackAvailable() && voicebridgeUserInterest.getUser().getCallbackActive())
		{
			addFeature(features, "callback_1",  voicebridgeUserInterest.getUser().getCallback());
		}

		if (isVoiceDropAvailable())
		{
			Iterator<VMessage> iter4 = component.voicebridgeVmsService.getVMessages(voicebridgeUserInterest.getUser()).iterator();

			while( iter4.hasNext() )
			{
				VMessage message = (VMessage)iter4.next();
				String vmId = String.valueOf(message.getId());

				Element featureElement = component.voicebridgeVmsService.getFeature(vmId);

				if (featureElement != null)
				{
					features.add(featureElement);
					component.voicebridgeVmsService.removeFeature(vmId);
				}
			}
		}

		Element participants  = call.addElement("participants");

		Iterator it3 = voicebridgeInterest.getUserInterests().values().iterator();

		while( it3.hasNext() )
		{
			VoiceBridgeUserInterest voicebridgeParticipant = (VoiceBridgeUserInterest)it3.next();

			if (!"0000000000".equals(voicebridgeParticipant.getUser().getDeviceNo()))
			{
				VoiceBridgeCall participantCall = voicebridgeParticipant.getCallByLine(voicebridgeCall.getLine());

				if (participantCall != null)
				{
					Element participant  = participants.addElement("participant");
					participant.addAttribute("jid", voicebridgeParticipant.getUser().getUserId() + "@" + component.getDomain());

					participant.addAttribute("type", participantCall.getParticipation());
					participant.addAttribute("direction", participantCall.getDirection());

					if (participantCall.firstTimeStamp != 0)
					{
						participant.addAttribute("timestamp", String.valueOf(new Date(participantCall.firstTimeStamp)));
					}
				}
			}
		}
	}


	private void addFeature(Element features, String id, String value)
	{
		Element feature = features.addElement("feature");
		feature.addAttribute("id", id);
		feature.setText(value);
	}

	public synchronized void publishVoiceBridgeUserDeviceEvent(VoiceBridgeUser voicebridgeUser)
	{
		Log.debug( "["+ siteName + "] publishVoiceBridgeUserDeviceEvent - " + voicebridgeUser.getUserId());

		if (!"0000000000".equals(voicebridgeUser.getDeviceNo()))
		{
			VoiceBridgeInterest voicebridgeInterest = voicebridgeUser.getDefaultInterest();

			if (voicebridgeInterest != null)
			{
				String interestNode = voicebridgeInterest.getInterestId() + voicebridgeUser.getUserNo();

				IQ iq = new IQ(IQ.Type.set);
				iq.setFrom(component.getName() + "." + component.getDomain());
				iq.setTo("pubsub." + component.getDomain());
				Element pubsub = iq.setChildElement("pubsub", "http://jabber.org/protocol/pubsub");
				Element publish = pubsub.addElement("publish").addAttribute("node", interestNode);
				Element item = publish.addElement("item").addAttribute("id", interestNode);
				Element device = item.addElement("devicestatus", "http://xmpp.org/protocol/openlink:01:00:00#device-status");

				Element features  = device.addElement("features");
				addFeature(features, "icom_1", voicebridgeUser.intercom() ? "true" : "false");

				component.sendPacket(iq);
			}
		}
	}


//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	private static String oldReplace(String aInput, String aOldPattern, String aNewPattern) {
		StringBuffer result = new StringBuffer();
		int startIdx = 0;
		int idxOld = 0;

		if ( aOldPattern.equals("") ) {
			return "";
		}

		while ((idxOld = aInput.indexOf(aOldPattern, startIdx)) >= 0) {

			result.append( aInput.substring(startIdx, idxOld) );
			result.append( aNewPattern );

			startIdx = idxOld + aOldPattern.length();
		}

		result.append(aInput.substring(startIdx) );
		return result.toString();
	}


	public void sendMessageToVDIS(String message)
	{
		try {


		} catch (Exception e) {
			Log.error("["+ siteName + "] sendMessageToVDIS error: " + e.toString());
		}
	}

	public void requestVersion()
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_VERSION);

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] requestVersion error: " + e.toString());
		}
	}


	public void setECKey(String console, String group, String function, String status)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_ECKEY_SET + format10digits(console) + group + format5digits(function) + status);

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] getUserConsole error: " + e.toString());
		}
	}

	public void setECKeyLabel(String console, String group, String function, String label)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_ECKEY_SETLABEL + format10digits(console) + group + format5digits(function) + pad(label, 20));

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] getUserConsole error: " + e.toString());
		}
	}


	public void getUserConsole(String userID)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_FREE_SEATING + format12digits(userID));

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] getUserConsole error: " + e.toString());
		}
	}

	public void selectDDI(String ddi, String console, String handset)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_CALLSET_SELECT + format10digits(console) + handset + "00000" + format5digits(ddi));

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] selectDDI error: " + e.toString());
		}
	}

	public void selectLine(String line, String console, String handset)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_CONNECT + format10digits(line) + format10digits(console) + handset);

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] selectLine error: " + e.toString());
		}
	}

	public void voicebridgeTransferCall(String console, String handset, String user)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_TRANSFER + format10digits(console) + handset + format12digits(user));

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] voicebridgeTransferCall error: " + e.toString());
		}
	}

	public void intercomCall(String console, String handset, String user)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_INTERCOM + format10digits(console) + handset + format12digits(user));

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] intercomCall error: " + e.toString());
		}
	}

	public void platformIntercomCall(String console, String user)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_PLATFORM_INTERCOM + format10digits(console) + format12digits(user) + "65");
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_PLATFORM_INTERCOM + format10digits(console) + format12digits(user) + "01");

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] platformIntercomCall error: " + e.toString());
		}
	}


	public void groupIntercomCall(String console, String group)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_GROUP_INTERCOM + format10digits(console) + format5digits(group) + "65");

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] groupIntercomCall error: " + e.toString());
		}
	}

	public void clearLine(String line)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_FORCE_LINE_CLEAR + format10digits(line));

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] clearLine error: " + e.toString());
		}
	}

	public void clearCall(String console, String handset)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_CLEAR + format10digits(console) + handset);

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] clearCall error: " + e.toString());
		}
	}

	public void clearIntercom(String console)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_CLEAR_PLATFORM_INTERCOM + format10digits(console));

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] clearIntercom error: " + e.toString());
		}
	}

	public void joinELC(String console)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_JOIN_ELC + format10digits(console) + "1");

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] joinELC error: " + e.toString());
		}
	}

	public void clearELC(String console)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_CLEAR_ELC + format10digits(console) + "1");

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] clearELC error: " + e.toString());
		}
	}

	public void privateCall(String console, String handset, String privacy)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_PRIVATE + format10digits(console) + handset + privacy);

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] privateCall error: " + e.toString());
		}
	}

	public void dialDigit(String console, String handset, String digit)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_DIAL_DIGIT + format10digits(console) + handset + digit);

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] dialDigit error: " + e.toString());
		}
	}

	public void transferCall(String console, String handset, String line, String digits)
	{
		try {
			ringRecall(console, handset);
			Thread.sleep(1000);

			dialDigits(line, digits);
			Thread.sleep(500);

			dialDigit(console, handset, "#");
			Thread.sleep(500);

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] transferCall error: " + e.toString());
		}
	}

	public void dialDigits(String line, String digits)
	{
		if (digits != null && !"".equals(digits))
		{
			try {
				String dialableNumber = digits;

				if (!"*".equals(digits.substring(0, 1)))
				{
					String cononicalNumber = component.formatCanonicalNumber(digits);
					dialableNumber = component.formatDialableNumber(cononicalNumber);
				}

				//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_DIAL_STRING + format10digits(line) + pad(dialableNumber, 30));

			}
			catch(Exception e) {
				Log.error("["+ siteName + "] dialDigits error: " + e.toString());
			}

		} else Log.error("["+ siteName + "] dialDigits is blank or null on line " + line);
	}

	public void ringRecall(String console, String handset)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_RING_RECALL + format10digits(console) + handset);

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] ringRecall error: " + e.toString());
		}
	}

	public void holdCall(String console, String handset)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_HOLD + format10digits(console) + handset);

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] holdCall error: " + e.toString());
		}
	}

	public void selectCallset(String callset, String console, String handset)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_CALLSET_SELECT + format10digits(console) + handset + format5digits(callset) + "00000");

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] selectCallset error: " + e.toString());
		}
	}


	public void requestLineInfo(String line)
	{
		try {
			//sendMessageToVDIS(VDIS_CLIENT_MSG_PREFIX_REQUEST_LINE_INFO + format10digits(line));

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] requestLineInfo error: " + e.toString());
		}
	}

	private String pad(String sStr, int nRequiredLength)
	{
		while (sStr.length() < nRequiredLength)
		{
			sStr = sStr + "                                                         ";
		}
		StringBuffer sStrBuf = new StringBuffer(sStr);
		sStrBuf.setLength(nRequiredLength);
		sStr = sStrBuf.toString();

		return(sStr);
	}

	private String format4digits(String theLine) {

		String theNewLine = "0000" + theLine;
		return(theNewLine.substring(theNewLine.length()-4, theNewLine.length()));

	}


	private String format12digits(String theLine) {

		String theNewLine = "000000000000" + theLine;
		return(theNewLine.substring(theNewLine.length()-12, theNewLine.length()));

	}


	private String format10digits(String theLine) {

		String theNewLine = "0000000000" + theLine;
		return(theNewLine.substring(theNewLine.length()-10, theNewLine.length()));

	}

	private String format5digits(String theLine) {

		String theNewLine = "00000" + theLine;
		return(theNewLine.substring(theNewLine.length()-5, theNewLine.length()));

	}
//-------------------------------------------------------
//
//
//
//-------------------------------------------------------




//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

	public void startWaitForEvent(String line, VoiceBridgeInterest voicebridgeInterest, int timeout)
	{
		if (voicebridgeTimeOuts.containsKey(line))
		{
			cancelWaitForEvent(line);	// stop pending waiting
		}

		Timer timer = new Timer();
		TimerCheck timerCheck = new TimerCheck(voicebridgeInterest, timer, line);

		timer.schedule(timerCheck, timeout);
		voicebridgeTimeOuts.put(line, timerCheck);
	}

	public void cancelWaitForEvent(String line)
	{
		TimerCheck timerCheck = voicebridgeTimeOuts.remove(line);

		if (timerCheck != null)
		{
			timerCheck.cancelTimer();
			timerCheck.cancel();
			timerCheck = null;
		}
	}

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

    private class TimerCheck extends TimerTask
    {
		private VoiceBridgeInterest voicebridgeInterest;
		private Timer timer = new Timer();
		private String line;

		public TimerCheck(VoiceBridgeInterest voicebridgeInterest, Timer timer, String line)
		{
			this.voicebridgeInterest = voicebridgeInterest;
			this.timer = timer;
			this.line = line;
		}

        public void run()
        {
			publishVoiceBridgeCallEvent(this.voicebridgeInterest);
			voicebridgeTimeOuts.remove(line);
			cancelTimer();
        }

        public void cancelTimer()
        {
			timer.cancel();
			timer = null;
        }
    }
}
