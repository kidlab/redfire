package com.ifsoft.redfire.plugin;

import org.apache.log4j.Logger;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.openfire.http.HttpBindManager;

import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;

import org.jivesoftware.util.JiveGlobals;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.webapp.voicebridge.*;
import com.sun.voip.server.*;
import com.ifsoft.cti.OpenlinkComponent;

import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;



public class RedfirePlugin implements Plugin {

	protected Logger Log = Logger.getLogger(getClass().getName());

	private static final String NAME 		= "redfire";
	private static final String DESCRIPTION = "Redfire Plugin for Openfire";

	private PluginManager manager;
    private File pluginDirectory;
    private WebAppContext context;


//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

	public void initializePlugin(PluginManager manager, File pluginDirectory)
	{
		Log.info( "["+ NAME + "] initialize " + NAME + " plugin resources");

		this.pluginDirectory = pluginDirectory;

		JiveGlobals.setProperty("cumulus.path.default", pluginDirectory + File.separator  + "cumulus" + File.separator + "CumulusServer.exe");

		try {

			Log.info("["+ NAME + "] Starting Red5 RTMP Server");

			ContextHandlerCollection contexts = HttpBindManager.getInstance().getContexts();
			context = new WebAppContext(contexts, pluginDirectory.getPath(), "/" + NAME);
			context.setWelcomeFiles(new String[]{"index.html"});

		}
		catch (Exception e) {
			Log.error("Error initializing Cumulus Plugin", e);
		}

	}

	public void destroyPlugin() {
		Log.info( "["+ NAME + "] destroy " + NAME + " plugin resources");

		try {
			context.stop();
			context.destroy();
 			HttpBindManager.getInstance().stop();

		}
		catch (Exception e) {
			Log.error("["+ NAME + "] destroyPlugin exception " + e);
		}
	}


	public String getName() {
		 return NAME;
	}

	public String getDescription() {
		return DESCRIPTION;
	}



    // ------------------------------------------------------------------------
    //
    // Cumulus Audio Messages Handlers
    //
    // ------------------------------------------------------------------------


    public Red5AudioHandler createRed5AudioHandler()
    {
		return null;
	}
}