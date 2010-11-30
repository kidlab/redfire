package org.red5.server.webapp.sip;


import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.net.*;

import org.slf4j.Logger;
import org.red5.logging.Red5LoggerFactory;

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


public class Application extends ApplicationAdapter implements IStreamAwareScopeHandler {

    protected static Logger log = Red5LoggerFactory.getLogger( Application.class, "sip" );

    private SIPManager sipManager;

    private boolean available = false;

    private int startSIPPort = 5070;

    private int sipPort;
    
    private int numSIPPorts;
    
    private int sipStep;

    private int startRTPPort = 3000;

    private int rtpPort;

    private int numRTPPorts;

    private int rtpStep;

    private LinkedList<Integer> availabeSIPPorts;
	
	private LinkedList<Integer> availabeRTPPorts;

    // Application version
	private String version = "0.0.7";
	
    private Map< String, String > userNames = new HashMap< String, String >();


    @Override
    public boolean appStart( IScope scope ) {

        loginfo( "Red5SIP starting in scope " + scope.getName() + " " + System.getProperty( "user.dir" ) );
        sipManager = SIPManager.getInstance();

        startSIPPort = Config.getInstance().getStartSIPPort();
        numSIPPorts = Config.getInstance().getSIPPortNum();
        sipStep = Config.getInstance().getSIPPortStep();
        startRTPPort = Config.getInstance().getStartRTPPort();
        numRTPPorts = Config.getInstance().getRTPPortNum();
        rtpStep = Config.getInstance().getRTPPortStep();
    	
        availabeSIPPorts = new LinkedList<Integer>();
        availabeRTPPorts = new LinkedList<Integer>();
        initAvaliablePorts(availabeSIPPorts, startSIPPort, numSIPPorts, sipStep);
        initAvaliablePorts(availabeRTPPorts, startRTPPort, numRTPPorts, rtpStep);

        loginfo(String.format("Start ports -  sip: %d numPorts: %d step: %d - rtp: %d numPorts %d step: %d", 
				startSIPPort, numSIPPorts, sipStep, startRTPPort, numRTPPorts, rtpStep));
                
        //Application version
        loginfo(String.format("Red5Phone version %s", version));
    	
        return true;
    }


    @Override
    public void appStop( IScope scope ) {

        loginfo( "Red5SIP stopping in scope " + scope.getName() );
        sipManager.destroyAllSessions();
    }


    @Override
    public boolean appConnect( IConnection conn, Object[] params ) {

        IServiceCapableConnection service = (IServiceCapableConnection) conn;
        loginfo( "Red5SIP Client connected " + conn.getClient().getId() + " service " + service );
        return true;
    }


    @Override
    public boolean appJoin( IClient client, IScope scope ) {

        loginfo( "Red5SIP Client joined app " + client.getId() );
        IConnection conn = Red5.getConnectionLocal();
        IServiceCapableConnection service = (IServiceCapableConnection) conn;

        return true;
    }


    @Override
    public void appLeave( IClient client, IScope scope ) {

        IConnection conn = Red5.getConnectionLocal();
        loginfo( "Red5SIP Client leaving app " + client.getId() );

        if ( userNames.containsKey( client.getId() ) ) {
            loginfo( "Red5SIP Client closing client " + userNames.get( client.getId() ) );
    		SIPUser sipUser = sipManager.getSIPUser( userNames.get( client.getId() ) );

    		if(sipUser != null) {
    			loginfo("Release ports: sip port " + sipUser.getSipPort() + " audio port " + sipUser.getRtpPort() );
    			releasePorts(availabeSIPPorts, sipUser.getSipPort());
    			releasePorts(availabeRTPPorts, sipUser.getRtpPort());
    		}
            sipManager.closeSIPUser( userNames.get( client.getId() ) );
            userNames.remove( client.getId() );
        }
    }


    public void streamPublishStart( IBroadcastStream stream ) {

        loginfo( "Red5SIP Stream publish start: " + stream.getPublishedName() );
        IConnection current = Red5.getConnectionLocal();

    }


    public void streamBroadcastClose( IBroadcastStream stream ) {

        loginfo( "Red5SIP Stream broadcast close: " + stream.getPublishedName() );
    }


    public void streamBroadcastStart( IBroadcastStream stream ) {

        loginfo( "Red5SIP Stream broadcast start: " + stream.getPublishedName() );

    }


    public void streamPlaylistItemPlay( IPlaylistSubscriberStream stream, IPlayItem item, boolean isLive ) {

        loginfo( "Red5SIP Stream play: " + item.getName() );
    }


    public void streamPlaylistItemStop( IPlaylistSubscriberStream stream, IPlayItem item ) {

        loginfo( "Red5SIP Stream stop: " + item.getName() );
    }


    public void streamPlaylistVODItemPause( IPlaylistSubscriberStream stream, IPlayItem item, int position ) {

    }


    public void streamPlaylistVODItemResume( IPlaylistSubscriberStream stream, IPlayItem item, int position ) {

    }


    public void streamPlaylistVODItemSeek( IPlaylistSubscriberStream stream, IPlayItem item, int position ) {

    }


    public void streamSubscriberClose( ISubscriberStream stream ) {

        loginfo( "Red5SIP Stream subscribe close: " + stream.getName() );

    }


    public void streamSubscriberStart( ISubscriberStream stream ) {

        loginfo( "Red5SIP Stream subscribe start: " + stream.getName() );

    }


    public List< String > getStreams() {

        IConnection conn = Red5.getConnectionLocal();
        return getBroadcastStreamNames( conn.getScope() );
    }


    public void onPing() {

        loginfo( "Red5SIP Ping" );
    }
    
    /*
     * Initializes SIPUser setting A1 Digest Parameter MD5 hash
     * This way we don't need send plain text password from Flash Application
     */
    public void initialize(String uid, String username, String A1ParamMD5) {
    	IConnection conn = Red5.getConnectionLocal();
    	IServiceCapableConnection service = (IServiceCapableConnection) conn;

    	SIPUser sipUser = sipManager.getSIPUser(uid);

    	if(sipUser == null) {
    		sipUser = createSipUser(uid, username, sipUser);
    		if (sipUser != null) {
    			sipUser.setA1Parameter(A1ParamMD5);
    		}
    	}
    }
    
    public void open(String obproxy,String uid, String phone,String username, String password, String realm, String proxy) {
    	loginfo("Red5SIP open");

		login(obproxy, uid, phone, username, password, realm, proxy);
		register(uid);
	}

	public void login(String obproxy, String uid, String phone, String username, String password, String realm, String proxy) {
		loginfo("Red5SIP login " + uid);

		SIPUser sipUser = sipManager.getSIPUser(uid);

		if(sipUser == null) {
			sipUser = createSipUser(uid, username, sipUser);
			if (sipUser == null) {
				logerror("There are no ports available!");
				IConnection conn = Red5.getConnectionLocal();
				IServiceCapableConnection service = (IServiceCapableConnection) conn;
		        if ( service != null ) {
		            ( (IServiceCapableConnection) service ).invoke( "registrationFailure", new Object[] { "registration failure" } );
		        }
				return;
			}
		}

		sipUser.login(obproxy,phone,username, password, realm, proxy);
	}


	private SIPUser createSipUser(String uid, String username, SIPUser sipUser) {

		IConnection conn = Red5.getConnectionLocal();
		IServiceCapableConnection service = (IServiceCapableConnection) conn;

		try {
			sipPort = allocPorts(availabeSIPPorts);
			rtpPort = allocPorts(availabeRTPPorts);

			sipUser = new SIPUser(conn.getClient().getId(), service, sipPort, rtpPort);
			sipManager.addSIPUser(uid, sipUser);
			userNames.put(conn.getClient().getId(), uid);

			loginfo("Red5SIP open creating sipUser for " + username + " on sip port " + sipPort + " audio port " + rtpPort + " uid " + uid );

		} catch (NoSuchElementException e) {
			logerror("No port avaliable: " + e);
		} catch (Exception e) {
			loginfo("open error " + e);
		}
		
		return sipUser;
	}
	

	public void register(String uid) {
		loginfo("Red5SIP register");

		SIPUser sipUser = sipManager.getSIPUser(uid);

		if(sipUser != null) {
			sipUser.register();
		}
	}


	public void call(String uid, String destination) {
		loginfo("Red5SIP Call " + destination);

		SIPUser sipUser = sipManager.getSIPUser(uid);

		if(sipUser != null) {
			loginfo("Red5SIP Call found user " + uid + " making call to " + destination);
			sipUser.call(destination);
		}

	}


	/** call tarensfer test by Lior */


	 public void transfer(String uid, String transferTo) {
			loginfo("Red5SIP transfer " + transferTo);

			SIPUser sipUser = sipManager.getSIPUser(uid);

			if(sipUser != null) {
				loginfo("Red5SIP Call found user " + uid + " transfering call to " + transferTo);
				sipUser.transfer(transferTo);
			}

		}

	/** transfer end tetst */



	public void addToConf(String uid, String conf) {
		loginfo("Red5SIP addToConf " + conf);

		SIPUser sipUser = sipManager.getSIPUser(uid);

		if(sipUser != null) {
			loginfo("Red5SIP addToConf found user " + uid + " adding to conf " + conf);
			sipUser.transfer("8" + conf);
		}

	}



	public void joinConf(String uid, String conf) {
		loginfo("Red5SIP joinConf " + conf);

		SIPUser sipUser = sipManager.getSIPUser(uid);

		if(sipUser != null) {
			loginfo("Red5SIP joinConf found user " + uid + " joining conf " + conf);
			sipUser.call("8" + conf );
		}

	}

	public void dtmf(String uid, String digits) {
		loginfo("Red5SIP DTMF " + digits);

		SIPUser sipUser = sipManager.getSIPUser(uid);

		if(sipUser != null) {
			loginfo("Red5SIP DTMF found user " + uid + " sending dtmf digits " + digits);
			sipUser.dtmf(digits);
		}

	}

	public void accept(String uid) {
		loginfo("Red5SIP Accept");

		SIPUser sipUser = sipManager.getSIPUser(uid);

		if(sipUser != null) {
			sipUser.accept();
		}
	}
	//Lior Add

	public void unregister(String uid) {
			loginfo("Red5SIP unregister");

			SIPUser sipUser = sipManager.getSIPUser(uid);

			if(sipUser != null) {
				sipUser.unregister();
			}
	}

	public void hangup(String uid) {
		loginfo("Red5SIP Hangup");

		SIPUser sipUser = sipManager.getSIPUser(uid);

		if(sipUser != null) {
			sipUser.hangup();
		}
	}

	public void streamStatus(String uid, String status) {
		loginfo("Red5SIP streamStatus");

		SIPUser sipUser = sipManager.getSIPUser(uid);

		if(sipUser != null) {
			sipUser.streamStatus(status);
		}
	}


	public void close(String uid) {
		loginfo("Red5SIP endRegister");

		IConnection conn = Red5.getConnectionLocal();
		sipManager.closeSIPUser(uid);
		userNames.remove(conn.getClient().getId());
	}



    private void loginfo( String s ) {

        log.info( s );
        System.out.println( s );
    }
    
    private void logerror( String s ) {

        log.error( s );
        System.out.println( "[ERROR] " + s );
    }
    
	private void initAvaliablePorts(LinkedList<Integer> list, int base, int size, int step)
	{
		loginfo("Alloc ports base: " + base + " size: " + size + " step: " + step);
		
		if (base <= 1024)
		{
			throw new IllegalArgumentException("Invalid base port: " + base);
		}

		if (step <= 0)
		{
			throw new IllegalArgumentException("Invalid port step: " + step);
		}
		
		if ((base + size * step) > 0xffff)
		{
			throw new IllegalArgumentException("Invalid port range");
		}

		int port = base;
		
		while (size-- > 0)
		{
			loginfo("Alloc port number:" + port);
			list.add(port);
			
			port += step;
		}
	}

	private Integer allocPorts(LinkedList<Integer> availablePorts) throws NoSuchElementException
	{
		Integer port = null;
		
		synchronized (availablePorts)
		{
			port = availablePorts.remove();
			loginfo("Alloc port number:" + port);
		}
		
		return port;
	}
	
	private void releasePorts(LinkedList<Integer> availablePorts, Integer port)
	{
		synchronized (availablePorts)
		{
			availablePorts.add(port);
			loginfo("Release port number:" + port);
		}
	}
	
}
