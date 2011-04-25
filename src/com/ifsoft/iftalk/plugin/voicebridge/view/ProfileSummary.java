package com.ifsoft.iftalk.plugin.voicebridge.view;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.jivesoftware.util.Log;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserNotFoundException;

import com.ifsoft.iftalk.plugin.voicebridge.RedfirePlugin;
import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeComponent;
import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeUser;
import com.ifsoft.iftalk.plugin.voicebridge.Site;
import com.ifsoft.iftalk.plugin.voicebridge.SiteDao;
import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeCallback;

public class ProfileSummary extends HttpServlet {

	private RedfirePlugin plugin;
	private SiteDao siteDao;
    private Collection<Site> sites;
	protected Logger Log = Logger.getLogger(getClass().getName());

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

		String siteID = request.getParameter("siteID");
		String siteName = request.getParameter("siteName");
		String callback = request.getParameter("callback");
		String remove = request.getParameter("remove");
		String userNo = request.getParameter("userno");

		try {
			VoiceBridgeComponent voicebridgeComponent = plugin.getVoiceBridgeComponentBySiteID(String.valueOf(siteID));

			if (remove != null && userNo != null)
			{
				voicebridgeComponent.voicebridgeLinkService.freeCallback(userNo);
				response.sendRedirect("voicebridge-profile-summary?siteID=" + siteID + "&siteName=" + siteName);
			}

			out.println("");
			out.println("<html>");
			out.println("    <head>");
			out.println("        <title>VoiceBridge User Profiles</title>");
			out.println("        <meta name=\"pageID\" content=\"VoiceBridge-SUMMARY\"/>");
			out.println("    </head>");
			out.println("    <body>");
			out.println("");
			out.println("<br>");

			out.println("<div id='jive-title'>" + siteName + "</div>");
			out.println("<table cellpadding=\"2\" cellspacing=\"2\" border=\"0\"><tr><td>Pages:[</td>");

			int linesCount = 20;
			int userCounter = voicebridgeComponent.getUserCount();
			int pageCounter = (userCounter/linesCount);

			pageCounter = userCounter > (linesCount * pageCounter) ? pageCounter + 1 : pageCounter;

			String start = request.getParameter("start");
			String count = request.getParameter("count");

			int pageStart = start == null ? 0 : Integer.parseInt(start);
			int pageCount = count == null ? linesCount : Integer.parseInt(count);


			for (int i=0; i<pageCounter; i++)
			{
				int iStart = (i * linesCount);
				int iCount = ((i * linesCount) + linesCount) > userCounter ? ((i * linesCount) + linesCount) - userCounter : linesCount;
				int page = i + 1;

				if (pageStart == iStart)
				{
					out.println("<td>" + page + "<td>");

				} else {

					out.println("<td><a href='voicebridge-profile-summary?siteID=" + siteID + "&siteName=" + siteName + "&start=" + iStart + "&count=" + iCount + "'>" + page + "</a><td>");
				}
			}

			out.println("<td>]</td></tr></table>");
			out.println("<div class=\"jive-table\">");
			out.println("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">");
			out.println("<thead>");
			out.println("<tr>");
			out.println("<th nowrap>Id</th>");
			out.println("<th nowrap>User Id</th>");
			out.println("<th nowrap>User No</th>");
			out.println("<th nowrap>Device</th>");
			//out.println("<th nowrap>HS</th>");
			//out.println("<th nowrap>Callset</th>");
			out.println("<th nowrap>Name</th>");
			out.println("<th nowrap>Dir No</th>");
			out.println("<th nowrap></th>");
			out.println("<th nowrap>Callback</th>");
			out.println("<th nowrap>Hold</th>");
			out.println("<th nowrap>Priv</th>");
			out.println("<th nowrap>Default</th>");
			out.println("</tr>");
			out.println("</thead>");
			out.println("<tbody>");

			List<VoiceBridgeUser> sortedProfiles = voicebridgeComponent.getUsers(pageStart, pageCount);

			Iterator it = sortedProfiles.iterator();

			int i = 0;
			String thisTSC = voicebridgeComponent.getComponentJID().toString();


			while( it.hasNext() )
			{
				VoiceBridgeUser voicebridgeUser = (VoiceBridgeUser)it.next();

				if (thisTSC.equals(plugin.getUserTSC(voicebridgeUser.getUserId())))
				{
					try
					{
						if (XMPPServer.getInstance().getUserManager().isRegisteredUser(voicebridgeUser.getUserId()))
						{
							if (callback != null && userNo != null && userNo.equals(voicebridgeUser.getUserNo()))
							{
								if (voicebridgeComponent.voicebridgeLinkService.getCallback(voicebridgeUser) == null)
									voicebridgeComponent.voicebridgeLinkService.allocateCallback(voicebridgeUser);
								else
									voicebridgeComponent.voicebridgeLinkService.freeCallback(voicebridgeUser.getUserNo());

								Thread.sleep(1000);

								response.sendRedirect("voicebridge-profile-summary?siteID=" + siteID + "&siteName=" + siteName);
							}

							User user = XMPPServer.getInstance().getUserManager().getUser(voicebridgeUser.getUserId());

							if(i % 2 == 1)
								out.println("<tr valign='top' class=\"jive-odd\">");
							else
								out.println("<tr valign='top' class=\"jive-even\">");

							out.println("<td width=\"6%\">");
							out.println("<a href='voicebridge-profile-detail?user=" + voicebridgeUser.getProfileName() + "&site=" + siteID + "&siteName=" + siteName + "'>" + voicebridgeUser.getProfileName() + "</a>");
							out.println("</td>");
							out.println("<td width=\"6%\">");
							out.println(voicebridgeUser.getUserId());
							out.println("</td>");
							out.println("<td width=\"6%\">");
							out.println(voicebridgeUser.getUserNo());
							out.println("</td>");

							int deviceNumber = 0;

							try
							{
								deviceNumber = Integer.parseInt(voicebridgeUser.getDeviceNo());
							}
							catch(Exception e) { }

							if (deviceNumber != 0)
							{
								out.println("<td width=\"6%\">");
								out.println(String.valueOf(deviceNumber));
								out.println("</td>");

							} else {
								out.println("<td style='background-color:#c04d27' width=\"6%\"><font color='#ffffff'>");
								out.println("offline");
								out.println("</font></td>");
							}

							//out.println("<td width=\"4%\">");
							//out.println(voicebridgeUser.getHandsetNo());
							//out.println("</td>");
							//out.println("<td width=\"6%\">");
							//out.println(voicebridgeUser.getCallset() == null ? "&nbsp;" : voicebridgeUser.getCallset());
							//out.println("</td>");
							out.println("<td width=\"21%\">");
							out.println(voicebridgeUser.getUserName());
							out.println("</td>");
							out.println("<td width=\"6%\">");
							out.println(voicebridgeUser.getPersonalDDI() == null ? "&nbsp;" : voicebridgeUser.getPersonalDDI());
							out.println("</td>");

							String callbackActive = voicebridgeUser.getPhoneCallback() != null ? "<img src=\"images/success-16x16.gif\" alt=\"Yes\" border=\"0\">" : "&nbsp;";
							String callbackLink =  "<a href='voicebridge-profile-summary?siteID=" + siteID + "&siteName=" + siteName + "&callback=" + voicebridgeUser.getCallback() + "&userno=" + voicebridgeUser.getUserNo() + "'>" + voicebridgeUser.getCallback() + "</a>";

							out.println("<td width=\"1%\">");
							out.println(callbackActive);
							out.println("</td>");

							out.println("<td width=\"5%\">");
							out.println(voicebridgeUser.getCallback() == null ? "&nbsp;" : callbackLink);
							out.println("</td>");

							out.println("<td width=\"6%\">");
							out.println(voicebridgeUser.autoHold() ? "<img src=\"images/success-16x16.gif\" alt=\"Yes\" border=\"0\">" : "&nbsp;");
							out.println("</td>");
							out.println("<td width=\"6%\">");
							out.println(voicebridgeUser.autoPrivate() ? "<img src=\"images/success-16x16.gif\" alt=\"Yes\" border=\"0\">" : "&nbsp;");
							out.println("</td>");
							out.println("<td width=\"6%\">");
							out.println("true".equals(voicebridgeUser.getDefault()) ? "<img src=\"images/success-16x16.gif\" alt=\"Yes\" border=\"0\">" : "&nbsp;");
							out.println("</td>");
							out.println("</tr>");

							i++;

						} else Log.warn( "["+ siteName + "] ProfileSummary - ignoring VoiceBridge User " + voicebridgeUser.getUserId());
					}
					catch(Exception e)
					{
						Log.error( "["+ siteName + "] ProfileSummary " + e);
        				e.printStackTrace();
					}
				}
			}
			out.println("<tr>");
			out.println("<td>&nbsp;</td>");
			out.println("</tr>");
			out.println("</tbody>");
			out.println("</table>");
			out.println("</div>");
			out.println("<p>&nbsp;</p>");

			if (voicebridgeComponent.voicebridgeLinkService != null && voicebridgeComponent.voicebridgeLinkService.isCallbackAvailable())
			{
				out.println("<div id='jive-title'>Virtual Devices (VSC)</div>");
				out.println("<div class=\"jive-table\">");
				out.println("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">");
				out.println("<thead>");
				out.println("<tr>");
				out.println("<th nowrap>Device</th>");
				out.println("<th nowrap>Local<br>Handset</th>");
				out.println("<th nowrap>Remote<br>Handset</th>");
				out.println("<th nowrap>User<br/>Id</th>");
				out.println("<th nowrap>User Name</th>");
				out.println("<th nowrap>Destination</th>");
				out.println("<th nowrap>Timestamp</th>");
				out.println("<th nowrap>Callback</th>");
				out.println("<th nowrap>Third<br/>Party</th>");
				out.println("<th nowrap>Active</th>");
				out.println("<th nowrap>Remove</th>");
				out.println("</tr>");
				out.println("</thead>");
				out.println("<tbody>");

				it = voicebridgeComponent.voicebridgeLinkService.getCallbacks().values().iterator();
				i = 0;

				while( it.hasNext() )
				{
					VoiceBridgeCallback voicebridgeCallback = (VoiceBridgeCallback)it.next();

					if(i % 2 == 1)
						out.println("<tr valign='top' class=\"jive-odd\">");
					else
						out.println("<tr valign='top' class=\"jive-even\">");


					out.println("<td width=\"4%\">");
					out.println(String.valueOf(Long.parseLong(voicebridgeCallback.getVirtualDeviceId())));
					out.println("</td>");

					out.println("<td width=\"5%\">");
					out.println(voicebridgeCallback.getLocalHandset());
					out.println("</td>");

					out.println("<td width=\"5%\">");
					out.println(voicebridgeCallback.getRemoteHandset());
					out.println("</td>");

					out.println("<td width=\5%\">");
					out.println(voicebridgeCallback.getVoiceBridgeUser() == null ? "&nbsp;" : voicebridgeCallback.getVoiceBridgeUser().getUserId());
					out.println("</td>");

					out.println("<td width=\"20%\">");
					out.println(voicebridgeCallback.getVoiceBridgeUser() == null ? "&nbsp;" : voicebridgeCallback.getVoiceBridgeUser().getUserName());
					out.println("</td>");

					out.println("<td width=\"10%\">");
					out.println(voicebridgeCallback.getVoiceBridgeUser() == null ? "&nbsp;" : voicebridgeCallback.getVoiceBridgeUser().getCallback());
					out.println("</td>");

					out.println("<td width=\"33%\">");
					out.println(voicebridgeCallback.getTimestamp() == null ? "&nbsp;" : String.valueOf(voicebridgeCallback.getTimestamp()));
					out.println("</td>");

					out.println("<td width=\"2%\">");
					out.println(voicebridgeCallback.getVoiceBridgeUser() != null && voicebridgeCallback.getVoiceBridgeUser().getUserType().equals("VoiceBridge") ? "<img src=\"images/success-16x16.gif\" alt=\"Yes\" border=\"0\">" : "&nbsp;");
					out.println("</td>");

					out.println("<td width=\"2%\">");
					out.println(voicebridgeCallback.getVoiceBridgeUser() != null && voicebridgeCallback.getVoiceBridgeUser().getUserType().equals("VSC")  ? "<img src=\"images/success-16x16.gif\" alt=\"Yes\" border=\"0\">" : "&nbsp;");
					out.println("</td>");

					out.println("<td width=\"2%\">");
					out.println(voicebridgeCallback.getVoiceBridgeUser() != null && voicebridgeCallback.getVoiceBridgeUser().getCallbackActive() ? "<img src=\"images/success-16x16.gif\" alt=\"Yes\" border=\"0\">" : "&nbsp;");
					out.println("</td>");

					out.println("<td width=\"2%\">");

					if (voicebridgeCallback.getVoiceBridgeUser() == null)
					{
						out.println("&nbsp;");

					} else {
						out.println("<script>function doVSCRemove" + voicebridgeCallback.getVirtualDeviceId() + "(){if(confirm('Do you wish to delete device " + voicebridgeCallback.getVirtualDeviceId() + "')){location.href='voicebridge-profile-summary?siteID=" + siteID + "&remove=" + voicebridgeCallback.getVoiceBridgeUser().getCallback() + "&userno=" + voicebridgeCallback.getVoiceBridgeUser().getUserNo() + "&siteName=" + siteName + "&start=" + pageStart + "&count=" + pageCount + "'}}</script><a href='javascript:doVSCRemove" + voicebridgeCallback.getVirtualDeviceId() + "()'><img src=\"images/delete-16x16.gif\" alt=\"Delete Message\" border=\"0\"</a>");
					}
					out.println("</td>");


					out.println("</tr>");

				}
				out.println("</tbody>");
				out.println("</table>");
				out.println("</div>");
			}

			out.println("<p></p>");
			out.println("</body>");
			out.println("</html>");

        }
        catch (Exception e) {
			Log.error( "["+ siteName + "] ProfileSummary " + e);
			e.printStackTrace();

        }
	}
}

