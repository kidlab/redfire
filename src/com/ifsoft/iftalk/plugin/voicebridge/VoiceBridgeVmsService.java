
package com.ifsoft.iftalk.plugin.voicebridge;

import java.util.*;
import org.dom4j.*;

import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

import org.jivesoftware.util.Log;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.database.SequenceManager;
import org.jivesoftware.database.JiveID;
import org.jivesoftware.openfire.group.Group;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.event.GroupEventDispatcher;
import org.jivesoftware.openfire.event.GroupEventListener;

import com.ifsoft.iftalk.plugin.tsc.*;
import com.ifsoft.iftalk.plugin.tsc.voicemessage.message.VMessage;
import com.ifsoft.iftalk.plugin.tsc.voicemessage.user.VUser;

@JiveID(201)
public class VoiceBridgeVmsService extends AbstractVmsService implements VoiceBridgeConstants, GroupEventListener
{
	//private VmsManager vmsManager;
	//private VmsListener vmsListener;
	public Site site;
	private VoiceBridgeComponent component;
	private long siteID;
	private String siteName;
	private Map<String, Element> vmFeatures;
	private Map<Integer, Integer> vmFeature2IdMap;
	private Map<String, VmsTransaction> vmExten2FeatureMap;

	public VoiceBridgeVmsService(VoiceBridgeComponent component, Site site)
	{
		this.component 		= component;
		this.site 			= site;
        this.siteID    		= site.getSiteID();
        this.siteName  		= site.getName();

		vmFeatures 			= new HashMap<String, Element>();
		vmFeature2IdMap		= new HashMap<Integer, Integer>();
		vmExten2FeatureMap	= new HashMap<String, VmsTransaction>();
	}

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	public void startup()
	{
		Log.info("["+ site.getName() + "] VMS startup");

		String vmsEnabled				= "false";
		String vmsDatabaseType			= "POSTGRESQL";
		String vmsDatabaseHost			= "localhost";
		String vmsDatabaseName			= "voicedrop";
		String vmsDatabaseUsername		= "postgres";
		String vmsDatabasePassword		= "969131";

		String vmsPhoneServerHost		= "192.168.100.46";
		String vmsPhoneServerPort		= "5038";
		String vmsPhoneServerUsername	= "manager";
		String vmsPhoneServerPassword	= "mysecret";
		String vmsPhoneServerChannel	= "LOCAL/85XXXX@vms";
		String vmsPhoneServerPrefix		= "555";
		String vmsPhoneServerContext	= "divoiceint";
		String vmsPhoneServerAGIContext	= "vmsagi";
		String vmsPhoneServerNumberLen	= "4";

		String pname = site.getName().toLowerCase();

		vmsEnabled				= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_ENABLED		 	+ "." + pname, vmsEnabled);
		vmsDatabaseType 		= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_DATABASE_TYPE 		+ "." + pname, vmsDatabaseType);
		vmsDatabaseHost			= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_DATABASE_HOST 		+ "." + pname, vmsDatabaseHost);
		vmsDatabaseName			= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_DATABASE_NAME 		+ "." + pname, vmsDatabaseName);
		vmsDatabaseUsername     = JiveGlobals.getProperty(Properties.VoiceBridge_VMS_DATABASE_USERNAME	+ "." + pname, vmsDatabaseUsername);
		vmsDatabasePassword		= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_DATABASE_PASSWORD	+ "." + pname, vmsDatabasePassword);

		vmsPhoneServerHost		= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_HOST	 		+ "." + pname, vmsPhoneServerHost);
		vmsPhoneServerPort		= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_PORT	 		+ "." + pname, vmsPhoneServerPort);
		vmsPhoneServerUsername  = JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_USERNAME 	+ "." + pname, vmsPhoneServerUsername);
		vmsPhoneServerPassword  = JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_PASSWORD 	+ "." + pname, vmsPhoneServerPassword);
		vmsPhoneServerChannel	= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_CHANNEL 		+ "." + pname, vmsPhoneServerChannel);
		vmsPhoneServerPrefix	= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_PREFIX 		+ "." + pname, vmsPhoneServerPrefix);
		vmsPhoneServerContext	= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_CONTEXT 		+ "." + pname, vmsPhoneServerContext);
		vmsPhoneServerAGIContext= JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_AGI_CONTEXT	+ "." + pname, vmsPhoneServerAGIContext);
		vmsPhoneServerNumberLen = JiveGlobals.getProperty(Properties.VoiceBridge_VMS_PHONE_NUMBER_LEN 	+ "." + pname, vmsPhoneServerNumberLen);

		//DatabaseType dbType = DatabaseType.POSTGRESQL;

		//if ("SQLSERVER".equals(vmsDatabaseType)) dbType = DatabaseType.SQLSERVER;
		//if ("MYSQL".equals(vmsDatabaseType)) dbType = DatabaseType.MYSQL;

		Log.info( "["+ site.getName() + "] VoiceBridgeVmsService - " + vmsPhoneServerHost + ":" + vmsPhoneServerPort);

		try {
/*
			vmsManager = new VmsManager();
			vmsManager.setupDatabase(dbType, vmsDatabaseHost, vmsDatabaseName, vmsDatabaseUsername, vmsDatabasePassword);

			vmsManager.setupVms(vmsPhoneServerHost, Integer.parseInt(vmsPhoneServerPort), vmsPhoneServerUsername,
                     			  vmsPhoneServerPassword, vmsPhoneServerChannel, vmsPhoneServerPrefix,
                     			  vmsPhoneServerAGIContext, vmsPhoneServerContext, Integer.parseInt(vmsPhoneServerNumberLen));

			vmsManager.initialize();
			vmsListener = new VmsListener();
			vmsManager.addEventListener(vmsListener);
*/
			GroupEventDispatcher.addListener(this);
		}
		catch(Exception e) {
        	Log.error("["+ site.getName() + "] VoiceBridgeVmsService " + e);
			e.printStackTrace();
        }
	}

	public boolean isTelephonyServerConnected()
	{
/*
		if (vmsManager == null)
			return false;
		else
			return vmsManager.isTelephonyServerConnected();
*/
		return false;
	}


	public void setupUser(VoiceBridgeUser voicebridgeUser)
	{
/*
		VUser vmUser = getVMUser(voicebridgeUser);

		if (vmUser != null)
		{
			Log.info("["+ site.getName() + "] VMS processing user " + voicebridgeUser.getUserId());

			try {
				MessageLibrary messageLibrary = new MessageLibrary(voicebridgeUser.getUserId());
				//MessageLibrary messageLibrary = vmUser.getMessageLibrary();

				Iterator<VMessage> iter4 = messageLibrary.getMessages().iterator();

				while( iter4.hasNext() )
				{
					VMessage vMessage = (Message)iter4.next();

					String msgName = vMessage.getName();
					int pos = msgName.indexOf("vm_");

					if (pos > -1)
					{
						Log.info("["+ site.getName() + "] VMS processing message " + msgName);

						String eckeyString = msgName.substring(pos + 3);
						int eckeyNumber = Integer.parseInt(eckeyString);

						// store msg id
						vmFeature2IdMap.put(eckeyNumber, vMessage.getId());

						Log.info("["+ site.getName() + "] VMS setting ECKeys " + eckeyString);

						// record
						component.voicebridgeLinkService.setECKey(voicebridgeUser.getDeviceNo(), "020", eckeyString, "101");
						component.voicebridgeLinkService.setECKey(voicebridgeUser.getDeviceNo(), "020", eckeyString, "103");
						component.voicebridgeLinkService.setECKeyLabel(voicebridgeUser.getDeviceNo(), "020", eckeyString, vMessage.getComment());

						// playback
						component.voicebridgeLinkService.setECKey(voicebridgeUser.getDeviceNo(), "021", eckeyString, "101");
						component.voicebridgeLinkService.setECKey(voicebridgeUser.getDeviceNo(), "021", eckeyString, "103");
						component.voicebridgeLinkService.setECKeyLabel(voicebridgeUser.getDeviceNo(), "021", eckeyString, vMessage.getComment());

					}
				}
			}
			catch(Exception e)
			{
				Log.error("["+ site.getName() + "] setupUser " + e);
				e.printStackTrace();
			}
		}
*/
	}

	public void shutdown()
	{
		Log.info("["+ site.getName() + "] VMS shutdown");
/*
		if (vmsManager != null)
		{
			try {
				GroupEventDispatcher.removeListener(this);
				vmsManager.removeEventListener(vmsListener);
				vmsManager.shutdown();
				vmsManager = null;
				vmsListener = null;
			}
			catch(Exception e) {
				Log.error("["+ site.getName() + "] VMS shutdown " + e);
			}
		}
*/
	}

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	public long getNextMsgId()
	{
		try {
			return SequenceManager.nextID(this);

		}
		catch(Exception e)
		{
			return -1;
		}
	}


	public synchronized Element getFeature(String msgId)
	{

		if (vmFeatures.containsKey(msgId))
			return vmFeatures.get(msgId);
		else
			return null;
	}


	public synchronized void removeFeature(String msgId)
	{
		if (vmFeatures.containsKey(msgId))
		{
			vmFeatures.remove(msgId);
		}
	}

	public String recordMessage(VoiceBridgeUser voicebridgeUser, String msgId, String msgComment)
	{
		return recordMessage(voicebridgeUser, msgId, msgComment, -1);
	}

	public String recordMessage(VoiceBridgeUser voicebridgeUser, String msgId, String msgComment, int oldId)
	{
		String actionNumber = null;
/*
		VUser vmUser = getVMUser(voicebridgeUser);

		if (vmUser != null)
		{
			try {
				MessageLibrary messageLibrary = new MessageLibrary(voicebridgeUser.getUserId());
				//MessageLibrary messageLibrary = vmUser.getMessageLibrary();
				actionNumber = messageLibrary.recordMessageCallIn(msgId, msgComment);

				VmsTransaction vmsTransaction = new VmsTransaction();
				vmsTransaction.newMsgName = msgId;
				vmsTransaction.newMsgComment = msgComment;
				vmsTransaction.oldMsgId = oldId;
				vmsTransaction.console = voicebridgeUser.getDeviceNo();
				vmsTransaction.exten = actionNumber;

				vmExten2FeatureMap.put(actionNumber, vmsTransaction);

			}
			catch(Exception e)
			{
				Log.error("["+ site.getName() + "] recordNewMessage " + e);
				e.printStackTrace();
			}
		}
*/
		return actionNumber;
	}



	public String recordMessage2(VoiceBridgeUser voicebridgeUser, String msgId, String msgComment)
	{
		String actionNumber = null;
/*
		String msgDesc = voicebridgeUser.getUserNo() + " - " + voicebridgeUser.getUserName();
		VUser vmUser = getVMUser(voicebridgeUser);

		if (vmUser != null)
		{
			try {
				actionNumber = ActionNumberManager.getInstance().getNumber();
				RecordAction action = new RecordAction(TelephonyCommandType.CALLIN, actionNumber, MessageManager.getInstance().getNextMessageId(), msgId, msgComment);
				action.setUserId(vmUser.getId());
				PendingActions.getInstance().addAction(actionNumber, action);
			}
			catch(Exception e)
			{
				Log.error("["+ site.getName() + "] recordNewMessage " + e);
				e.printStackTrace();
			}
		}
*/
		return actionNumber;
	}


	public VMessage deleteMessage(VoiceBridgeUser voicebridgeUser, int vmId, String msgName)
	{
		Log.debug("["+ site.getName() + "] deleteMessage " + voicebridgeUser.getUserId() + " " + vmId + " " + msgName);
		VMessage vMsg = null;
/*
		VUser vmUser = getVMUser(voicebridgeUser);


		if (vmUser != null)
		{
			try {

				MessageLibrary messageLibrary = new MessageLibrary(voicebridgeUser.getUserId());
				//MessageLibrary messageLibrary = vmUser.getMessageLibrary();
				vMsg = messageLibrary.deleteMessage(vmId);

				int pos = msgName.indexOf("vm_");
				String eckeyString = msgName.substring(pos + 3);
				int eckeyNumber = Integer.parseInt(eckeyString);

				vmFeature2IdMap.remove(eckeyNumber);

				deleteSharedMessage(voicebridgeUser.getUserId(), vmId, msgName);
			}
			catch(Exception e) {
				Log.error("["+ site.getName() + "] deleteMessage " + e);
			}
		}
*/
		return vMsg;
	}


	private void deleteSharedMessage(String userId, int vmId, String msgName)
	{
		Log.debug("["+ site.getName() + "] deleteSharedMessage " + userId + " " + vmId + " " + msgName);
/*
		try {
			List<User> users = getPeerUsers(msgName);

			Iterator<User> it = users.iterator();

			while (it.hasNext())
			{
				VUser user = (User)it.next();

				if (!userId.equals(user.getId()))
				{
					Log.debug("["+ site.getName() + "] deleteMessage for peer user " +  user.getId());

					MessageManager.getInstance().getMessageProvider().deleteMessage(user.getId(), vmId);
				}
			}
		}
		catch(Exception e) {
			Log.error("["+ site.getName() + "] deleteSharedMessage " + e);
		}
*/
	}


	public String getVMExtenToDial(VoiceBridgeUser voicebridgeUser, int vmId, String msgId)
	{
		Log.debug("["+ site.getName() + "] getVMExtenToDial " + voicebridgeUser.getUserId() + " " + vmId);

		String exten = null;
/*
		VUser vmUser = getVMUser(voicebridgeUser);

		if (vmUser != null)
		{
			try {
				MessageLibrary messageLibrary = new MessageLibrary(voicebridgeUser.getUserId());
				//MessageLibrary messageLibrary = vmUser.getMessageLibrary();
				exten = messageLibrary.playbackMessageCallIn(new int[] { vmId });

				VmsTransaction vmsTransaction = new VmsTransaction();
				vmsTransaction.newMsgName = msgId;
				vmsTransaction.console = voicebridgeUser.getDeviceNo();
				vmsTransaction.exten = exten;

				vmExten2FeatureMap.put(exten, vmsTransaction);

			}
			catch(Exception e) {
				Log.error("["+ site.getName() + "] getVMExtenToDial " + e);
			}
		}
*/
		return exten;
	}

	public Collection<VMessage> getVMessages(VoiceBridgeUser voicebridgeUser)
	{
		Log.debug("["+ site.getName() + "] getVMessages " + voicebridgeUser.getUserId());
		List<VMessage> vMessages = new ArrayList<VMessage>();
/*
		VUser vmUser = getVMUser(voicebridgeUser);

		if (vmUser != null)
		{
			MessageLibrary messageLibrary = new MessageLibrary(voicebridgeUser.getUserId());
			//MessageLibrary messageLibrary = vmUser.getMessageLibrary();
			vMessages = messageLibrary.getMessages();
		}
*/
		return vMessages;
	}


	public VMessage getVMessageFromName(VoiceBridgeUser voicebridgeUser, String msgName)
	{
		Log.debug("["+ site.getName() + "] getVMessageFromName " + voicebridgeUser.getUserId() + " " + msgName);

		VMessage message = null;
/*
		VUser vmUser = getVMUser(voicebridgeUser);

		if (vmUser != null)
		{
			//MessageLibrary messageLibrary = vmUser.getMessageLibrary();
			MessageLibrary messageLibrary = new MessageLibrary(voicebridgeUser.getUserId());

			Iterator<VMessage> iter4 = messageLibrary.getMessages().iterator();

			while( iter4.hasNext() )
			{
				Message vMessage = (Message)iter4.next();

				if (msgName.equals(vMessage.getName()))
				{
					message = vMessage;
				}
			}
		}
*/
		return message;
	}


	public VUser getVMUser(VoiceBridgeUser voicebridgeUser)
	{
		Log.debug("["+ site.getName() + "] getVMUser " + voicebridgeUser.getUserId());

		VUser vmUser = null;
/*
		try {
			vmUser = UserManager.getInstance().createUser(voicebridgeUser.getUserId(), voicebridgeUser.getUserName());

		} catch (UserAlreadyExistsException e1) {

			try {
				vmUser = UserManager.getInstance().getUser(voicebridgeUser.getUserId());

			} catch (UserAccessException e2) {

				Log.error("["+ site.getName() + "] getVMUser  - user not accessible " + e2);

			} catch (UserNotFoundException e3) {

				Log.error("["+ site.getName() + "] getVMUser - user not found " + e3);
			}

		} catch (UserAccessException e4) {

			Log.error("["+ site.getName() + "] getVMUser  - user not accessible " + e4);
		}
*/
		return vmUser;
	}

	public List<VUser> getPeerUsers(String msgName)
	{
		Log.debug("["+ site.getName() + "] getPeerUsers " + msgName);

		List<VUser> vmUsers = new ArrayList<VUser>();

		Group group = getGroup(msgName);

		if (group != null)
		{
			addGroupUsers(vmUsers, group.getMembers());
			addGroupUsers(vmUsers, group.getAdmins());
		}

		return vmUsers;
	}


	private void addGroupUsers(List<VUser> vmUsers, Collection<JID> jids)
	{
/*
		org.jivesoftware.openfire.user.UserManager userManager = XMPPServer.getInstance().getUserManager();

		for (JID jid : jids)
		{
			VUser vmUser = null;

			try {

				if (userManager.isRegisteredUser(jid.getNode()))
				{
					String userName = userManager.getUser(jid.getNode()).getName();
					vmUser = UserManager.getInstance().getUser(jid.getNode());

					if (vmUser == null)
					{
						vmUser = UserManager.getInstance().createUser(jid.getNode(), userName);
					}
				}

			} catch (Exception e4) {

				Log.error("["+ site.getName() + "] addGroupUsers  -  " + e4);
			}

			if (vmUser != null)
			{
				Log.debug("["+ site.getName() + "] addGroupUsers adding user " + vmUser.getId());

				vmUsers.add(vmUser);
			}
		}
*/
	}


	public boolean hasEditPermissions(String userId, String msgName)
	{
		Group group = getGroup(msgName);

		if (group == null) // personal message, no matching goup name
			return true;

		else {

			JID userJID = new JID(userId + "@" + component.getDomain());
			return group.getAdmins().contains(userJID);
		}
	}

	private Group getGroup(String groupName)
	{
		GroupManager groupManager =  GroupManager.getInstance();
		Collection<Group> groups = groupManager.getGroups();
		Group theGroup = null;

		for (Group group : groups) {

			if (groupName.equals(group.getName()))
			{
				theGroup = group;
				break;
			}
		}
		return theGroup;
	}

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


    public void groupCreated(Group group, Map params)
    {

    }

    public void groupDeleting(Group group, Map params)
    {

    }

    public void groupModified(Group group, Map params)
    {

	}

    public void memberAdded(Group group, Map params)
    {
        String msgName = group.getName();

        if (msgName.length() > 3)
        {
			int pos = msgName.indexOf("vm_");
			String eckeyString = msgName.substring(pos + 3);
			int eckeyNumber = Integer.parseInt(eckeyString);

			if (vmFeature2IdMap.containsKey(eckeyNumber))
			{
				try {
					JID addedUser = new JID((String) params.get("member"));

					Log.debug("["+ site.getName() + "] VMS memberAdded " + msgName+ " " + addedUser.getNode());

					int msgId = vmFeature2IdMap.get(eckeyNumber);
					//MessageManager.getInstance().createUserMessageLink(addedUser.getNode(), msgId);

				} catch (Exception e) {

					Log.error("["+ site.getName() + "] VMS memberAdded " + e);
					e.printStackTrace();
				}
			}
		}
    }

    public void memberRemoved(Group group, Map params)
    {
        String msgName = group.getName();

        if (msgName.length() > 3)
        {
			int pos = msgName.indexOf("vm_");
			String eckeyString = msgName.substring(pos + 3);
			int eckeyNumber = Integer.parseInt(eckeyString);

			if (vmFeature2IdMap.containsKey(eckeyNumber))

			{
				try {
					JID removedUser = new JID((String) params.get("member"));

					Log.debug("["+ site.getName() + "] VMS memberRemoved " + msgName + " " + removedUser.getNode());

					int msgId = vmFeature2IdMap.get(eckeyNumber);
					//MessageManager.getInstance().getMessageProvider().deleteMessage(removedUser.getNode(), msgId);

				} catch (Exception e) {

					Log.error("["+ site.getName() + "]  VMS memberRemoved " + e);
					e.printStackTrace();
				}
			}
		}
    }

    public void adminAdded(Group group, Map params)
    {
        JID addedUser = new JID((String) params.get("admin"));

        if (group.getMembers().contains(addedUser))   // Do nothing if the user was a member that became an admin
        {
            return;
        }
    }

    public void adminRemoved(Group group, Map params)
    {
        JID addedUser = new JID((String) params.get("admin"));

        if (group.getMembers().contains(addedUser))   // Do nothing if the user was a member that became an admin
        {
            return;
        }
    }

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

/*
	private class VmsListener extends BaseVmsEventListener
	{
		@Override protected void handleEvent(MessageArchiveEvent event)
		{

		}

		@Override protected void handleEvent(MessageDeleteEvent event)
		{
			generatePubsubDeviceEvent(event, "MessageDeleteEvent");
		}

		@Override protected void handleEvent(MessagePlaybackEvent event)
		{
			generatePubsubDeviceEvent(event, "MessagePlaybackEvent");

			if (event instanceof MessageMediaEvent)
			{
				Log.debug("["+ site.getName() + "] handleEvent (MessagePlaybackEvent) " + event.toString());

				MessageMediaEvent event2 = (MessageMediaEvent) event;

				if (vmExten2FeatureMap.containsKey(event2.getTsCalledId()))
				{
					VmsTransaction vmsTransaction = vmExten2FeatureMap.get(event2.getTsCalledId());

					String msgName = vmsTransaction.newMsgName;
					int pos = msgName.indexOf("vm_");
					String eckeyString = msgName.substring(pos + 3);
					int eckeyNumber = Integer.parseInt(eckeyString);

					if (vmsTransaction.console != null)
					{
						if ("STATE_STOP".equals(event2.getTsState()))
						{
							component.voicebridgeLinkService.setECKey(vmsTransaction.console, "021", eckeyString, "103");

						} else {

							component.voicebridgeLinkService.setECKey(vmsTransaction.console, "021", eckeyString, "105");
						}
					}
				}
			}

		}

		@Override protected void handleEvent(MessageRecordEvent event)
		{
			generatePubsubDeviceEvent(event, "MessageRecordEvent");
			handlePeerUsers(event, "MessageRecordEvent");
		}

		@Override protected void handleEvent(MessageSaveEvent event)
		{

		}

		@Override protected void handleEvent(VssConnectEvent event)
		{
			Log.debug("["+ site.getName() + "] VssConnectEvent " + event.toString());
		}

		@Override protected void handleEvent(VssDisconnectEvent event)
		{
			Log.debug("["+ site.getName() + "] VssDisconnectEvent " + event.toString());
		}

		private VoiceBridgeUser getVoiceBridgeUser(String userId)
		{
			VoiceBridgeUser theUser = null;

			Iterator<VoiceBridgeUser> it = component.voicebridgeLdapService.voicebridgeUserTable.values().iterator();

			while( it.hasNext() )
			{
				VoiceBridgeUser voicebridgeUser = (VoiceBridgeUser)it.next();

				if (userId.equals(voicebridgeUser.getUserId()))
				{
					theUser = voicebridgeUser;
					break;
				}
			}

			return theUser;
		}


		private void handlePeerUsers(BaseVmsEvent event, String eventName)
		{
			Log.debug("["+ site.getName() + "] handlePeerUsers " + eventName + " " + event.toString());

			try {

				if (event instanceof MessageMediaEvent)
				{
					Log.debug("["+ site.getName() + "] handleEvent (" + eventName + ") " + event.toString());

					MessageMediaEvent event2 = (MessageMediaEvent) event;

					if (vmExten2FeatureMap.containsKey(event2.getTsCalledId()))
					{
						VmsTransaction vmsTransaction = vmExten2FeatureMap.get(event2.getTsCalledId());
						String msgName = vmsTransaction.newMsgName;
						int msgId = Integer.parseInt(event.getTsMsgId());

						int pos = msgName.indexOf("vm_");
						String eckeyString = msgName.substring(pos + 3);
						int eckeyNumber = Integer.parseInt(eckeyString);

						List<User> users = getPeerUsers(msgName);

						String userId = event.getTsUserId();

						if ("MessageRecordEvent".equals(eventName))
						{
							vmFeature2IdMap.put(eckeyNumber, msgId);

                   			RecordAction recordAction = (RecordAction) PendingActions.getInstance().getAction(event2.getTsCalledId());
                            MessageManager.getInstance().createMessage(recordAction.getMessageId(), recordAction.getMessageName(), recordAction.getMessageComment(), event2.getTsMsgPath());

                            if (vmsTransaction.console != null)
                            {
								if ("STATE_STOP".equals(event2.getTsState()))
								{
									component.voicebridgeLinkService.setECKey(vmsTransaction.console, "020", eckeyString, "103");

								} else {
									component.voicebridgeLinkService.setECKey(vmsTransaction.console, "020", eckeyString, "105");
								}
							}
						}

						Iterator<User> it = users.iterator();

						while (it.hasNext())
						{
							User user = (User)it.next();

							//if (!userId.equals(user.getId()))
							//{
								Log.debug("["+ site.getName() + "] handleEvent (" + eventName + ") associate " + user.getId() + " " + msgId);

								if ("MessageRecordEvent".equals(eventName))
								{
									MessageManager.getInstance().createUserMessageLink(user.getId(), msgId);

									if (vmsTransaction.oldMsgId > -1) // remove old message link
									{
										Log.debug("["+ site.getName() + "] handleEvent (" + eventName + ") delete old message " + user.getId() + " " + msgId);

										MessageManager.getInstance().getMessageProvider().deleteMessage(user.getId(), vmsTransaction.oldMsgId);
									}
								}
							//}
						}
					}
				}

			} catch (Exception e) {

				Log.error("["+ site.getName() + "]  handlePeerUsers (" + eventName + ") " + e);
				e.printStackTrace();
			}
		}


		private void generatePubsubDeviceEvent(BaseVmsEvent event, String eventName)
		{
			Log.debug("["+ site.getName() + "] generatePubsubDeviceEvent " + eventName + " " + event.toString());

			try {
				VoiceBridgeUser voicebridgeUser = getVoiceBridgeUser(event.getTsUserId());

				if (voicebridgeUser != null)
				{
					VoiceBridgeInterest voicebridgeInterest = voicebridgeUser.getDefaultInterest();

					if (voicebridgeInterest !=  null)
					{
						String interestNode = voicebridgeInterest.getInterestId() + voicebridgeUser.getUserNo();

						IQ iq = new IQ(IQ.Type.set);
						iq.setFrom(component.getName() + "." + component.getDomain());
						iq.setTo("pubsub." + component.getDomain());
						Element pubsub = iq.setChildElement("pubsub", "http://jabber.org/protocol/pubsub");
						Element publish = pubsub.addElement("publish").addAttribute("node", interestNode);
						Element item = publish.addElement("item").addAttribute("id", interestNode);
						Element devicestatus = item.addElement("devicestatus", "http://xmpp.org/protocol/openlink:01:00:00#device-status");
						Element features = devicestatus.addElement("features");
						Element feature = features.addElement("feature").addAttribute("id", "vm_" + event.getTsMsgId());
						Element voicemessage = feature.addElement("voicemessage").addAttribute("xmlns", "http://xmpp.org/protocol/openlink:01:00:00/features#voice-message");

						voicemessage.addElement("status").setText(event.getTsStatus() == null ? "" : event.getTsStatus());
						voicemessage.addElement("statusdescriptor").setText(event.getTsStatusDescriptor() == null ? "" : event.getTsStatusDescriptor());

        				if(event instanceof MessageMediaEvent)
        				{
							MessageMediaEvent event2 = (MessageMediaEvent) event;

							voicemessage.addElement("msglen").setText(event2.getTsMsgLength() == null ? "" : event2.getTsMsgLength());
							voicemessage.addElement("state").setText(event2.getTsState() == null ? "" : event2.getTsState());
							voicemessage.addElement("exten").setText(event2.getTsExtension() == null ? "" : event2.getTsExtension());
						}

						component.sendPacket(iq);
						vmFeatures.put(event.getTsMsgId(), feature.createCopy());

					} else Log.error("["+ site.getName() + "] " + eventName + " - user has no default Interest " + voicebridgeUser.getUserId());

				} else Log.error("["+ site.getName() + "]  " + eventName + " - user has no VoiceBridgeUser object " + event.getTsUserId());

			} catch (Exception e) {

				Log.error("["+ site.getName() + "]  " + eventName + " -  " + e);
				e.printStackTrace();
			}
		}
	}
*/

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	private class VmsTransaction
	{

		public int oldMsgId = -1;
		public String newMsgName = null;
		public String newMsgComment = null;
		public String console = null;
		public String exten = null;
	}

}