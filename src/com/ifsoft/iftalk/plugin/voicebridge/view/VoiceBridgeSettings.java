
package com.ifsoft.iftalk.plugin.voicebridge.view;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.database.SequenceManager;

import com.ifsoft.iftalk.plugin.voicebridge.SiteDao;
import com.ifsoft.iftalk.plugin.voicebridge.Site;
import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeConstants;
import com.ifsoft.iftalk.plugin.voicebridge.RedfirePlugin;
import com.ifsoft.iftalk.plugin.tsc.numberformatter.TelephoneNumberFormatter;


public class VoiceBridgeSettings extends HttpServlet implements VoiceBridgeConstants  {

	private String action                   = "";
	private long   siteID					= 0;
	private String name  					= "";
	private String privateHost  			= "";
	private String publicHost  			= "";
	private String defaultProxy  		= "";
	private String defaultExten  		= "";

	private String pbxAccessDigits 			= "9";
	private String pbxCountryCode 			= Locale.getDefault().getCountry();
	private String pbxAreaCode 				= "0";
	private String pbxNumberLength 			= "5";
	private String pbxFWDCodePrefix			= "*41";
	private String pbxFWDCodeSuffix			= "";
	private String pbxFWDCodeCancel			= "*41";

	private String vmsEnabled				= "false";
	private String vmsDatabaseType			= "POSTGRESQL";
	private String vmsDatabaseHost			= "localhost";
	private String vmsDatabaseName			= "voicedrop";
	private String vmsDatabaseUsername		= "postgres";
	private String vmsDatabasePassword		= "969131";

	private String vmsPhoneServerHost		= "192.168.100.46";
	private String vmsPhoneServerPort		= "5038";
	private String vmsPhoneServerUsername	= "manager";
	private String vmsPhoneServerPassword	= "mysecret";
	private String vmsPhoneServerChannel	= "LOCAL/85XXXX@vms";
	private String vmsPhoneServerPrefix		= "555";
	private String vmsPhoneServerContext	= "divoiceint";
	private String vmsPhoneServerAGIContext	= "vmsagi";
	private String vmsPhoneServerNumberLen	= "4";

	protected Logger Log = Logger.getLogger(getClass().getName());

	private SiteDao siteDao;

	private String errorMessage = null;

	private RedfirePlugin plugin;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
		plugin = (RedfirePlugin)XMPPServer.getInstance().getPluginManager().getPlugin("redfire");
    }


	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response.addHeader("Cache-Control", "post-check=0, pre-check=0");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Content-Type", "text/html");
		response.setHeader("Connection", "close");

		ServletOutputStream out = response.getOutputStream();
		Map<String, String> errors = new HashMap<String, String>();


		siteDao = new SiteDao(plugin);

		action = request.getParameter("action");

		if(action == null) {
			action = " ";
		}

		if(action.equals("top")) {
			name  					= "";
			privateHost  			= "";
			publicHost  			= "";
			defaultProxy  			= "";
			defaultExten			= "";

			pbxAccessDigits 		= "9";
			pbxCountryCode 			= Locale.getDefault().getCountry();
			pbxAreaCode 			= "0";
			pbxNumberLength 		= "5";
			pbxFWDCodePrefix		= "*41";
			pbxFWDCodeSuffix		= "";
			pbxFWDCodeCancel		= "*41";

			displayPage(out, errors.size());
		}

		else if(action.equals("add")) {

			name 					= request.getParameter("name");
			privateHost 				= request.getParameter("privateHost");
			publicHost 				= request.getParameter("publicHost");
			defaultProxy 			= request.getParameter("defaultProxy");
			defaultExten 			= request.getParameter("defaultExten");

			vmsEnabled				= request.getParameter("vmsEnabled");
			vmsDatabaseType			= request.getParameter("vmsDatabaseType");
			vmsDatabaseHost			= request.getParameter("vmsDatabaseHost");
			vmsDatabaseName			= request.getParameter("vmsDatabaseName");
			vmsDatabaseUsername		= request.getParameter("vmsDatabaseUsername");
			vmsDatabasePassword		= request.getParameter("vmsDatabasePassword");

			vmsPhoneServerHost		= request.getParameter("vmsPhoneServerHost");
			vmsPhoneServerPort		= request.getParameter("vmsPhoneServerPort");
			vmsPhoneServerUsername	= request.getParameter("vmsPhoneServerUsername");
			vmsPhoneServerPassword	= request.getParameter("vmsPhoneServerPassword");
			vmsPhoneServerChannel	= request.getParameter("vmsPhoneServerChannel");
			vmsPhoneServerPrefix	= request.getParameter("vmsPhoneServerPrefix");
			vmsPhoneServerContext	= request.getParameter("vmsPhoneServerContext");
			vmsPhoneServerAGIContext= request.getParameter("vmsPhoneServerAGIContext");
			vmsPhoneServerNumberLen	= request.getParameter("vmsPhoneServerNumberLen");

			pbxNumberLength			= request.getParameter("pbxNumberLength");
			pbxCountryCode			= request.getParameter("pbxCountryCode");

			validateFields(errors);

			if(errors.isEmpty()) {

				Site site = new Site();
				site.setSiteID(SequenceManager.nextID(site));
				site.setName(name);
				site.setPrivateHost(privateHost);
				site.setPublicHost(publicHost);
				site.setDefaultProxy(defaultProxy);
				site.setDefaultExten(defaultExten);

				try {
					String pname = name.toLowerCase();

					JiveGlobals.setProperty(Properties.VoiceBridge_PBX_COUNTRY_CODE 		+ "." + pname, request.getParameter("pbxCountryCode"));
					JiveGlobals.setProperty(Properties.VoiceBridge_PBX_ACCESS_DIGITS 		+ "." + pname, request.getParameter("pbxAccessDigits"));
					JiveGlobals.setProperty(Properties.VoiceBridge_AREA_CODE 				+ "." + pname, request.getParameter("pbxAreaCode"));
					JiveGlobals.setProperty(Properties.VoiceBridge_PBX_NUMBER_LENGTH 		+ "." + pname, request.getParameter("pbxNumberLength"));
					JiveGlobals.setProperty(Properties.VoiceBridge_PBX_FWD_CODE_PREFIX 	+ "." + pname, request.getParameter("pbxFWDCodePrefix"));
					JiveGlobals.setProperty(Properties.VoiceBridge_PBX_FWD_CODE_SUFFIX 	+ "." + pname, request.getParameter("pbxFWDCodeSuffix"));
					JiveGlobals.setProperty(Properties.VoiceBridge_PBX_FWD_CODE_CANCEL 	+ "." + pname, request.getParameter("pbxFWDCodeCancel"));

					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_ENABLED		 		+ "." + pname, request.getParameter("vmsEnabled"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_DATABASE_TYPE 		+ "." + pname, request.getParameter("vmsDatabaseType"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_DATABASE_HOST 		+ "." + pname, request.getParameter("vmsDatabaseHost"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_DATABASE_NAME 		+ "." + pname, request.getParameter("vmsDatabaseName"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_DATABASE_USERNAME	+ "." + pname, request.getParameter("vmsDatabaseUsername"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_DATABASE_PASSWORD	+ "." + pname, request.getParameter("vmsDatabasePassword"));

					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_HOST	 		+ "." + pname, request.getParameter("vmsPhoneServerHost"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_PORT	 		+ "." + pname, request.getParameter("vmsPhoneServerPort"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_USERNAME 		+ "." + pname, request.getParameter("vmsPhoneServerUsername"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_PASSWORD 		+ "." + pname, request.getParameter("vmsPhoneServerPassword"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_CHANNEL 		+ "." + pname, request.getParameter("vmsPhoneServerChannel"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_PREFIX 		+ "." + pname, request.getParameter("vmsPhoneServerPrefix"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_CONTEXT 		+ "." + pname, request.getParameter("vmsPhoneServerContext"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_AGI_CONTEXT	+ "." + pname, request.getParameter("vmsPhoneServerAGIContext"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_NUMBER_LEN 	+ "." + pname, request.getParameter("vmsPhoneServerNumberLen"));

					siteDao.insert(site);

					Log.info("VoiceBridge Setings: Database added: site " + site.getSiteID());
				}
				catch (SQLException e) {
					Log.error(e.getMessage(), e);
				}

				response.sendRedirect("voicebridge-summary");

			}
			else {
				displayPage(out, errors.size());
			}
		}
		else if(action.equals("edit")) {
			siteID = Long.parseLong(request.getParameter("siteID"));
			Site site = siteDao.getSiteByID(siteID);
			name  					= site.getName();
			privateHost  			= site.getPrivateHost();
			publicHost  			= site.getPublicHost();
			defaultProxy  			= site.getDefaultProxy();
			defaultExten  			= site.getDefaultExten();

			String pname = name.toLowerCase();

			pbxCountryCode			= JiveGlobals.getProperty(Properties.VoiceBridge_PBX_COUNTRY_CODE 		+ "." + pname, pbxCountryCode);
			pbxAccessDigits 		= JiveGlobals.getProperty(Properties.VoiceBridge_PBX_ACCESS_DIGITS		+ "." + pname, pbxAccessDigits);
			pbxAreaCode 			= JiveGlobals.getProperty(Properties.VoiceBridge_AREA_CODE 			+ "." + pname, pbxAreaCode);
			pbxNumberLength 		= JiveGlobals.getProperty(Properties.VoiceBridge_PBX_NUMBER_LENGTH 	+ "." + pname, pbxNumberLength);
			pbxFWDCodePrefix		= JiveGlobals.getProperty(Properties.VoiceBridge_PBX_FWD_CODE_PREFIX 	+ "." + pname, pbxFWDCodePrefix);
			pbxFWDCodeSuffix 		= JiveGlobals.getProperty(Properties.VoiceBridge_PBX_FWD_CODE_SUFFIX 	+ "." + pname, pbxFWDCodeSuffix);
			pbxFWDCodeCancel		= JiveGlobals.getProperty(Properties.VoiceBridge_PBX_FWD_CODE_CANCEL 	+ "." + pname, pbxFWDCodeCancel);

			vmsEnabled				= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_ENABLED		 	+ "." + pname, vmsEnabled);
			vmsDatabaseType 		= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_DATABASE_TYPE 	+ "." + pname, vmsDatabaseType);
			vmsDatabaseHost			= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_DATABASE_HOST 	+ "." + pname, vmsDatabaseHost);
			vmsDatabaseName			= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_DATABASE_NAME 	+ "." + pname, vmsDatabaseName);
			vmsDatabaseUsername     = JiveGlobals.getProperty(Properties.VoiceBridge_VMS_DATABASE_USERNAME	+ "." + pname, vmsDatabaseUsername);
			vmsDatabasePassword		= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_DATABASE_PASSWORD	+ "." + pname, vmsDatabasePassword);

			vmsPhoneServerHost		= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_HOST	 	+ "." + pname, vmsPhoneServerHost);
			vmsPhoneServerPort		= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_PORT	 	+ "." + pname, vmsPhoneServerPort);
			vmsPhoneServerUsername  = JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_USERNAME 	+ "." + pname, vmsPhoneServerUsername);
			vmsPhoneServerPassword  = JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_PASSWORD 	+ "." + pname, vmsPhoneServerPassword);
			vmsPhoneServerChannel	= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_CHANNEL 	+ "." + pname, vmsPhoneServerChannel);
			vmsPhoneServerPrefix	= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_PREFIX 		+ "." + pname, vmsPhoneServerPrefix);
			vmsPhoneServerContext	= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_CONTEXT 	+ "." + pname, vmsPhoneServerContext);
			vmsPhoneServerAGIContext= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_AGI_CONTEXT	+ "." + pname, vmsPhoneServerAGIContext);
			vmsPhoneServerNumberLen = JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_NUMBER_LEN 	+ "." + pname, vmsPhoneServerNumberLen);

			displayPage(out, errors.size());
		}

		else if(action.equals("update")) {
			siteID = Long.parseLong(request.getParameter("siteID"));
			name 					= request.getParameter("name");
			privateHost 			= request.getParameter("privateHost");
			publicHost 			= request.getParameter("publicHost");
			defaultProxy 		= request.getParameter("defaultProxy");
			defaultExten 		= request.getParameter("defaultExten");




			pbxNumberLength			= request.getParameter("pbxNumberLength");
			pbxCountryCode			= request.getParameter("pbxCountryCode");

			vmsEnabled				= request.getParameter("vmsEnabled");
			vmsDatabaseType			= request.getParameter("vmsDatabaseType");
			vmsDatabaseHost			= request.getParameter("vmsDatabaseHost");
			vmsDatabaseName			= request.getParameter("vmsDatabaseName");
			vmsDatabaseUsername		= request.getParameter("vmsDatabaseUsername");
			vmsDatabasePassword		= request.getParameter("vmsDatabasePassword");

			vmsPhoneServerHost		= request.getParameter("vmsPhoneServerHost");
			vmsPhoneServerPort		= request.getParameter("vmsPhoneServerPort");
			vmsPhoneServerUsername	= request.getParameter("vmsPhoneServerUsername");
			vmsPhoneServerPassword	= request.getParameter("vmsPhoneServerPassword");
			vmsPhoneServerChannel	= request.getParameter("vmsPhoneServerChannel");
			vmsPhoneServerPrefix	= request.getParameter("vmsPhoneServerPrefix");
			vmsPhoneServerContext	= request.getParameter("vmsPhoneServerContext");
			vmsPhoneServerAGIContext= request.getParameter("vmsPhoneServerAGIContext");
			vmsPhoneServerNumberLen	= request.getParameter("vmsPhoneServerNumberLen");

			validateFields(errors);

			if(errors.isEmpty()) {

				Site site = siteDao.getSiteByID(siteID);
				site.setName(name);
				site.setPrivateHost(privateHost);
				site.setPublicHost(publicHost);
				site.setDefaultProxy(defaultProxy);
				site.setDefaultExten(defaultExten);

				try {

					String pname = name.toLowerCase();

					JiveGlobals.setProperty(Properties.VoiceBridge_PBX_COUNTRY_CODE 		+ "." + pname, request.getParameter("pbxCountryCode"));
					JiveGlobals.setProperty(Properties.VoiceBridge_PBX_ACCESS_DIGITS 		+ "." + pname, request.getParameter("pbxAccessDigits"));
					JiveGlobals.setProperty(Properties.VoiceBridge_AREA_CODE 				+ "." + pname, request.getParameter("pbxAreaCode"));
					JiveGlobals.setProperty(Properties.VoiceBridge_PBX_NUMBER_LENGTH 		+ "." + pname, request.getParameter("pbxNumberLength"));
					JiveGlobals.setProperty(Properties.VoiceBridge_PBX_FWD_CODE_PREFIX 	+ "." + pname, request.getParameter("pbxFWDCodePrefix"));
					JiveGlobals.setProperty(Properties.VoiceBridge_PBX_FWD_CODE_SUFFIX 	+ "." + pname, request.getParameter("pbxFWDCodeSuffix"));
					JiveGlobals.setProperty(Properties.VoiceBridge_PBX_FWD_CODE_CANCEL 	+ "." + pname, request.getParameter("pbxFWDCodeCancel"));

					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_ENABLED		 		+ "." + pname, request.getParameter("vmsEnabled"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_DATABASE_TYPE 		+ "." + pname, request.getParameter("vmsDatabaseType"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_DATABASE_HOST 		+ "." + pname, request.getParameter("vmsDatabaseHost"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_DATABASE_NAME 		+ "." + pname, request.getParameter("vmsDatabaseName"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_DATABASE_USERNAME	+ "." + pname, request.getParameter("vmsDatabaseUsername"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_DATABASE_PASSWORD	+ "." + pname, request.getParameter("vmsDatabasePassword"));

					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_HOST	 		+ "." + pname, request.getParameter("vmsPhoneServerHost"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_PORT	 		+ "." + pname, request.getParameter("vmsPhoneServerPort"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_USERNAME 		+ "." + pname, request.getParameter("vmsPhoneServerUsername"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_PASSWORD 		+ "." + pname, request.getParameter("vmsPhoneServerPassword"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_CHANNEL 		+ "." + pname, request.getParameter("vmsPhoneServerChannel"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_PREFIX 		+ "." + pname, request.getParameter("vmsPhoneServerPrefix"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_CONTEXT 		+ "." + pname, request.getParameter("vmsPhoneServerContext"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_AGI_CONTEXT	+ "." + pname, request.getParameter("vmsPhoneServerAGIContext"));
					JiveGlobals.setProperty(Properties.VoiceBridge_VMS_PHONE_NUMBER_LEN 	+ "." + pname, request.getParameter("vmsPhoneServerNumberLen"));

					siteDao.update(site);

					Log.info("VoiceBridge Setings: Database updated site: " + site.getSiteID());
				}
				catch (SQLException e) {
					Log.error(e.getMessage(), e);
				}

				response.sendRedirect("voicebridge-summary");

			}
			else {
				displayPage(out, errors.size());
			}

		}
		else {

			displayPage(out, errors.size());
		}
	}

	private void displayPage(ServletOutputStream out, int errorSize) {

		try {
			out.println("");
			out.println("<html>");
			out.println("    <head>");
			out.println("        <title>Voice Bridge Properties</title>");
			out.println("        <meta name=\"pageID\" content=\"VoiceBridge-SUMMARY\"/>");
			out.println("    </head>");
			out.println("    <body>");

			if (errorSize > 0) {
				out.println("<div class=\"error\">");
				out.println(errorMessage);
				out.println("</div>");
			}
			out.println("");
			out.println("Use the form below to edit VoiceBridge Properties.<br>");
			out.println("</p>");
			out.println("<form action=\"voicebridge-settings\" method=\"get\">");

			if(action.equals("edit")) {
				out.println("<input type='hidden' name='action' value='update'>");
				out.println("<input type='hidden' name='siteID' value='" + siteID + "'>");

			} else if(action.equals("top")) {
				out.println("<input type='hidden' name='action' value='add'>");

			} else {
				out.println("<input type='hidden' name='action' value='" + action + "'>");
				out.println("<input type='hidden' name='siteID' value='" + siteID + "'>");
			}
			out.println("");

			out.println("<div class=\"jive-contentBoxHeader\">General</div>");
			out.println("<div class=\"jive-contentBox\">");
			out.println("	 <table>");
			out.println("	 	<tr><td>Site Name</td><td><input size='20' type='text' name='name' value='" + name + "'></td>");
			out.println("	 		<td>Name of this VoiceBridge site</td></tr>");
			out.println("	 	<tr><td>Private Host</td><td><input size='20' type='text' name='privateHost' value='" + privateHost + "' ></td>");
			out.println("	 		<td>Host name on local network</td></tr>");
			out.println("	 	<tr><td>Public Name</td><td><input size='20' type='text' name='publicHost' value='" + publicHost + "' ></td>");
			out.println("	 		<td>Host name on public network</td></tr>");
			out.println("	 	<tr><td>Default Proxy</td><td><input size='20' type='text' name='defaultProxy' value='" + defaultProxy + "'></td>");
			out.println("	 		<td>Default proxy for telephone number calls</td></tr>");
			out.println("	 	<tr><td>Default Extension</td><td><input size='20' type='text' name='defaultExten' value='" + defaultExten + "'></td>");
			out.println("	 		<td>Default extension for telephone number calls</td></tr>");

			out.println("	 </table>");
			out.println("</div>");
			out.println("");

			out.println("<div class=\"jive-contentBoxHeader\">Phone Number Formatting</div>");
			out.println("<div class=\"jive-contentBox\">");
			out.println("	 <table>");
			out.println("	 	<tr><td>Country Code</td><td><select name='pbxCountryCode'>");

			String[] listofCountryCodes = TelephoneNumberFormatter.getISOCountryCodes();

			for (int i=0; i<listofCountryCodes.length; i++)
			{
				if (pbxCountryCode.equals(listofCountryCodes[i]))
					out.println(	 	"<option selected value='" +  listofCountryCodes[i] + "'>" + listofCountryCodes[i] + "</option>");
				else
					out.println(	 	"<option value='" +  listofCountryCodes[i] + "'>" + listofCountryCodes[i] + "</option>");
			}

			out.println("	 	    </select></td>");
			out.println("	 		<td>Country code for determining international dial code. For example GB for United Kingdom, US for United states of America.<p/> Your server country code is " + Locale.getDefault().getCountry() + "</td></tr>");
			out.println("	 	<tr><td>External/PBX Access Digits</td><td><input size='20' type='text' name='pbxAccessDigits' value='" + pbxAccessDigits + "'></td>");
			out.println("	 		<td>Dial digits required to access external lines through PBX. Usually 9</td></tr>");
			out.println("	 	<tr><td>Internal/PBX Number Length</td><td><input size='20' type='text' name='pbxNumberLength' value='" + pbxNumberLength + "'></td>");
			out.println("	 		<td>How many digits are in an internal PBX phone number. Usually 4 or 5.</td></tr>");
			out.println("	 	<tr><td>Area Code</td><td><input size='20' type='text' name='pbxAreaCode' value='" + pbxAreaCode + "'></td>");
			out.println("	 		<td>The area code for own town, state or province within your country</td></tr>");
			out.println("	 </table>");
			out.println("</div>");
			out.println("");
/*
			out.println("<div class=\"jive-contentBoxHeader\">Call Forwarding</div>");
			out.println("<div class=\"jive-contentBox\">");
			out.println("	 <table>");
			out.println("	 	<tr><td>Prefix Code</td><td><input size='20' type='text' name='pbxFWDCodePrefix' value='" + pbxFWDCodePrefix + "'></td>");
			out.println("	 		<td>Dial digits in front of the call forward destination number</td></tr>");
			out.println("	 	<tr><td>Suffix Code</td><td><input size='20' type='text' name='pbxFWDCodeSuffix' value='" + pbxFWDCodeSuffix + "'></td>");
			out.println("	 		<td>Dial digits at end of the call forward destination number.</td></tr>");
			out.println("	 	<tr><td>Cancel Code</td><td><input size='20' type='text' name='pbxFWDCodeCancel' value='" + pbxFWDCodeCancel + "'></td>");
			out.println("	 		<td>Dial digits to cancel call forward.</td></tr>");
			out.println("	 </table>");
			out.println("</div>");
			out.println("");

			out.println("<div class=\"jive-contentBoxHeader\">Voice Message Service (VMS)</div>");
			out.println("<div class=\"jive-contentBox\">");
			out.println("	 <table>");
			out.println("	 	<tr><td>Database Type</td><td><select name='vmsDatabaseType'>");

			String[] databaseTypeCodes = {"MYSQL", "POSTGRESQL", "SQLSERVER"};

			for (int i=0; i<databaseTypeCodes.length; i++)
			{
				if (vmsDatabaseType.equals(databaseTypeCodes[i]))
					out.println(	 	"<option selected value='" +  databaseTypeCodes[i] + "'>" + databaseTypeCodes[i] + "</option>");
				else
					out.println(	 	"<option value='" +  databaseTypeCodes[i] + "'>" + databaseTypeCodes[i] + "</option>");
			}

			out.println("	 	    </select></td>");
			out.println("	 		<td>Database type used for VMS Server</td></tr>");

			out.println("	 	<tr><td>Database Host</td><td><input size='20' type='text' name='vmsDatabaseHost' value='" + vmsDatabaseHost + "'></td>");
			out.println("	 		<td>Database Server name name or IP address</td></tr>");
			out.println("       <tr>");
			out.println("            <td colspan='3'>");
			out.println("            <input type=\"radio\" name=\"vmsEnabled\" value=\"true\" id=\"rb03\"");

			if(vmsEnabled.equals("true")) {
				out.println("            checked");
			}

			out.println("            >");
			out.println("                <label for=\"rb03\"><b>Enabled</b></label> - VMS is enabled");
			out.println("            </td>");
			out.println("        </tr>");
			out.println("        <tr>");
			out.println("            <td colspan='3'>");
			out.println("            <input type=\"radio\" name=\"vmsEnabled\" value=\"false\" id=\"rb04\"");

			if(vmsEnabled.equals("false")) {
				out.println("            checked");
			}
			out.println("            >");
			out.println("                <label for=\"rb04\"><b>Disabled</b></label> - VMS is disabled");

			out.println("            </td>");
			out.println("       </tr>");
			out.println("	 	<tr><td>Database Name</td><td><input size='20' type='text' name='vmsDatabaseName' value='" + vmsDatabaseName + "'></td>");
			out.println("	 		<td>Database name to be used for VMS</td></tr>");
			out.println("	 	<tr><td>Database Username</td><td><input size='20' type='text' name='vmsDatabaseUsername' value='" + vmsDatabaseUsername + "'></td>");
			out.println("	 		<td>User name to be used to log into database</td></tr>");
			out.println("	 	<tr><td>Database Password</td><td><input size='20' type='password' name='vmsDatabasePassword' value='" + vmsDatabasePassword + "'></td>");
			out.println("	 		<td>Password to be used with username</td></tr>");
			out.println("	 	<tr><td>VMS Host</td><td><input size='20' type='text' name='vmsPhoneServerHost' value='" + vmsPhoneServerHost + "'></td>");
			out.println("	 		<td>Password to be used with username</td></tr>");
			out.println("	 	<tr><td>VMS Port</td><td><input size='20' type='text' name='vmsPhoneServerPort' value='" + vmsPhoneServerPort + "'></td>");
			out.println("	 		<td>VMS server name or IP.</td></tr>");
			out.println("	 	<tr><td>VMS Username</td><td><input size='20' type='text' name='vmsPhoneServerUsername' value='" + vmsPhoneServerUsername + "'></td>");
			out.println("	 		<td>VMS server manager username</td></tr>");
			out.println("	 	<tr><td>VMS Password</td><td><input size='20' type='password' name='vmsPhoneServerPassword' value='" + vmsPhoneServerPassword + "'></td>");
			out.println("	 		<td>VMS server manager password</td></tr>");
			out.println("	 	<tr><td>VMS Channel</td><td><input size='20' type='text' name='vmsPhoneServerChannel' value='" + vmsPhoneServerChannel + "'></td>");
			out.println("	 		<td>VMS Channels to be used by the Voice Drop server for internal recording/playback</td></tr>");
			out.println("	 	<tr><td>VMS Prefix</td><td><input size='20' type='text' name='vmsPhoneServerPrefix' value='" + vmsPhoneServerPrefix + "'></td>");
			out.println("	 		<td>Prefix to be used to reach the Voice Drop server on the internal telephony network</td></tr>");
			out.println("	 	<tr><td>VMS Context</td><td><input size='20' type='text' name='vmsPhoneServerContext' value='" + vmsPhoneServerContext + "'></td>");
			out.println("	 		<td>Context used by VMS</td></tr>");
			out.println("	 	<tr><td>VMS AGI Context</td><td><input size='20' type='text' name='vmsPhoneServerAGIContext' value='" + vmsPhoneServerAGIContext + "'></td>");
			out.println("	 		<td>Context used by the AGI script in VMS</td></tr>");
			out.println("	 	<tr><td>VMS Number Length</td><td><input size='20' type='text' name='vmsPhoneServerNumberLen' value='" + vmsPhoneServerNumberLen + "'></td>");
			out.println("	 		<td>Significant digits for the action number generator to determine the maximum number of supported actions at any given time in the system.</td></tr>");
			out.println("	 </table>");
			out.println("</div>");
			out.println("");
*/
			out.println("&nbsp;<p/>&nbsp;<p/><input type=\"submit\" value=\"Save Properties\">");
			out.println("</form>");
			out.println("");
			out.println("</body>");
			out.println("</html>");
        }
        catch (Exception e) {
        	Log.error(e);
        }
	}

	private static boolean isNumb(String str)
	{
		try {

			Integer.parseInt(str);
			return true;

		} catch (Exception e) {

			return false;
		}
	}

	private void validateFields(Map<String, String> errors)
	{

		if(name.length() < 1 ) {
			errors.put("sitename", "");
			errorMessage = "Please specify Site Name";
		}
		if(privateHost.length() < 1 ) {
			errors.put("privateHost", "");
			errorMessage = "Please specify Private Host name";
		}

		if(publicHost.length() < 1 ) {
			errors.put("publicHost", "");
			errorMessage = "Please specify Public Host name";
		}

		if(pbxCountryCode.length() < 1 ) {
			errors.put("pbxCountryCode", "");
			errorMessage = "Please specify Country Code";
		}

		if(pbxNumberLength.length() < 1 ) {
			errors.put("pbxNumberLength", "");
			errorMessage = "Please specify PBX Internal extension length";
		}
	}

}

