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
	private String cumulusPort	            = "5555";
	private String cumulusPath	            = "C:\\Program Files\\openfire\\plugins\\redfire\\cumulus\\CumulusServer.exe";

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
			cumulusPort		= JiveGlobals.getProperty("cumulus.port", cumulusPort);
			cumulusPath		= JiveGlobals.getProperty("cumulus.path", cumulusPath);

			displayPage(out, errors.size());
		}

		else if(action.equals("update")) {

			cumulusPort			= request.getParameter("cumulusPort");
			cumulusPath			= request.getParameter("cumulusPath");

			validateFields(errors);

			if(errors.isEmpty()) {

				try {

					JiveGlobals.setProperty("cumulus.port", cumulusPort);
					JiveGlobals.setProperty("cumulus.path", cumulusPath);

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
			out.println("	 	<tr><td>Cumulus Port</td><td><input size='20' type='text' name='cumulusPort' value='" + cumulusPort + "'></td>");
			out.println("	 		<td>TCP listening port for media between Openfire and Cumulus.</td></tr>");
			out.println("	 	<tr><td>Cumulus Aplication path</td><td><input size='50' type='text' name='cumulusPath' value='" + cumulusPath + "'></td>");
			out.println("	 		<td>Full path to the Cumulus executable file.</td></tr>");
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

		if(cumulusPath.length() < 1 ) {
			errors.put("cumulusPath", "");
			errorMessage = "Please specify the path to the Cumulus executable";
		}
	}

}

