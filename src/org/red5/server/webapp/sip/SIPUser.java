package org.red5.server.webapp.sip;


import java.util.Map;
import java.util.HashMap;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.File;

import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.ITagWriter;
import org.red5.io.flv.IFLV;
import org.red5.io.flv.meta.IMetaData;
import org.red5.io.flv.meta.IMetaService;
import org.red5.server.api.cache.ICacheStore;
import org.red5.server.api.cache.ICacheable;

import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.IConnection;

import local.ua.*;
import org.zoolu.sip.address.*;
import org.zoolu.sip.provider.*;
import org.zoolu.net.SocketAddress;

import org.slf4j.Logger;
import org.red5.logging.Red5LoggerFactory;


public class SIPUser implements SIPUserAgentListener, SIPRegisterAgentListener {

    protected static Logger log = Red5LoggerFactory.getLogger( SIPUser.class, "sip" );

    public boolean sipReady = false;

    private IConnection service;

    private long lastCheck;

    private String sessionID;

    private SIPUserAgentProfile user_profile;

    private SipProvider sip_provider;

    private boolean opt_regist = false;

    private boolean opt_unregist = false;

    private boolean opt_unregist_all = false;

    private int opt_expires = -1;

    private long opt_keepalive_time = -1;

    private boolean opt_no_offer = false;

    private String opt_call_to = null;

    private int opt_accept_time = -1;

    private int opt_hangup_time = -1;

    private String opt_redirect_to = null;

    private String opt_transfer_to = null;

    private int opt_transfer_time = -1;

    private int opt_re_invite_time = -1;

    private boolean opt_audio = false;

    private boolean opt_video = false;

    private int opt_media_port = 0;

    private boolean opt_recv_only = false;

    private boolean opt_send_only = false;

    private boolean opt_send_tone = false;

    private String opt_send_file = null;

    private String opt_recv_file = null;

    private boolean opt_no_prompt = false;

    private String opt_from_url = null;

    private String opt_contact_url = null;

    private String opt_username = null;

    private String opt_realm = null;

    private String opt_passwd = null;

    private int opt_debug_level = -1;

    private String opt_outbound_proxy = null;

    private String opt_via_addr = SipProvider.AUTO_CONFIGURATION;

    private int opt_host_port = SipStack.default_port;

    private SIPUserAgent ua;

    private SIPRegisterAgent ra;

    private RTMPUser rtmpUser;

    private PipedOutputStream publishStream;

    private String username;

    private String password;

    private String publishName;

    private String playName;

    private int sipPort;

    private int rtpPort;

    private String proxy;

    private String realm;

    private String obproxy;
    
    private String A1ParamMD5;

    public SIPUser( String sessionID, IConnection service, int sipPort, int rtpPort ) throws IOException {

        p( "Constructor: sip port " + sipPort + " rtp port:" + rtpPort );

        try {

            this.sessionID = sessionID;
            this.service = service;
            this.sipPort = sipPort;
            this.rtpPort = rtpPort;

        }
        catch ( Exception e ) {
            p( "constructor: Exception:>\n" + e );

        }
    }


    public boolean isRunning() {

        boolean resp = false;

        try {
            resp = ua.audioApp.receiver.isRunning();

        }
        catch ( Exception e ) {
            resp = false;
        }

        return resp;
    }


    public void login( String obproxy, String phone, String username, String password, String realm, String proxy ) {

        p( "login" );

        this.username = username;
        this.password = password;
        this.proxy = proxy;
		this.opt_outbound_proxy = obproxy;
		this.realm = realm;

        String fromURL = "\"" + phone + "\" <sip:" + phone + "@" + proxy + ">";

        try {
            rtmpUser = new RTMPUser();
            SipStack.init();
            SipStack.debug_level = 8;
            SipStack.log_path = "log";

            sip_provider = new SipProvider( null, sipPort );
            sip_provider.setOutboundProxy(new SocketAddress(opt_outbound_proxy));

            user_profile = new SIPUserAgentProfile();
            user_profile.audioPort = rtpPort;
            user_profile.username = username;
            user_profile.passwd = password;
            user_profile.realm = realm;
            user_profile.fromUrl = fromURL;
			user_profile.contactUrl = "sip:" + phone + "@" + sip_provider.getViaAddress();
			user_profile.A1ParamMD5 = A1ParamMD5;

            if ( sip_provider.getPort() != SipStack.default_port ) {
                user_profile.contactUrl += ":" + sip_provider.getPort();
            }

            user_profile.keepaliveTime=8000;
			user_profile.acceptTime=0;
			user_profile.hangupTime=20;

            ua = new SIPUserAgent( sip_provider, user_profile, this, rtmpUser );

            sipReady = false;
            ua.listen();

        }
        catch ( Exception e ) {
            p( "login: Exception:>\n" + e );
        }
    }


    public void register() {

        p( "register" );

        try {

            if ( sip_provider != null ) {
                ra = new SIPRegisterAgent( sip_provider, user_profile.fromUrl, user_profile.contactUrl, username,
                    user_profile.realm, password, this );
            	ra.setA1Parameter(A1ParamMD5);
                loopRegister( user_profile.expires, user_profile.expires / 2, user_profile.keepaliveTime );
            }

        }
        catch ( Exception e ) {
            p( "register: Exception:>\n" + e );
        }
    }


    public void dtmf( String digits ) {

        p( "dtmf " + digits );

        try {

            if ( ua != null && ua.audioApp != null && ua.audioApp.sender != null ) {
                ua.audioApp.sender.queueSipDtmfDigits( digits );
            }

        }
        catch ( Exception e ) {
            p( "dtmf: Exception:>\n" + e );
        }
    }


    public void call( String destination ) {

        p( "Calling " + destination );

        try {

            publishName = "microphone_" + System.currentTimeMillis();
            playName = "speaker_" + System.currentTimeMillis();

            rtmpUser.startStream( "localhost", "sip", 1935, publishName, playName );

            sipReady = false;
            ua.setMedia( rtmpUser );
            ua.hangup();

            if ( destination.indexOf( "@" ) == -1 ) {
                destination = destination + "@" + proxy;
            }

            if ( destination.indexOf( "sip:" ) > -1 ) {
                destination = destination.substring( 4 );
            }

            ua.call( destination );

        }
        catch ( Exception e ) {
            p( "call: Exception:>\n" + e );
        }
    }

	/** Add by Lior call transfer test */


	   public void transfer( String transferTo ) {

	           p( "Transfer To: " + transferTo );

	           try {
	               if (transferTo.indexOf("@") == -1) {
					transferTo = transferTo + "@" + proxy ;
			   }

	               ua.transfer( transferTo );

	           }
	           catch ( Exception e ) {
	               p( "call: Exception:>\n" + e );
	           }
	       }

	/** end of transfer code */




	public void close() {
		p("close1");
         try {

			hangup();
			unregister();
		    new Thread().sleep(3000);

		} catch(Exception e) {
			p("close: Exception:>\n" + e);
		}

        try {
            p("provider.halt");
			sip_provider.halt();

	    } catch(Exception e) {
			p("close: Exception:>\n" + e);
	    }
	}


    public void accept() {

        p( "accept" );

        if ( ua != null ) {

            try {
                publishName = "microphone_" + System.currentTimeMillis();
                playName = "speaker_" + System.currentTimeMillis();

                rtmpUser.startStream( "localhost", "sip", 1935, publishName, playName );

                sipReady = false;
                ua.setMedia( rtmpUser );
                ua.accept();

            }
            catch ( Exception e ) {
                p( "SIPUser: accept - Exception:>\n" + e );
            }
        }
    }


    public void hangup() {

        p( "hangup" );

        if ( ua != null ) {

            if ( ua.call_state != SIPUserAgent.UA_IDLE ) {
                ua.hangup();
                ua.listen();
            }
        }

        closeStreams();
    }


    public void streamStatus( String status ) {

        p( "streamStatus " + status );

        if ( "stop".equals( status ) ) {
            // ua.listen();
        }
    }


    public void unregister() {

        p( "unregister" );

        if ( ra != null ) {
            if ( ra.isRegistering() ) {
                ra.halt();
            }
            ra.unregister();
            ra = null;
        }

        if ( ua != null ) {
            ua.hangup();
        }
        ua = null;
    }


    private void closeStreams() {

        p( "closeStreams" );

        try {
        	rtmpUser.stopStream();
        }
        catch ( Exception e ) {
            p( "closeStreams: Exception:>\n" + e );
        }
    }


    public long getLastCheck() {

        return lastCheck;
    }


    public boolean isClosed() {

        return ua == null;
    }


    public String getSessionID() {

        return sessionID;
    }


    private void loopRegister( int expire_time, int renew_time, long keepalive_time ) {

        if ( ra.isRegistering() ) {
            ra.halt();
        }
        ra.loopRegister( expire_time, renew_time, keepalive_time );
    }


    public void onUaCallIncoming( SIPUserAgent ua, NameAddress callee, NameAddress caller ) {

        String source = caller.getAddress().toString();
        String sourceName = caller.hasDisplayName() ? caller.getDisplayName() : "";
        String destination = callee.getAddress().toString();
        String destinationName = callee.hasDisplayName() ? callee.getDisplayName() : "";

        p( "onUaCallIncoming " + source + " " + destination);

        if ( service != null ) {
            ( (IServiceCapableConnection) service ).invoke( "incoming", new Object[] { source, sourceName, destination,
                destinationName } );
        }
    }


    public void onUaCallRinging( SIPUserAgent ua ) {
        p( "onUaCallRinging" );

        if ( service != null ) {
            ( (IServiceCapableConnection) service ).invoke( "callState", new Object[] { "onUaCallRinging" } );
        }
    }


    public void onUaCallAccepted( SIPUserAgent ua ) {
        p( "onUaCallAccepted" );

        if ( service != null ) {
            ( (IServiceCapableConnection) service ).invoke( "callState", new Object[] { "onUaCallAccepted" } );
        }

    }


    public void onUaCallConnected( SIPUserAgent ua ) {

        p( "onUaCallConnected" );
        sipReady = true;

        if ( service != null ) {
            ( (IServiceCapableConnection) service ).invoke( "connected", new Object[] { playName, publishName } );
        }

    }


    public void onUaCallTrasferred( SIPUserAgent ua ) {
        p( "onUaCallTrasferred");

		 if (service != null) {
		 	((IServiceCapableConnection) service).invoke("callState", new Object[] {"onUaCallTrasferred"});
		}

    }


    public void onUaCallCancelled( SIPUserAgent ua ) {
        p( "onUaCallCancelled");

        sipReady = false;
        closeStreams();

        if ( service != null ) {
            ( (IServiceCapableConnection) service ).invoke( "callState", new Object[] { "onUaCallCancelled" } );
        }

        ua.listen();
    }


    public void onUaCallFailed( SIPUserAgent ua ) {
        p( "onUaCallFailed");

        sipReady = false;
        closeStreams();

        if ( service != null ) {
            ( (IServiceCapableConnection) service ).invoke( "callState", new Object[] { "onUaCallFailed" } );
        }

        ua.listen();
    }


    public void onUaCallClosed( SIPUserAgent ua ) {
        p( "onUaCallClosed");

        sipReady = false;
        closeStreams();

        if ( service != null ) {
            ( (IServiceCapableConnection) service ).invoke( "callState", new Object[] { "onUaCallClosed" } );
        }

        ua.listen();
    }


    public void onUaRegistrationSuccess( SIPRegisterAgent ra, NameAddress target, NameAddress contact, String result ) {

        p( "SIP Registration success " + result );

        if ( service != null ) {
            ( (IServiceCapableConnection) service ).invoke( "registrationSucess", new Object[] { result } );
        }
    }


    public void onUaRegistrationFailure( SIPRegisterAgent ra, NameAddress target, NameAddress contact, String result ) {

        p( "SIP Registration failure " + result );

        if ( service != null ) {
            ( (IServiceCapableConnection) service ).invoke( "registrationFailure", new Object[] { result } );
        }
    }


    private void p( String s ) {

        log.debug( s );
		System.out.println("[SIPUser] " + s);
    }

    public void setA1Parameter(String A1ParamMD5) {
		this.A1ParamMD5 = A1ParamMD5;		
	}
	
	public int getSipPort() {
		return this.sipPort;
	}

	public int getRtpPort() {
		return this.rtpPort;
	}
}
