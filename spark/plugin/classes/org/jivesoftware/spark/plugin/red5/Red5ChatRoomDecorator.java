/**
 * $RCSfile: ,v $
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2004-2010 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 package org.jivesoftware.spark.plugin.red5;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Collection;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.component.RolloverButton;
import org.jivesoftware.spark.ui.ChatRoom;
import org.jivesoftware.spark.ui.rooms.ChatRoomImpl;
import org.jivesoftware.spark.ui.rooms.GroupChatRoom;
import org.jivesoftware.spark.ui.ChatRoomClosingListener;
import org.jivesoftware.spark.util.GraphicUtils;
import org.jivesoftware.spark.util.log.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.filter.*;

import org.redfire.screen.*;

public class Red5ChatRoomDecorator  implements ChatRoomClosingListener
{
	private RolloverButton red5Button;
	private RolloverButton screenButton;
	private final ChatRoom room;
	private final String url;
	public static ScreenShare screenShare = null;

	public Red5ChatRoomDecorator(final ChatRoom room, final String url)
	{
		this.room = room;
		this.url = url;

		//Red5Preference preference = (Red5Preference) SparkManager.getPreferenceManager().getPreference(Red5Preference.NAMESPACE);

		//if (preference.getPreferences().isRed5Enabled()) {

	        ClassLoader cl = getClass().getClassLoader();
			ImageIcon red5Icon = new ImageIcon(cl.getResource("images/logo_small.gif"));
	        red5Button = new RolloverButton(red5Icon);
	        red5Button.setToolTipText(GraphicUtils.createToolTip("Red5 Audio/Video"));

			red5Button.addActionListener( new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					//room.getChatInputEditor().requestFocusInWindow();

					String sessionID = SparkManager.getConnection().getConnectionID();

					String jid = SparkManager.getSessionManager().getJID();
					String me = getNode(jid);
					String roomJID = room.getRoomname();
					String nickName = SparkManager.getUserManager().getNickname();

					String newUrl;

					if ("groupchat".equals(room.getChatType().toString()))
					{
						String others = "";
						Collection<String> participants = ((GroupChatRoom)room).getParticipants();

						for (String participant : participants)
						{
							Log.warning("Red5ChatRoomDecorator: found participant " + participant);
							others = others + "$" + java.net.URLEncoder.encode(getResource(participant));
						}

						newUrl = url + "/redfire/video/videoConf.html?key=" + sessionID + "&others=" + others + "&me=" + java.net.URLEncoder.encode(nickName);

						for (String participant : participants)
						{
							String member = getResource(participant);

							if (member.equals(nickName) == false)
							{
								String youURL = url + "/redfire/video/videoConf.html?key=" + sessionID + "&others=" + others + "&me=" + java.net.URLEncoder.encode(member);
								Message message = new Message();
								message.setType(Message.Type.chat);

								sendInvite(message, roomJID + "/" + getResource(participant), youURL);
							}
						}


					} else {
						String youJID = ((ChatRoomImpl)room).getParticipantJID();
						String you = getNode(youJID);

						newUrl = url + "/redfire/video/video320x240.html?me=" + me + "&you=" + you + "&key=" + sessionID;
						String youURL = url + "/redfire/video/video320x240.html?you=" + me + "&me=" + you + "&key=" + sessionID;

						Message message = new Message();
						message.setType(Message.Type.chat);
						sendInvite(message, youJID, youURL);

						Log.warning("Red5ChatRoomDecorator: red5Button " + youURL);

					}

					BareBonesBrowserLaunch.openURL(newUrl);
				}
			});

			screenShare = ScreenShare.getInstance();

	        ImageIcon screenIcon = new ImageIcon(cl.getResource("images/screen_share.gif"));
	        screenButton = new RolloverButton(screenIcon);
	        screenButton.setToolTipText(GraphicUtils.createToolTip("Red5 Screen Share"));

			screenButton.addActionListener( new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					Log.warning("host: " + screenShare.host + ", app: " + screenShare.app + ", port: " + screenShare.port + ", publish: " + screenShare.publishName);

					screenShare.createWindow();

					String screenSessionID = SparkManager.getConnection().getConnectionID();
					String playStream = "screen_share_" + screenSessionID;
					String newUrl = url + "/redfire/screen/screenviewer.html?stream=" + playStream;
					String roomJID = room.getRoomname();

					Message message = new Message();

					if ("groupchat".equals(room.getChatType().toString()))
						message.setType(Message.Type.groupchat);
					else
						message.setType(Message.Type.chat);

					sendInvite(message, roomJID, newUrl);

					Log.warning("Red5ChatRoomDecorator: screenButton " + newUrl);
				}
			});

	        room.getEditorBar().add(red5Button);
	        room.getEditorBar().add(screenButton);
		//}
	}


	public void closing()
	{

	}


	private String getNode(String jid)
	{
		String node = jid;
		int pos = node.indexOf("@");

		if (pos > -1)
			node = jid.substring(0, pos);

		return node;
	}

	private String getResource(String jid)
	{
		String node = jid;
		int pos = node.indexOf("/");

		if (pos > -1)
			node = jid.substring(pos + 1);

		return node;
	}

	private String getDomain(String jid)
	{
		String node = jid;
		int pos = node.indexOf("@");

		if (pos > -1)
		{
			node = node.substring(pos + 1);

			pos = node.indexOf("/");

			if (pos > -1)
				node = jid.substring(0, pos);
		}

		return node;
	}


	private void sendInvite(Message message, String jid, String url)
	{
		message.setTo(jid);
		message.setBody(url);

		RedfireExtension redfireExtension = new RedfireExtension("redfire-invite", "http://redfire.4ng.net/xmlns/redfire-invite");
		redfireExtension.setValue("sessionID", SparkManager.getConnection().getConnectionID());
		redfireExtension.setValue("roomID", room.getRoomTitle());
		message.addExtension(redfireExtension);

		SparkManager.getConnection().sendPacket(message);
	}
}
