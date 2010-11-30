package com.ifsoft.redfire.servlets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;

import java.io.*;
import java.util.*;
import org.jivesoftware.util.JiveGlobals;
import com.ifsoft.redfire.RedfireConstants;

public class JnlpServlet extends HttpServlet  implements RedfireConstants {

	public static final long serialVersionUID = 24362462L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("application/x-java-jnlp-file");
		response.setHeader("Content-Disposition","Inline; filename=screencast.jnlp");

        try {

			ServletOutputStream out = response.getOutputStream();

			String stream = request.getParameter("stream");
			String app = request.getParameter("app");
			String port = request.getParameter("port");

			if (stream == null) {
				stream = "screen_share";
			}

			if (app == null) {
				app = "oflaDemo";
			}

			if (port == null) {
				port = "1935";
			}

			out.println("<?xml version='1.0' encoding='utf-8'?>");
			out.println("<jnlp spec='1.0+' codebase='http://" + request.getServerName() + ":" + request.getServerPort() + "/redfire/screen'> ");
			out.println("	<information> ");
			out.println("		<title>Redfire ScreenShare</title> ");
			out.println("		<vendor>Dele Olajide</vendor> ");
			out.println("		<homepage>http://code.google.com/p/red5screnshare</homepage>");
			//out.println("		<icon href='icon.jpg' />");
			//out.println("		<icon kind='splash' href='splashicon.jpg' />");
			out.println("		<description>ScreenShare Client Application</description> ");
			out.println("		<description kind='short'>An Java Webstart application that publishes desktop screen as RTMP video stream</description> ");
			out.println("		<offline-allowed/> ");
			out.println("	</information>");
			out.println("	<security>");
			out.println("		<all-permissions/>");
			out.println("	</security>	");
			out.println("	<resources> ");
			out.println("	<j2se version='1.4+'/> ");
			out.println("		<jar href='screenshare.jar'/> ");
			out.println("	</resources> ");
			out.println("	<application-desc main-class='org.redfire.screen.ScreenShare'>");
			out.println("		<argument>" + request.getServerName() + "</argument>");
			out.println("		<argument>" + app + "</argument>");
			out.println("		<argument>" + port + "</argument>");
			out.println("		<argument>" + stream + "</argument> ");
			out.println("	</application-desc> ");
			out.println("</jnlp>");
			        }
        catch (Exception e) {
        }
	}
}
