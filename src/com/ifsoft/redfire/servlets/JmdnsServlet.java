package com.ifsoft.redfire.servlets;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.http.HttpServlet;
import org.apache.log4j.Logger;
import com.ifsoft.jmdns.*;

import org.jivesoftware.util.JiveGlobals;

public class JmdnsServlet extends HttpServlet
{
	private Logger Log = Logger.getLogger(getClass().getName());
	private JmDNS jmdnsServer;


	public void init()
	{
        try
        {
			String sPort = JiveGlobals.getProperty("httpbind.port.plain", "7070");
			int port = Integer.parseInt(sPort);
			InetAddress addr = InetAddress.getLocalHost();

			Log.info("Init JmdnsServlet - reporting jMDNS for " + addr.getHostName() + " on  port " + port);

			jmdnsServer = new JmDNS(addr);
			ServiceInfo si = new ServiceInfo("_http._tcp.local.", "redfire_urlserver", port, "");
			jmdnsServer.registerService(si);
        }
        catch (Exception e)
        {
            Log.error("JmdnsServlet init failed", e);
        }
	}


	public void destroy()
	{
        try
        {
			Log.info("Exit JmdnsServlet stopping jMDNS");

			jmdnsServer.unregisterAllServices();
			jmdnsServer.close();

        }
        catch (Exception e)
        {
            Log.error("JmdnsServlet destroy failed", e);
        }
	}
}
