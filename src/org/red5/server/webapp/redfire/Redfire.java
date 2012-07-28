package org.red5.server.webapp.redfire;

import org.apache.log4j.Logger;

import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;

import org.jivesoftware.util.JiveGlobals;

import java.util.*;
import java.io.*;
import java.net.*;


import org.red5.server.webapp.voicebridge.*;
import com.sun.voip.server.*;
import com.ifsoft.cti.OpenlinkComponent;



public class Redfire implements Red5Container {

	protected Logger Log = Logger.getLogger(getClass().getName());

	private static final String NAME 		= "redfire";

    private org.red5.server.webapp.voicebridge.Application application;
    public OpenlinkComponent component;


//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

	public void initialize()
	{
		Log.info( "["+ NAME + "] initialize " + NAME + " redfire resources");

		try {

			Log.info("["+ NAME + "] Starting VOIP Server");

			component = new OpenlinkComponent(null);
			application = new org.red5.server.webapp.voicebridge.Application();
			application.appStart(component);
			component.setApplication(application);

			Log.info("["+ NAME + "] Starting Openlink Component");

			component.componentEnable();

			RtmpParticipant.handler = this;

		}
		catch (Exception e) {
			Log.error("Error initializing Cumulus Redfire", e);
		}

	}

	public void destroy() {
		Log.info( "["+ NAME + "] destroy " + NAME + " redfire resources");

		try {

			Log.info( "["+ NAME + "] Stopping VOIP Server");
			application.appStop();

			Log.info("["+ NAME + "] Stopping Openlink Component");
			component.componentDestroyed();
		}
		catch (Exception e) {
			Log.error("["+ NAME + "] destroyRedfire exception " + e);
		}
	}


	public String getName() {
		 return NAME;
	}

    // ------------------------------------------------------------------------
    //
    // Red5 RTMP Interface
    //
    // ------------------------------------------------------------------------

    public Red5AudioHandler createRed5AudioHandler()
    {
		return new Red5Participant();
	}
}