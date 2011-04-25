
package com.ifsoft.iftalk.plugin.voicebridge.commands;


import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.w3c.dom.NodeList;

import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeComponent;
import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeConstants;
import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeUser;
import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeInterest;
import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeUserInterest;
import com.ifsoft.iftalk.plugin.voicebridge.VoiceBridgeCall;

import org.jivesoftware.openfire.admin.AdminManager;

public class GetInterest extends OpenlinkCommand {

	public GetInterest(VoiceBridgeComponent voicebridgeComponent) {
		super(voicebridgeComponent);
	}

	/**
	 * Returns the max number of stages for this command. The number of stages
	 * may vary according to the collected data in previous stages. Therefore, a
	 * SessionData object is passed as a parameter. When the max number of
	 * stages has been reached then the command is ready to be executed.
	 *
	 * @param data
	 *            the gathered data through the command stages or <tt>null</tt>
	 *            if the command does not have stages or the requester is
	 *            requesting the execution for the first time.
	 * @return the max number of stages for this command.
	 */

	public int getMaxStages(SessionData data) {
		return 0;
	}

	/**
	 * Adds to the command element the data form or notes required by the
	 * current stage. The current stage is specified in the SessionData. This
	 * method will never be invoked for commands that have no stages.
	 *
	 * @param data
	 *            the gathered data through the command stages or <tt>null</tt>
	 *            if the command does not have stages or the requester is
	 *            requesting the execution for the first time.
	 * @param command
	 *            the command element to be sent to the command requester.
	 */

	protected boolean addStageInformation(SessionData data, Element newCommand,
			Element oldCommand) {
		return true;
	}

	/**
	 * Executes the command with the specified session data.
	 *
	 * @param data
	 *            the gathered data through the command stages or <tt>null</tt>
	 *            if the command does not have stages.
	 * @param command
	 *            the command element to be sent to the command requester with a
	 *            reported data result or note element with the answer of the
	 *            execution.
	 * @return
	 */

	public Element execute(SessionData data, Element newCommand, Element oldCommand)
	{
		try {
			String errorMessage = null;
			Element in = oldCommand.element("iodata").element("in");
			Element iodata = newCommand.addElement("iodata", "urn:xmpp:tmp:io-data");

			String interestID = in.element("interest").getText();

			if (!"".equals(interestID))
			{
				VoiceBridgeUserInterest voicebridgeUserInterest = this.getVoiceBridgeComponent().getOpenlinkInterest(interestID);
				VoiceBridgeInterest voicebridgeInterest = voicebridgeUserInterest.getInterest();
				VoiceBridgeUser voicebridgeUser = voicebridgeUserInterest.getUser();

				if (voicebridgeUserInterest != null)
				{
					if (!validPermissions(data, voicebridgeUserInterest.getUser().getUserId(), newCommand))
					{
						return newCommand;
					}

					String interestType = "L".equals(voicebridgeInterest.getInterestType()) ?  "DirectLine"  : "DirectoryNumber";

					Element interests = iodata.addElement("out").addElement("interests", "http://xmpp.org/protocol/openlink:01:00:00/interests");
					Element interest = interests.addElement("interest");

					interest.addAttribute("id", interestID);
					interest.addAttribute("label", voicebridgeInterest.getInterestLabel());
					interest.addAttribute("type", interestType);

					interest.addAttribute("default", voicebridgeUserInterest.getDefault());

					if ("true".equals(voicebridgeUserInterest.getCallFWD()))
					{
						interest.addAttribute("fwd", voicebridgeUserInterest.getCallFWDDigits());
					}

					Iterator it2 = voicebridgeUserInterest.getCalls().values().iterator();

					if (voicebridgeUserInterest.getCalls().size() > 0)
					{
						Element calls = interest.addElement("callstatus", "http://xmpp.org/protocol/openlink:01:00:00#call-status");

						boolean busy = voicebridgeUserInterest.getBusyStatus();
						calls.addAttribute("busy", busy ? "true" : "false");

						while( it2.hasNext() )
						{
							VoiceBridgeCall voicebridgeCall = (VoiceBridgeCall)it2.next();

							Element call = calls.addElement("call");

							this.getVoiceBridgeComponent().voicebridgeLinkService.restoreCallState(voicebridgeCall.line, voicebridgeCall.callid); // we wait for VoiceBridge responses
							Thread.sleep(1000);

							this.getVoiceBridgeComponent().voicebridgeLinkService.addVoiceBridgeCallEvents(voicebridgeInterest, voicebridgeUserInterest, call, voicebridgeCall);
						}
					}

				} else errorMessage = "Interest not found";


			} else errorMessage = "Interest not specified";


		} catch (Exception e) {
			Log.error("[Openlink] GetInterest execute error " + e);

			Element note = newCommand.addElement("note");
			note.addAttribute("type", "error");
			note.setText("Get Interest Internal error");
		}
		return newCommand;
	}

	/**
	 * Returns a collection with the allowed actions based on the current stage
	 * as defined in the SessionData. Possible actions are: <tt>prev</tt>,
	 * <tt>next</tt> and <tt>complete</tt>. This method will never be
	 * invoked for commands that have no stages.
	 *
	 * @param data
	 *            the gathered data through the command stages or <tt>null</tt>
	 *            if the command does not have stages or the requester is
	 *            requesting the execution for the first time.
	 * @return a collection with the allowed actions based on the current stage
	 *         as defined in the SessionData.
	 */

	protected List getActions(SessionData data) {
		return Arrays.asList(new Action[] { Action.complete });
	}

	/**
	 * Returns the unique identifier for this command for the containing JID.
	 * The code will be used as the node in the disco#items or the node when
	 * executing the command.
	 *
	 * @return the unique identifier for this command for the containing JID.
	 */

	public String getCode() {
		return "http://xmpp.org/protocol/openlink:01:00:00#get-interest";
	}

	/**
	 * Returns the default label used for describing this commmand. This
	 * information is usually used when returning commands as disco#items.
	 * Admins can later use {@link #setLabel(String)} to set a new label and
	 * reset to the default value at any time.
	 *
	 * @return the default label used for describing this commmand.
	 */

	public String getDefaultLabel() {
		return "Get Interests";
	}

	/**
	 * Returns which of the actions available for the current stage is
	 * considered the equivalent to "execute". When the requester sends his
	 * reply, if no action was defined in the command then the action will be
	 * assumed "execute" thus assuming the action returned by this method. This
	 * method will never be invoked for commands that have no stages.
	 *
	 * @param data
	 *            the gathered data through the command stages or <tt>null</tt>
	 *            if the command does not have stages or the requester is
	 *            requesting the execution for the first time.
	 * @return which of the actions available for the current stage is
	 *         considered the equivalent to "execute".
	 */

	protected Action getExecuteAction(SessionData data) {
		return Action.complete;
	}
}
