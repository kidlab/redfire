package com.ifsoft.redfire;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.util.Log;
import org.jivesoftware.openfire.http.HttpBindManager;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import java.io.File;

// uncomment for openfire 3.6.4
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;

// uncomment for openfire 3.7.0
//import org.eclipse.jetty.server.handler.ContextHandlerCollection;
//import org.eclipse.jetty.webapp.WebAppContext;


public class RedfirePlugin implements Plugin, RedfireConstants {

	private static final String NAME 		= "redfire";
	private static final String DESCRIPTION = "Red5 Plugin for Openfire";

	private PluginManager manager;
    private File pluginDirectory;

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		Log.info( "["+ NAME + "] initialize " + NAME + " plugin resources");

		try {

			ContextHandlerCollection contexts = HttpBindManager.getInstance().getContexts();

			try {
				WebAppContext context = new WebAppContext(contexts, pluginDirectory.getPath(), "/" + NAME);
				context.setWelcomeFiles(new String[]{"index.html"});

			}
			catch(Exception e) {

        	}

		}
		catch (Exception e) {
			Log.error("Error initializing Redfire Plugin", e);
		}
	}

	public void destroyPlugin() {
		Log.info( "["+ NAME + "] destroy " + NAME + " plugin resources");

		try {


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
}