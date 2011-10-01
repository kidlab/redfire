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
		return VoiceBridgePlugin.application.manageCallParticipant(userJID.toString(), uid, parameter, value);
	}

    public void handleMessage(Message received)
    {
		VoiceBridgePlugin.application.handleMessage(received);
    }

	public void interceptMessage(Message received)
	{


	}

	public void handlePostBridge(List<String> uids)
	{
		VoiceBridgePlugin.application.handlePostBridge(uids);
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

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] requestVersion error: " + e.toString());
		}
	}


	public void setECKey(String console, String group, String function, String status)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] getUserConsole error: " + e.toString());
		}
	}

	public void setECKeyLabel(String console, String group, String function, String label)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] getUserConsole error: " + e.toString());
		}
	}


	public void getUserConsole(String userID)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] getUserConsole error: " + e.toString());
		}
	}

	public void selectDDI(String ddi, String console, String handset)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] selectDDI error: " + e.toString());
		}
	}

	public void selectLine(String line, String console, String handset)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] selectLine error: " + e.toString());
		}
	}

	public void voicebridgeTransferCall(String console, String handset, String user)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] voicebridgeTransferCall error: " + e.toString());
		}
	}

	public void intercomCall(String console, String handset, String user)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] intercomCall error: " + e.toString());
		}
	}

	public void platformIntercomCall(String console, String user)
	{
		try {
		}
		catch(Exception e) {
			Log.error("["+ siteName + "] platformIntercomCall error: " + e.toString());
		}
	}


	public void groupIntercomCall(String console, String group)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] groupIntercomCall error: " + e.toString());
		}
	}

	public void clearLine(String line)
	{
		try {
		}
		catch(Exception e) {
			Log.error("["+ siteName + "] clearLine error: " + e.toString());
		}
	}

	public void clearCall(String console, String handset)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] clearCall error: " + e.toString());
		}
	}

	public void clearIntercom(String console)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] clearIntercom error: " + e.toString());
		}
	}

	public void joinELC(String console)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] joinELC error: " + e.toString());
		}
	}

	public void clearELC(String console)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] clearELC error: " + e.toString());
		}
	}

	public void privateCall(String console, String handset, String privacy)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] privateCall error: " + e.toString());
		}
	}

	public void dialDigit(String console, String handset, String digit)
	{
		try {

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

			}
			catch(Exception e) {
				Log.error("["+ siteName + "] dialDigits error: " + e.toString());
			}

		} else Log.error("["+ siteName + "] dialDigits is blank or null on line " + line);
	}

	public void ringRecall(String console, String handset)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] ringRecall error: " + e.toString());
		}
	}

	public void holdCall(String console, String handset)
	{
		try {

		}
		catch(Exception e) {
			Log.error("["+ siteName + "] holdCall error: " + e.toString());
		}
	}

	public void selectCallset(String callset, String console, String handset)
	{
		try {
		}
		catch(Exception e) {
			Log.error("["+ siteName + "] selectCallset error: " + e.toString());
		}
	}


	public void requestLineInfo(String line)
	{
		try {

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
