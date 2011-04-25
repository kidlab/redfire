package com.ifsoft.iftalk.plugin.voicebridge.view;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.jivesoftware.util.*;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.User;

import com.ifsoft.iftalk.plugin.tsc.calllog.CallLog;
import com.ifsoft.iftalk.plugin.tsc.calllog.CallLogDAO;
import com.ifsoft.iftalk.plugin.tsc.calllog.CallFilter;

import org.xmpp.packet.JID;

import org.apache.log4j.Logger;

public class CallHistory extends HttpServlet {

	private UserManager userManager;
	protected Logger Log = Logger.getLogger(getClass().getName());

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
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

		try {
			CallFilter filter = null;

			out.println("<html>");
			out.println("<head>");
			out.println("<title>VoiceBridge Call History</title>");
			out.println("<meta name=\"pageID\" content=\"VoiceBridge-CALL-HISTORY\"/>");

			out.println("<script src='/js/prototype.js' type='text/javascript'></script>");
			out.println("<script src='/js/scriptaculous.js' type='text/javascript'></script>");
			out.println("<script type='text/javascript' language='javascript' src='/js/tooltips/domLib.js'></script>");
			out.println("<script type='text/javascript' language='javascript' src='/js/tooltips/domTT.js'></script>");
			out.println("<script type='text/javascript' src='/js/jscalendar/calendar.js'></script>");
			out.println("<script type='text/javascript' src='/js/jscalendar/i18n.jsp'></script>");
			out.println("<script type='text/javascript' src='/js/jscalendar/calendar-setup.js'></script>");
			out.println("<script type='text/javascript' src='/js/behaviour.js'></script>");
			out.println("<script type='text/javascript' src='js/callhistory.js'></script>");
			out.println("<link rel='stylesheet' type='text/css' href='/js/jscalendar/calendar-win2k-cold-1.css' />");
			out.println("<link rel='stylesheet' type='text/css' href='style/callhistory.css' />");

			out.println("</head>");

			out.println("<body>");


			String msg 	= request.getParameter("msg");

			if (msg != null)
			{
				out.println("<div class='jive-success'>");
				out.println("<table cellpadding='0' cellspacing='0' border='0'>");
				out.println("<tbody>");
				out.println("<tr><td class='jive-icon'><img src='images/success-16x16.gif' width='16' height='16' border='0' alt=''></td>");
				out.println("<td class='jive-icon-label'>");
				out.println(msg);
				out.println("</td></tr>");
				out.println("</tbody>");
				out.println("</table>");
				out.println("</div>");
			}


			String userName		= request.getParameter("username") != null ? request.getParameter("username") : "";
			String caller		= request.getParameter("caller") != null ? request.getParameter("caller") : "";
			String called		= request.getParameter("called") != null ? request.getParameter("called") : "";
			String callType		= request.getParameter("callType") != null ? request.getParameter("callType") : "";
			String startDate	= request.getParameter("startDate") != null ? request.getParameter("startDate") : "";
			String endDate		= request.getParameter("endDate") != null ? request.getParameter("endDate") : "";

			String action = request.getParameter("action");

			if (action == null)
			{
				Calendar calendar = Calendar.getInstance();
				int day = calendar.get(Calendar.DATE);
				int month = calendar.get(Calendar.MONTH) + 1;
				int year = calendar.get(Calendar.YEAR);

				startDate = month + "/" + day + "/" + year;
			}

			Date fromDate = null;

			if (startDate != null && startDate.length() > 0) {
				DateFormat formatter = new SimpleDateFormat("MM/dd/yy");
				try {
					fromDate = formatter.parse(startDate);
				}
				catch (Exception e) {
					Log.error("CallHistory " + e);
					e.printStackTrace();
				}

			}

			Date uptoDate = null;

			if (endDate != null && endDate.length() > 0) {
				DateFormat formatter = new SimpleDateFormat("MM/dd/yy");
				try {
					Date date = formatter.parse(endDate);
					// The user has chosen an end date and expects that any conversation
					// that falls on that day will be included in the search results. For
					// example, say the user choose 6/17/2006 as an end date. If a conversation
					// occurs at 5:33 PM that day, it should be included in the results. In
					// order to make this possible, we need to make the end date one millisecond
					// before the next day starts.
					uptoDate = new Date(date.getTime() + JiveConstants.DAY - 1);
				}
				catch (Exception e) {
					Log.error("CallHistory " + e);
					e.printStackTrace();
				}
			}

			out.println("<form name='jid' action='voicebridge-call-history' method='get'>");
			out.println("    <div>");
			out.println("    <table class='stat'>");
			out.println("        <tr valign='top'>");
			out.println("            <td>");
			out.println("            <table cellpadding='3' cellspacing='0' border='0' width='100%'>");
			out.println("                <tbody>");
			out.println("                    <tr>");
			out.println("                        <td align='left' width='150'>User name:&nbsp");
			out.println("                        </td>");
			out.println("                        <td align='left'>");
			out.println("                            <input type='text' size='20' maxlength='100' name='username' value='" + (userName != null ? userName : "") + "'>");
			out.println("                        </td>");
			out.println("                    </tr>");
			out.println("                    <tr>");
			out.println("                        <td align='left' width='150'>Caller:&nbsp");
			out.println("                        </td>");
			out.println("                        <td align='left'>");
			out.println("                            <input type='text' size='20' maxlength='100' name='caller' value='" + (caller != null ? caller : "") + "'>");
			out.println("                        </td>");
			out.println("                    </tr>");
			out.println("                    <tr>");
			out.println("                        <td align='left' width='150'>Called:&nbsp");
			out.println("                        </td>");
			out.println("                        <td align='left'>");
			out.println("                            <input type='text' size='20' maxlength='100' name='called' value='" + (called != null ? called : "") + "'>");
			out.println("                        </td>");
			out.println("                    </tr>");
			out.println("                    <tr>");
			out.println("                        <td align='left' width='150'>Type:&nbsp");
			out.println("                        </td>");
			out.println("                        <td align='left'>");
			out.println("                            <select name='callType' size='1'>");
			out.println("                                <option value='all' "  	+ (("all".equals(callType)   || callType == null) ? "selected" : "") + ">All</option>");
			out.println("                                <option value='in' " 		+ ("in".equals(callType) 	 ? "selected" : "") + ">Incoming</option>");
			out.println("                                <option value='out' "		+ ("out".equals(callType) 	 ? "selected" : "") + ">Outgoing</option>");
			out.println("                                <option value='missed' "	+ ("missed".equals(callType) ? "selected" : "") + ">Missed</option>");
			out.println("                            </select>");
			out.println("                        </td>");
			out.println("                    </tr>");
			out.println("                </tbody>");
			out.println("            </table>");
			out.println("            </td>");
			out.println("            <td width='0' height='100%' valign='middle'>");
			out.println("                <div class='verticalrule'></div>");
			out.println("            </td>");
			out.println("            <td>");
			out.println("                <table>");
			out.println("                    <tr>");
			out.println("                        <td colspan='3'>");
			out.println("                            <b>Date Range:</b>");
			out.println("                            <a title='Enter specific date ranges to search between. You can specify a start date and/or end date.'><img src='images/icon_help_14x14.gif' vspace='2' align='texttop'/></a>");
			out.println("                        </td>");
			out.println("                    </tr>");
			out.println("                    <tr valign='top'>");
			out.println("                        <td>Start:</td>");
			out.println("                        <td>");
			out.println("                            <input type='text' id='startDate' name='startDate' size='13'");
			out.println("                                   value='" + (startDate != null ? startDate : "") + "' class='textfield'/><br/>");
			out.println("                            <span class='jive-description'>Use mm/dd/yy</span>");
			out.println("                        </td>");
			out.println("                        <td>");
			out.println("                            <img src='images/icon_calendarpicker.gif' vspace='3' id='startDateTrigger'>");
			out.println("                        </td>");
			out.println("                    </tr>");
			out.println("                    <tr valign='top'>");
			out.println("                        <td>End:</td>");
			out.println("                        <td>");
			out.println("                            <input type='text' id='endDate' name='endDate' size='13'");
			out.println("                                    value='" + (endDate != null ? endDate : "") + "' class='textfield'/><br/>");
			out.println("                             <span class='jive-description'>Use mm/dd/yy</span>");
			out.println("                        </td>");
			out.println("                        <td>");
			out.println("                            <img src='images/icon_calendarpicker.gif' vspace='3' id='endDateTrigger'>");
			out.println("                        </td>");
			out.println("                    </tr>");
			out.println("                </table>");
			out.println("            </td>");
			out.println("        </tr>");
			out.println("    </table>");
			out.println("    </div>");
			out.println("    <div align='left'><input type='submit' name='action' value='Query'></div>");
			out.println("</form>");
			out.println("");
			out.println("<br>");

			filter = CallLogDAO.createSQLFilter(userName, caller, called, callType, fromDate, uptoDate, "voicebridge");

			if (filter != null)
			{
				int linesCount = 100;
				int numberOfCalls = CallLogDAO.getLogCount(filter);
				int pageCounter = (numberOfCalls/linesCount);

				pageCounter = numberOfCalls > (linesCount * pageCounter) ? pageCounter + 1 : pageCounter;

				String start = request.getParameter("start");
				String count = request.getParameter("count");

				int pageStart = start == null ? 0 : Integer.parseInt(start);
				int pageCount = count == null ? linesCount : Integer.parseInt(count);

				out.println("<table width='100%' cellpadding=\"1\" cellspacing=\"1\" border=\"0\"><tr><td>Total: " + numberOfCalls + " Pages:[");

				for (int i=0; i<pageCounter; i++)
				{
					int iStart = (i * linesCount);
					int iCount = ((i * linesCount) + linesCount) > numberOfCalls ? ((i * linesCount) + linesCount) - numberOfCalls : linesCount;
					int page = i + 1;

					if (pageStart == iStart)
					{
						out.println("<span nowrap>&nbsp;" + page + "&nbsp;</span>");

					} else {

						out.println("<span nowrap><a href='voicebridge-call-history?action=Query&username=" + userName + "&caller=" + caller + "&called=" + called + "&callType=" + callType + "&startDate=" + startDate + "&endDate=" + endDate + "&start=" + iStart + "&count=" + iCount + "'>&nbsp;" + page + "&nbsp;</a></span>");
					}
				}

				out.println("]</td></tr></table>");
				out.println("<div class=\"jive-table\">");
				out.println("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">");
				out.println("<thead>");
				out.println("<tr>");
				out.println("<th nowrap>User Name</th>");
				out.println("<th nowrap>Full Name</th>");
				out.println("<th nowrap>Direction</th>");
				out.println("<th nowrap>Missed</th>");
				out.println("<th nowrap>Active</th>");
				out.println("<th nowrap>Caller Number</th>");
				out.println("<th nowrap>Caller Name</th>");
				out.println("<th nowrap>Called Number</th>");
				out.println("<th nowrap>Called Name</th>");
				out.println("<th nowrap>Time</th>");
				out.println("<th nowrap>Duration (secs)</th>");
				out.println("</tr>");
				out.println("</thead>");
				out.println("<tbody>");

				Collection<CallLog> calls = CallLogDAO.getCalls(filter, pageStart, pageCount);

				Iterator it = calls.iterator();
				int i = 0;

				while( it.hasNext() )
				{
					try
					{
						CallLog callLog = (CallLog)it.next();

						try {
							JID jid = new JID(callLog.getParticipantLog().getJid());
							String userId = jid.getNode();

							if (userManager.isRegisteredUser(userId))
							{
								User user = userManager.getUser(userId);

								if(i % 2 == 1)
									out.println("<tr class=\"jive-odd\">");
								else
									out.println("<tr class=\"jive-even\">");

								out.println("<td width=\"5%\">");
								out.println(user.getUsername());
								out.println("</td>");

								out.println("<td width=\"15%\">");
								out.println(user.getName());
								out.println("</td>");

								out.println("<td width=\"5%\">");
								out.println("Incoming".equals(callLog.getDirection()) ? "<img src=\"images/incoming.jpg\" alt=\"Incoming\" border=\"0\">In" : "<img src=\"images/outgoing.jpg\" alt=\"Outgoing\" border=\"0\">Out");
								out.println("</td>");

								out.println("<td width=\"5%\">");
								out.println("CallMissed".equals(callLog.getState()) ? "<img src=\"images/success-16x16.gif\" alt=\"Missed\" border=\"0\">" : "&nbsp;");
								out.println("</td>");

								out.println("<td width=\"5%\">");
								out.println("Active".equals(callLog.getParticipantLog().getType()) ? "<img src=\"images/success-16x16.gif\" alt=\"Active\" border=\"0\">" : "&nbsp;");
								out.println("</td>");

								out.println("<td width=\"10%\">");
								out.println(callLog.getCallerNumber());
								out.println("</td>");

								out.println("<td width=\"10%\">");
								out.println(callLog.getCallerName());
								out.println("</td>");

								out.println("<td width=\"10%\">");
								out.println(callLog.getCalledNumber());
								out.println("</td>");

								out.println("<td width=\"10%\">");
								out.println(callLog.getCalledName());
								out.println("</td>");

								out.println("<td width=\"15%\">");
								out.println(String.valueOf(callLog.getParticipantLog().getStartTimestamp()));
								out.println("</td>");

								out.println("<td width=\"5%\">");
								out.println(String.valueOf(callLog.getParticipantLog().getDuration()));
								out.println("</td>");

								out.println("</tr>");

								i++;
							}

						} catch (Exception e) {
							Log.error("CallHistory " + e);
							e.printStackTrace();

						}
					}
					catch(Exception e)
					{
						Log.error("CallHistory " + e);
						e.printStackTrace();
					}
				}

				out.println("</tbody>");
				out.println("</table>");
				out.println("</div>");
			}

			out.println("<script>loaded();</script>");
			out.println("</body>");
			out.println("</html>");

        }
        catch (Exception e) {
        	Log.error("CallHistory " + e);
        	e.printStackTrace();
        }
	}

}

