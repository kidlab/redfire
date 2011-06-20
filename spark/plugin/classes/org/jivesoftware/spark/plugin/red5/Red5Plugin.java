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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;

import org.jivesoftware.spark.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.spark.component.*;
import org.jivesoftware.spark.component.browser.*;
import org.jivesoftware.spark.plugin.*;
import org.jivesoftware.spark.ui.rooms.*;
import org.jivesoftware.spark.ui.*;
import org.jivesoftware.spark.util.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.spark.util.log.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.packet.*;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.provider.ProviderManager;

import org.redfire.screen.*;



public class Red5Plugin implements Plugin, ChatRoomListener, PacketListener, PacketFilter, TranscriptWindowInterceptor
{
	private org.jivesoftware.spark.ChatManager chatManager;
	private ImageIcon red5Icon;
	private UserManager userManager;

	private String protocol = "http://";
	private String red5server = "localhost";
	private String red5port = "7070";
	private String url = protocol + red5server + ":" + red5port;

	private static File pluginsettings = new File(System.getProperty("user.home") + System.getProperty("file.separator") + "Spark" + System.getProperty("file.separator") + "red5.properties"); //new
	private Map<String, Red5ChatRoomDecorator> decorators = new HashMap<String, Red5ChatRoomDecorator>();
	private Map<String, JPanel> windows = new HashMap<String, JPanel>();

    public Red5Plugin() {
		ClassLoader cl = getClass().getClassLoader();
		red5Icon = new ImageIcon(cl.getResource("images/logo_small.gif"));
    }

    public void initialize()
    {
		chatManager = SparkManager.getChatManager();
       	chatManager.addTranscriptWindowInterceptor(this);

		userManager = SparkManager.getUserManager();

		red5server = SparkManager.getConnection().getServiceName();
		url = protocol + red5server + ":" + red5port;

    	Properties props = new Properties();

		if (pluginsettings.exists())
		{
			Log.warning("Red5-Info: Properties-file does exist= " + pluginsettings.getPath());

			try {
				props.load(new FileInputStream(pluginsettings));

				if (props.getProperty("server") != null)
				{
					red5server = props.getProperty("server");
					Log.warning("Red-Info: Red5-servername from properties-file is= " + red5server);
				}

				if (props.getProperty("port") != null)
				{
					red5port = props.getProperty("port");
					Log.warning("Red5-Info: Red5-port from properties-file is= " + red5port);
				}

				if (props.getProperty("protocol") != null)
				{
					protocol = props.getProperty("protocol");
					Log.warning("Red5-Info: Red5-protocol from properties-file is= " + protocol);
				}

				url = protocol + red5server + ":" + red5port;

			} catch (IOException ioe) {

				System.err.println(ioe);
				//TODO handle error better.
			}

		} else {

		  	Log.warning("Red5-Error: Properties-file does not exist= " + pluginsettings.getPath() + ", using default " + url);
		}

		ScreenShare.getInstance().host = red5server;
		ScreenShare.getInstance().app = "xmpp";
		ScreenShare.getInstance().port = 1935;
		ScreenShare.getInstance().codec = "flashsv2";
		ScreenShare.getInstance().frameRate = 15;
		ScreenShare.getInstance().publishName = "screen_share_" + SparkManager.getConnection().getConnectionID();

		chatManager.addChatRoomListener(this);

		SparkManager.getConnection().addPacketListener(this, this);

    }


    public void shutdown()
    {
		chatManager.removeChatRoomListener(this);
		SparkManager.getConnection().removePacketListener(this);
    }

    public boolean canShutDown()
    {
        return true;
    }

    public void uninstall()
    {

    }


    public void chatRoomLeft(ChatRoom chatroom)
    {
    }

    public void chatRoomClosed(ChatRoom chatroom)
    {
		String roomId = chatroom.getRoomname();

		Log.warning("chatRoomClosed:  " + roomId);

		if (decorators.containsKey(roomId))
		{
			Red5ChatRoomDecorator decorator = decorators.remove(roomId);
			decorator.finished();
			decorator = null;

			String window1 = roomId + "_screen";
			String window2 = roomId + "_video";

			BareBonesBrowserLaunch.closeURL(window1);
			BareBonesBrowserLaunch.closeURL(window2);

			if (windows.containsKey(window1))
			{
				JPanel panel = windows.remove(window1);
				panel = null;
			}

			if (windows.containsKey(window2))
			{
				JPanel panel = windows.remove(window2);
				panel = null;
			}
		}
    }

    public void chatRoomActivated(ChatRoom chatroom)
    {
    }

    public void userHasJoined(ChatRoom room, String s)
    {

    }

    public void userHasLeft(ChatRoom room, String s)
    {

    }

    public void chatRoomOpened(final ChatRoom room)
    {
		decorators.put(room.getRoomname(), new Red5ChatRoomDecorator(room, url, red5server));
    }

	public boolean accept(Packet packet) {

			return true;
	}

	public void processPacket(Packet packet) {

		try {

			PacketExtension redfireExtension = null;

			if (packet instanceof Message)
			{
				Message message = (Message) packet;

				redfireExtension = message.getExtension("redfire-invite", "http://redfire.4ng.net/xmlns/redfire-invite");

				if (redfireExtension != null && "error".equals(message.getType().toString()) == false)
				{
					String xml = redfireExtension.toXML();

					String nickname = getTag(xml, "nickname");
					String url = message.getBody();
					String prompt = getTag(xml, "prompt");
					String windowType = getTag(xml, "windowType");
					String roomType = getTag(xml, "roomType");

					int width = Integer.parseInt(getTag(xml, "width"));
					int height = Integer.parseInt(getTag(xml, "height"));

					String roomId = message.getFrom();

					//Log.warning("RedfireExtension  invite recieved " + roomId);

					ChatRoom chatRoom = getRoom(roomId);

					if (chatRoom != null)
						windows.put(roomId + windowType, showInvitationAlert(width, height, roomId, nickname, prompt, url, windowType));

				}
			}
		}
		catch (Exception e) {

			Log.error("Error process packet:", e);
		}

	}

	private ChatRoom getRoom(String roomId)
	{
		ChatRoom chatRoom = null;

		try {
			chatRoom = chatManager.getChatContainer().getChatRoom(roomId);

		}
		catch (Exception e) {

			try {
				chatRoom = chatManager.getChatContainer().getChatRoom(getBare(roomId));
			}
			catch (Exception e1) {

				try {

					Thread.sleep(2500);
				}
				catch (Exception e2) { }

				chatRoom = chatManager.getChatRoom(getBare(roomId));
			}
		}

		return chatRoom;
	}


	private String getTag(String xml, String tag) {
		String tagValue = null;

		int pos = xml.indexOf("<" + tag + ">");

		if (pos > -1) {
			String temp = xml.substring(pos + tag.length() + 2);
			pos = temp.indexOf("</" + tag + ">");

			if (pos > -1)
			{
				tagValue = temp.substring(0, pos);
			}
		}

		return (tagValue);
	}

    private JPanel showInvitationAlert(final int width, final int height, final String roomID, final String nickname, final String prompt, final String url, final String windowType)
    {
		final String roomId = roomID;
        final String buttonType = windowType;
        final JPanel inviteAlert = new JPanel();

        ChatRoom chatroom = getRoom(roomId);
        setButton(chatroom, buttonType, false);
        inviteAlert.setLayout(new BorderLayout());

        JPanel invitePanel = new JPanel()
        {
			private static final long serialVersionUID = 5942001917654498678L;

			protected void paintComponent(Graphics g)
			{
                ImageIcon imageIcon = new ImageIcon(getClass().getClassLoader().getResource("images/logo_small.gif"));
                Image image = imageIcon.getImage();
                g.drawImage(image, 0, 0, null);
            }
        };

        invitePanel.setPreferredSize(new Dimension(24,24));
        inviteAlert.add(invitePanel, BorderLayout.WEST);
        JPanel content = new JPanel(new BorderLayout());

        content.add(new JLabel(nickname + prompt), BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();

        JButton acceptButton = new JButton("Accept");

        acceptButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
				ChatRoom chatroom = getRoom(roomId);
				BareBonesBrowserLaunch.openURL(width, height, url, chatroom.getRoomname() + buttonType);
        		setButton(chatroom, buttonType, true);
				inviteAlert.setVisible(false);
				chatroom.getTranscriptWindow().remove(inviteAlert);
            }
        });
        buttonPanel.add(acceptButton);

        JButton declineButton = new JButton("Decline");

        declineButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
				ChatRoom chatroom = getRoom(roomId);
        		setButton(chatroom, buttonType, true);
				inviteAlert.setVisible(false);
 				chatroom.getTranscriptWindow().remove(inviteAlert);
            }
        });
        buttonPanel.add(declineButton);

        content.add(buttonPanel, BorderLayout.SOUTH);
        inviteAlert.add(content, BorderLayout.CENTER);

		try {
			Thread.sleep(1000);
		} catch (Exception e) {}

        chatroom.getTranscriptWindow().addComponent(inviteAlert);

        return inviteAlert;
    }

	private void setButton(ChatRoom chatroom, String windowType, boolean flag)
	{
		String roomId = chatroom.getRoomname();

		if (decorators.containsKey(roomId))
		{
			Red5ChatRoomDecorator decorator = decorators.get(roomId);

			if ("_screen".equals(windowType))
				decorator.screenButton.setEnabled(flag);
			else
				decorator.red5Button.setEnabled(flag);
		}
	}


    public boolean isMessageIntercepted(TranscriptWindow window, String userid, Message message)
    {
		String xml = message.toXML();

		if (xml.indexOf("http://redfire.4ng.net/xmlns/redfire-invite") > -1)
		{
			if (xml.indexOf("<roomType>groupchat</roomType>") > -1)
			{
				try {
					ChatRoom chatRoom = chatManager.getChatContainer().getActiveChatRoom();
					chatManager.getChatContainer().closeTab(chatRoom);

				} catch (Exception e) {}
			}

			return true;
		}
        return false;
    }

	private String getBare(String jid)
	{
		String node = jid;
		int pos = node.indexOf("/");

		if (pos > -1)
			node = jid.substring(0, pos);

		return node;
	}
}
