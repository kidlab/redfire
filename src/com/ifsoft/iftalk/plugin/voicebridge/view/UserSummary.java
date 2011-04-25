package com.ifsoft.iftalk.plugin.voicebridge.view;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import org.jivesoftware.util.Log;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.User;

import com.ifsoft.iftalk.plugin.voicebridge.RedfirePlugin;
import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeComponent;
import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeUser;
import com.ifsoft.iftalk.plugin.voicebridge.SiteDao;
import com.ifsoft.iftalk.plugin.voicebridge.Site;


public class UserSummary extends HttpServlet {

	private RedfirePlugin plugin;
	private SiteDao siteDao;
	private UserManager userManager;
	protected Logger Log = Logger.getLogger(getClass().getName());


    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
		plugin = (RedfirePlugin)XMPPServer.getInstance().getPluginManager().getPlugin("redfire");
		userManager = XMPPServer.getInstance().getUserManager();
    }

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response.addHeader("Cache-Control", "post-check=0, pre-check=0");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Content-Type", "text/html");
		response.setHeader("Connection", "close");

		ServletOutputStream out = response.getOutputStream();

		siteDao = new SiteDao(plugin);
        Collection<Site> sites = siteDao.getSites();

		try {
			String oldTSC = request.getParameter("old");
			String newTSC = request.getParameter("new");
			String userName = request.getParameter("user");
			String action = request.getParameter("action");

			String resetMessage = null;

			if (oldTSC != null && newTSC != null && userName != null)
			{
				if (("none".equals(newTSC)) && !("none".equals(oldTSC)))	// remove user from site
				{
					removeUser(oldTSC, userName);
					resetMessage = userName + " is de-allocated from " + oldTSC;
				}

				if (!("none".equals(newTSC)) && ("none".equals(oldTSC)))	// add/replace user with new site
				{
					addUser(newTSC, userName);
					resetMessage = userName + " is allocated to " + newTSC;
				}

				if (!("none".equals(newTSC)) && !("none".equals(oldTSC)))	// update user site
				{
					removeUser(oldTSC, userName);
					addUser(newTSC, userName);
					resetMessage = userName + " is re-allocated from " + oldTSC + " to " + newTSC;
				}
			}

			if ("reset".equals(action))
			{
				String siteID = request.getParameter("site");
				resetUser(siteID, userName);
				resetMessage = "Cache reset for " + userName;
			}

			out.println("");
			out.println("<html>");
			out.println("<head>");
			out.println("<title>VoiceBridge Users</title>");
			out.println("<meta name=\"pageID\" content=\"VoiceBridge-USER-SUMMARY\"/>");
			out.println("</head>");
			out.println("<body>");

			if (resetMessage != null)
			{
				out.println("<div class='jive-success'>");
				out.println("<table cellpadding='0' cellspacing='0' border='0'>");
				out.println("<tbody>");
				out.println("<tr><td class='jive-icon'><img src='images/success-16x16.gif' width='16' height='16' border='0' alt=''></td>");
				out.println("<td class='jive-icon-label'>");
				out.println(resetMessage);
				out.println("</td></tr>");
				out.println("</tbody>");
				out.println("</table>");
				out.println("</div>");
			}

			out.println("");
			out.println("<br>");

			out.println("<div id='jive-title'>VoiceBridge Plugin User Summary</div>");
			out.println("<table cellpadding=\"2\" cellspacing=\"2\" border=\"0\"><tr><td>Pages:[</td>");

			int linesCount = 20;
			int userCounter = userManager.getUserCount();
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

					out.println("<td><a href='voicebridge-user-summary?start=" + iStart + "&count=" + iCount + "'>" + page + "</a><td>");
				}
			}

			out.println("<td>]</td></tr></table>");
			out.println("<div class=\"jive-table\">");
			out.println("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">");
			out.println("<thead>");
			out.println("<tr>");
			out.println("<th nowrap></th>");
			out.println("<th nowrap>User Name</th>");
			out.println("<th nowrap>Full Name</th>");
			out.println("<th nowrap>VoiceBridge Site</th>");
			out.println("<th nowrap>Reset Cache</th>");
			out.println("</tr>");
			out.println("</thead>");
			out.println("<tbody>");

			Collection<User> users = userManager.getUsers(pageStart, pageCount);

            Iterator it = users.iterator();
            int i = 0;

			while( it.hasNext() )
			{
				try
				{
               		User user = (User)it.next();

					if(i % 2 == 1)
						out.println("<tr class=\"jive-odd\">");
					else
						out.println("<tr class=\"jive-even\">");

					out.println("<td width=\"1%\">");
					out.println((pageStart + i + 1));
					out.println("</td>");
					out.println("<td width=\"9%\">");
					out.println(user.getUsername());
					out.println("</td>");
					out.println("<td width=\"70%\">");
					out.println(user.getName());
					out.println("</td>");
					out.println("<td width=\"10%\">");

            		Iterator<Site> iter4 = sites.iterator();
            		String userTSC = plugin.getUserTSC(user.getUsername());
            		String previousTSC = userTSC;
					String siteID = null;

            		int pos = userTSC.indexOf(".");

            		if (pos > -1)
            		{
						userTSC = userTSC.substring(0, pos);
					}

					out.println("<select size='1' onchange='location.href=\"voicebridge-user-summary?start=" + pageStart + "&count=" + pageCount + "&user=" + user.getUsername() + "&old=" + previousTSC +"&new=\" +  this.options[this.selectedIndex].value'><option value='none'>None</option>");

					while( iter4.hasNext() )
					{
						Site site = (Site)iter4.next();

						if (userTSC.equals("voicebridge" + site.getSiteID()))
						{
							out.println("<option selected value='" +  site.getSiteID() + "'>" + site.getName() + "</option>");
							siteID = String.valueOf(site.getSiteID());

						} else {

							out.println("<option value='" +  "voicebridge" + site.getSiteID() + "." + plugin.getDomain() + "'>" + site.getName() + "</option>");
						}
					}
					out.println("</select>");
					out.println("</td>");
					out.println("<td width=\"10%\">");

					if (siteID != null)
            			out.println("<a href='voicebridge-user-summary?action=reset&user=" + user.getUsername() + "&site=" + siteID + "&start=" + pageStart + "&count=" + pageCount + "'><img src=\"images/refresh-16x16.gif\" alt=\"Reset User Cache\" border=\"0\"></a>");
            		else
            			out.println("&nbsp;");

					out.println("</td>");
					out.println("</tr>");

					i++;
				}
				catch(Exception e)
				{
					Log.error("UserSummary " + e);
					e.printStackTrace();
				}
			}
			out.println("</tbody>");
			out.println("</table>");
			out.println("</div>");
			out.println("</body>");
			out.println("</html>");

        }
        catch (Exception e) {
        	Log.error("UserSummary " + e);
        	e.printStackTrace();
        }
	}

	private VoiceBridgeComponent getVoiceBridgeComponent(String tsc)
	{
		VoiceBridgeComponent voicebridgeComponent = null;
		int pos = tsc.indexOf(".");

		if (pos > 11)
		{
			String siteID = tsc.substring(11, pos);
			voicebridgeComponent = plugin.getVoiceBridgeComponentBySiteID(siteID);
		}

		return voicebridgeComponent;
	}

	private void removeUser(String oldTSC, String userName)
	{
		plugin.removeUserTSC(userName, oldTSC);

		VoiceBridgeComponent voicebridgeComponent = getVoiceBridgeComponent(oldTSC);

		if (voicebridgeComponent != null)
		{
			voicebridgeComponent.voicebridgeLdapService.removeVoiceBridgeUser(userName);

		} else {

			Log.error("removeUser - can't find " + oldTSC);
		}
	}


	private void addUser(String newTSC, String userName)
	{
		VoiceBridgeComponent voicebridgeComponent = getVoiceBridgeComponent(newTSC);

		if (voicebridgeComponent != null)
		{
			plugin.addUserTSC(userName, newTSC, voicebridgeComponent.getSiteName());
			voicebridgeComponent.voicebridgeLdapService.addVoiceBridgeUser(userName);

		} else {

			Log.error("addUser - can't find " + newTSC);
		}
	}

	private void resetUser(String siteID, String userName)
	{
		VoiceBridgeComponent voicebridgeComponent = plugin.getVoiceBridgeComponentBySiteID(siteID);

		if (voicebridgeComponent != null)
		{
			VoiceBridgeUser voicebridgeUser = voicebridgeComponent.getVoiceBridgeUser(userName);

			if (voicebridgeUser != null)
			{
				try
				{
					voicebridgeComponent.voicebridgeLdapService.getUserProfile(voicebridgeUser);
				}
				catch (Exception e) {
					Log.error("UserSummary resetUser " + e);
				}

			} else {

				Log.error("resetUser - can't find user for " + userName);
			}

		} else {

			Log.error("resetUser - can't find component for " + siteID);
		}
	}
}

