
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


public class GetProfile extends OpenlinkCommand {

	public GetProfile(VoiceBridgeComponent voicebridgeComponent) {
		super(voicebridgeComponent);
	}

	@Override
	protected boolean addStageInformation(SessionData data, Element newCommand,	Element oldCommand)
	{
		return false;
	}

	@Override
	public Element execute(SessionData data, Element newCommand, Element oldCommand)
	{
		try {
			String profileID = oldCommand.element("iodata").element("in").element("profile").getText();

			Element iodata = newCommand.addElement("iodata", "urn:xmpp:tmp:io-data");
			iodata.addAttribute("type","output");

			Element profile = iodata.addElement("out").addElement("profile");
			Element keypages = profile.addElement("keypages");

			VoiceBridgeUser voicebridgeUser = this.getVoiceBridgeComponent().getOpenlinkProfile(profileID);

			if (!validPermissions(data, voicebridgeUser.getUserId(), newCommand))
			{
				return newCommand;
			}

			profile.addAttribute("online", "0000000000".equals(voicebridgeUser.getDeviceNo()) ? "false" : "true");
/*
			Iterator it = voicebridgeUser.getKeypages().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeKeypage voicebridgeKeypage = (VoiceBridgeKeypage)it.next();
				Element keypage = keypages.addElement("keypage");
				keypage.addElement("localkeypagenumber").setText(voicebridgeKeypage.getLocalKeypageNumber());
				keypage.addElement("globalkeypagenumber").setText(voicebridgeKeypage.getLocalKeypageNumber());
				keypage.addElement("keyregiontypeid").setText(voicebridgeKeypage.getKeyRegionTypeID());
				keypage.addElement("keyregioninstanceid").setText(voicebridgeKeypage.getKeyRegionInstanceID());
				keypage.addElement("keypagename").setText(voicebridgeKeypage.getName());

				Element keys = keypage.addElement("keys");

				Iterator it2 = voicebridgeKeypage.getKeys().iterator();

				while( it2.hasNext() )
				{
					VoiceBridgeKey voicebridgeKey = (VoiceBridgeKey)it2.next();
					Element key = keypage.addElement("key");
					key.addElement("number").setText(voicebridgeKey.getKeyNumber());
					key.addElement("name").setText(voicebridgeKey.getName());
					key.addElement("label1").setText(voicebridgeKey.getLabel1());
					key.addElement("label2").setText(voicebridgeKey.getLabel2());
					key.addElement("modifier").setText(voicebridgeKey.getModifier());
					key.addElement("function").setText(voicebridgeKey.getTurretFunction());
					key.addElement("qualifier").setText(voicebridgeKey.getQualifier());
					key.addElement("typeid").setText(voicebridgeKey.getKeyTypeID());
					key.addElement("longlabel").setText(voicebridgeKey.getLongLabel());
					key.addElement("color").setText(voicebridgeKey.getColourIndex());
					key.addElement("interest").setText(voicebridgeKey.getInterest());
				}
			}
*/

		} catch (Exception e) {
			Log.error("[Openlink] GetProfile execute error "	+ e.getMessage());
		}
		return newCommand;
	}

	@Override
	protected List<Action> getActions(SessionData data) {
		return Arrays.asList(new Action[] { Action.complete });
	}

	@Override
	public String getCode() {
		return "http://xmpp.org/protocol/openlink:01:00:00#query-features";
	}

	@Override
	public String getDefaultLabel() {
		return "Query Features";
	}

	@Override
	protected Action getExecuteAction(SessionData data) {
		return Action.complete;
	}

	@Override
	public int getMaxStages(SessionData data) {
		return 0;
	}

}
