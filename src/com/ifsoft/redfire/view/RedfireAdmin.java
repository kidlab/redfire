package com.ifsoft.redfire.view;

import java.io.IOException;
import java.util.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jivesoftware.util.JiveGlobals;

import org.apache.log4j.Logger;


public class RedfireAdmin extends HttpServlet
{
	private String action                   = "edit";
	private String cumulusPort	            = "1935";
	private String cumulusKeepAliveServer	= "5";
	private String cumulusKeepAlivePeer	    = "5";

    protected Logger Log = Logger.getLogger(getClass().getName());

	private String errorMessage = null;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
    }


	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response.addHeader("Cache-Control", "post-check=0, pre-check=0");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Content-Type", "text/html");
		response.setHeader("Connection", "close");

		ServletOutputStream out = response.getOutputStream();
		Map<String, String> errors = new HashMap<String, String>();

		action = request.getParameter("action");

		if (action == null) {
			action = "edit";
		}

		if(action.equals("edit"))
		{
			cumulusPort		= JiveGlobals.getProperty("voicebridge.rtmfp.port", cumulusPort);
			cumulusKeepAliveServer		= JiveGlobals.getProperty("voicebridge.rtmfp.keep.alive.server", cumulusKeepAliveServer);
			cumulusKeepAlivePeer		= JiveGlobals.getProperty("voicebridge.rtmfp.keep.alive.peer", cumulusKeepAlivePeer);

			displayPage(out, errors.size());
		}

		else if(action.equals("update")) {

			cumulusPort				= request.getParameter("cumulusPort");
			cumulusKeepAliveServer  = request.getParameter("cumulusKeepAliveServer");
			cumulusKeepAlivePeer  = request.getParameter("cumulusKeepAlivePeer");


			validateFields(errors);

			if(errors.isEmpty()) {

				try {

					JiveGlobals.setProperty("cumulus.port", cumulusPort);
					JiveGlobals.setProperty("voicebridge.rtmfp.keep.alive.server", cumulusKeepAliveServer);
					JiveGlobals.setProperty("voicebridge.rtmfp.keep.alive.peer", cumulusKeepAlivePeer);

					Log.info("Redfire Properties updated");
				}
				catch (Exception e) {
					Log.error(e.getMessage(), e);
				}

				response.sendRedirect("redfire-properties");

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
			out.println("        <title>Properties</title>");
			out.println("        <meta name=\"pageID\" content=\"REDFIRE-PROPERTIES\"/>");
			out.println("    </head>");
			out.println("    <body>");

			if (errorSize > 0) {
				out.println("<div class=\"error\">");
				out.println(errorMessage);
				out.println("</div>");
			}
			out.println("");
			out.println("Use the form below to edit Redfire Properties.<br>");
			out.println("</p>");
			out.println("<form action=\"redfire-properties\" method=\"get\">");

			if(action.equals("edit")) {
				out.println("<input type='hidden' name='action' value='update'>");

			} else {
				out.println("<input type='hidden' name='action' value='edit'>");
			}
			out.println("");

			out.println("<div class=\"jive-contentBoxHeader\">Cumulus OpenRTMFP Server</div>");
			out.println("<div class=\"jive-contentBox\">");
			out.println("	 <table>");
			out.println("	 	<tr><td>RTMFP UDP Port</td><td><input size='20' type='text' name='cumulusPort' value='" + cumulusPort + "'></td>");
			out.println("	 		<td>UDP listening port for media between RTMFP server and Flash Player</td></tr>");
			out.println("	 	<tr><td>Server Keep Alive</td><td><input size='20' type='text' name='cumulusKeepAliveServer' value='" + cumulusKeepAliveServer + "'></td>");
			out.println("	 		<td>time in seconds for periodically sending packets keep-alive with server, 15s by default (valid value is from 5s to 255s)</td></tr>");
			out.println("	 	<tr><td>Peer Keep Alive</td><td><input size='20' type='text' name='cumulusKeepAlivePeer' value='" + cumulusKeepAlivePeer + "'></td>");
			out.println("	 		<td> time in seconds for periodically sending packets keep-alive between peers, 10s by default (valid value is from 5s to 255s)</td></tr>");
			out.println("	 </table>");
			out.println("</div>");
			out.println("");

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


	private void validateFields(Map<String, String> errors)
	{
		if(cumulusPort.length() < 1 ) {
			errors.put("cumulusPort", "");
			errorMessage = "Please specify a port for Cumulus";
		}

		if(cumulusKeepAliveServer.length() < 1 ) {
			errors.put("cumulusKeepAliveServer", "");
			errorMessage = "Please specify the server keep alive time period";
		}

		if(cumulusKeepAlivePeer.length() < 1 ) {
			errors.put("cumulusKeepAlivePeer", "");
			errorMessage = "Please specify the peer keep alive time period";
		}
	}

}

