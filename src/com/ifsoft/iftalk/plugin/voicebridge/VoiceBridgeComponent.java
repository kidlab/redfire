
package com.ifsoft.iftalk.plugin.voicebridge;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Vector;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.Timer;
import java.util.TimerTask;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import org.jivesoftware.openfire.http.HttpBindManager;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.RoutingTable;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.PrivateStorage;

import org.jivesoftware.util.Log;
import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.util.JiveGlobals;

import org.xmpp.component.Component;
import org.xmpp.component.AbstractComponent;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;

import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import com.ifsoft.iftalk.plugin.voicebridge.commands.*;
import com.ifsoft.iftalk.plugin.tsc.*;
import com.ifsoft.iftalk.plugin.tsc.numberformatter.TelephoneNumberFormatter;
import com.ifsoft.iftalk.plugin.tsc.voicemessage.message.VMessage;


public class VoiceBridgeComponent extends AbstractTSComponent implements VoiceBridgeConstants {

	private ComponentManager componentManager;
	private JID componentJID = null;
	public RedfirePlugin voicebridgePlugin;

    private ExecutorService executorLink;
    private ExecutorService executorLdap;
    private ExecutorService executorVMS;

	private PrivateStorage privateStorage;
	private OpenlinkCommandManager openlinkManger;
	public Site site;

	public Date lastRefreshedDate = null;

	public VoiceBridgeLdapService voicebridgeLdapService 			= null;
	public VoiceBridgeLinkService voicebridgeLinkService		 	= null;
    public VoiceBridgeVmsService voicebridgeVmsService			= null;

	public Map<String, VoiceBridgeUserInterest> openlinkInterests;
    public TelephoneNumberFormatter telephoneNumberFormatter;

    private Timer timer = null;
    private RefreshCacheCheck refreshCacheCheck = null;
    private ManageVoiceMessage voiceMessageComand;

	private Vector<VoiceBridgeUser> sortedProfiles;


//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	public VoiceBridgeComponent(Site site)
	{
        super(16, 1000, true);

		Log.info( "["+ site.getName() + "] VoiceBridgeComponent");
        this.site = site;
        this.componentJID = new JID(getName() + "." + getDomain());

		voicebridgeLinkService = new VoiceBridgeLinkService(this, site);
		voicebridgeLdapService = new VoiceBridgeLdapService(this, site);
		voicebridgeVmsService = new VoiceBridgeVmsService(this, site);

		sortedProfiles =  new Vector<VoiceBridgeUser>();
	}

	public void componentEnable()
	{
		voicebridgePlugin 	= (RedfirePlugin)XMPPServer.getInstance().getPluginManager().getPlugin("redfire");
        privateStorage 		= XMPPServer.getInstance().getPrivateStorage();
        componentManager 	= ComponentManagerFactory.getComponentManager();
        openlinkManger 		= new OpenlinkCommandManager();
        openlinkInterests 	= Collections.synchronizedMap( new HashMap<String, VoiceBridgeUserInterest>());

        timer = new Timer();

		if (site != null)
		{
			Log.info( "["+ site.getName() + "] Creating telephoneNumberFormatter object");
			setupTelephoneNumberFormatter();


			executorLink = Executors.newCachedThreadPool();

			executorLink.submit(new Callable<Boolean>()
			{
				public Boolean call() throws Exception
				{
					try {
						return setVoiceBridgelink();
					}
					catch (Exception e) {
						Log.error("Error initializing VoiceBridgelink", e);
						throw e;
					}
				}
			});

			executorLdap = Executors.newCachedThreadPool();

			executorLdap.submit(new Callable<Boolean>()
			{
				public Boolean call() throws Exception
				{
					try {
						return setVoiceBridgeLdap();
					}
					catch (Exception e) {
						Log.error("Error initializing VoiceBridgeldap", e);
						throw e;
					}
				}
			});

			executorVMS = Executors.newCachedThreadPool();

			openlinkManger.addCommand(new GetProfiles(this));
			openlinkManger.addCommand(new GetProfile(this));
			openlinkManger.addCommand(new GetInterests(this));
			openlinkManger.addCommand(new GetInterest(this));
			openlinkManger.addCommand(new GetFeatures(this));
			openlinkManger.addCommand(new MakeCall(this));
			openlinkManger.addCommand(new IntercomCall(this));
			openlinkManger.addCommand(new RequestAction(this));
			openlinkManger.addCommand(new SetFeature(this));
			openlinkManger.addCommand(new QueryFeatures(this));
			openlinkManger.addCommand(new ManageVoiceBridge(this));
		}
    }


	public void componentDestroyed()
	{
		try {
			voicebridgeLinkService.freeCallbacks();
			voicebridgeLinkService.logoutFromVoiceBridgeLink();
			voicebridgeLdapService.stop();
			voicebridgeVmsService.shutdown();

			executorLink.shutdownNow();
			executorLdap.shutdownNow();
			executorVMS.shutdownNow();

			openlinkManger.stop();

        	if (timer != null) {
                timer.cancel();
            	timer = null;
        	}

        	if (refreshCacheCheck != null)
        	{
                refreshCacheCheck.cancel();
            	refreshCacheCheck = null;
        	}
		}
		catch(Exception e) {
			Log.error(e.toString());
		}
	}


	public void setupTelephoneNumberFormatter()
	{
		Log.info( "["+ site.getName() + "] setupTelephoneNumberFormatter");

		try
		{
			String pname = site.getName().toLowerCase();
			String country = JiveGlobals.getProperty(Properties.VoiceBridge_PBX_COUNTRY_CODE + "."  + pname, Locale.getDefault().getCountry());

			String pbxAccessDigits 	= JiveGlobals.getProperty(Properties.VoiceBridge_PBX_ACCESS_DIGITS	+ "."  + pname, "9");
			String areaCode 		= JiveGlobals.getProperty(Properties.VoiceBridge_AREA_CODE  		+ "."  + pname, "0");
			String pbxNumberLength 	= JiveGlobals.getProperty(Properties.VoiceBridge_PBX_NUMBER_LENGTH + "."  + pname, "5");

			telephoneNumberFormatter = new TelephoneNumberFormatter(new Locale("en", country));
			telephoneNumberFormatter.setExtensionNumberLength(Integer.parseInt(pbxNumberLength));
			telephoneNumberFormatter.setOutsideAccess(pbxAccessDigits);
			telephoneNumberFormatter.setAreaCode(areaCode);
			telephoneNumberFormatter.setLocale(new Locale("en", country));
		}
		catch (Exception e)
		{
	        Log.error( "["+ site.getName() + "] setupTelephoneNumberFormatter " + e);
		}
	}

	public String formatCanonicalNumber(String dialDigits)
	{
		String canonicalNumber = dialDigits;

		try
		{
			canonicalNumber = telephoneNumberFormatter.formatCanonicalNumber(dialDigits);
		}
		catch (Exception e)
		{
	        Log.error( "["+ site.getName() + "] formatCanonicalNumber " + e);
		}

		return canonicalNumber;
	}

	public String formatDialableNumber(String cononicalNumber)
	{
		String dialableNumber = cononicalNumber;

		try
		{
			dialableNumber = telephoneNumberFormatter.formatDialableNumber(cononicalNumber);
		}
		catch (Exception e)
		{
			cononicalNumber = formatCanonicalNumber(cononicalNumber);

			try
			{
				dialableNumber = telephoneNumberFormatter.formatDialableNumber(cononicalNumber);
			}

			catch (Exception e1)
			{
	        	Log.error( "["+ site.getName() + "] formatDialableNumber " + e1);
			}
		}

		return dialableNumber;
	}

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

	@Override public String getDescription()
	{
		String siteDesc = "VoiceBridge Component for site ";

		if (site == null)
			return siteDesc;
		else
			return siteDesc + site.getName();
	}


	@Override public String getName()
	{
		String siteName = "voicebridge";

		if (site == null)
			return siteName;
		else
			return siteName + site.getSiteID();
	}

	@Override public String getDomain()
	{
		String hostName =  XMPPServer.getInstance().getServerInfo().getHostname();
		return JiveGlobals.getProperty("xmpp.domain", hostName);
	}

	@Override public void postComponentStart()
	{

	}

	@Override public void postComponentShutdown()
	{

	}

	public JID getComponentJID()
	{
		return new JID(getName() + "." + getDomain());
	}

	public String getSiteName()
	{
		if (site == null)
			return "";
		else
			return site.getName();
	}

    public int getUserCount()
    {
        return voicebridgeLdapService.voicebridgeUserTable.values().size();
    }

    public List<VoiceBridgeUser> getUsers(int startIndex, int numResults)
    {
		List<VoiceBridgeUser> profiles  = new ArrayList<VoiceBridgeUser>();
		int counter = 0;

		if (startIndex == 0 || sortedProfiles.size() == 0)
		{
			sortedProfiles = new Vector<VoiceBridgeUser>(voicebridgeLdapService.voicebridgeUserTable.values());
			Collections.sort(sortedProfiles);
		}

		Iterator it = sortedProfiles.iterator();

		while( it.hasNext() )
		{
			VoiceBridgeUser voicebridgeUser = (VoiceBridgeUser)it.next();

			if (counter > (startIndex + numResults))
			{
				break;
			}

			if (counter >= startIndex)
			{
				profiles.add(voicebridgeUser);
			}

			counter++;
		}

        return profiles;
    }


//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	public void interceptMessage(Message received)
	{
		voicebridgeLinkService.interceptMessage(received);
	}

    @Override protected void handleMessage(Message received)
    {
		Log.debug("["+ site.getName() + "] handleMessage \n"+ received.toString());

		voicebridgeLinkService.handleMessage(received);
    }

	@Override protected void handleIQResult(IQ iq)
	{
		Log.debug("["+ site.getName() + "] handleIQResult \n"+ iq.toString());

		Element element = iq.getChildElement();

		if (element != null)
		{
			String namespace = element.getNamespaceURI();

			if("http://jabber.org/protocol/pubsub#owner".equals(namespace))
			{
				Element subscriptions = element.element("subscriptions");

				if (subscriptions != null)
				{
					String node = subscriptions.attributeValue("node");

					Log.debug("["+ site.getName() + "] handleIQResult found subscription node " + node);

					if (openlinkInterests.containsKey(node))
					{
						Log.debug("["+ site.getName() + "] handleIQResult found user interest " + node);

						VoiceBridgeUserInterest voicebridgeUserInterest = openlinkInterests.get(node);

						for ( Iterator<Element> i = subscriptions.elementIterator( "subscription" ); i.hasNext(); )
						{
							Element subscription = (Element) i.next();
							JID jid = new JID(subscription.attributeValue("jid"));
							String sub = subscription.attributeValue("subscription");

							VoiceBridgeSubscriber voicebridgeSubscriber = voicebridgeUserInterest.getSubscriber(jid);
							voicebridgeSubscriber.setSubscription(sub);

							voicebridgePlugin.setSubscriberDetails(jid, voicebridgeSubscriber);

							Log.debug("["+ site.getName() + "] handleIQResult added subscriber " + jid);

						}
					}
				}
			}
		}
	}

	@Override protected void handleIQError(IQ iq)
	{
		String xml = iq.toString();

		if (xml.indexOf("<create node=") == -1)
			Log.debug("["+ site.getName() + "] handleIQError \n"+ iq.toString());
	}

   @Override public IQ handleDiscoInfo(IQ iq)
    {
    	JID jid = iq.getFrom();
		Element child = iq.getChildElement();
		String node = child.attributeValue("node");

		IQ iq1 = IQ.createResultIQ(iq);
		iq1.setType(org.xmpp.packet.IQ.Type.result);
		iq1.setChildElement(iq.getChildElement().createCopy());

		Element queryElement = iq1.getChildElement();
		Element identity = queryElement.addElement("identity");

		queryElement.addElement("feature").addAttribute("var",NAMESPACE_DISCO_INFO);
		queryElement.addElement("feature").addAttribute("var",NAMESPACE_XMPP_PING);

		identity.addAttribute("category", "component");
		identity.addAttribute("name", "voicebridge");

		if (node == null) 				// Disco discovery of openlink
		{
			identity.addAttribute("type", "command-list");
			queryElement.addElement("feature").addAttribute("var", "http://jabber.org/protocol/commands");
			queryElement.addElement("feature").addAttribute("var", "http://xmpp.org/protocol/openlink:01:00:00");
			queryElement.addElement("feature").addAttribute("var", "http://xmpp.org/protocol/openlink:01:00:00#tsc");


		} else {

			// Disco discovery of Openlink command

			OpenlinkCommand command = openlinkManger.getCommand(node);

			if (command != null && command.hasPermission(jid))
			{
				identity.addAttribute("type", "command-node");
				queryElement.addElement("feature").addAttribute("var", "http://jabber.org/protocol/commands");
				queryElement.addElement("feature").addAttribute("var", "http://xmpp.org/protocol/openlink:01:00:00");
			}

		}
		//Log.debug("["+ site.getName() + "] handleDiscoInfo "+ iq1.toString());
		return iq1;
    }


   @Override public IQ handleDiscoItems(IQ iq)
    {
    	JID jid = iq.getFrom();
		Element child = iq.getChildElement();
		String node = child.attributeValue("node");

		IQ iq1 = IQ.createResultIQ(iq);
		iq1.setType(org.xmpp.packet.IQ.Type.result);
		iq1.setChildElement(iq.getChildElement().createCopy());

		Element queryElement = iq1.getChildElement();
		Element identity = queryElement.addElement("identity");

		identity.addAttribute("category", "component");
		identity.addAttribute("name", "openlink");
		identity.addAttribute("type", "command-list");

		if ("http://jabber.org/protocol/commands".equals(node))
		{
			for (OpenlinkCommand command : openlinkManger.getCommands())
			{
				// Only include commands that the sender can invoke (i.e. has enough permissions)

				if (command.hasPermission(jid))
				{
					Element item = queryElement.addElement("item");
					item.addAttribute("jid", componentJID.toString());
					item.addAttribute("node", command.getCode());
					item.addAttribute("name", command.getLabel());
				}
			}
		}
		//Log.debug("["+ site.getName() + "] handleDiscoItems "+ iq1.toString());
		return iq1;
    }

   @Override public IQ handleIQGet(IQ iq)
    {
		return handleIQPacket(iq);
	}

   @Override public IQ handleIQSet(IQ iq)
    {
		return handleIQPacket(iq);
	}

   private IQ handleIQPacket(IQ iq)
    {
		Log.debug("["+ site.getName() + "] handleIQPacket \n"+ iq.toString());

		Element element = iq.getChildElement();
		IQ iq1 = IQ.createResultIQ(iq);
		iq1.setType(org.xmpp.packet.IQ.Type.result);
		iq1.setChildElement(iq.getChildElement().createCopy());

		if (element != null)
		{
			String namespace = element.getNamespaceURI();

			if("http://jabber.org/protocol/commands".equals(namespace))
				iq1 = openlinkManger.process(iq);
		}
		return iq1;
	}


//-------------------------------------------------------
//
//
//
//-------------------------------------------------------



	public String makeCallDefault(Element newCommand, JID jid, String handset, String privacy, String autoHold, String dialDigits)
	{
		Log.debug( "["+ site.getName() + "] makeCallDefault "+ jid + " " + handset + " " + dialDigits + " " + privacy);
		String errorMessage = "No default profile found";

		try {

			if (dialDigits != null && !"".equals(dialDigits))
			{
				dialDigits = makeDialableNumber(dialDigits);

				if (dialDigits == null || "".equals(dialDigits))
				{
					errorMessage = "Destination is not a dialable number";
					return errorMessage;
				}
			}

			String userName = jid.getNode();
			Iterator<VoiceBridgeUser> it = voicebridgeLdapService.voicebridgeUserTable.values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeUser voicebridgeUser = (VoiceBridgeUser)it.next();

				if (userName.equals(voicebridgeUser.getUserId()) && "true".equals(voicebridgeUser.getDefault()))
				{
					handset = handset == null ? voicebridgeUser.getHandsetNo() : handset;
					privacy = privacy == null ? voicebridgeUser.getLastPrivacy() : privacy;

					if (voicebridgeUser.getCallset() != null)
					{
						voicebridgeUser.setWaitingInterest(null);
						voicebridgeUser.selectCallset(this, voicebridgeUser.getCallset(), handset, privacy, autoHold, dialDigits);
						errorMessage = waitForFirstEvent(newCommand, voicebridgeUser, true, handset);
						break;

					} else errorMessage = "Default VoiceBridge Callset is missing";

					return errorMessage;
				}
			}
		}
		catch(Exception e) {
        	Log.error("makeCallDefault " + e);
        	e.printStackTrace();
        	errorMessage = "Internal error - " + e.toString();
        }
        return errorMessage;
	}



	public String makeCall(Element newCommand, String userInterest, String handset, String privacy, String autoHold, String dialDigits)
	{
		Log.debug( "["+ site.getName() + "] makeCall "+ userInterest + " " + dialDigits);
		String errorMessage = "Interest not found";

		try {

			if (openlinkInterests.containsKey(userInterest))
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = openlinkInterests.get(userInterest);
				VoiceBridgeUser voicebridgeUser = voicebridgeUserInterest.getUser();

				handset = handset == null ? voicebridgeUser.getHandsetNo() : handset;
				privacy = privacy == null ? voicebridgeUser.getLastPrivacy() : privacy;

				if ("D".equals(voicebridgeUserInterest.getInterest().getInterestType()))
				{
					if (dialDigits != null && !"".equals(dialDigits))
					{
						dialDigits = makeDialableNumber(dialDigits);

						if (dialDigits == null || "".equals(dialDigits))
						{
							errorMessage = "Destination is not a dialable number";
							return errorMessage;
						}
					}

					voicebridgeUser.setWaitingInterest(null);

					if (voicebridgeUserInterest.getInterest().getCallset() != null)
					{
						voicebridgeUser.selectCallset(this, voicebridgeUserInterest.getInterest().getCallset(), handset, privacy, autoHold, dialDigits);

					} else {

						voicebridgeUser.selectDDI(this, voicebridgeUserInterest.getInterest().getInterestValue(), handset, privacy, autoHold, dialDigits);
					}

					errorMessage = waitForFirstEvent(newCommand, voicebridgeUser, true, handset);

				}

				else if ("L".equals(voicebridgeUserInterest.getInterest().getInterestType()))
				{
					String interestLine = voicebridgeUserInterest.getInterest().getInterestValue();

					if (!voicebridgeUserInterest.isLineActive(interestLine))
					{
						voicebridgeUser.setWaitingInterest(null);
						voicebridgeUser.selectLine(this, interestLine, handset, privacy, autoHold);
						errorMessage = waitForFirstEvent(newCommand, voicebridgeUser, true, handset);

					} else errorMessage = "Direct line is in use";
				}
			}
		}
		catch(Exception e) {
        	Log.error("makeCall " + e);
        	e.printStackTrace();
        	errorMessage = "Internal error - " + e.toString();
        }
        return errorMessage;
	}


	private String waitForFirstEvent(Element newCommand, VoiceBridgeUser voicebridgeUser, boolean statusRequired, String handset)
	{
		VoiceBridgeUserInterest voicebridgeUserInterest = null;
		String errorMessage = "Timeout exceeded for call";

		int numTries = 0;

		while (numTries < 20)
		{
			Log.debug("["+ site.getName() + " waitForFirstEvent " + numTries);

			voicebridgeUserInterest = voicebridgeUser.getWaitingInterest();

			if (voicebridgeUserInterest != null)
			{
				Log.debug("["+ site.getName() + " waitForFirstEvent " + voicebridgeUserInterest.getUser().nextStepsDone());

				VoiceBridgeCall voicebridgeCall = voicebridgeUserInterest.getCurrentCall(handset);

				if (voicebridgeUserInterest.getUser().nextStepsDone() && voicebridgeCall != null)
				{
					try
					{
						if (newCommand != null && statusRequired) 		// request from web console has no command xml object
						{
							Element call = addWaitingCallEventHdr(newCommand, voicebridgeUserInterest);
							voicebridgeLinkService.addVoiceBridgeCallEvents(voicebridgeUserInterest.getInterest(), voicebridgeUserInterest, call, voicebridgeCall);
							errorMessage = null;
						}

						voicebridgeUser.setWaitingInterest(null);
					}
					catch (Exception e)
					{
						Log.error( "["+ site.getName() + "] waitForNextEvent "+ e);
        				e.printStackTrace();

						errorMessage = "Internal error " + e;
					}

					return errorMessage;
				}
			}

			 try {Thread.sleep(500);} catch (Exception e) {}

			numTries++;
		}

		voicebridgeUser.resetNextSteps();
		voicebridgeUser.setWaitingInterest(null);

		return errorMessage;
	}

	private String waitForNextEvent(Element newCommand, VoiceBridgeUserInterest voicebridgeUserInterest, VoiceBridgeCall voicebridgeCall)
	{
		String errorMessage = "Timeout exceeded for call";

		int numTries = 0;

		while (numTries < 20)
		{
			Log.debug("["+ site.getName() + " waitForNextEvent " + numTries);

			if (voicebridgeCall.published || "ConnectionCleared".equals(voicebridgeCall.getState()))
			{
				try
				{
					if (newCommand != null)			// request from web console has no command xml object
					{
						Element call = addWaitingCallEventHdr(newCommand, voicebridgeUserInterest);
						voicebridgeLinkService.addVoiceBridgeCallEvents(voicebridgeUserInterest.getInterest(), voicebridgeUserInterest, call, voicebridgeCall);
						errorMessage = null;
					}

				}
				catch (Exception e)
				{
					Log.error( "["+ site.getName() + "] waitForNextEvent "+ e);
        			e.printStackTrace();

					errorMessage = "Internal error " + e;
				}

				return errorMessage;

			} else  try {Thread.sleep(500);} catch (Exception e) {}

			numTries++;
		}

		return errorMessage;
	}

	private Element addWaitingCallEventHdr(Element newCommand, VoiceBridgeUserInterest voicebridgeUserInterest)
	{
		Element iodata = newCommand.addElement("iodata", "urn:xmpp:tmp:io-data");
		iodata.addAttribute("type","output");
		Element calls = iodata.addElement("out").addElement("callstatus", "http://xmpp.org/protocol/openlink:01:00:00#call-status");
		calls.addAttribute("busy", voicebridgeUserInterest.getBusyStatus() ? "true" : "false");
		Element call = calls.addElement("call");
		return call;
	}


	public String intercomCall(Element newCommand, String profileID, JID to, String groupID)
	{
		Log.debug( "["+ site.getName() + "] intercomCall "+ profileID + " -> " + to + " => " + groupID);
		String errorMessage = null;

		try {
			VoiceBridgeUser fromUser = getOpenlinkProfile(profileID);

			if (groupID == null)
			{
				VoiceBridgeUser toUser = getVoiceBridgeUser(to);
				voicebridgeLinkService.platformIntercomCall(fromUser.getDeviceNo(), toUser.getUserNo());

			} else {

				voicebridgeLinkService.groupIntercomCall(fromUser.getDeviceNo(), groupID);
			}

			errorMessage = waitForFirstEvent(newCommand, fromUser, true, "0");
		}
		catch(Exception e) {
        	Log.error("["+ site.getName() + "] intercomCall " + e);
        	e.printStackTrace();
        	errorMessage = "Internal error - " + e.toString();
        }
        return errorMessage;
	}


	private boolean isValidAction(VoiceBridgeCall voicebridgeCall, String validAction)
	{
		boolean valid = false;

		Iterator it4 = voicebridgeCall.getValidActions().iterator();

		while( it4.hasNext() )
		{
			String action = (String)it4.next();

			if (action.equals(validAction))
			{
				valid = true;
				break;
			}
		}

		return valid;
	}


	public void processUserAction(Element command, String userInterest, String action, String callID, String value1)
	{
		Log.debug( "["+ site.getName() + "] processUserAction " + userInterest + " " + action + " " + callID + " " + value1);
		String errorMessage = null;

		try {

			if (openlinkInterests.containsKey(userInterest))
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = openlinkInterests.get(userInterest);
				VoiceBridgeCall voicebridgeCall = voicebridgeUserInterest.getCallById(callID);

				if (voicebridgeCall != null)
				{
					voicebridgeCall.published = false;

					if (isValidAction(voicebridgeCall, action))
					{
						if ("AnswerCall".equals(action) || "JoinCall".equals(action) || "RetrieveCall".equals(action))
						{
							if (voicebridgeUserInterest.getCurrentCall(voicebridgeCall.handset) != null)
							{
								// active call, first, we hold or clear

								if (voicebridgeUserInterest.getUser().autoHold())
								{
									voicebridgeLinkService.holdCall(voicebridgeUserInterest.getUser().getDeviceNo(), voicebridgeUserInterest.getUser().getHandsetNo());

								} else {

									voicebridgeLinkService.clearCall(voicebridgeCall.getConsole(), voicebridgeCall.getHandset());
								}
							}

							voicebridgeLinkService.selectLine(voicebridgeCall.getLine(), voicebridgeUserInterest.getUser().getDeviceNo(), voicebridgeUserInterest.getUser().getHandsetNo());

							if (voicebridgeUserInterest.getUser().autoPrivate())
							{
								Thread.sleep(500);
								voicebridgeLinkService.privateCall(voicebridgeUserInterest.getUser().getDeviceNo(), voicebridgeUserInterest.getUser().getHandsetNo(), "Y");
							}

						}

						if ("ClearConnection".equals(action))
						{
							if (voicebridgeCall.platformIntercom)
							{
								voicebridgeLinkService.clearIntercom(voicebridgeCall.getConsole());

							} else {

								voicebridgeLinkService.clearCall(voicebridgeCall.getConsole(), voicebridgeCall.getHandset());
							}
						}

						if ("PrivateCall".equals(action))
						{
							voicebridgeLinkService.privateCall(voicebridgeCall.getConsole(), voicebridgeCall.getHandset(), "Y");
						}

						if ("PublicCall".equals(action))
						{
							voicebridgeLinkService.privateCall(voicebridgeCall.getConsole(), voicebridgeCall.getHandset(), "N");
						}

						if ("SendDigits".equals(action))
						{
							value1 = makeDialableNumber(value1);

							if (value1 == null || "".equals(value1))
							{
								errorMessage = "A dialable number is required for SendDigits";

							} else {

								voicebridgeLinkService.dialDigits(voicebridgeCall.getLine(), value1);
							}

						}

						if ("SendDigit".equals(action))
						{
							if (value1 != null && value1.length() > 0)
							{
								voicebridgeLinkService.dialDigit(voicebridgeCall.getConsole(), voicebridgeCall.getHandset(), value1.substring(0, 1));
								//voicebridgeLinkService.publishVoiceBridgeUserCallEvent(voicebridgeUserInterest);  // no event, so we force pub-sub of current event

							} else errorMessage = "A dialable digit must be provided for SendDigit action";
						}

						if ("ClearCall".equals(action))
						{
							voicebridgeLinkService.clearLine(voicebridgeCall.getLine());
						}

						if ("ConferenceCall".equals(action))
						{
							voicebridgeLinkService.joinELC(voicebridgeCall.getConsole());
						}

						if ("ClearConference".equals(action))
						{
							voicebridgeLinkService.clearELC(voicebridgeCall.getConsole());
							//voicebridgeLinkService.clearLine(voicebridgeCall.getLine());
						}

						if ("IntercomTransfer".equals(action))
						{
							try {
								VoiceBridgeUser voicebridgeUser = getVoiceBridgeUser(new JID(value1));

								if (voicebridgeUser != null)
								{
									voicebridgeLinkService.voicebridgeTransferCall(voicebridgeCall.getConsole(), voicebridgeCall.getHandset(), voicebridgeUser.getUserNo());

								} else errorMessage = value1 + " is either not a valid user or logged into a device";

							} catch (Exception e) {

								errorMessage = value1 + " is not a valid user identity";
							}
						}

						if ("ConsultationCall".equals(action))
						{
							if (!voicebridgeCall.transferFlag)
							{
								value1 = makeDialableNumber(value1);

								if (value1 == null || "".equals(value1))
								{
									errorMessage = "A dialable number must be provided for ConsultationCall action";

								} else {

									voicebridgeCall.previousCalledNumber = voicebridgeCall.proceedingDigits;	// store old called number.
									voicebridgeCall.previousCalledLabel = voicebridgeCall.proceedingDigitsLabel;

									voicebridgeLinkService.transferCall(voicebridgeCall.getConsole(), voicebridgeCall.getHandset(), voicebridgeCall.getLine(), value1);
									voicebridgeCall.transferFlag = true;
								}

							} else {

								voicebridgeLinkService.ringRecall(voicebridgeCall.getConsole(), voicebridgeCall.getHandset());	// terminate current ConsultationCall
								voicebridgeCall.transferFlag = false;

								if (voicebridgeCall.previousCalledNumber != null)
								{
									Iterator<VoiceBridgeUserInterest> it3 = voicebridgeUserInterest.getInterest().getUserInterests().values().iterator();

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
							}

							voicebridgeCall.setValidActions();
							//voicebridgeLinkService.publishVoiceBridgeUserCallEvent(voicebridgeUserInterest);  // no event, so we force pub-sub of current event
						}

						if ("TransferCall".equals(action))
						{
							if (voicebridgeCall.transferFlag)
							{
								voicebridgeLinkService.clearCall(voicebridgeCall.getConsole(), voicebridgeCall.getHandset());
								voicebridgeCall.transferFlag = false;

							} else errorMessage = "ConsultationCall must be done before TransferCall";
						}

						if ("SingleStepTransfer".equals(action))
						{
							value1 = makeDialableNumber(value1);

							if (value1 == null || "".equals(value1))
							{
								errorMessage = "A dialable number must be provided for a SingleStepTransfer action";

							} else {

								voicebridgeLinkService.transferCall(voicebridgeCall.getConsole(), voicebridgeCall.getHandset(), voicebridgeCall.getLine(), value1);
								voicebridgeLinkService.clearCall(voicebridgeCall.getConsole(), voicebridgeCall.getHandset());
							}
						}

						if ("AddThirdParty".equals(action))
						{
							value1 = makeDialableNumber(value1);

							if (value1 == null || "".equals(value1))
							{
								errorMessage = "A dialable number must be provided for a AddThirdParty action";

							} else {

								errorMessage = voicebridgeLinkService.addExternalCall(voicebridgeCall.getLine(), value1);
							}
						}

						if ("RemoveThirdParty".equals(action))
						{
							errorMessage = voicebridgeLinkService.removeExternalCall(voicebridgeCall.getLine(), makeDialableNumber(value1));
						}


						if ("HoldCall".equals(action))
						{
							voicebridgeLinkService.holdCall(voicebridgeCall.getConsole(), voicebridgeCall.getHandset());
						}


						if ("StartVoiceDrop".equals(action))
						{
							VMessage message = getVMId(voicebridgeUserInterest.getUser(), value1);

							if (message == null)
							{
								errorMessage = "A valid voice message feature Id must be provided for a StartVoiceDrop action";

							} else {

								String exten = voicebridgeVmsService.getVMExtenToDial(voicebridgeUserInterest.getUser(), message.getId(), message.getName());
								errorMessage = voicebridgeLinkService.addExternalCall(voicebridgeCall.getLine(), makeDialableNumber(exten));
							}
						}

						if ("StopVoiceDrop".equals(action))
						{
							VMessage message = getVMId(voicebridgeUserInterest.getUser(), value1);

							if (message == null)
							{
								errorMessage = "A valid voice message feature Id must be provided for a StartVoiceDrop action";

							} else {

								String exten = voicebridgeVmsService.getVMExtenToDial(voicebridgeUserInterest.getUser(), message.getId(), message.getName());
								errorMessage = voicebridgeLinkService.removeExternalCall(voicebridgeCall.getLine(), makeDialableNumber(exten));
							}
						}



						if (errorMessage == null)
						{
							errorMessage = waitForNextEvent(command, voicebridgeUserInterest, voicebridgeCall);
						}



					} else errorMessage = "Action is not valid";

				} else errorMessage = "Call id not found";


			} else errorMessage = "Interest not found";

		}
		catch(Exception e) {
        	Log.error("["+ site.getName() + "] processUserAction " + e);
        	e.printStackTrace();
        	errorMessage = "Request Action internal error - " + e.toString();
        }


        if (errorMessage != null && command != null)
        {
			Element note = command.addElement("note");
			note.addAttribute("type", "error");
			note.setText("Request Action - " + errorMessage);
		}
	}


	public String setFeature(Element newCommand, String profileID, String featureID, String value1, String value2)
	{
		Log.debug( "["+ site.getName() + "] setFeature " + profileID + " " + featureID + " " + value1 + " " + value2);
		String errorMessage = null;

		try {

			if (value1 != null && value1.length() > 0)
			{
				VoiceBridgeUser voicebridgeUser = getOpenlinkProfile(profileID);

				if (voicebridgeUser != null)
				{
					if ("hs_1".equals(featureID))
					{
						if (validateTrueFalse(value1))
							voicebridgeUser.setHandsetNo("true".equals(value1.toLowerCase()) ? "1" : "2");
						else
							errorMessage = "value1 is not true or false";

					}

					else if ("hs_2".equals(featureID))
					{
						if (validateTrueFalse(value1))
							voicebridgeUser.setHandsetNo("true".equals(value1.toLowerCase()) ? "2" : "1");
						else
							errorMessage = "value1 is not true or false";
					}

					else if ("priv_1".equals(featureID))
					{
						if (validateTrueFalse(value1))
							voicebridgeUser.setAutoPrivate("true".equals(value1.toLowerCase()));
						else
							errorMessage = "value1 is not true or false";
					}

					else if ("hold_1".equals(featureID))
					{
						if (validateTrueFalse(value1))
							voicebridgeUser.setAutoHold("true".equals(value1.toLowerCase()));
						else
							errorMessage = "value1 is not true or false";
					}

					else if ("callback_1".equals(featureID))
					{
						if (validateTrueFalse(value1))
						{
							if ("true".equals(value1.toLowerCase()))
							{
								if (value2 != null && !"".equals(value2))
								{
									String dialableNumber = makeDialableNumber(value2);

									if (dialableNumber != null && !"".equals(dialableNumber))
									{
										voicebridgeUser.setCallback(dialableNumber);
										VoiceBridgeCallback voicebridgeCallback = voicebridgeLinkService.allocateCallback(voicebridgeUser);

										if (voicebridgeCallback == null)
											errorMessage = "unable to allocate a virtual turret";

									} else errorMessage = "value2 is not a dialable number";

								} else {

									if (voicebridgeUser.getCallback() != null)
									{
										VoiceBridgeCallback voicebridgeCallback = voicebridgeLinkService.allocateCallback(voicebridgeUser);

										if (voicebridgeCallback == null)
											errorMessage = "unable to allocate a callback";

									} else errorMessage = "calback destination is missing";
								}

							} else  {

								voicebridgeLinkService.freeCallback(voicebridgeUser.getUserNo());
								voicebridgeUser.setPhoneCallback(null);
							}
						}
						else errorMessage = "value1 is not true or false";
					}

					else if ("fwd_1".equals(featureID))	// call forward
					{
						if (openlinkInterests.containsKey(value1))	// value is interest id
						{
							VoiceBridgeUserInterest voicebridgeUserInterest = openlinkInterests.get(value1);

							if (voicebridgeUser.getUserNo().equals(voicebridgeUserInterest.getUser().getUserNo()))
							{
								if ("D".equals(voicebridgeUserInterest.getInterest().getInterestType()))
								{
									String pname = site.getName().toLowerCase();

									String pbxFWDCodePrefix	= JiveGlobals.getProperty(Properties.VoiceBridge_PBX_FWD_CODE_PREFIX + "." + pname, "*41");
									String pbxFWDCodeSuffix = JiveGlobals.getProperty(Properties.VoiceBridge_PBX_FWD_CODE_SUFFIX + "." + pname, "");
									String pbxFWDCodeCancel	= JiveGlobals.getProperty(Properties.VoiceBridge_PBX_FWD_CODE_CANCEL + "." + pname, "*41");

									String dialDigits = null;

									if (value2 == null || "".equals(value2))
									{
										dialDigits = pbxFWDCodeCancel;
										errorMessage = doCallForward(dialDigits, voicebridgeUserInterest, newCommand);

										if (errorMessage == null)
										{
											Iterator<VoiceBridgeUserInterest> iter2 = voicebridgeUserInterest.getInterest().getUserInterests().values().iterator();

											while( iter2.hasNext() )
											{
												VoiceBridgeUserInterest theUserInterest = (VoiceBridgeUserInterest)iter2.next();
												theUserInterest.setCallFWD("false");
											}

											voicebridgeUser.setLastCallForward("");
										}

									} else {

										String dialableNumber = makeDialableNumber(value2);

										if (dialableNumber != null && !"".equals(dialableNumber))
										{
											dialDigits = pbxFWDCodePrefix + dialableNumber + pbxFWDCodeSuffix;
											errorMessage = doCallForward(dialDigits, voicebridgeUserInterest, newCommand);

											if (errorMessage == null)
											{
												Iterator<VoiceBridgeUserInterest> iter2 = voicebridgeUserInterest.getInterest().getUserInterests().values().iterator();

												while( iter2.hasNext() )
												{
													VoiceBridgeUserInterest theUserInterest = (VoiceBridgeUserInterest)iter2.next();
													theUserInterest.setCallFWD("true");
													theUserInterest.setCallFWDDigits(value2);
												}

												voicebridgeUser.setLastCallForwardInterest(value1);
												voicebridgeUser.setLastCallForward(value2);
											}

										} else errorMessage = "value2 is not a dialable number";
									}

								} else errorMessage = "CallForward requires a directory number interest";

							} else errorMessage = "Interest does not belong to this profile";

						} else errorMessage = "Interest not found";
					}
					else errorMessage = "Feature not found";

				} else errorMessage = "Profile not found";

			} else errorMessage = "Input1 is missing";
		}
		catch(Exception e) {
			Log.error("["+ site.getName() + "] setFeature " + e);
        	e.printStackTrace();
			errorMessage = "Internal error - " + e.toString();
		}

        return errorMessage;
	}


	private String doCallForward(String dialDigits, VoiceBridgeUserInterest voicebridgeUserInterest, Element newCommand)
	{
		voicebridgeUserInterest.getUser().selectCallset(this, voicebridgeUserInterest.getInterest().getCallset(), voicebridgeUserInterest.getUser().getHandsetNo(), "true", "true", dialDigits);
		String errorMessage = waitForFirstEvent(newCommand, voicebridgeUserInterest.getUser(), false, voicebridgeUserInterest.getUser().getHandsetNo());
		voicebridgeLinkService.clearCall(voicebridgeUserInterest.getUser().getDeviceNo(), voicebridgeUserInterest.getUser().getHandsetNo());

		return errorMessage;
	}



	public String manageVoiceBridge(Element newCommand, JID userJID, List<Object[]> actions)
	{
		Log.debug( "["+ site.getName() + "] manageVoiceMessage " + userJID + " ");
		String errorMessage = "";
		List<String> actionList = new ArrayList<String>();

		try {

			if (actions != null && actions.size() > 0)
			{
				Iterator it = actions.iterator();

				while( it.hasNext() )
				{
					Object[] action = (Object[])it.next();
					String name = (String) action[0];
					String value1 = (String) action[1];
					String value2 = (String) action[2];

					String thisErrorMessage = voicebridgeLinkService.manageCallParticipant(userJID, value1, name, value2);

					if (thisErrorMessage == null)
					{
						if ("MakeCall".equalsIgnoreCase(name))
						{
							actionList.add(value1);
						}

					} else {

						errorMessage = errorMessage + thisErrorMessage + "; ";
					}
				}

				if (actionList.size() > 0)
				{
					voicebridgeLinkService.handlePostBridge(actionList);
				}

			} else errorMessage = "Voice message features are missing";

		}
		catch(Exception e) {
			Log.error("["+ site.getName() + "] manageVoiceBridge " + e);
			e.printStackTrace();
			errorMessage = "Internal error - " + e.toString();
		}

        return errorMessage.length() == 0 ? null : errorMessage;
	}




	public String manageVoiceMessage(Element newCommand, String profileID, String featureId, String action, String value1)
	{
		Log.debug( "["+ site.getName() + "] manageVoiceMessage " + profileID + " " + featureId + " " + action + " " + value1);
		String errorMessage = null;

		try {

			if (action != null && action.length() > 0)
			{
				VoiceBridgeUser voicebridgeUser = getOpenlinkProfile(profileID);

				if (voicebridgeUser != null)
				{
					action = action.toLowerCase();

					if ("record".equals(action))
					{
						if (value1 != null && value1.length() > 0)
						{
							if (featureId == null || featureId.length() == 0)
							{
								String msgId = "vm_" + voicebridgeVmsService.getNextMsgId();
								String callInNumber = voicebridgeVmsService.recordMessage(voicebridgeUser, msgId, value1);

								if (callInNumber != null)
								{
									addVoiceMessageExtension(newCommand, callInNumber, voicebridgeUser, msgId);

								} else  errorMessage = "Error creating new voice message";


							} else {

								VMessage message = getVMId(voicebridgeUser, featureId);

								if (message != null)
								{
									String msgName = message.getName();
									String callInNumber = voicebridgeVmsService.recordMessage(voicebridgeUser, msgName, value1, message.getId());

									if (callInNumber != null)
									{
										addVoiceMessageExtension(newCommand, callInNumber, voicebridgeUser, msgName);

									} else  errorMessage = "Error creating new voice message";


								} else errorMessage = "A valid voice message feature Id must be provided for a re-record action";
							}

						} else errorMessage = "A description must be provided to record a voice message";
					}

					else if ("edit".equals(action))
					{
						VMessage message = getVMId(voicebridgeUser, featureId);

						if (message == null)
						{
							errorMessage = "A valid voice message feature Id must be provided for an edit action";

						} else {

							if (value1 != null && value1.length() > 0)
							{
								message.setComment(value1);			// set label for both record and playback eckeys

								voicebridgeLinkService.setECKeyLabel(voicebridgeUser.getDeviceNo(), "020", featureId.substring(3), value1);
								voicebridgeLinkService.setECKeyLabel(voicebridgeUser.getDeviceNo(), "021", featureId.substring(3), value1);

							} else errorMessage = "A description must be provided to edit a voice message";
						}
					}

					else if ("playback".equals(action))
					{
						VMessage message = getVMId(voicebridgeUser, featureId);

						if (message == null)
						{
							errorMessage = "A valid voice message feature Id must be provided for a playback action";

						} else {

							String callInNumber = voicebridgeVmsService.getVMExtenToDial(voicebridgeUser, message.getId(), message.getName());

							if (callInNumber != null)
							{
								addVoiceMessageExtension(newCommand, callInNumber, voicebridgeUser, message.getName());

							} else  errorMessage = "Error with playback of voice message";
						}
					}

					else if ("delete".equals(action))
					{
						VMessage message = getVMId(voicebridgeUser, featureId);

						if (message == null)
						{
							errorMessage = "A valid voice message feature Id must be provided for a delete action";

						} else {

							if (voicebridgeVmsService.deleteMessage(voicebridgeUser, message.getId(), message.getName()) != null)
							{
								addVoiceMessageExtension(newCommand, null, voicebridgeUser, message.getName());

							} else errorMessage = "Voice message cannot be deleted";
						}
					}

					else if ("save".equals(action))
					{

					}

					else if ("archive".equals(action))
					{

					} else  errorMessage = "Action not supported";

				} else errorMessage = "Profile not found";

			} else errorMessage = "Action is missing";
		}
		catch(Exception e) {
			Log.error("["+ site.getName() + "] manageVoiceMessage " + e);
			e.printStackTrace();
			errorMessage = "Internal error - " + e.toString();
		}

        return errorMessage;
	}

	private void addVoiceMessageExtension(Element newCommand, String exten, VoiceBridgeUser voicebridgeUser, String msgId)
	{
		Element iodata = newCommand.addElement("iodata", "urn:xmpp:tmp:io-data");
		iodata.addAttribute("type","output");
		Element devicestatus = iodata.addElement("out").addElement("devicestatus", "http://xmpp.org/protocol/openlink:01:00:00#device-status");
		devicestatus.addElement("profile").setText(voicebridgeUser.getProfileName());
		Element feature = devicestatus.addElement("features").addElement("feature").addAttribute("id", msgId);
		Element voicemessage = feature.addElement("voicemessage").addAttribute("xmlns", "http://xmpp.org/protocol/openlink:01:00:00/features#voice-message");

		voicemessage.addElement("msglen");
		voicemessage.addElement("status").setText("ok");
		voicemessage.addElement("statusdescriptor");
		voicemessage.addElement("state");

		if (exten == null || exten.length() == 0)
			voicemessage.addElement("exten");
		else
			voicemessage.addElement("exten").setText(exten);
	}

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

	public List<VoiceBridgeUser> getOpenlinkProfiles(JID jid)
	{
		List<VoiceBridgeUser> voicebridgeUsers = new ArrayList();
		String userName = jid.getNode();

		if (jid.getDomain().indexOf(getDomain()) > -1)
		{
			Iterator<VoiceBridgeUser> it = voicebridgeLdapService.voicebridgeUserTable.values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeUser voicebridgeUser = (VoiceBridgeUser)it.next();

				if (userName.equals(voicebridgeUser.getUserId()))
				{
					voicebridgeUsers.add(voicebridgeUser);
				}
			}
		}

		return voicebridgeUsers;
	}


	public VoiceBridgeUser getVoiceBridgeUser(JID jid)
	{
		return getVoiceBridgeUser(jid.getNode());
	}


	public VoiceBridgeUser getVoiceBridgeUser(String userName)
	{
		Iterator<VoiceBridgeUser> it = voicebridgeLdapService.voicebridgeUserTable.values().iterator();

		while( it.hasNext() )
		{
			VoiceBridgeUser voicebridgeUser = (VoiceBridgeUser)it.next();

			if (userName.equals(voicebridgeUser.getUserId()) && !"0000000000".equals(voicebridgeUser.getDeviceNo()))
			{
				return voicebridgeUser;
			}
		}
		return null;
	}


	public VoiceBridgeUser getOpenlinkProfile(String profileID)
	{
		VoiceBridgeUser voicebridgeUser = null;

		if (voicebridgeLdapService.voicebridgeUserTable.containsKey(profileID))
		{
			voicebridgeUser = voicebridgeLdapService.voicebridgeUserTable.get(profileID);
		}

		return voicebridgeUser;
	}

	public VoiceBridgeUserInterest getOpenlinkInterest(String userInterest)
	{
		VoiceBridgeUserInterest voicebridgeUserInterest = null;

		if (openlinkInterests.containsKey(userInterest))
		{
			voicebridgeUserInterest = openlinkInterests.get(userInterest);
		}

		return voicebridgeUserInterest;
	}

	public String getSiteID()
	{
		return String.valueOf(site.getSiteID());
	}

	public void sendPacket(Packet packet)
	{
		try {
			componentManager.sendPacket(this, packet);

		} catch (Exception e) {
			Log.error("Exception occured while sending packet." + e);

		}
	}


	public synchronized void asyncLoadCachesFromLDAP()
	{
		Future<Boolean> cacheProcess = executorLdap.submit(new Callable<Boolean>()
		{
			public Boolean call() throws Exception
			{
				try {
					loadCachesFromLDAP();
					return true;
				}
				catch (Exception e) {
					Log.error("asyncLoadCachesFromLDAP ", e);
					throw e;
				}
			}
		});
	}

	private synchronized void loadCachesFromLDAP()
	{
		try {
			Log.info( "["+ site.getName() + "] loadCachesFromLDAP");

			voicebridgeLdapService.getProfiles();

		}
		catch (Exception e) {
			Log.error(e.toString());
		}
	}

	public boolean isVmsAvailable()
	{
		String pname = site.getName().toLowerCase();
		String vmsEnabled = JiveGlobals.getProperty(Properties.VoiceBridge_VMS_ENABLED + "." + pname, "false");
		return "true".equals(vmsEnabled);
	}


	public void checkVMS()
	{
		executorVMS.submit(new Callable<Boolean>()
		{
			public Boolean call() throws Exception
			{
				try {

					if (isVmsAvailable())
					{
						if (!voicebridgeVmsService.isTelephonyServerConnected())
						{
							voicebridgeVmsService.startup();

							Thread.sleep(2000); // wait for VMS

							if (voicebridgeVmsService.isTelephonyServerConnected())
							{
								Iterator<VoiceBridgeUser> it = voicebridgeLdapService.voicebridgeUserTable.values().iterator();

								while( it.hasNext() )
								{
									VoiceBridgeUser voicebridgeUser = (VoiceBridgeUser)it.next();
									voicebridgeVmsService.setupUser(voicebridgeUser);
								}
							}
						}

					} else {

						if (voicebridgeVmsService.isTelephonyServerConnected())
						{
							voicebridgeVmsService.shutdown();
						}
					}
				}
				catch (Exception e) {
					Log.error("["+ site.getName() + "] checkVMS " + e);
					throw e;
				}

				return true;
			}
		});


		if (voiceMessageComand == null)
		{
			voiceMessageComand = new ManageVoiceMessage(this);
			openlinkManger.addCommand(voiceMessageComand);
		}
	}


	private boolean setVoiceBridgeLdap()
	{
		 Log.info( "["+ site.getName() + "] setVoiceBridgeLdap - creating voicebridgeLdapService object");

		try {
			voicebridgeLdapService.startup();

			if ((ClusterManager.isClusteringEnabled() && ClusterManager.isSeniorClusterMember()) || !ClusterManager.isClusteringEnabled())
			{
				Log.info( "["+ site.getName() + "] exposing component " + "voicebridge" + site.getSiteID());
				componentManager.addComponent("voicebridge" + site.getSiteID(), this);	// only if we are senior or an island node
			}

			loadCachesFromLDAP();	// load up caches

			return voicebridgeLdapService.isLdapAlive();
		}
		catch(Exception e) {
        	Log.error("["+ site.getName() + "] setVoiceBridgeLdap " + e);
        	return false;
        }
	}


	private boolean setVoiceBridgelink()
	{
		Log.info( "["+ site.getName() + "] setVoiceBridgelink - creating voicebridgeLinkService object");

		try {
			voicebridgeLinkService.startup();
			return voicebridgeLinkService.isLinkAlive();
		}
		catch(Exception e) {
        	Log.error("["+ site.getName() + "] setVoiceBridgelink " + e);
        	return false;
        }
	}


    public void getInterestSubscriptions()
    {
		Log.debug( "["+ site.getName() + "] getInterestSubscriptions");

		if (voicebridgeLdapService != null)
		{
			try {
				Iterator<VoiceBridgeUser> iter = voicebridgeLdapService.voicebridgeUserTable.values().iterator();

				while(iter.hasNext())
				{
					VoiceBridgeUser voicebridgeUser = (VoiceBridgeUser)iter.next();

					Iterator<VoiceBridgeInterest> iter2 = voicebridgeUser.getInterests().values().iterator();

					while( iter2.hasNext() )
					{
						VoiceBridgeInterest voicebridgeInterest = (VoiceBridgeInterest)iter2.next();
						voicebridgeLdapService.getInterestSubscriptions(voicebridgeInterest, voicebridgeUser.getUserNo());
					}
				}
			}
			catch(Exception e) {
				Log.error("["+ site.getName() + "] getInterestSubscriptions " + e);
			}
		}
    }

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


    public boolean validateTrueFalse(String value1)
    {
		boolean valid = false;
		String flag = value1.toLowerCase();

		if ("true".equals(flag) || "false".equals(flag))
		{
			valid = true;
		}
		return valid;
	}


	public VMessage getVMId(VoiceBridgeUser voicebridgeUser, String featureId)
	{
		VMessage message = null;

		if (featureId != null && !"".equals(featureId) && voicebridgeUser != null)
		{
			message = voicebridgeVmsService.getVMessageFromName(voicebridgeUser, featureId);
		}

		return message;
	}

    public String makeDialableNumber(String digits)
    {
		String dialableNumber = null;

		if (digits != null && !"".equals(digits))
		{
			String cononicalNumber = formatCanonicalNumber(convertAlpha(digits));

			if (cononicalNumber != null && !"".equals(cononicalNumber))
			{
				dialableNumber = formatDialableNumber(cononicalNumber);
			}

			Log.debug( "["+ site.getName() + "] makeDialableNumber " + digits + "=>" + dialableNumber);
		}

		return dialableNumber;
	}

	private String convertAlpha(String input)
	{
		int inputlength = input.length();
		input = input.toLowerCase();
		String phonenumber = "";

		for (int i = 0; i < inputlength; i++) {
			int character = input.charAt(i);

			switch(character) {
				case '+': phonenumber+="+";break;
				case '*': phonenumber+="*";break;
				case '#': phonenumber+="#";break;
				case '0': phonenumber+="0";break;
				case '1': phonenumber+="1";break;
				case '2': phonenumber+="2";break;
				case '3': phonenumber+="3";break;
				case '4': phonenumber+="4";break;
				case '5': phonenumber+="5";break;
				case '6': phonenumber+="6";break;
				case '7': phonenumber+="7";break;
				case '8': phonenumber+="8";break;
				case '9': phonenumber+="9";break;
				case  'a': case 'b': case 'c': phonenumber+="2";break;
				case  'd': case 'e': case 'f': phonenumber+="3";break;
				case  'g': case 'h': case 'i': phonenumber+="4";break;
				case  'j': case 'k': case 'l': phonenumber+="5";break;
				case  'm': case 'n': case 'o': phonenumber+="6";break;
				case  'p': case 'q': case 'r': case 's': phonenumber+="7";break;
				case  't': case 'u': case 'v': phonenumber+="8";break;
				case  'w': case 'x': case 'y': case 'z': phonenumber+="9";break;
		   }
		}

		return (phonenumber);
	}

	public boolean isComponent(JID jid) {
		final RoutingTable routingTable = XMPPServer.getInstance().getRoutingTable();

		if (routingTable != null)
		{
			return routingTable.hasComponentRoute(jid);
		}
		return false;
	}

	public void setRefreshCacheInterval()
	{
		Log.info( "["+ site.getName() + "] setRefreshCacheInterval ");

		try {


		}
		catch (Exception e)
		{
			Log.error("["+ site.getName() + "] setRefreshCacheInterval " + e);
		}
	}



    private class RefreshCacheCheck extends TimerTask {

        public void run()
        {
			Log.info( "["+ site.getName() + "] RefreshCacheCheck " + voicebridgeLdapService.isCacheExpired());

			if (lastRefreshedDate != null)
			{
				if (voicebridgeLdapService.isCacheExpired())
				{
					loadCachesFromLDAP();
				}

			} else {

				Log.warn("["+ site.getName() + "] RefreshCacheCheck refresh too soon, waiting for inital ldap read to finish");

			}
        }
    }

}
