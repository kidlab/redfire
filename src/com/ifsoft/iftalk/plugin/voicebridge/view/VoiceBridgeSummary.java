package com.ifsoft.iftalk.plugin.voicebridge.view;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.jivesoftware.util.Log;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;

import com.ifsoft.iftalk.plugin.voicebridge.RedfirePlugin;
import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeComponent;
import com.ifsoft.iftalk.plugin.voicebridge.Site;
import com.ifsoft.iftalk.plugin.voicebridge.SiteDao;


public class VoiceBridgeSummary extends HttpServlet {

	private long   siteID;
	private SiteDao siteDao;
	private RedfirePlugin plugin;
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
		siteDao = new SiteDao(plugin);
		String action = request.getParameter("action");

		if(request.getParameter("siteID") != null) {
			siteID = Long.parseLong(request.getParameter("siteID"));
		}

		if(action == null) {
			action = " ";
		}

		if(action.equals("deleteConfirm")) {

			displayDeleteConfirm(out);
		}

		else if(action.equals("resetConfirm")) {

			displayResetConfirm(out);
		}

		else if(action.equals("delete")) {

			boolean cancel = request.getParameter("cancel") != null;
		    boolean delete = request.getParameter("delete") != null;

		    if (cancel) {
		    	displayPage(out, "Deletion request for site " + siteID + " cancelled");
		    }

		    if(delete) {
		    	siteDao.remove(siteID);
		    	displayPage(out, "Site " + siteID + " is deleted");
		    }
		}

		else if(action.equals("resetSite")) {

			String siteID = request.getParameter("site");
			VoiceBridgeComponent voicebridgeComponent = plugin.getVoiceBridgeComponentBySiteID(siteID);

			if (voicebridgeComponent != null)
			{
				voicebridgeComponent.asyncLoadCachesFromLDAP();
				displayPage(out, "Reset of Cache for site " + siteID + " has been scheduled as a background task");

			} else {

				displayPage(out, "Cache for site " + siteID + " is not refreshed. Site Id not found");
			}

		} else if(action.equals("reset")) {
			boolean cancel = request.getParameter("cancel") != null;
		    boolean reset = request.getParameter("reset") != null;

		    if (cancel) {
		    	displayPage(out, "Reset request for all sites cancelled");

		    } else {

				try {
					plugin.resetComponentCaches();

					displayPage(out, "Reset of Caches for all sites have been scheduled as background tasks");
				}
				catch (Exception e) {
					Log.error("VoiceBridgeSummary " + e);
				}
			}

		}
		else {
			displayPage(out, null);
		}
	}

	private void displayPage(ServletOutputStream out, String resetMessage) {
		try {
			out.println("");
			out.println("<html>");
			out.println("    <head>");
			out.println("        <title>Voice Bridge Summary</title>");
			out.println("        <meta name=\"pageID\" content=\"VoiceBridge-SUMMARY\"/>");
			out.println("    </head>");
			out.println("    <body>");

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
			out.println("<div id='jive-title'>" + plugin.lastLoadedDate == null ? "&nbsp" : "Active since "  + plugin.lastLoadedDate  + "</div>");
            out.println("<div class=\"jive-table\">");
            out.println("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
            out.println("<thead>");
            out.println("<tr>");
            out.println("<th nowrap>&nbsp;</th>");
            out.println("<th nowrap>Site Name</th>");
            out.println("<th nowrap>Edit</th>");
            //out.println("<th nowrap>Delete</th>");
            out.println("</tr>");
            out.println("</thead>");
            out.println("<tbody>");

            Collection<Site> sites = siteDao.getSites();
            Iterator<Site> iter4 = sites.iterator();
            int i = 1;

            while(iter4.hasNext()) {
                Site site = (Site)iter4.next();
				VoiceBridgeComponent voicebridgeComponent = plugin.getVoiceBridgeComponentBySiteID(String.valueOf(site.getSiteID()));

                if(site != null) {
                    if(i % 2 == 1)
                        out.println("<tr class=\"jive-odd\">");
                    else
                        out.println("<tr class=\"jive-even\">");

                    out.println("<td width=\"1%\">");
                    out.println(site.getSiteID());
                    out.println("</td>");

                    out.println("<td width=\"19%\">");
                   	out.println("<a href='voicebridge-profile-summary?siteID=" + site.getSiteID() + "&siteName=" + site.getName() + "'>" + site.getName() + "</a>");
                    out.println("</td>");

                    out.println("<td width=\"5%\">");
                    out.println("<a href=\"voicebridge-settings?action=edit&siteID=" + site.getSiteID() + "\"><img src=\"images/edit-16x16.gif\" alt=\"Edit Site\" border=\"0\"></a>");
                    out.println("</td>");

                    //out.println("<td width=\"15%\">");
                    //out.println("<a href=\"voicebridge-summary?action=deleteConfirm&siteID=" + site.getSiteID() + "\"><img src=\"images/delete-16x16.gif\" alt=\"Delete Site\" border=\"0\"></a>");
                    //out.println("</td>");

                    out.println("</tr>");
                    i++;
                }
            }

            out.println("</tbody>");
            out.println("</table>");
	        out.println("</div>");
			out.println("<br/>");

            //out.println("<p>");
            //out.println("<a href=\"voicebridge-settings?action=top\"><img src=\"images/add-16x16.gif\" alt=\"Add new Site\" border=\"0\">Add new Site</a>");
            //out.println("</p");

            out.println("</body>");
            out.println("</html>");
        }
        catch (Exception e) {
        	Log.error(e);
        }
	}

	private void displayDeleteConfirm(ServletOutputStream out) {
		try {
			out.println("");
			out.println("<html>");
			out.println("    <head>");
			out.println("        <title>Delete Site?</title>");
			out.println("        <meta name=\"pageID\" content=\"VoiceBridge-SUMMARY\"/>");
			out.println("    </head>");
			out.println("    <body>");
			out.println("");
			out.println("<p>");
			out.println("<b>");
			out.println("Are you sure you want to delete the site from the system?");
			out.println("</p>");
			out.println("</b>");
			out.println("<form action=\"voicebridge-summary\">");
			out.println("<input type='hidden' name='siteID' value='" + siteID + "'>");
			out.println("<input type='hidden' name='action' value='delete'>");
			out.println("<input type=\"submit\" name=\"delete\" value=\"Delete\">");
			out.println("<input type=\"submit\" name=\"cancel\" value=\"Cancel\">");
			out.println("</form>");
            out.println("</body>");
            out.println("</html>");
        }
        catch (Exception e) {
        	Log.error(e);
        }
	}

	private void displayResetConfirm(ServletOutputStream out) {
		try {
			out.println("");
			out.println("<html>");
			out.println("    <head>");
			out.println("        <title>Reset Cache?</title>");
			out.println("        <meta name=\"pageID\" content=\"VoiceBridge-SUMMARY\"/>");
			out.println("    </head>");
			out.println("    <body>");
			out.println("");
			out.println("<p>");
			out.println("<b>");
			out.println("Are you sure you want to reset the VoiceBridge LDAP cache for all sites?");
			out.println("</p>");
			out.println("</b>");
			out.println("<form action=\"voicebridge-summary\">");
			out.println("<input type='hidden' name='action' value='reset'>");
			out.println("<input type=\"submit\" name=\"reset\" value=\"Reset\">");
			out.println("<input type=\"submit\" name=\"cancel\" value=\"Cancel\">");
			out.println("</form>");
            out.println("</body>");
            out.println("</html>");
        }
        catch (Exception e) {
        	Log.error(e);
        }
	}

}
