package com.ifsoft.iftalk.plugin.voicebridge;


import java.io.File;
import java.net.InetSocketAddress;

import java.util.*;
import java.util.concurrent.*;

import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.openfire.event.UserEventDispatcher;
import org.jivesoftware.openfire.event.UserEventListener;
import org.jivesoftware.openfire.admin.AdminManager;
import org.jivesoftware.openfire.http.HttpBindManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.PrivateStorage;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.PresenceManager;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserNotFoundException;

import org.jivesoftware.util.Log;

import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;

import org.jivesoftware.openfire.cluster.ClusterEventListener;
import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;

import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import org.jivesoftware.database.SequenceManager;
import com.ifsoft.iftalk.plugin.tsc.*;
import com.ifsoft.iftalk.plugin.voicebridge.*;

import org.red5.server.webapp.voicebridge.*;


public class VoiceBridgePlugin extends AbstractPlugin implements PropertyEventListener, ClusterEventListener,  UserEventListener, SessionEventListener, PacketInterceptor, VoiceBridgeConstants {

	private static final String NAME 		= "voicebridge";
	private static final String DESCRIPTION = "Redfire voice bridge";
    public Cache<String, String[]> cachedVoiceBridgeCalls;

    private ComponentManager componentManager;
    private UserManager userManager;
    private PresenceManager presenceManager;

    private ExecutorService executor;
	private PrivateStorage privateStorage;
	private SiteDao siteDao;
    private ArrayList<Site> sites;

	private Map<String, VoiceBridgeComponent> components;
	private boolean isShutDown = false;

	public String lastLoadedDate = null;
	public static VoiceBridgePlugin plugin;
	public static Application application;

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

	public void initializePlugin(Application application) {
		Log.info( "["+ NAME + "] Initializing VoiceBridge Plugin");

		VoiceBridgePlugin.plugin = this;
		VoiceBridgePlugin.application = application;

		cachedVoiceBridgeCalls = CacheFactory.createCache("VoiceBridge Calls");
		componentManager 	= ComponentManagerFactory.getComponentManager();
        userManager 		= UserManager.getInstance();
		presenceManager 	= XMPPServer.getInstance().getPresenceManager();
        privateStorage 		= XMPPServer.getInstance().getPrivateStorage();

		SessionEventDispatcher.addListener(this);
		UserEventDispatcher.addListener(this);
        PropertyEventDispatcher.addListener(this);

		InterceptorManager.getInstance().addInterceptor(this);

		executor = Executors.newCachedThreadPool();

        executor.submit(new Callable<Boolean>()
        {
            public Boolean call() throws Exception {
                try {
                    startVoiceBridgeComponents();
					startCluster();
                }
                catch (Exception e) {
                    Log.error("Error initializing VoiceBridge Plugin", e);
                }

                return true;
            }
        });

        lastLoadedDate = String.valueOf(new Date());
	}

	public void destroyPlugin() {
		Log.info( "["+ NAME + "] unloading " + NAME + " plugin resources");

		try {
			SessionEventDispatcher.removeListener(this);
        	UserEventDispatcher.removeListener(this);
			InterceptorManager.getInstance().removeInterceptor(this);

			stopCluster();
			stopVoiceBridgeComponents();
			executor.shutdown();
		}
		catch (Exception e) {
			Log.error("destroyPlugin " + e);
		}
	}

	public String getName() {
		 return NAME;
	}

	public String getDescription() {
		return DESCRIPTION;
	}

	public ComponentManager getComponentManager() {
        return componentManager;
    }

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

    public void joinedCluster()
    {
		Log.info( "["+ NAME + "] joinedCluster");
		controlVoiceBridgeComponents(false);
    }

    public void joinedCluster(byte[] nodeID)
    {

    }

    public void leftCluster()
    {
		Log.info( "["+ NAME + "] leftCluster");
		controlVoiceBridgeComponents(true);
    }

    public void leftCluster(byte[] nodeID)
    {

    }

    public void markedAsSeniorClusterMember()
    {
		Log.info( "["+ NAME + "] markedAsSeniorClusterMember");
		controlVoiceBridgeComponents(true);
    }

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	public synchronized void siteAdded(Site site)
	{
		siteRemoved(site);
		sites.add(site);
		addVoiceBridgeComponent(site);
	}

	public synchronized void resetComponentCaches()
	{
		try {
			Log.info( "["+ NAME + "] resetComponentCaches");

	        Iterator<Site> iter = sites.iterator();

			while(iter.hasNext())
			{
				Site site = (Site)iter.next();
				String siteID = String.valueOf(site.getSiteID());

				if (components.containsKey(siteID))
				{
					VoiceBridgeComponent voicebridgeComponent = components.get(siteID);
					voicebridgeComponent.asyncLoadCachesFromLDAP();
				}

			}
		}
		catch (Exception e) {
			Log.error("["+ NAME + "] resetComponentCaches exception " + e);
		}
	}

	public synchronized void siteUpdated(Site site)
	{
		long siteID = site.getSiteID();
		Iterator<Site> iter = sites.iterator();
		boolean isFound = false;
		Site tmpSite;
		long tmpSiteID;
		String tmpItslinkEnabled;
		String tmpItsAnywhereEnabled;


		while(iter.hasNext() && !isFound) {
			tmpSite               = (Site)iter.next();
			tmpSiteID             = tmpSite.getSiteID();

			if(siteID == tmpSiteID)
			{
				String siteId = String.valueOf(siteID);

				if (components.containsKey(siteId))
				{
					VoiceBridgeComponent voicebridgeComponent = components.get(siteId);
					voicebridgeComponent.site = site;
					voicebridgeComponent.voicebridgeLinkService.site = site;
					voicebridgeComponent.voicebridgeLdapService.site = site;
					voicebridgeComponent.voicebridgeVmsService.site = site;

					Log.info( "["+ NAME + "] siteUpdated - Reseting site " + site.getName());

					voicebridgeComponent.setupTelephoneNumberFormatter();
					voicebridgeComponent.setRefreshCacheInterval();
					voicebridgeComponent.voicebridgeLinkService.allocateCallbacks();
					voicebridgeComponent.checkVMS();
				}

				isFound = true;
				sites.remove(tmpSite);
			}
		}

		sites.add(site);
		//Config.getInstance().terminate();
		//Config.getInstance().initialise(site);
	}

	public synchronized void siteRemoved(Site site)
	{
		long siteID = site.getSiteID();
		Iterator<Site> iter = sites.iterator();
		boolean isFound = false;
		Site tmpSite;
		long tmpSiteID;

		while (iter.hasNext() && !isFound)
		{
			tmpSite		= (Site)iter.next();
			tmpSiteID   = tmpSite.getSiteID();

			if (siteID == tmpSiteID)
			{
				isFound = true;
				removeVoiceBridgeComponent(site);
				sites.remove(tmpSite);
			}
		}
	}


//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

    public void startCluster()
    {
		Log.info( "["+ NAME + "] startCluster - Adding listener");
		ClusterManager.addListener(this);
	}

    private void stopCluster()
    {
		Log.info( "["+ NAME + "] stopCluster - Removing listener");
        ClusterManager.removeListener(this);
	}

    private boolean startVoiceBridgeComponents()
    {
		try {
			Log.info( "["+ NAME + "] startVoiceBridgeComponents");
			loadSites();

			if (components == null)
			{
				components = Collections.synchronizedMap(new HashMap<String, VoiceBridgeComponent>());
			}

	        Iterator<Site> iter = sites.iterator();

			while(iter.hasNext())
			{
				Site site = (Site)iter.next();
				addVoiceBridgeComponent(site);
			}

			return true;
		}
		catch (Exception e) {
			Log.error("["+ NAME + "] startVoiceBridgeComponents exception " + e);
			return false;
		}
	}

	private void loadSites()
	{
		try {
			Log.info( "["+ NAME + "] loadSites - Load All VoiceBridge Sites");

			siteDao = new SiteDao(this);
			sites = (ArrayList) siteDao.getSites();

			if (sites.size() == 0)
			{
				String hostName =  XMPPServer.getInstance().getServerInfo().getHostname();

				Site site = new Site();
				site.setSiteID(SequenceManager.nextID(site));
				site.setName(hostName);
				site.setPrivateHost(hostName);
				site.setPublicHost(hostName);
				site.setDefaultProxy("");
				site.setDefaultExten("");

				siteDao.insert(site);
				sites = (ArrayList) siteDao.getSites();
			}

		}
		catch (Exception e) {
			Log.error("["+ NAME + "] loadSites exception " + e);
		}
	}


	public VoiceBridgeComponent getVoiceBridgeComponentBySiteID(String siteID)
	{
		VoiceBridgeComponent voicebridgeComponent = null;

		if (components.containsKey(siteID))
		{
			voicebridgeComponent = components.get(siteID);
		}

		return voicebridgeComponent;
	}


	private void addVoiceBridgeComponent(Site site)
	{
		try {
			String siteID = String.valueOf(site.getSiteID());

			Log.info( "["+ NAME + "] addVoiceBridgeComponent - Adding site " + siteID + " " + site.getName());
			VoiceBridgeComponent voicebridgeComponent = null;

			if (!components.containsKey(siteID))
			{
				Log.info( "["+ NAME + "] addVoiceBridgeComponent - creating site " + siteID);

				voicebridgeComponent = new VoiceBridgeComponent(site);
				components.put(siteID, voicebridgeComponent);

				Log.info( "["+ NAME + "] addVoiceBridgeComponent - Enabling site " + site.getName());
				voicebridgeComponent.componentEnable();


			} else {

				Log.warn( "["+ NAME + "] addVoiceBridgeComponent - already exists " + site.getName());
			}

		}
		catch (Exception e) {
			Log.error("["+ NAME + "] addVoiceBridgeComponent exception " + e);
		}
	}

    private void stopVoiceBridgeComponents()
    {
		try {
			Log.info( "["+ NAME + "] stopVoiceBridgeComponents - removing all VoiceBridge Components");

	        Iterator<Site> iter = sites.iterator();

			while(iter.hasNext())
			{
				Site site = (Site)iter.next();
				removeVoiceBridgeComponent(site);
			}
		}
		catch (Exception e) {
			Log.error("["+ NAME + "] stopVoiceBridgeComponents exception " + e);
		}
	}


	private void removeVoiceBridgeComponent(Site site)
	{
		try {
			String siteID = String.valueOf(site.getSiteID());

			Log.info( "["+ NAME + "] removeVoiceBridgeComponent - Removing site " + siteID);

			if (components.containsKey(siteID))
			{
				VoiceBridgeComponent voicebridgeComponent = components.get(siteID);
				voicebridgeComponent.componentDestroyed();
				components.remove(siteID);
				voicebridgeComponent = null;
			}

		}
		catch (Exception e) {
			Log.error("["+ NAME + "] removeVoiceBridgeComponent exception " + e);
		}
	}

    private void controlVoiceBridgeComponents(boolean expose)
    {
		try {
			Log.info( "["+ NAME + "] controlVoiceBridgeComponents - control visiblity of all VoiceBridge Components");

	        Iterator<Site> iter = sites.iterator();

			while(iter.hasNext())
			{
				Site site = (Site)iter.next();

				if (expose)
				{
					restoreCacheContent(String.valueOf(site.getSiteID()));
					exposeVoiceBridgeComponent(site);
				}
				else
					hideVoiceBridgeComponent(site);
			}
		}
		catch (Exception e) {
			Log.error("["+ NAME + "] controlVoiceBridgeComponents exception " + e);
		}
	}

	private void exposeVoiceBridgeComponent(Site site)
	{
		try {
			String siteID = String.valueOf(site.getSiteID());

			Log.info( "["+ NAME + "] exposeVoiceBridgeComponent - site " + siteID);

			if (components.containsKey(siteID))
			{
				VoiceBridgeComponent voicebridgeComponent = components.get(siteID);
				componentManager.addComponent("voicebridge" + siteID, voicebridgeComponent);
				voicebridgeComponent.getInterestSubscriptions();
			}

		}
		catch (Exception e) {
			Log.error("["+ NAME + "] exposeVoiceBridgeComponent exception " + e);
		}
	}

	private void hideVoiceBridgeComponent(Site site)
	{
		try {
			String siteID = String.valueOf(site.getSiteID());

			Log.info( "["+ NAME + "] hideVoiceBridgeComponent - site " + siteID);
			componentManager.removeComponent("voicebridge" + siteID);

		}
		catch (Exception e) {
			Log.error("["+ NAME + "] hideVoiceBridgeComponent exception " + e);
		}
	}


    public synchronized void removeCacheContent(VoiceBridgeInterest voicebridgeInterest, VoiceBridgeUserInterest voicebridgeUserInterest, VoiceBridgeCall voicebridgeCall)
    {
		Log.debug("removeCacheContent - remove call " + voicebridgeCall.getCallID());

		if (cachedVoiceBridgeCalls.containsKey(voicebridgeCall.getCallID()))
		{
			cachedVoiceBridgeCalls.remove(voicebridgeCall.getCallID());
		}
	}


    public synchronized void updateCacheContent(VoiceBridgeInterest voicebridgeInterest, VoiceBridgeUserInterest voicebridgeUserInterest, VoiceBridgeCall voicebridgeCall)
    {
		Log.debug("["+ NAME + "] updateCacheContent - update call " + voicebridgeCall.getCallID());

		String[] call = {
							voicebridgeInterest.getSiteName(),						// 0
							voicebridgeInterest.getInterestId(),					// 1
							voicebridgeUserInterest.getUser().getUserNo(),			// 2
							voicebridgeCall.callid,									// 3

							String.valueOf(voicebridgeCall.completionTimeStamp),
							voicebridgeCall.ddi,
							voicebridgeCall.ddiLabel,
							voicebridgeCall.prefix,
							voicebridgeCall.phantomDDI,
							voicebridgeCall.getCLI(),
							voicebridgeCall.getCLILabel(),
							voicebridgeCall.line,
							voicebridgeCall.label,
							voicebridgeCall.getState(),
							voicebridgeCall.console,
							voicebridgeCall.handset,
							voicebridgeCall.direction,
							voicebridgeCall.participation,
							String.valueOf(voicebridgeCall.creationTimeStamp),
							String.valueOf(voicebridgeCall.startTimeStamp),
							String.valueOf(voicebridgeCall.firstTimeStamp),
							voicebridgeCall.proceedingDigits,
							voicebridgeCall.proceedingDigitsLabel,
							voicebridgeCall.getPrivacy(),
							voicebridgeCall.getCallbackActive() ? "true"  : "false",
							voicebridgeCall.getCallbackAvailable() ? "true"  : "false",
							voicebridgeCall.getCallProgress(),
							voicebridgeCall.getVoiceDropActive() ? "true"  : "false"
						};

		cachedVoiceBridgeCalls.put(voicebridgeCall.getCallID(), call);
	}


    public synchronized void restoreCacheContent(String siteId)
    {
		Iterator<String[]> iter = cachedVoiceBridgeCalls.values().iterator();

		Log.info( "["+ NAME + "] restoreCacheContent - cache size " + cachedVoiceBridgeCalls.getCacheSize());

		while(iter.hasNext())
		{
			String[] call = (String[])iter.next();

			Log.debug( "["+ NAME + "] restoreCacheContent - found site " + call[0]);

			if (siteId.equals(call[0]))														// site id
			{
				VoiceBridgeComponent voicebridgeComponent = getVoiceBridgeComponentBySiteID(call[0]);

				if (voicebridgeComponent != null)
				{
					Log.debug( "["+ NAME + "] restoreCacheContent - found interest id " + call[1]);

					if (voicebridgeComponent.voicebridgeLdapService.voicebridgeInterests.containsKey(call[1]))		// interest id
					{
						VoiceBridgeInterest voicebridgeInterest = voicebridgeComponent.voicebridgeLdapService.voicebridgeInterests.get(call[1]);

						if (voicebridgeInterest.getUserInterests().containsKey(call[2])) 			// user no
						{
							Log.debug( "["+ NAME + "] restoreCacheContent - found user id " + call[2]);

							VoiceBridgeUserInterest voicebridgeUserInterest = voicebridgeInterest.getUserInterests().get(call[2]);

							if (call[3] != null && !"".equals(call[3]))
							{
								VoiceBridgeCall voicebridgeCall = voicebridgeUserInterest.createCallById(call[3]);		// call id

								if (voicebridgeCall != null)
								{
									Log.debug( "["+ NAME + "] restoreCacheContent - restore call " + voicebridgeInterest.getInterestId() + voicebridgeUserInterest.getUser().getUserNo() + voicebridgeCall.getCallID());

									voicebridgeCall.callid 						= call[3];
									voicebridgeCall.completionTimeStamp			= Long.parseLong(call[4]);
									voicebridgeCall.ddi 						= call[5];
									voicebridgeCall.ddiLabel					= call[6];
									voicebridgeCall.prefix						= call[7];
									voicebridgeCall.phantomDDI					= call[8];
									voicebridgeCall.setCLI(call[9]);
									voicebridgeCall.setCLILabel(call[10]);
									voicebridgeCall.line 						= call[11];
									voicebridgeCall.label 						= call[12];
									voicebridgeCall.setState(call[13]);
									voicebridgeCall.console 					= call[14];
									voicebridgeCall.handset 					= call[15];
									voicebridgeCall.direction 					= call[16];
									voicebridgeCall.participation 				= call[17];
									voicebridgeCall.creationTimeStamp			= Long.parseLong(call[18]);
									voicebridgeCall.startTimeStamp				= Long.parseLong(call[19]);
									voicebridgeCall.firstTimeStamp				= Long.parseLong(call[20]);
									voicebridgeCall.proceedingDigits  			= call[21];
									voicebridgeCall.proceedingDigitsLabel 		= call[22];
									voicebridgeCall.setPrivacy(call[23]);
									voicebridgeCall.setCallbackActive("true".equals(call[24]));
									voicebridgeCall.setCallbackAvailable("true".equals(call[25]));
									voicebridgeCall.setCallProgress(call[26]);
									voicebridgeCall.setVoiceDropActive("true".equals(call[27]));


									voicebridgeComponent.voicebridgeLinkService.restoreCallState(voicebridgeCall.line, voicebridgeCall.callid);
								}
							}
						}
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


	public void anonymousSessionCreated(Session session)
	{

	}

	public void anonymousSessionDestroyed(Session session)
	{

	}

	public void resourceBound(Session session)
	{

	}

	public void sessionCreated(Session session)
	{
		setSubscriberOnlineStatus(session.getAddress(), true);
	}

	public void sessionDestroyed(Session session)
	{
		setSubscriberOnlineStatus(session.getAddress(), false);
	}

	private void setSubscriberOnlineStatus(JID userAgent, boolean status)
	{
		Log.debug( "["+ NAME + "] setSubscriberOnlineStatus " + userAgent);

		try {

	        Iterator<Site> iter = sites.iterator();

			while(iter.hasNext())
			{
				Site site = (Site)iter.next();
				String siteID = String.valueOf(site.getSiteID());

				VoiceBridgeComponent voicebridgeComponent = components.get(siteID);

				if (voicebridgeComponent != null && voicebridgeComponent.openlinkInterests != null)
				{
					Iterator<VoiceBridgeUserInterest> it = voicebridgeComponent.openlinkInterests.values().iterator();

					while( it.hasNext() )
					{
						VoiceBridgeUserInterest voicebridgeUserInterest = (VoiceBridgeUserInterest)it.next();

						if (voicebridgeUserInterest.isSubscribed(userAgent))
						{
							VoiceBridgeSubscriber voicebridgeSubscriber = voicebridgeUserInterest.getSubscriber(userAgent); // only uses getNode() to fetch subscriber.
							voicebridgeSubscriber.setOnline(status);
							voicebridgeSubscriber.setJID(userAgent); // we need the full JID including resource to get session object

							if (status) // user agent online, re-publish last call event for user interest
							{
								Log.debug( "["+ NAME + "] setSubscriberOnlineStatus publish " + voicebridgeUserInterest.getInterestName());
								voicebridgeComponent.voicebridgeLinkService.publishVoiceBridgeUserCallEvent(voicebridgeUserInterest);
							}
						}
					}
				}
			}

		}
		catch(Exception e) {
			Log.error("["+ NAME + "] setSubscriberOnlineStatus exception " + e);
        	e.printStackTrace();
		}
	}

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


    public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) throws PacketRejectedException
    {
        if (!processed && packet instanceof Message)
        {
			Iterator<Site> iterM = sites.iterator();

			while(iterM.hasNext())
			{
				Site site = (Site)iterM.next();
				String siteID = String.valueOf(site.getSiteID());

				VoiceBridgeComponent voicebridgeComponent = components.get(siteID);
				voicebridgeComponent.interceptMessage((Message) packet);
			}
	 	}

        if (!processed && packet instanceof IQ)
        {
            // Check for the Pub-sub subscriptions

            IQ iq = (IQ)packet;
            Element childElement = iq.getChildElement();

            if (childElement == null || iq.getType() != IQ.Type.result) {
                return;
            }

            String namespace = childElement.getNamespaceURI();

            if ("http://jabber.org/protocol/pubsub".equals(namespace))
            {
                Element subscription = childElement.element("subscription");

                if (subscription == null) {
                    return;
                }

				String node = subscription.attributeValue("node");

                if (node == null) {
                    return;
                }

				Log.debug("interceptPacket subscription " + incoming + "\n"+ iq.toString());

				Iterator<Site> iter = sites.iterator();

				while(iter.hasNext())
				{
					Site site = (Site)iter.next();
					String siteID = String.valueOf(site.getSiteID());

					VoiceBridgeComponent voicebridgeComponent = components.get(siteID);

					if (voicebridgeComponent.openlinkInterests.containsKey(node))
					{
						VoiceBridgeUserInterest voicebridgeUserInterest = voicebridgeComponent.openlinkInterests.get(node);

						if (voicebridgeUserInterest != null)
						{
							JID jid = new JID(subscription.attributeValue("jid"));

							if (voicebridgeComponent.isComponent(jid) || (jid.getNode() != null && (jid.getNode().equals(voicebridgeUserInterest.getUser().getUserId()) || AdminManager.getInstance().isUserAdmin(jid.getNode(), false))))
							{
								VoiceBridgeSubscriber voicebridgeSubscriber = voicebridgeUserInterest.getSubscriber(jid);
								voicebridgeSubscriber.setSubscription(subscription.attributeValue("subscription"));

								setSubscriberDetails(jid, voicebridgeSubscriber);

							} else {

								PacketRejectedException rejected = new PacketRejectedException("Packet rejected with invalid subscription!");
								rejected.setRejectionMessage("Subscription rejected. This user is not authorised to subscribe to Openlink Interest " + node);
								throw rejected;
							}
						}
					}
				}
            }
        }

        if (!processed && packet instanceof IQ)
        {
            // Check for the Pub-sub unsubscribe request

            IQ iq = (IQ)packet;
            Element childElement = iq.getChildElement();

            if (childElement == null || iq.getType() != IQ.Type.set) {
                return;
            }

            String namespace = childElement.getNamespaceURI();

            if ("http://jabber.org/protocol/pubsub".equals(namespace))
            {
                Element unsubscribe = childElement.element("unsubscribe");

                if (unsubscribe == null) {
                    return;
                }

				String node = unsubscribe.attributeValue("node");

                if (node == null) {
                    return;
                }

				Log.debug("interceptPacket unsubscribe " + incoming + "\n"+ iq.toString());

				Iterator<Site> iter = sites.iterator();

				while(iter.hasNext())
				{
					Site site = (Site)iter.next();
					String siteID = String.valueOf(site.getSiteID());

					VoiceBridgeComponent voicebridgeComponent = components.get(siteID);

					if (voicebridgeComponent.openlinkInterests.containsKey(node))
					{
						VoiceBridgeUserInterest voicebridgeUserInterest = voicebridgeComponent.openlinkInterests.get(node);

						if (voicebridgeUserInterest != null)
						{
							voicebridgeUserInterest.removeSubscriber(new JID(unsubscribe.attributeValue("jid")));
						}
					}
				}
            }
        }
    }

    public void setSubscriberDetails(JID jid, VoiceBridgeSubscriber voicebridgeSubscriber)
    {
		if (userManager.isRegisteredUser(jid.getNode()))
		{
			User user = null;

			try {
				user = userManager.getUser(jid.getNode());
			}
			catch(Exception e) { }

			if (user != null)
			{
				voicebridgeSubscriber.setOnline(presenceManager.isAvailable(user));
				voicebridgeSubscriber.setName(user.getName());
				voicebridgeSubscriber.setJID(jid); 				// we need the full JID including resource to get session object
			}
		}
	}

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

    public void userCreated(User user, Map params)
    {

    }

    public void userDeleting(User user, Map params)
    {

    }

    public void userModified(User user, Map params)
    {

    }

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	public boolean isRegisteredUser(String userName)
	{
		return userManager.isRegisteredUser(userName);
	}


    public synchronized void addUserTSC(String userName, String tscJID, String site)
    {
		Log.info( "["+ NAME + "] addUserTSC - " + userName + " " + tscJID + " " + site);

		try
		{
			Document document = DocumentHelper.parseText("<openlink xmlns=\"http://xmpp.org/protocol/openlink:01:00:00#tsc\"></openlink>");

			if(document != null)
			{
				Element searchElement = document.getRootElement();
				Element olElement = privateStorage.get(userName, searchElement);

				boolean foundTSC = false;

				for ( Iterator i = olElement.elementIterator( "tsc" ); i.hasNext(); )
				{
					Element tsc = (Element) i.next();

					if (tscJID.equals(tsc.getText()))
					{
						Log.debug("addUserTSC - found " + tscJID + " " + userName);
						foundTSC = true;
						break;
					}
				}

				if (!foundTSC)
				{
					Log.debug("addUserTSC - adding " + tscJID + " " + userName);

					synchronized (privateStorage)
					{
						olElement = privateStorage.get(userName, searchElement);
						Element tsc = olElement.addElement("tsc");
						tsc.setText(tscJID);
						tsc.addAttribute("name", site);

						privateStorage.add(userName, olElement);
					}
				}
			}
		}
		catch(Exception e)
		{
			Log.error("["+ NAME + "] addUserTSC " + e + tscJID);
		}
	}

    public synchronized String getUserTSC(String userName)
    {
		String foundTSC = "none";

		try
		{
			Document document = DocumentHelper.parseText("<openlink xmlns=\"http://xmpp.org/protocol/openlink:01:00:00#tsc\"></openlink>");

			if(document != null)
			{
				Element searchElement = document.getRootElement();
				Element olElement = privateStorage.get(userName, searchElement);

				for ( Iterator i = olElement.elementIterator( "tsc" ); i.hasNext(); )
				{
					Element tsc = (Element) i.next();
					String theTSC = tsc.getText();

					if ("voicebridge".equals(theTSC.substring(0, 11)))
					{
						foundTSC = theTSC;
						break;
					}
				}

			}
		}
		catch(Exception e)
		{

		}
		return foundTSC;
	}


    public synchronized void removeUserTSC(String userName, String tscJID)
    {
		Log.info( "["+ NAME + "] removeUserTSC - " + userName + " " + tscJID);

		try
		{
			Document document = DocumentHelper.parseText("<openlink xmlns=\"http://xmpp.org/protocol/openlink:01:00:00#tsc\"></openlink>");

			if(document != null)
			{
				Element searchElement = document.getRootElement();
				Element olElement = privateStorage.get(userName, searchElement);

				for ( Iterator i = olElement.elementIterator( "tsc" ); i.hasNext(); )
				{
					Element tsc = (Element) i.next();

					if (tscJID.equals(tsc.getText()))
					{
						Log.debug("removeUserTSC - removing " + tscJID);

						olElement.remove(tsc);
						privateStorage.add(userName, olElement);
						break;
					}
				}
			}
		}
		catch(Exception e)
		{
			Log.error("["+ NAME + "] removeUserTSC " + e + tscJID);
		}
	}

	public String getDomain()
	{
		String hostName =  XMPPServer.getInstance().getServerInfo().getHostname();
		return JiveGlobals.getProperty("xmpp.domain", hostName);
	}

	public VoiceBridgeComponent getComponentByUserId(String userId)
	{
		VoiceBridgeComponent voicebridgeComponent = null;

		String userTSC = getUserTSC(userId);

		int pos = userTSC.indexOf(".");

		if (pos > 11 && "voicebridge".equals(userTSC.substring(0, 11)))
		{
			String siteID = userTSC.substring(11, pos);
			voicebridgeComponent = getVoiceBridgeComponentBySiteID(siteID);
		}

		return voicebridgeComponent;
	}

    public void propertySet(String property, Map params)
    {
		Log.info( "["+ NAME + "] propertySet - " + property);
    }

    public void propertyDeleted(String property, Map params)
    {

    }

    public void xmlPropertySet(String property, Map params) {

    }

    public void xmlPropertyDeleted(String property, Map params) {

    }
}