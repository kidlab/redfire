package org.red5.server.webapp.redfire;

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

import org.red5.server.adapter.ApplicationAdapter;
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

import org.red5.server.webapp.redfire.Redfire;

import org.xmpp.packet.*;

import org.dom4j.*;

import org.apache.log4j.Logger;

public class Application extends ApplicationAdapter implements IStreamAwareScopeHandler {

    private Map< String, LocalClientSession > sessions 				= new ConcurrentHashMap<String, LocalClientSession>();
    private Map< String, IServiceCapableConnection > publishers 	= new ConcurrentHashMap<String, IServiceCapableConnection>();
    private Map< String, String > digests 							= new ConcurrentHashMap<String, String>();
    private Redfire redfire;

	protected Logger Log = Logger.getLogger(getClass().getName());

    // ------------------------------------------------------------------------
    //
    // Overide
    //
    // ------------------------------------------------------------------------


    @Override
    public boolean appStart( IScope scope )
    {
		try{
			loginfo( "Redfire starting in scope " + scope.getName() + " " + System.getProperty( "user.dir" ) );

			redfire = new Redfire();
			redfire.initialize();

		} catch (Exception e) {

			e.printStackTrace();
		}
        return true;
    }


    @Override
    public void appStop( IScope scope )
    {
        loginfo( "Redfire stopping in scope " + scope.getName() );

		IConnection conn = Red5.getConnectionLocal();
		IServiceCapableConnection service = (IServiceCapableConnection) conn;

        for (LocalClientSession session : sessions.values())
        {
			session.close();
			session = null;
		}

		redfire.destroy();
    }


    @Override
    public boolean appConnect( IConnection conn, Object[] params ) {

        IServiceCapableConnection service = (IServiceCapableConnection) conn;
        loginfo( "Redfire Client connected " + conn.getClient().getId() + " service " + service );

        return true;
    }


    @Override
    public boolean appJoin( IClient client, IScope scope ) {

        loginfo( "Redfire Client joined app " + client.getId() );
        IConnection conn = Red5.getConnectionLocal();
        IServiceCapableConnection service = (IServiceCapableConnection) conn;

        return true;
    }


    @Override
    public void appLeave( IClient client, IScope scope ) {

        IConnection conn = Red5.getConnectionLocal();
		IServiceCapableConnection thisService = (IServiceCapableConnection) conn;

        loginfo( "Redfire Client leaving app " + client.getId() );

        if (digests.containsKey(client.getId()))
        {
			xmppDisconnect(digests.get(client.getId()));
		}
    }

    @Override
	public void streamBroadcastStart(IBroadcastStream stream)
	{
        loginfo( "Redfire streamBroadcastStart " + stream.getPublishedName() );
        IConnection conn = Red5.getConnectionLocal();
        IServiceCapableConnection service = (IServiceCapableConnection) conn;

        publishers.put(stream.getPublishedName(), service);
	}

    @Override
	public void streamBroadcastClose(IBroadcastStream stream)
	{
        loginfo( "Redfire streamBroadcastClose " + stream.getPublishedName() );

        publishers.remove(stream.getPublishedName());
	}

    // ------------------------------------------------------------------------
    //
    // Phono remote calls
    //
    // ------------------------------------------------------------------------

    public void sendDigit(String stream, String digit)
    {
        loginfo( "Redfire sendDigit " + stream + " " + digit );

		redfire.component.sendDigit(stream, digit);
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

        loginfo( "Redfire xmppConnect " + username + " service " + service + " digest " + digest);

        if (sessions.containsKey(digest))
        {
			LocalClientSession session = sessions.get(digest);

			try {
				RedfireConnection connection = (RedfireConnection) session.getConnection();
				connection.setService(service, peerId);
				service.invoke("xmppConnect", new Object[] {digest, peerId});
				return digest;

			} catch (Exception e) {
				logerror("Redfire xmppConnect " + e);
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
				logerror("Redfire xmppConnect " + e);
				return null;
			}
		}
    }

    public void xmppDisconnect(String digest)
    {
        loginfo( "Redfire xmppDisconnect " + digest);

        if (sessions.containsKey(digest))
        {
			LocalClientSession session = sessions.remove(digest);

			session.close();
			session = null;
		}
    }

    public void xmppSend(String digest, String xml)
    {
        loginfo( "Redfire xmppSend \n" + xml);

        if (sessions.containsKey(digest))
        {
			LocalClientSession session = sessions.get(digest);

			try {
				RedfireConnection connection = (RedfireConnection) session.getConnection();
				connection.getRouter().route(DocumentHelper.parseText(xml).getRootElement());

			} catch (Exception e) {

				logerror("Redfire xmppSend " + e);
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
		loginfo("Redfire mousePress " + button + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("mousePress", new Object[] {button});

		} else logerror("mousePress stream not found " + streamName);
	}

	public void mouseRelease(String streamName, Integer button)
	{
		loginfo("Redfire mouseRelease " + button + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("mouseRelease", new Object[] {button});

		} else logerror("mouseRelease stream not found " + streamName);
	}

	public void doubleClick(String streamName, Integer x, Integer y, Integer width, Integer height)
	{
		loginfo("Redfire doubleClick " + x + " " + y + " " + width + " " + height + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("doubleClick", new Object[] {x, y, width, height});

		} else logerror("doubleClick stream not found " + streamName);
	}

	public void keyPress(String streamName, Integer key)
	{
		loginfo("Redfire keyPress " + key + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("keyPress", new Object[] {key});

		} else logerror("keyPress stream not found " + streamName);
	}

	public void keyRelease(String streamName, Integer key)
	{
		loginfo("Redfire keyRelease " + key + " " + streamName);

		IServiceCapableConnection service = getPublisher(streamName);

		if (service != null)
		{
			service.invoke("keyRelease", new Object[] {key});

		} else logerror("keyRelease stream not found " + streamName);
	}

	public void mouseMove(String streamName, Integer x, Integer y, Integer width, Integer height)
	{
		loginfo("Redfire mouseMove " + x + " " + y + " " + width + " " + height + " " + streamName);

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
    // Logging
    //
    // ------------------------------------------------------------------------

    private void loginfo( String s )
    {
        Log.info( s );
    }

    private void logerror( String s ) {

        Log.error( s );
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
}
