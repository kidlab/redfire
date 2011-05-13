package org.red5.server.webapp.voicebridge;

import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.*;
import java.text.ParseException;
import java.net.*;
import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.stream.ISubscriberStream;

import com.sun.voip.server.*;
import com.sun.voip.client.*;
import com.sun.voip.*;

import org.jivesoftware.openfire.Connection;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.StreamID;
import org.jivesoftware.openfire.session.LocalClientSession;
import org.jivesoftware.openfire.net.VirtualConnection;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.auth.AuthToken;
import org.jivesoftware.openfire.auth.AuthFactory;
import org.jivesoftware.openfire.SessionPacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.database.SequenceManager;

import org.jivesoftware.openfire.component.InternalComponentManager;

import org.xmpp.packet.*;

import org.dom4j.*;

import com.ifsoft.iftalk.plugin.voicebridge.*;



public class Application extends MultiThreadedApplicationAdapter implements IStreamAwareScopeHandler, CallEventListener  {

 	private String version = "0.0.0.1";
	private Config config;

	private static Site site;
	private static VoiceBridgeComponent component;
	private static Map< String, BridgeParticipant > bridgeParticipants 	= new ConcurrentHashMap< String, BridgeParticipant >();

    private Map< String, LocalClientSession > sessions 				= new ConcurrentHashMap<String, LocalClientSession>();
    private Map< String, IServiceCapableConnection > publishers 	= new ConcurrentHashMap<String, IServiceCapableConnection>();
    private Map< String, String > digests 							= new ConcurrentHashMap<String, String>();



    // ------------------------------------------------------------------------
    //
    // Overide
    //
    // ------------------------------------------------------------------------


    @Override
    public boolean appStart( IScope scope )
    {
		try{
			loginfo( "Red5XMPP starting in scope " + scope.getName() + " " + System.getProperty( "user.dir" ) );
			loginfo(String.format("Red5XMPP version %s", version));

			SiteDao siteDao = new SiteDao(null);
			ArrayList<Site> sites = (ArrayList) siteDao.getSites();

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

			site = sites.get(0);

			config = Config.getInstance();
			config.initialise(site);

			String appPath = System.getProperty("user.dir");
			//String logDir = appPath + File.separator + "log" + File.separator;
			String logDir = appPath + File.separator + ".." + File.separator + "logs" + File.separator;


			Properties properties = new Properties();

			System.setProperty("com.sun.voip.server.LOGLEVEL", "99");
			System.setProperty("com.sun.voip.server.FIRST_RTP_PORT", "3200");
			System.setProperty("com.sun.voip.server.LAST_RTP_PORT", "3299");
			System.setProperty("com.sun.voip.server.Bridge.logDirectory", logDir);
			System.setProperty("com.sun.voip.server.BRIDGE_LOG", "bridge.log");
			System.setProperty("com.sun.voip.server.LOGLEVEL", "99");
			System.setProperty("com.sun.voip.server.PUBLIC_IP_ADDRESS", config.getPublicHost());

			properties.setProperty("javax.sip.STACK_NAME", "JAIN SIP 1.1");
			properties.setProperty("javax.sip.RETRANSMISSION_FILTER", "on");
			properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "99");
			properties.setProperty("gov.nist.javax.sip.SERVER_LOG", logDir + "sip_server.log");
			properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", logDir + "sip_debug.log");
			properties.setProperty("javax.sip.IP_ADDRESS",config.getPrivateHost());

			Bridge.setPublicHost(config.getPublicHost());
			Bridge.setPrivateHost(config.getPrivateHost());
			Bridge.setBridgeLocation("VB" + site.getSiteID());

			new SipServer(config, properties);

		} catch (Exception e) {

			e.printStackTrace();
		}
        return true;
    }


    @Override
    public void appStop( IScope scope )
    {
        loginfo( "Red5XMPP stopping in scope " + scope.getName() );

		IConnection conn = Red5.getConnectionLocal();
		IServiceCapableConnection service = (IServiceCapableConnection) conn;

		CallHandler.shutdown();
		config.terminate();

        for (LocalClientSession session : sessions.values())
        {
			session.close();
			session = null;
		}
    }


    @Override
    public boolean appConnect( IConnection conn, Object[] params ) {

        IServiceCapableConnection service = (IServiceCapableConnection) conn;
        loginfo( "Red5XMPP Client connected " + conn.getClient().getId() + " service " + service );

        return true;
    }


    @Override
    public boolean appJoin( IClient client, IScope scope ) {

        loginfo( "Red5XMPP Client joined app " + client.getId() );
        IConnection conn = Red5.getConnectionLocal();
        IServiceCapableConnection service = (IServiceCapableConnection) conn;

        return true;
    }


    @Override
    public void appLeave( IClient client, IScope scope ) {

        IConnection conn = Red5.getConnectionLocal();
		IServiceCapableConnection thisService = (IServiceCapableConnection) conn;

        loginfo( "Red5XMPP Client leaving app " + client.getId() );

        if (digests.containsKey(client.getId()))
        {
			xmppDisconnect(digests.get(client.getId()));
		}
    }

    @Override
	public void streamBroadcastStart(IBroadcastStream stream)
	{
        loginfo( "Red5XMPP streamBroadcastStart " + stream.getPublishedName() );
        IConnection conn = Red5.getConnectionLocal();
        IServiceCapableConnection service = (IServiceCapableConnection) conn;

        publishers.put(stream.getPublishedName(), service);
	}

    @Override
	public void streamBroadcastClose(IBroadcastStream stream)
	{
        loginfo( "Red5XMPP streamBroadcastClose " + stream.getPublishedName() );

        publishers.remove(stream.getPublishedName());
	}


    // ------------------------------------------------------------------------
    //
    // XMPP
    //
    // ------------------------------------------------------------------------



    public String xmppConnect(String username, String password, String peerId)
    {
		IConnection conn = Red5.getConnectionLocal();
		IServiceCapableConnection service = (IServiceCapableConnection) conn;

		String digest = AuthFactory.createDigest(username, password);

        loginfo( "Red5XMPP xmppConnect " + username + " service " + service + " digest " + digest);

        if (sessions.containsKey(digest))
        {
			LocalClientSession session = sessions.get(digest);

			try {
				RedfireConnection connection = (RedfireConnection) session.getConnection();
				connection.setService(service, peerId);
				service.invoke("xmppConnect", new Object[] {digest, peerId});
				return digest;

			} catch (Exception e) {
				logerror("Red5XMPP xmppConnect " + e);
				return null;
			}

		} else {

			RedfireConnection connection = new RedfireConnection(service, peerId, digest);
			LocalClientSession session = SessionManager.getInstance().createClientSession(connection, new BasicStreamID(peerId));
			connection.setRouter(new SessionPacketRouter(session));

			try {
				AuthToken authToken = AuthFactory.authenticate(username, password);
				session.setAuthToken(authToken, peerId);
				sessions.put(digest, session);
				service.invoke("xmppConnect", new Object[] {digest, peerId});

				if (digests.containsKey(conn.getClient().getId()) == false)
				{
					digests.put(conn.getClient().getId(), digest);
				}


				return digest;

			} catch (Exception e) {
				logerror("Red5XMPP xmppConnect " + e);
				return null;
			}
		}
    }

    public void xmppDisconnect(String digest)
    {
        loginfo( "Red5XMPP xmppDisconnect " + digest);

        if (sessions.containsKey(digest))
        {
			LocalClientSession session = sessions.remove(digest);

			session.close();
			session = null;
		}
    }

    public void xmppSend(String digest, String xml)
    {
        loginfo( "Red5XMPP xmppSend \n" + xml);

        if (sessions.containsKey(digest))
        {
			LocalClientSession session = sessions.get(digest);

			try {
				RedfireConnection connection = (RedfireConnection) session.getConnection();
				connection.getRouter().route(DocumentHelper.parseText(xml).getRootElement());

			} catch (Exception e) {

				logerror("Red5XMPP xmppSend " + e);
			}
		}
    }

    // ------------------------------------------------------------------------
    //
    // ScreenShare
    //
    // ------------------------------------------------------------------------

	public void mousePress(String streamName, Integer button)
	{
		loginfo("Red5XMPP mousePress " + button + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("mousePress", new Object[] {button});

		} else logerror("mousePress stream not found " + streamName);
	}

	public void mouseRelease(String streamName, Integer button)
	{
		loginfo("Red5XMPP mouseRelease " + button + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("mouseRelease", new Object[] {button});

		} else logerror("mouseRelease stream not found " + streamName);
	}

	public void doubleClick(String streamName, Integer x, Integer y, Integer width, Integer height)
	{
		loginfo("Red5XMPP doubleClick " + x + " " + y + " " + width + " " + height + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("doubleClick", new Object[] {x, y, width, height});

		} else logerror("doubleClick stream not found " + streamName);
	}

	public void keyPress(String streamName, Integer key)
	{
		loginfo("Red5XMPP keyPress " + key + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("keyPress", new Object[] {key});

		} else logerror("keyPress stream not found " + streamName);
	}

	public void keyRelease(String streamName, Integer key)
	{
		loginfo("Red5XMPP keyRelease " + key + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("keyRelease", new Object[] {key});

		} else logerror("keyRelease stream not found " + streamName);
	}

	public void mouseMove(String streamName, Integer x, Integer y, Integer width, Integer height)
	{
		loginfo("Red5XMPP mouseMove " + x + " " + y + " " + width + " " + height + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("mouseMove", new Object[] {x, y, width, height});

		} else logerror("mouseMove stream not found " + streamName);
	}


	private IServiceCapableConnection getPublisher(String streamName)
	{
		IServiceCapableConnection service = null;

		if (publishers.containsKey(streamName))
		{
			service = publishers.get(streamName);
		}

		return service;
	}


    // ------------------------------------------------------------------------
    //
    // VoiceBridge
    //
    // ------------------------------------------------------------------------


	private void connectToMUC(final BridgeParticipant bp)
	{
		final JID jid = bp.participantJID;
		final CallParticipant cp = bp.callParticipant;

    	loginfo("VoiceBridge connectToMUC " + jid + " " + cp.getConferenceId() + " " + cp.getCallId());

		final String conferenceId = cp.getConferenceId();

		Message message = new Message();
		message.setFrom(jid);
		message.setTo(jid);
		Element invite = message.addChildElement("x", "jabber:x:conference");
		invite.addAttribute("jid", conferenceId + "@conference." + getComponent().getDomain());
		getComponent().sendPacket(message);

		loginfo("VoiceBridge connectToMUC outgoing message \n" + message);
	}


	private void disconnectFromMUC(BridgeParticipant bp)
	{
		JID jid = bp.participantJID;
		CallParticipant cp = bp.callParticipant;

    	loginfo("VoiceBridge disconnectFromMUC " + jid + " " + cp.getConferenceId() + " " + cp.getCallId());

		String conferenceId = cp.getConferenceId();

		// <presence type="unavailable" to="1111@conference.btg199251/Kimme"/>

		Presence presence = new Presence(Presence.Type.unavailable);
		presence.setTo(conferenceId + "@conference." + getComponent().getDomain() + "/" + jid.getNode());
        presence.addChildElement("x", "http://jabber.org/protocol/muc");
		presence.setFrom(jid);

		getComponent().sendPacket(presence);

		loginfo("VoiceBridge disconnectFromMUC outgoing presence \n" + presence);
	}

    public String manageCallParticipant(JID userJID, String uid, String parameter, String value)
    {
		String response = null;

		try {
			loginfo("VoiceBridge manageParticipant");

			if ( bridgeParticipants.containsKey(uid) == false)
			{
				CallParticipant cp = new CallParticipant();
				cp.setCallId(uid);

				BridgeParticipant bp = new BridgeParticipant();
				bp.userJID = userJID;
				bp.callParticipant = cp;

				bridgeParticipants.put(uid, bp);
			}

			response = parseCallParameters(parameter, value, bridgeParticipants.get(uid), uid);

			if ("CancelCall".equalsIgnoreCase(parameter))
			{
				try {
					BridgeParticipant bp = bridgeParticipants.remove(uid);
					CallParticipant cp = bp.callParticipant;

					if (bp.participantJID != null)
					{
						disconnectFromMUC(bp);
					}

					bp = null;

				} catch (Exception e) {

					response = e.toString();
				}
			}

		} catch (Exception e) {

			response = (e.toString());
		}

		return response;
	}


	public void handlePostBridge(List<String> uids)
	{
		for (String uid : uids)
		{
    		loginfo("VoiceBridge handlePostBridge " + uid);

			BridgeParticipant bp = bridgeParticipants.get(uid);

			if (bp.participantJID != null)
			{
				CallParticipant cp = bp.callParticipant;


			}
		}
	}


    public String manageVoiceBridge(String parameter, String value)
    {
		String response = null;
    	loginfo("VoiceBridge manageConference");

		try {

			parseBridgeParameters(parameter, value);

		} catch (Exception e) {

			response = (e.toString());
		}
		return response;
	}


    private String makeOutgoingCall(BridgeParticipant bp, String uid)
    {
		CallParticipant cp = bp.callParticipant;

		String response = null;
    	loginfo("VoiceBridge makeOutgoingCall " + uid + " " + cp.getCallId() + " " + bp.participantJID);

		try {

			response = validateAndAdjustParameters(bp);

			if (response == null)
			{
				if (cp.getSecondPartyNumber() == null)
				{
					if (cp.getPhoneNumber() != null)
					{
						OutgoingCallHandler outgoingCallHandler = new OutgoingCallHandler(this, cp);
						outgoingCallHandler.start();

						bp.callHandler = outgoingCallHandler;
					}

					if (bp.participantJID != null)
					{
						connectToMUC(bp);
					}

				} else {

					TwoPartyCallHandler twoPartyCallHandler = new TwoPartyCallHandler(this, cp);
					twoPartyCallHandler.start();

					bp.callHandler = twoPartyCallHandler;
				}

			}

		} catch (Exception e) {

			response = (e.toString());
		}

		return response;
	}

    private String migrateCall(BridgeParticipant bp)
    {
		CallParticipant cp = bp.callParticipant;
		String response = null;
    	loginfo("VoiceBridge migrateCall");

		try {

			response = validateAndAdjustParameters(bp);

			if (response == null)
			{
				if (cp.migrateCall())
				{
					new CallMigrator(this, cp).start();

				} else {

					response = ("Call participant is not configured for migration");
				}
			}

		} catch (Exception e) {

			response = (e.toString());
		}

		return response;
	}


   private void parseBridgeParameters(String parameter, String value)
    {
		if ("nAvg".equalsIgnoreCase(parameter))
		{
			try {
				LowPassFilter.setNAvg(getInteger(value));
			} catch (Exception e) {
				logerror("parseBridgeParameters : nAvg " + value + " is not numeric" );
			}
			return;
		}

		if ("lpfv".equalsIgnoreCase(parameter))
		{
			try {
				LowPassFilter.setLpfVolumeAdjustment(getDouble(value));
			} catch (Exception e) {
				logerror("parseBridgeParameters : lpfv " + value + " is not numeric" );
			}
			return;
		}

		if ("forceGatewayError".equalsIgnoreCase(parameter))
		{
			try {
				SipTPCCallAgent.forceGatewayError(stringToBoolean(value));
			} catch (ParseException e) {
				logerror(e.toString());
			}
			return;
		}

		if ("addCallToWhisperGroup".equalsIgnoreCase(parameter))
		{
			String[] tokens = value.split(":");

			if (tokens.length != 2)
			{
				logerror("You must specify both a whisperGroupId and a callId");
				return;
			}

			String whisperGroupId = tokens[0];
			String callId = tokens[1];

			CallHandler callHandler = CallHandler.findCall(callId);

			if (callHandler == null)
			{
				logerror("Invalid callId:  " + callId);
				return;
			}

			try {
				callHandler.getMember().addCall(whisperGroupId);
			} catch (ParseException e) {
				logerror(e.toString());
			}

			return;
		}

		if ("allowShortNames".equalsIgnoreCase(parameter))
		{
			try {
				ConferenceManager.setAllowShortNames(stringToBoolean(value));
			} catch (ParseException e) {
				logerror(e.toString());
			}
			return;
		}

		if ("bridgeLocation".equalsIgnoreCase(parameter))
		{
			Bridge.setBridgeLocation(value);
			return;
		}

		if ("cancelCall".equalsIgnoreCase(parameter))
		{
			CallHandler.hangup(value, "User requested call termination");
			return;
		}

		if ("cancelMigration".equalsIgnoreCase(parameter))
		{
			CallMigrator.hangup(value, "User requested call termination");
			return;
		}

		if ("cnThresh".equalsIgnoreCase(parameter))
		{
			try {
				CallHandler.setCnThresh(getQualifierString(value), getInteger(value));
			} catch (ParseException e) {
				logerror(e.toString());
			}
            return;
		}

		if ("comfortNoiseType".equalsIgnoreCase(parameter))
		{
			try {
				MemberSender.setComfortNoiseType(getInteger(value));
			} catch (ParseException e) {
				logerror(e.toString());
			}
            return;
		}

		if ("comfortNoiseLevel".equalsIgnoreCase(parameter))
		{
			try {
				RtpPacket.setDefaultComfortNoiseLevel((byte) getInteger(value));
			} catch (ParseException e) {
				logerror(e.toString());
			}
            return;
		}

		if ("commonMixDefault".equalsIgnoreCase(parameter))
		{
			try {
				WhisperGroup.setCommonMixDefault(stringToBoolean(value));
			} catch (ParseException e) {
				logerror(e.toString());
			}
			return;
		}

		if ("conferenceInfoShort".equalsIgnoreCase(parameter))
		{
			loginfo(ConferenceManager.getAbbreviatedConferenceInfo());
			return;
		}

		if ("conferenceInfo".equalsIgnoreCase(parameter))
		{
			loginfo(ConferenceManager.getDetailedConferenceInfo());
			return;
		}

		if ("createConference".equalsIgnoreCase(parameter))
		{
			String[] tokens = value.split(":");

			if (tokens.length < 2)
			{
				logerror("Missing parameters");
				return;
			}

			String conferenceId = tokens[0];

			if (tokens[1].indexOf("PCM") != 0 && tokens[1].indexOf("SPEEX") != 0)
			{
				logerror("invalid media specification");
				return;
			}

			String mediaPreference = tokens[1];

			String displayName = null;

			if (tokens.length > 2)
			{
				displayName = tokens[2];
			}

			try {
				ConferenceManager.createConference(conferenceId, mediaPreference, displayName);
			} catch (ParseException e) {
				logerror(e.toString());
			}

			return;
		}

		if ("createWhisperGroup".equalsIgnoreCase(parameter))
		{
			String[] tokens = value.split(":");

			if (tokens.length < 2)
			{
				logerror("You must specify both a conferenceId and a whisperGroupId");
				return;
			}

			try {
				String conferenceId = tokens[0];
				String whisperGroupId = tokens[1];

				double attenuation = WhisperGroup.getDefaultAttenuation();

				if (tokens.length == 3)
				{
					attenuation = getDouble(tokens[2]);
				}

				ConferenceManager.createWhisperGroup(conferenceId,	whisperGroupId, attenuation);

			} catch (ParseException e) {

				logerror("Can't create Whisper group " + tokens[1] + " " + e.getMessage());
			}
			return;
		}

		if ("deferMixing".equalsIgnoreCase(parameter))
		{
			try {
				MemberReceiver.deferMixing(getBoolean(value));
			} catch (ParseException e) {
				logerror(e.toString());
			}
			return;
		}

		if ("destroyWhisperGroup".equalsIgnoreCase(parameter))
		{
			String[] tokens = value.split(":");

			if (tokens.length != 2)
			{
				logerror("You must specify both a conferenceId and a whisperGroupId");
				return;
			}

			try {
				ConferenceManager.destroyWhisperGroup(tokens[0], tokens[1]);
			} catch (ParseException e) {
				logerror(e.toString());
			}

			return;
		}

		if ("callAnswerTimeout".equalsIgnoreCase(parameter))
		{
			try {
				CallSetupAgent.setDefaultCallAnswerTimeout(getInteger(value));
			} catch (Exception e) {
				logerror("callAnswerTimeout " + value + " is not numeric" );
			}
			return;
		}

		if ("doNotRecord".equalsIgnoreCase(parameter))
		{
			try {
				boolean booleanValue = getBoolean(value);
				String callId = getQualifierString(value);

				if (callId == null)
				{
					logerror("Call id is missing");
					return;
				}

				try {

					CallHandler.setDoNotRecord(callId, booleanValue);

				} catch (NoSuchElementException e) {
					logerror("Invalid callId specified:  " + value);
				}

			} catch (Exception e) {
				logerror(e.toString());
			}

			return;
		}

		if ("dtmfSuppression".equalsIgnoreCase(parameter))
		{
			try {
				boolean booleanValue = getBoolean(value);
				String callId = getQualifierString(value);

				if (callId == null)
				{
					logerror("Call id is missing");
					return;
				}

				try {

					CallHandler.setDtmfSuppression(callId, booleanValue);

				} catch (NoSuchElementException e) {
					logerror("Invalid callId specified:  " + value);
				}

			} catch (Exception e) {
				logerror(e.toString());
			}

			return;
		}

		if ("drop".equalsIgnoreCase(parameter))
		{
			try {
				int integerValue = getInteger(value);
				String callId = getQualifierString(value);

				if (callId == null)
				{
					logerror("Call id is missing");
					return;
				}

				try {

					CallHandler.setDropPackets(callId, integerValue);

				} catch (NoSuchElementException e) {
					logerror("Invalid callId specified:  " + value);
				}

			} catch (Exception e) {
				logerror(e.toString());
			}

			return;
		}

		if ("duplicateCallLimit".equalsIgnoreCase(parameter))
		{
			try {
				CallHandler.setDuplicateCallLimit(getInteger(value));
			} catch (Exception e) {
				logerror("duplicateCallLimit " + value + " is not numeric" );
			}
			return;
		}


		if ("directConferencing".equalsIgnoreCase(parameter))
		{
			try {
				IncomingCallHandler.setDirectConferencing(getBoolean(value));
			} catch (ParseException e) {
				logerror(e.toString());
			}
			return;
		}

		if ("distributedConferenceInfo".equalsIgnoreCase(parameter))
		{
			loginfo(ConferenceManager.getDistributedConferenceInfo());
			return;
		}

		if ("dropDb".equalsIgnoreCase(parameter))
		{
			ConferenceManager.dropDb();
			return;
		}

		if ("enablePSTNCalls".equalsIgnoreCase(parameter))
		{
			try {
				CallHandler.enablePSTNCalls(getBoolean(value));
			} catch (ParseException e) {
				logerror(e.toString());
			}
			return;
		}

		if ("endConference".equalsIgnoreCase(parameter))
		{
			try {
				ConferenceManager.endConference(value);

			} catch (ParseException e) {
				logerror(e.toString());
			}
			return;
		}

		if ("firstRtpPort".equalsIgnoreCase(parameter))
		{
			try {
				ConferenceMember.setFirstRtpPort(getInteger(value));
			} catch (Exception e) {
				logerror("firstRtpPort " + value + " is not numeric" );
			}
			return;
		}

		if ("setForcePrivateMix".equalsIgnoreCase(parameter))
		{
			try {
				MixManager.setForcePrivateMix(getBoolean(value));
			} catch (ParseException e) {
				logerror(e.toString());
			}
			return;
		}

		if ("forwardData".equalsIgnoreCase(parameter))
		{
			try {
				String[] tokens = value.split(":");

				if (tokens.length < 2)
				{
					logerror("Missing parameters:  " + value);
					return;
				}

				CallHandler dest = CallHandler.findCall(tokens[0]);

				if (dest == null)
				{
					logerror("Invalid callId:  " + tokens[0]);
					return;
				}

				CallHandler src = CallHandler.findCall(tokens[1]);

				if (src == null)
				{
					logerror("Invalid callId:  " + tokens[1]);
					return;
				}

				src.getMember().getMemberReceiver().addForwardMember(dest.getMember().getMemberSender());

			} catch (Exception e) {
				logerror(e.toString());
			}
			return;
		}


		if ("forwardDtmfKeys".equalsIgnoreCase(parameter))
		{
			try {
				MemberReceiver.setForwardDtmfKeys(getBoolean(value));
			} catch (ParseException e) {
				logerror(e.toString());
			}
			return;
		}

		if ("gc".equalsIgnoreCase(parameter))
		{
			System.gc();
			return;
		}

		if ("gcs".equalsIgnoreCase(parameter))
		{
			loginfo(CallHandler.getCallStateForAllCalls());
			return;
		}

		if ("getCallState".equalsIgnoreCase(parameter))
		{
			String callId = getString(value);

			CallHandler callHandler = CallHandler.findCall(callId);

			if (callHandler == null)
			{
				logerror("Invalid callId:  " + callId);
				return;
			}

			loginfo(callHandler.getCallState());

			return;
		}

		if ("getAllAbbreviatedMixDescriptors".equalsIgnoreCase(parameter))
		{
			loginfo(CallHandler.getAllAbbreviatedMixDescriptors());
			return;
		}

		if ("getAbbreviatedMixDescriptors".equalsIgnoreCase(parameter))
		{
			String callId = getString(value);

			CallHandler callHandler = CallHandler.findCall(callId);

			if (callHandler == null)
			{
				logerror("Invalid callId:  " + callId);
				return;
			}

	        loginfo(callHandler.getMember().getAbbreviatedMixDescriptors());
			return;
		}

		if ("getAllMixDescriptors".equalsIgnoreCase(parameter))
		{
			loginfo(CallHandler.getAllMixDescriptors());
			return;
		}

		if ("getMixDescriptors".equalsIgnoreCase(parameter))
		{
			String callId = getString(value);

			CallHandler callHandler = CallHandler.findCall(callId);

			if (callHandler == null)
			{
				logerror("Invalid callId:  " + callId);
				return;
			}

	        loginfo(callHandler.getMember().getMixDescriptors());
			return;
		}

		if ("getStatistics".equalsIgnoreCase(parameter))
		{

/*
				(  service ).invoke( "statisticsNotification", new Object[] { String.valueOf(ConferenceManager.getNumberOfConferences()),
																			  String.valueOf(ConferenceManager.getTotalMembers()),
																			  String.valueOf(CallHandler.getTotalSpeaking()),
																			  String.valueOf(Math.round(ConferenceSender.getTimeBetweenSends() * 10000) / 10000.),
																			  String.valueOf(Math.round(ConferenceSender.getAverageSendTime() * 10000) / 10000.),
																			  String.valueOf(Math.round(ConferenceSender.getMaxSendTime() * 10000) / 10000.)

																			});
*/

		}

		if ("getBriefConferenceInfo".equalsIgnoreCase(parameter))
		{
			loginfo(ConferenceManager.getBriefConferenceInfo());
			return;
		}

		if ("gc".equalsIgnoreCase(parameter))
		{
			System.gc();
			return;
		}

		if ("incomingCallTreatment".equalsIgnoreCase(parameter))
		{
			IncomingCallHandler.setIncomingCallTreatment(value);
			return;
		}

		if ("incomingCallVoiceDetection".equalsIgnoreCase(parameter))
		{
			try {
				IncomingCallHandler.setIncomingCallVoiceDetection(getBoolean(value));
			} catch (ParseException e) {
				logerror(e.toString());
			}
			return;
		}

		if ("internationalPrefix".equalsIgnoreCase(parameter))
		{
			if (value.equals("\"\"") || value.equals("''"))
			{
				value = "";
			}

			config.setInternationalPrefix(value);
			return;
		}

		if ("internalExtenLength".equalsIgnoreCase(parameter))
		{
			try {
				config.setInternalExtenLength(getInteger(value));
			} catch (Exception e) {
				logerror("internalExtenLength " + value + " is not numeric" );
			}
		}

		if ("conferenceJoinTreatment".equalsIgnoreCase(parameter))
		{
			try {
				String[] tokens = value.split(":");

				if (tokens.length != 2)
				{
				   logerror("conferenceJoinTreatment requires two inputs");
				   return;
				}

				ConferenceManager.setConferenceJoinTreatment(tokens[1], tokens[0]);

			} catch (ParseException e) {
				logerror(e.toString());
			}
			return;
		}

		if ("conferenceLeaveTreatment".equalsIgnoreCase(parameter))
		{
			try {
				String[] tokens = value.split(":");

				if (tokens.length != 2)
				{
				   logerror("conferenceLeaveTreatment requires two inputs");
				   return;
				}

				ConferenceManager.setConferenceLeaveTreatment(tokens[1], tokens[0]);

			} catch (ParseException e) {
				logerror(e.toString());
			}
			return;
		}


		if ("lastRtpPort".equalsIgnoreCase(parameter))
		{
			try {
				ConferenceMember.setLastRtpPort(getInteger(value));
			} catch (Exception e) {
				logerror("lastRtpPort " + value + " is not numeric" );
			}
			return;
		}


		if ("loneReceiverPort".equalsIgnoreCase(parameter))
		{
			try {
				ConferenceManager.setLoneReceiverPort(getInteger(value));
			} catch (Exception e) {
				logerror("loneReceiverPort " + value + " is not numeric" );
			}
			return;
		}

		if ("longDistancePrefix".equalsIgnoreCase(parameter))
		{
			if (value.equals("\"\"") || value.equals("''"))
			{
				value = "";
			}

			config.setLongDistancePrefix(value);
			return;
		}

		if ("migrateToBridge".equalsIgnoreCase(parameter))
		{
			try {
				String[] tokens = value.split(":");

				if (tokens.length != 3)
				{
				   logerror("migrateToBridge requires three inputs");
				   return;
				}

				String bridge = tokens[0];
				String port = tokens[1];
				String callId = tokens[2];

				CallHandler callHandler = CallHandler.findCall(callId);

				if (callHandler == null)
				{
					logerror("Invalid callId: " + callId);
					return;
				}

				CallParticipant cp = callHandler.getCallParticipant();

				if (cp.getInputTreatment() != null)
				{
					cp.setPhoneNumber(null);
				}
/*
				BridgeConnector bridgeConnector;

				int serverPort;

				try {
					serverPort = Integer.parseInt(port);

				} catch (NumberFormatException e) {

					logerror("Invalid bridge server port:  " + port);
					return;
				}

				try {
					bridgeConnector = new BridgeConnector(bridge, serverPort, 5000);

				} catch (IOException e) {
					logerror("Unable to connect to bridge " + bridge	+ " " + e.getMessage());
					return;
				}

				callHandler.suppressStatus(true);

				try {
					String s = cp.getCallSetupRequest();
					s = s.substring(0, s.length() - 1);  // get rid of last new line
					bridgeConnector.sendCommand(s);

				} catch (IOException e) {

					logerror("Unable to send command to bridge:  " + e.getMessage());
					return;
				}

				bridgeConnector.addCallEventListener(requestHandler);

				// XXX need to figure out how to deal with Private Mixes now that the call has moved!
				//TODO convert to RTMP or Openlink
*/
			} catch (Exception e) {
				logerror(e.toString());
			}
			return;
		}

		if ("senderThreads".equalsIgnoreCase(parameter))
		{
			try {
				ConferenceSender.setSenderThreads(getInteger(value));
			} catch (Exception e) {
				logerror("senderThreads " + value + " is not numeric" );
			}
			return;
		}

		if ("minJitterBufferSize".equalsIgnoreCase(parameter))
		{
			try {
				String[] tokens = value.split(":");

				if (tokens.length != 2)
				{
				   logerror("minJitterBufferSize requires two inputs");
				   return;
				}

				int minJitterBufferSize = getInteger(tokens[0]);
				CallHandler callHandler = CallHandler.findCall(tokens[1]);

				if (callHandler == null)
				{
					logerror("Invalid callId:  " + tokens[1]);
					return;
				}

				callHandler.getMember().getMemberReceiver().setMinJitterBufferSize(minJitterBufferSize);

			} catch (Exception e) {
				logerror(e.toString());
			}
			return;
		}

		if ("maxJitterBufferSize".equalsIgnoreCase(parameter))
		{
			try {
				String[] tokens = value.split(":");

				if (tokens.length != 2)
				{
				   logerror("maxJitterBufferSize requires two inputs");
				   return;
				}

				int minJitterBufferSize = getInteger(tokens[0]);
				CallHandler callHandler = CallHandler.findCall(tokens[1]);

				if (callHandler == null)
				{
					logerror("Invalid callId:  " + tokens[1]);
					return;
				}

				callHandler.getMember().getMemberReceiver().setMaxJitterBufferSize(minJitterBufferSize);

			} catch (Exception e) {
				logerror(e.toString());
			}
			return;
		}

		if ("monitorCallStatus".equalsIgnoreCase(parameter))
		{
			try {
				boolean booleanValue = getBoolean(value);
				String callId = getQualifierString(value);

				if (callId == null)
				{
					logerror("Call id is missing");
					return;
				}

				try {
					CallHandler callHandler = CallHandler.findCall(callId);

					if (callHandler == null)
					{
						logerror("No such callId:  " + callId);
						return;
					}

					if (booleanValue == true)
					{
						callHandler.addCallEventListener(this);

					} else {

						callHandler.removeCallEventListener(this);
					}

				} catch (NoSuchElementException e) {
					logerror("Invalid callId specified:  " + value);
				}

			} catch (Exception e) {
				logerror(e.toString());
			}

			return;
		}


		if ("muteCall".equalsIgnoreCase(parameter))
		{
			try {
				boolean booleanValue = getBoolean(value);
				String callId = getQualifierString(value);

				if (callId == null)
				{
					logerror("Call id is missing");
					return;
				}

				try {
					CallHandler callHandler = CallHandler.findCall(callId);

					if (callHandler == null)
					{
						logerror("No such callId:  " + callId);
						return;
					}

	    			CallHandler.setMuted(callId, booleanValue);

				} catch (NoSuchElementException e) {
					logerror("Invalid callId specified:  " + value);
				}

			} catch (Exception e) {
				logerror(e.toString());
			}

			return;
		}

		if ("muteWhisperGroup".equalsIgnoreCase(parameter))
		{
			try {
				boolean booleanValue = getBoolean(value);
				String callId = getQualifierString(value);

				if (callId == null)
				{
					logerror("Call id is missing");
					return;
				}

				try {
					CallHandler callHandler = CallHandler.findCall(callId);

					if (callHandler == null)
					{
						logerror("No such callId:  " + callId);
						return;
					}

	    			CallHandler.setMuteWhisperGroup(callId, booleanValue);

				} catch (NoSuchElementException e) {
					logerror("Invalid callId specified:  " + value);
				}

			} catch (Exception e) {
				logerror(e.toString());
			}

			return;
		}

		if ("muteConference".equalsIgnoreCase(parameter))
		{
			try {
				boolean booleanValue = getBoolean(value);
				String callId = getQualifierString(value);

				if (callId == null)
				{
					logerror("Call id is missing");
					return;
				}

				try {
					CallHandler callHandler = CallHandler.findCall(callId);

					if (callHandler == null)
					{
						logerror("No such callId:  " + callId);
						return;
					}

	    			CallHandler.setConferenceMuted(callId, booleanValue);

				} catch (NoSuchElementException e) {
					logerror("Invalid callId specified:  " + value);
				}

			} catch (Exception e) {
				logerror(e.toString());
			}

			return;
		}

		if ("numberOfCalls".equalsIgnoreCase(parameter))
		{
			try {
				String[] tokens = value.split(":");

				if (tokens.length != 1)
				{
					logerror("You must specify a conference id");
					return;
				}

				CallEvent event = new CallEvent(CallEvent.NUMBER_OF_CALLS);
				event.setNumberOfCalls(ConferenceManager.getNumberOfMembers(tokens[0]));
				callEventNotification(event);

			} catch (Exception e) {
				logerror(e.toString());
			}
			return;
		}

		if ("outsideLinePrefix".equalsIgnoreCase(parameter))
		{
			if (value.equals("\"\"") || value.equals("''"))
			{
				value = "";
			}

			config.setOutsideLinePrefix(value);
			return;
		}

		if ("receiverPause".equalsIgnoreCase(parameter))
		{
			try {
				ConferenceReceiver.setReceiverPause(getInteger(value));
			} catch (Exception e) {
				logerror("pause " + value + " is not numeric" );
			}
			return;
		}

		if ("pauseTreatmentToCall".equalsIgnoreCase(parameter))
		{
			try {
				String[] tokens = value.split(":");

				CallHandler callHandler = CallHandler.findCall(tokens[0]);

				if (callHandler == null)
				{
					logerror("Invalid callId:  " + tokens[0]);
					return;
				}

				String treatmentId = null;

				if (tokens.length > 1)
				{
					treatmentId = tokens[1];
				}

           	 	callHandler.getMember().pauseTreatment(treatmentId, true);

			} catch (Exception e) {
				logerror(e.toString());
			}
			return;
		}

		if ("pauseTreatmentToConference".equalsIgnoreCase(parameter))
		{
			try {
				String treatment = getTreatment(value);
				value = value.substring(treatment.length());
				String conferenceId = getQualifierString(value);

				if (conferenceId == null)
				{
					logerror("conferenceId must be specified:  " + value);
					return;
				}

            	ConferenceManager.pauseTreatment(conferenceId, treatment, true);

			} catch (Exception e) {
				logerror(e.toString());
			}
			return;
		}

		if ("playTreatmentToCall".equalsIgnoreCase(parameter))
		{
			try {
				String treatment = getTreatment(value);
				double volume[] = getVolume(value);

				value = value.substring(treatment.length());
				String callId = getQualifierString(value);

				if (callId == null)
				{
					logerror("callId must be specified:  " + value);
					return;
				}

				try {
					CallHandler.playTreatmentToCall(callId, treatment);

				} catch (NoSuchElementException e) {
					logerror("Invalid callId specified:  " + value);
					return;

				} catch (IOException e) {
					logerror("Unable to read treatment file " + treatment + " " + e.getMessage());
					return;
				}

			} catch (Exception e) {
				logerror(e.toString());
			}
			return;
		}

		if ("playTreatmentToConference".equalsIgnoreCase(parameter))
		{
			try {
				String treatment = getTreatment(value);
				double volume[] = getVolume(value);

				value = value.substring(treatment.length());
				String conferenceId = getQualifierString(value);

				if (conferenceId == null)
				{
					logerror("conference Id must be specified:  " + value);
					return;
				}

				try {
					 ConferenceManager.playTreatment(conferenceId, treatment);

				} catch (NoSuchElementException e) {
					logerror("Invalid conference Id specified:  " + value);
					return;
				}

			} catch (Exception e) {
				logerror(e.toString());
			}
			return;
		}

		if ("playTreatmentToAllConferences".equalsIgnoreCase(parameter))
		{
			try {
				String treatment = getTreatment(value);
				double volume[] = getVolume(value);
				value = value.substring(treatment.length());

				try {
					 ConferenceManager.playTreatmentToAllConferences(treatment);

				} catch (Exception e) {
					logerror("Error playing treatment :  " + value);
					return;
				}

			} catch (Exception e) {
				logerror(e.toString());
			}
			return;
		}

		if ("transferCall".equalsIgnoreCase(parameter))
		{
			try {
				String callId = getString(value);

				if (callId == null)
				{
					logerror("Call id is missing");
					return;
				}

				try {
					CallHandler callHandler = CallHandler.findCall(callId);

					if (callHandler == null)
					{
						logerror("No such callId:  " + callId);
						return;
					}

					String conferenceId = getQualifierString(value);
					IncomingCallHandler.transferCall(callId, conferenceId);

				} catch (NoSuchElementException e) {
					logerror("Invalid callId specified:  " + value);
				}

			} catch (Exception e) {
				logerror(e.toString());
			}

			return;
		}

		if ("sendDtmfKey".equalsIgnoreCase(parameter))
		{
			try {
				String callId = getString(value);

				if (callId == null)
				{
					logerror("Call id is missing");
					return;
				}

				try {
					CallHandler callHandler = CallHandler.findCall(callId);

					if (callHandler == null)
					{
						logerror("No such callId:  " + callId);
						return;
					}

					String dtmfKey = getQualifierString(value);
					callHandler.dtmfKeys(dtmfKey);

				} catch (NoSuchElementException e) {
					logerror("Invalid callId specified:  " + value);
				}

			} catch (Exception e) {
				logerror(e.toString());
			}

			return;
		}
	}


    private String parseCallParameters(String parameter, String value, BridgeParticipant bp, String uid)
    {
		CallParticipant cp = bp.callParticipant;
		String response = null;

		if ("SetJID".equalsIgnoreCase(parameter))
		{
			try {
				bp.participantJID = new JID(value);

			} catch (Exception e) {
				response = e.toString();
			}

			return response;
		}

		if ("Set2ndPartyJID".equalsIgnoreCase(parameter))
		{
			try {
				bp.secondPartyJID = new JID(value);

			} catch (Exception e) {
				response = e.toString();
			}

			return response;
		}

		if ("SetRtmpVideoStream".equalsIgnoreCase(parameter))
		{
			try {
				cp.setRtmpVideoStream(value);

			} catch (Exception e) {
				response = e.toString();
			}

			return response;
		}

		if ("SetEmailAddress".equalsIgnoreCase(parameter))
		{
			try {
				cp.setEmailAddress(value);

			} catch (Exception e) {
				response = e.toString();
			}

			return response;
		}

		if ("MakeCall".equalsIgnoreCase(parameter))
		{
			try {
				makeOutgoingCall(bp, uid);
			} catch (Exception e) {
				logerror(e.toString());
			}
			return response;
		}

		if ("migrateCall".equalsIgnoreCase(parameter))
		{
			try {
				migrateCall(bp);
			} catch (Exception e) {
				response = (e.toString());
			}
			return response;
		}

		if ("cancelCall".equalsIgnoreCase(parameter))
		{
			CallHandler.hangup(cp.getCallId(), "User requested call termination");
			return response;
		}

		if ("sendDtmf".equalsIgnoreCase(parameter))
		{
			try {
				CallHandler callHandler = CallHandler.findCall(cp.getCallId());
				callHandler.dtmfKeys(value);

			} catch (NoSuchElementException e) {
				response = ("Invalid callId specified:  " + value);
			}

			return response;
		}

		if ("transferCall".equalsIgnoreCase(parameter))
		{
			try {
				IncomingCallHandler.transferCall(cp.getCallId(), value);

			} catch (NoSuchElementException e) {
				response = ("Invalid callId specified:  " + value);

			} catch (Exception e) {
				response = ("Internal error:  " + e);

			}

			return response;
		}


		if ("conferenceJoinTreatment".equalsIgnoreCase(parameter))
		{
			cp.setConferenceJoinTreatment(value);
			return response;
		}

		if ("conferenceLeaveTreatment".equalsIgnoreCase(parameter))
		{
			cp.setConferenceLeaveTreatment(value);
			return response;
		}

		if ("callAnsweredTreatment".equalsIgnoreCase(parameter))
		{
			 cp.setCallAnsweredTreatment(value);
			return response;
		}

		if ("callanswertimeout".equalsIgnoreCase(parameter))
		{
			try {
				cp.setCallAnswerTimeout(getInteger(value));
			} catch (Exception e) {
				response = ("callAnswerTimeout " + value + " is not numeric" );
			}
			return response;
		}

		if ("calltimeout".equalsIgnoreCase(parameter))
		{
			try {
				cp.setCallTimeout(getInteger(value) * 1000);
			} catch (Exception e) {
				response = ("callTimeout " + value + " is not numeric" );
			}
			return response;
		}

		if ("callendtreatment".equalsIgnoreCase(parameter))
		{
			cp.setCallEndTreatment(value);
			return response;
		}

		if ("callestablishedtreatment".equalsIgnoreCase(parameter))
		{
			cp.setCallEstablishedTreatment(value);
			return response;
		}

		if ("callid".equalsIgnoreCase(parameter))
		{
			cp.setCallId(value);
			return response;
		}

		if ("SetConference".equalsIgnoreCase(parameter))
		{
			try {
				String[] tokens = value.split(":");

				cp.setConferenceId(tokens[0].trim());

				if (tokens.length > 1) {
					cp.setMediaPreference(tokens[1]);
				}

				if (tokens.length > 2) {
					cp.setConferenceDisplayName(tokens[2]);
				}

			} catch (Exception e) {
				response = ("conferenceId " + value + " is invalid" );
			}
			return response;
		}

		if ("displayname".equalsIgnoreCase(parameter))
		{
			cp.setDisplayName(value);
			return response;
		}

		if ("distributedbridge".equalsIgnoreCase(parameter))
		{
			try {
				cp.setDistributedBridge(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("donotrecord".equalsIgnoreCase(parameter))
		{
			try {
				cp.setDoNotRecord(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("dtmfdetection".equalsIgnoreCase(parameter))
		{
			try {
				cp.setDtmfDetection(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("dtmfsuppression".equalsIgnoreCase(parameter))
		{
			try {
				cp.setDtmfSuppression(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("forwarddatafrom".equalsIgnoreCase(parameter))
		{
			cp.setForwardingCallId(value);
			return response;
		}

		if ("ignoretelephoneevents".equalsIgnoreCase(parameter))
		{
			try {
				cp.setIgnoreTelephoneEvents(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("inputtreatment".equalsIgnoreCase(parameter))
		{
			cp.setInputTreatment(value);
			return response;
		}

		if ("encryptkey".equalsIgnoreCase(parameter))
		{
			cp.setEncryptionKey(value);
			return response;
		}

		if ("encryptalgorithm".equalsIgnoreCase(parameter))
		{
			cp.setEncryptionAlgorithm(value);
			return response;
		}

		if ("firstConferenceMemberTreatment".equalsIgnoreCase(parameter))
		{
			cp.setFirstConferenceMemberTreatment(value);
			return response;
		}

		if ("handlesessionprogress".equalsIgnoreCase(parameter))
		{
			try {
				cp.setHandleSessionProgress(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("joinconfirmationkey".equalsIgnoreCase(parameter))
		{
			try {
				MemberReceiver.setJoinConfirmationKey(value);

			} catch (Exception e) {
				response = (e.toString());
			}
			return response;
		}

		if ("joinconfirmationtimeout".equalsIgnoreCase(parameter))
		{
			try {
				cp.setJoinConfirmationTimeout(getInteger(value));
			} catch (Exception e) {
				response = ("callAnswerTimeout " + value + " is not numeric" );
			}
			return response;
		}

		if ("mediapreference".equalsIgnoreCase(parameter))
		{
			cp.setMediaPreference(value);
			return response;
		}

		if ("migrate".equalsIgnoreCase(parameter))
		{
            String callId = getFirstString(value);
            cp.setCallId(callId);

			/*
			 * The second party number may be a sip address
			 * with colons.  So we treat everything after the
			 * first colon as the second party number.
			 */

			int ix;

			if ((ix = value.indexOf(":")) < 0) {

				response = ("secondPartyNumber must be specified:  " + value);
				return response;
			}

            String secondPartyNumber = value.substring(ix + 1);

            if (secondPartyNumber == null)
            {
                response = ("secondPartyNumber must be specified:  " + value);
				return response;
            }

			try {
				cp.setSecondPartyNumber(secondPartyNumber);
				cp.setMigrateCall(true);

			} catch (Exception e) {
				response = (e.toString());
			}

            return response;
		}

		if ("MuteCall".equalsIgnoreCase(parameter))
		{
			try {
				cp.setMuted(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("mutewhispergroup".equalsIgnoreCase(parameter))
		{
			try {
				cp.setMuteWhisperGroup(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("muteconference".equalsIgnoreCase(parameter))
		{
			try {
				cp.setConferenceMuted(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("name".equalsIgnoreCase(parameter))
		{
			cp.setName(value);
			return response;
		}

		if ("SetPhoneNo".equalsIgnoreCase(parameter))
		{
			cp.setPhoneNumber(value);
			return response;
		}

		if ("phonenumberlocation".equalsIgnoreCase(parameter))
		{
			cp.setPhoneNumberLocation(value);
		}

		if ("protocol".equalsIgnoreCase(parameter))
		{
			if (value.equalsIgnoreCase("SIP") == false && value.equalsIgnoreCase("NS") == false && value.equalsIgnoreCase("RTMP") == false)
			{
				response = ("Invalid protocol:  " + value);
				return response;
			}

			cp.setProtocol(value);
			return response;
		}

		if ("recorder".equalsIgnoreCase(parameter))
		{
			try {
				cp.setRecorder(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}


		if ("recorddirectory".equalsIgnoreCase(parameter))
		{
			cp.setRecordDirectory(value);
			return response;
		}

		if ("remotecallid".equalsIgnoreCase(parameter))
		{
			cp.setRemoteCallId(value);
			return response;
		}

		if ("voiceDetectionWhileMuted".equalsIgnoreCase(parameter))
		{
			try {
				cp.setVoiceDetectionWhileMuted(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("useConferenceReceiverThread".equalsIgnoreCase(parameter))
		{
			try {
				cp.setUseConferenceReceiverThread(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("voiceDetection".equalsIgnoreCase(parameter))
		{
			try {
				cp.setVoiceDetection(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("whisperGroup".equalsIgnoreCase(parameter))
		{
			cp.setWhisperGroupId(value);
			return response;
		}
		if ("voipGateway".equalsIgnoreCase(parameter))
		{
			cp.setVoIPGateway(value);
			return response;
		}

		if ("secondPartyCallId".equalsIgnoreCase(parameter))
		{
			cp.setSecondPartyCallId(value);
			return response;
		}
		if ("sipProxy".equalsIgnoreCase(parameter))
		{
			cp.setSipProxy(value);
			return response;
		}

		if ("secondPartyCallEndTreatment".equalsIgnoreCase(parameter))
		{
			cp.setSecondPartyCallEndTreatment(value);
			return response;
		}

		if ("secondPartyName".equalsIgnoreCase(parameter))
		{
			cp.setSecondPartyName(value);
			return response;
		}

		if ("Set2ndPartyPhoneNo".equalsIgnoreCase(parameter))
		{
			cp.setSecondPartyNumber(value);
			return response;
		}

		if ("secondPartyTreatment".equalsIgnoreCase(parameter))
		{
			cp.setSecondPartyTreatment(value);
			return response;
		}

		if ("secondpartyVoiceDetection".equalsIgnoreCase(parameter))
		{
			try {
				cp.setSecondPartyVoiceDetection(stringToBoolean(value));
			} catch (ParseException e) {
				response = (e.toString());
			}
			return response;
		}

		if ("secondPartyTimeout".equalsIgnoreCase(parameter))
		{
			try {
				cp.setSecondPartyTimeout(getInteger(value));
			} catch (Exception e) {
				response = ("setSecondPartyTimeout " + value + " is not numeric" );
			}
			return response;
		}


		return response;
    }

    private String getString(String value)
    {
		int n;

		if ((n = value.lastIndexOf(":")) > 0) {
			value = value.substring(0, n);
		}

		return value;
    }

    private double[] getVolume(String value) throws ParseException
    {
		String v = new String(value);

		int n;

			if ((n = v.indexOf(":volume=")) < 0) {
			return null;
		}

		v = v.substring(n + 8);

		String[] tokens = v.split(":");

		double[] volume = new double[tokens.length];

		for (int i = 0; i < volume.length; i++)
		{
			try {
				volume[i] = Double.parseDouble(tokens[i]);

			} catch (NumberFormatException e) {

			throw new ParseException("Invalid floating point value: "	+ tokens[i], 0);
			}
		}

		return volume;
    }

    private String getTreatment(String value)
    {
		String v = new String(value);

		int n;

		if ((n = v.indexOf(":volume=")) >= 0)
		{
			v = v.substring(0, n);
		}

		if ((n = v.lastIndexOf(":")) < 0) {
			/*
			 * There's no ":", so the whole string is the treatment
			 */
			return v;
		}

		String s = v.substring(0, n);

		if (s.equalsIgnoreCase("f") || s.equalsIgnoreCase("file")
			   || s.equalsIgnoreCase("d") || s.equalsIgnoreCase("dtmf")
			   || s.equalsIgnoreCase("t") || s.equalsIgnoreCase("tts")) {

			/*
			 * The only ":" is preceded by the type of treatment.
			 * The whole string is the treatment.
			 */
			return v;
		}

		/*
		 * The treatment is the string up to the last ":".
		 */
		return s;
    }

    private boolean getBoolean(String value) throws ParseException
    {
		int n;

		if ((n = value.indexOf(":")) > 0) {
			value = value.substring(0, n);
		}

		return stringToBoolean(value);
    }


    private boolean stringToBoolean(String value) throws ParseException
    {
		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("t")) {
			return true;
		}

		if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("F")) {
			return false;
		}

        throw new ParseException("Invalid boolean value, must be true or false:  " + value, 0);
    }

    private int getInteger(String value) throws ParseException
    {
		int n;

		if ((n = value.indexOf(":")) > 0) {
			value = value.substring(0, n);
		}

		int i = 0;

		try {
				i = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new ParseException("Invalid integer value: " + value, 0);
		}

		return i;
    }

    private String getQualifierString(String value)
    {
		int n;

		if (value == null) {
			return null;
		}

		if ((n = value.lastIndexOf(":")) >= 0) {
			return value.substring(n+1);
		}

		return null;
    }

    private double getDouble(String value) throws ParseException
    {
		int n;

		if ((n = value.indexOf(":")) > 0) {
			value = value.substring(0, n);
		}

		double f = 0.0;

		try {
				f = Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new ParseException("Invalid double value: " + value, 0);
		}

		return f;
    }

    private String getFirstString(String value)
    {
		int ix;

		if ((ix = value.indexOf(":")) < 0) {
			return value;
		}

		return value.substring(0, ix);
    }

    private String validateAndAdjustParameters(BridgeParticipant bp) throws ParseException
    {
		CallParticipant cp = bp.callParticipant;
		String response = null;
		String callId = cp.getCallId();

		if (callId == null) {
			cp.setCallId(CallHandler.getNewCallId());

		} else {

			if (callId.equals("0"))
			{
				response = ("Zero is an invalid callId");
				return response;
			}

			if (cp.migrateCall() == false)
			{
				CallHandler callHandler = CallHandler.findCall(callId);

				if (callHandler != null)
				{
					if (callHandler.isCallEnding() == false)
					{
						response = ("CallId " + callId + " is already in use");
						return response;

					} else {
						response = ("Reusing callId for ending call " + callId);
					}
				}
			}
		}

        handleCallAttendant(cp);

		cp.setSecondPartyNumber(config.formatPhoneNumber(cp.getSecondPartyNumber(), cp.getPhoneNumberLocation()));

		if (cp.migrateCall() == false)
		{
			if (cp.getProtocol() == null || "SIP".equals(cp.getProtocol()))
			{
				if (cp.getPhoneNumber() == null)
				{
					if (cp.getInputTreatment() == null)
					{
						if (bp.participantJID == null)
						{
							response = ("You must specify a phone number or an IM address to bridge");
							return response;
						}

					} else {

						if (cp.getInputTreatment().equals("null"))
						{
							cp.setInputTreatment("");
						}

						cp.setPhoneNumber(cp.getInputTreatment());
						cp.setProtocol("NS");
					}

				} else 	{

					cp.setPhoneNumber(config.formatPhoneNumber(cp.getPhoneNumber(), cp.getPhoneNumberLocation()));
				}

			} else if ("RTMP".equals(cp.getProtocol())) {

				cp.setRtmpSendStream("vb_sender_" + System.currentTimeMillis());
				cp.setRtmpRecieveStream("vb_reciever_" + System.currentTimeMillis());
				cp.setPhoneNumber("voicebridge@rtmp:/" + cp.getRtmpRecieveStream() + "/" + cp.getRtmpSendStream());

			} else response = "Unsupported bridge protocol";
		}

        if (cp.getName() == null || cp.getName().equals(""))
        {
            cp.setName("Anonymous");
		}

		if (cp.migrateCall() == true)
		{
	    	if (cp.getCallId() == null || cp.getSecondPartyNumber() == null)
	    	{
				response = ("You must specify old and new phone numbers to migrate a call");
				return response;
	    	}
		}

		if (cp.getEmailAddress() != null &&	cp.getEmailAddress().indexOf("@") == -1)
		{
			response = ("Email address is not valid");
			return response;
		}

		if (cp.getConferenceId() == null &&	cp.getSecondPartyNumber() == null)
		{
			response = ("You must specify a conference Id");
			return response;
		}

		if (cp.getDisplayName() == null)
		{
			if (cp.getSecondPartyNumber() == null)
			{
				cp.setDisplayName(cp.getConferenceId());

			} else {

				if (cp.getSecondPartyName() != null)
				{
					cp.setDisplayName(cp.getSecondPartyName());
				} else {
					cp.setDisplayName(cp.getSecondPartyNumber());
				}
			}
		}

		/*
		 * For two party calls.
		 */

		if (cp.getConferenceId() != null)
		{
			if (cp.getSecondPartyTreatment() != null)
			{
				cp.setConferenceJoinTreatment(cp.getSecondPartyTreatment());
			}

			if (cp.getSecondPartyCallEndTreatment() != null)
			{
				cp.setConferenceLeaveTreatment(
				cp.getSecondPartyCallEndTreatment());
			}
		}

		if (cp.getSecondPartyNumber() != null)
		{
			if (cp.getConferenceId() == null)
			{
				cp.setConferenceId(cp.getPhoneNumber());
			}
		}

		return response;
    }


    /*
     * Some Sun sites such as China have an automated call attendent
     * which asks the user to enter the extension again.
     *
     * If a phone number has ">" in it, we replace the phone number
     * to call with everything before the ">" and set the call
     * answer treatment to be dtmf keys of everything after the ">".
     */

    private void handleCallAttendant(CallParticipant cp)
    {
        String phoneNumber = cp.getPhoneNumber();

        int ix;

        if (phoneNumber != null && phoneNumber.indexOf("sip:") < 0 && phoneNumber.indexOf("@") < 0)
        {
            if ((ix = phoneNumber.indexOf(">")) > 0)
            {
                /*
                 * Must have 5 digit extension
                 */
                if (phoneNumber.length() >= ix + 1 + 5) {
                    cp.setCallAnsweredTreatment("dtmf:" +
                        phoneNumber.substring(ix + 1));

                    cp.setPhoneNumber(phoneNumber.substring(0, ix));
                }
            }
        }

        phoneNumber = cp.getSecondPartyNumber();

        if (phoneNumber != null &&
                phoneNumber.indexOf("sip:") < 0 &&
                phoneNumber.indexOf("@") < 0) {

            if ((ix = phoneNumber.indexOf(">")) > 0) {
                /*
                 * Must have 5 digit extension
                 */
                if (phoneNumber.length() >= ix + 1 + 5) {
                    cp.setSecondPartyTreatment("dtmf:" +
                        phoneNumber.substring(ix + 1));

                    cp.setSecondPartyNumber(phoneNumber.substring(0, ix));
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    //
    // Event management
    //
    // ------------------------------------------------------------------------

    public void handleMessage(Message received)
    {

    }

	public void interceptMessage(Message received)
	{
		//loginfo("VoiceBridge interceptMessage incoming\n" + received);
	}


    public void callEventNotification(CallEvent callEvent)
    {
 		loginfo( "VoiceBridge callEventNotification " + callEvent.toString());

		reportCallEventNotification(callEvent, "monitorCallStatus");
    }


    private static void reportCallEventNotification(CallEvent callEvent, String monitorName)
    {
 		System.out.println( "VoiceBridge reportCallEventNotification " + monitorName + " " + callEvent.toString());

		if ( bridgeParticipants.containsKey(callEvent.getCallId()))
		{
			BridgeParticipant bp = bridgeParticipants.get(callEvent.getCallId());

			sendCallEventNotification(callEvent, monitorName, bp.userJID.toBareJID(), bp);

		} else {	// incoming, send to every one interested
/*
			SessionManager sessionManager = XMPPServer.getInstance().getSessionManager();

			Collection<ClientSession> sessions = sessionManager.getSessions();

			for (ClientSession session : sessions)
			{
				sendCallEventNotification(callEvent, monitorName, session.getAddress().toBareJID(), null);
			}
*/
		}
    }

    private static void sendCallEventNotification(CallEvent callEvent, String monitorName, String jid, BridgeParticipant bp)
    {
		String myEvent = CallEvent.getEventString(callEvent.getEvent());
		String callState = callEvent.getCallState().toString();

		String info = callEvent.getInfo() == null ? "" : callEvent.getInfo();
		String dtmf = callEvent.getDtmfKey() == null ? "" : callEvent.getDtmfKey();
		String treatmentdId = callEvent.getTreatmentId() == null ? "" : callEvent.getTreatmentId();
		int noOfCalls = callEvent.getNumberOfCalls();
		String callId = callEvent.getCallId() == null ? "" : callEvent.getCallId();
		String confId = callEvent.getConferenceId() == null ? "" : callEvent.getConferenceId();
		String callInfo = callEvent.getCallInfo() == null ? "" : callEvent.getCallInfo();

		IQ iq = new IQ(IQ.Type.set);
		iq.setFrom(getComponent().getName() + "." + getComponent().getDomain());
		iq.setTo("pubsub." + getComponent().getDomain());
		Element pubsub = iq.setChildElement("pubsub", "http://jabber.org/protocol/pubsub");
		Element publish = pubsub.addElement("publish").addAttribute("node", jid);
		Element item = publish.addElement("item").addAttribute("id", jid);
		Element voicebridge = item.addElement("voicebridge", "http://xmpp.org/protocol/openlink:01:00:00/features#voice-bridge");

		voicebridge.addElement("jid").setText(jid);
		voicebridge.addElement("source").setText(monitorName);
		voicebridge.addElement("eventtype").setText(myEvent);
		voicebridge.addElement("dtmf").setText(dtmf);
		voicebridge.addElement("participants").setText(String.valueOf(noOfCalls));
		voicebridge.addElement("callstate").setText(callState);
		voicebridge.addElement("conference").setText(confId);
		voicebridge.addElement("participant").setText(callId);
		voicebridge.addElement("callinfo").setText(callInfo);
		voicebridge.addElement("eventinfo").setText(info);

		if (bp != null)
		{
			if (bp.callParticipant.getRtmpSendStream() != null)
			{
				voicebridge.addElement("sendstream").setText(bp.callParticipant.getRtmpSendStream());
			}

			if (bp.callParticipant.getRtmpRecieveStream() != null)
			{
				voicebridge.addElement("recievestream").setText(bp.callParticipant.getRtmpRecieveStream());
			}

			if (bp.callParticipant.getRtmpVideoStream() != null)
			{
				voicebridge.addElement("videostream").setText(bp.callParticipant.getRtmpVideoStream());
			}
		}

		getComponent().sendPacket(iq);
    }


    public static void registerNotification(String status, ProxyCredentials credentials)
    {
		System.out.println("registerNotification " + status + " " + credentials.getXmppUserName());

		try {
			Config.updateStatus(credentials.getXmppUserName(), status);

		} catch (Exception e) {

			System.out.println("registerNotification " + e);
		}
    }


    public static void incomingCallNotification(CallEvent callEvent)
    {
		System.out.println("incomingCallNotification " + callEvent.toString());
		reportCallEventNotification(callEvent, "incomingCallNotification");
    }

    public static void outgoingCallNotification(CallEvent callEvent)
    {
		System.out.println("outgoingCallNotification " + callEvent.toString());
		reportCallEventNotification(callEvent, "outgoingCallNotification");
    }

    public static void notifyConferenceMonitors(CallEvent callEvent)
    {
		System.out.println("notifyConferenceMonitors " + callEvent.toString());
		reportCallEventNotification(callEvent, "notifyConferenceMonitors");
    }

	private static VoiceBridgeComponent getComponent()
	{
		if (component == null)
		{
			try {
				component = RedfirePlugin.plugin.getVoiceBridgeComponentBySiteID(String.valueOf(site.getSiteID()));

			} catch (Exception e) {

				System.out.println("getComponent failure " + e);
			}
		}
		return component;
	}

    // ------------------------------------------------------------------------
    //
    // Logging
    //
    // ------------------------------------------------------------------------

    private void loginfo( String s )
    {
        log.info( s );
        System.out.println( s );
    }

    private void logerror( String s ) {

        log.error( s );
        System.out.println( "[ERROR] " + s );
    }


    // ------------------------------------------------------------------------
    //
    // ConferenceMonitor
    //
    // ------------------------------------------------------------------------

    class ConferenceMonitor {

        private IServiceCapableConnection service;
        private String conferenceId;

        public ConferenceMonitor(IServiceCapableConnection service, String conferenceId) {
            this.service = service;
            this.conferenceId = conferenceId;
        }

        public IServiceCapableConnection getService() {
            return service;
        }

        public String getConferenceId() {
            return conferenceId;
        }

    }

    // ------------------------------------------------------------------------
    //
    // BasicStreamID
    //
    // ------------------------------------------------------------------------


    private class BasicStreamID implements StreamID {
        String id;

        public BasicStreamID(String id) {
            this.id = id;
        }

        public String getID() {
            return id;
        }

        public String toString() {
            return id;
        }

        public int hashCode() {
            return id.hashCode();
        }
    }

    // ------------------------------------------------------------------------
    //
    // RedfireConnection
    //
    // ------------------------------------------------------------------------



    public class RedfireConnection extends VirtualConnection {

        private IServiceCapableConnection service;
        private SessionPacketRouter router;
        private String peerId;
        private String digest;

        public RedfireConnection(IServiceCapableConnection service, String peerId, String digest)
        {
            this.service = service;
            this.peerId = peerId;
			this.digest = digest;
        }

		public SessionPacketRouter getRouter()
		{
			return router;
		}

		public IServiceCapableConnection getService()
		{
			return service;
		}

		public String getDigest()
		{
			return digest;
		}

		public void setService(IServiceCapableConnection service, String peerId)
		{
			this.service = service;
            this.peerId = peerId;
		}

		public void setRouter(SessionPacketRouter router)
		{
			this.router = router;
		}

        public void closeVirtualConnection()
        {
            loginfo("RedfireConnection - close ");
            service.close();
			xmppDisconnect(digest);
        }

        public byte[] getAddress() throws UnknownHostException {
            return service.getRemoteAddress().getBytes();
        }

        public String getHostAddress() throws UnknownHostException {
            return service.getRemoteAddress();
        }

        public String getHostName() throws UnknownHostException {
            return service.getHost();
        }

        public void systemShutdown() {
            //close();
        }

        public void deliver(Packet packet) throws UnauthorizedException
        {
			deliverRawText(packet.toXML());
        }

        public void deliverRawText(String text)
        {
            loginfo("RedfireConnection - deliverRawText \n" + text);

			if (service != null) {
				service.invoke("xmppRecieve", new Object[] {text, peerId});
			}
        }

        public Certificate[] getPeerCertificates() {
            return null; // ((RTMPSession) session).getPeerCertificates();
        }
    }

    // ------------------------------------------------------------------------
    //
    // BridgeParticipant
    //
    // ------------------------------------------------------------------------

    class BridgeParticipant {

        public JID userJID;
        public JID participantJID = null;
        public JID secondPartyJID = null;
        public CallParticipant callParticipant;
		public Object callHandler;
		public IServiceCapableConnection service;
    }
}
