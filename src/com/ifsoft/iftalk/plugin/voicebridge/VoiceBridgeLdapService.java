
package com.ifsoft.iftalk.plugin.voicebridge;

import java.io.FileInputStream;
import java.util.*;
import java.text.*;
import org.dom4j.*;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.jivesoftware.openfire.vcard.*;
import org.jivesoftware.openfire.cluster.ClusterManager;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.User;

import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;

import com.ifsoft.iftalk.plugin.tsc.*;


public class VoiceBridgeLdapService extends AbstractLdapService implements VoiceBridgeConstants
{
	public final Map<String, VoiceBridgeUser> voicebridgeUserTable;
	public final Map<String, VoiceBridgeInterest> voicebridgeInterests;
	public final Map<String, VoiceBridgeSpeedDial> speedDialTable;

	public final Map<String, String> cliLookupTable;
	public final Map<String, String> ddiLookupTable;
	public final Map<String, Integer> phantomLookupTable;
	public final Map<String, String> callsetLookupTable;

	public Map<Integer, String> pwLookupTable;

	private String siteID				= null;
 	private String siteName 			= null;
	private String ldapServer 			= null;
	private String ldapUserId			= null;
	private String ldapPassword			= null;
	private String rootDn				= null;
	private String ldapSchemaVersion	= "unknown";
	private String voicebridgeDownloadStatus 	= "unknown";
	private String voicebridgeDownloadedAt  	= "unknown";
	private DirContext ctx 				= null;
	private boolean ldapAlive 			= true;
	private Timer timer 				= null;
	private VoiceBridgeComponent component		= null;
	private boolean stopFlag			= false;

    private ComponentManager componentManager;
    public Site site;
    private List<VoiceBridgeGroup> voicebridgeGroupList;

	public VoiceBridgeLdapService(VoiceBridgeComponent component, Site site)
	{
		Log.info( "["+ site.getName() + "] VoiceBridgeLdapService");

		this.component 		= component;
		this.site 			= site;
        this.siteID    		= String.valueOf(site.getSiteID());
		this.siteName 		= site.getName();

		timer 				= new Timer();

		voicebridgeUserTable 	= Collections.synchronizedMap( new HashMap<String, VoiceBridgeUser>());
		voicebridgeInterests 	= Collections.synchronizedMap( new HashMap<String, VoiceBridgeInterest>());
		speedDialTable 		= Collections.synchronizedMap( new HashMap<String, VoiceBridgeSpeedDial>());
		cliLookupTable 		= Collections.synchronizedMap( new HashMap<String, String>());
		ddiLookupTable 		= Collections.synchronizedMap( new HashMap<String, String>());
		phantomLookupTable	= Collections.synchronizedMap( new HashMap<String, Integer>());
		callsetLookupTable 	= Collections.synchronizedMap( new HashMap<String, String>());
		pwLookupTable 		= Collections.synchronizedMap( new HashMap<Integer, String>());

		voicebridgeGroupList	= new ArrayList();

		componentManager = ComponentManagerFactory.getComponentManager();

	}

	public void startup()
	{
		try
		{

			Log.info("["+ siteName + "] VoiceBridgeLdapService version " + ldapSchemaVersion);
		}
		catch (Exception e)
		{
	        Log.error( "["+ siteName + "] VoiceBridgeLdapService " + e);
		}
	}

	public void stop()
	{
		stopFlag = true;
	}


	public boolean isCacheExpired()
	{
		boolean expired = false;
		return expired;
	}


	public void resetCache()
	{
		voicebridgeUserTable.clear();
		voicebridgeInterests.clear();
	}


	public void getProfiles() throws NamingException
	{
		try
		{
			Collection<User> users = XMPPServer.getInstance().getUserManager().getUsers();

            Iterator it = users.iterator();
            int userNo = 1000;

			while( it.hasNext() )
			{
               	User user = (User)it.next();
               	userNo++;

				VoiceBridgeUser vbUser = new VoiceBridgeUser();
				vbUser.setUserName(user.getName());
				vbUser.setUserId(user.getUsername());
				vbUser.setUserNo(String.valueOf(userNo));
				vbUser.setSiteName(siteName);
				vbUser.setSiteID(Integer.parseInt(siteID));
				vbUser.setHandsetNo("1");

               	String deskPhone = null;

               	Element vCard = VCardManager.getInstance().getVCard(user.getUsername());

               	if (vCard != null)
               	{
					Log.debug( "["+ siteName + "] vcard for " + user.getName() + "\n" + vCard.asXML());

					deskPhone = getTelVoiceNumber(vCard);

					if (deskPhone != null && deskPhone.length() > 0)
					{
						try {
							String cononicalNumber = component.formatCanonicalNumber(deskPhone);
							deskPhone = component.formatDialableNumber(cononicalNumber);

						} catch (Exception e) { }
					}
				}

				if (deskPhone != null)
				{
					vbUser.setCallback(deskPhone);
					vbUser.setPersonalDDI(deskPhone);
					vbUser.setDeviceNo(deskPhone);
				}

				//vbUser.setDeviceType("voicebridge");

				synchronized (component.voicebridgePlugin)
				{
					String allocatedTSC = component.voicebridgePlugin.getUserTSC(user.getUsername());

					Log.debug( "["+ siteName + "] getUsers allocated TSC " + allocatedTSC + " " + component.getComponentJID().toString() + " " + user.getUsername());

					if ("none".equals(allocatedTSC) || allocatedTSC.equals(component.getComponentJID().toString()))
					{
						if ("none".equals(allocatedTSC))
						{
							component.voicebridgePlugin.addUserTSC(user.getUsername(), component.getComponentJID().toString(), siteName);
						}

						vbUser.setEnabled(true);

					} else {						// if it is allocated to another TSC

						if (component.voicebridgePlugin.getComponentByUserId(user.getUsername()) == null) // is it deleted
						{
							component.voicebridgePlugin.removeUserTSC(user.getUsername(), allocatedTSC);
							component.voicebridgePlugin.addUserTSC(user.getUsername(), component.getComponentJID().toString(), siteName);
							vbUser.setEnabled(true);

						} else {

							vbUser.setEnabled(false);
						}
					}
				}


				vbUser.setDefault("true");
				voicebridgeUserTable.put(vbUser.getUserNo(), vbUser);

				String interestNode = "VBD" + userNo;

				VoiceBridgeInterest voicebridgeInterest = new VoiceBridgeInterest();
				voicebridgeInterest.setInterestType("VBD");
				voicebridgeInterest.setSiteName(siteID);
				voicebridgeInterest.setInterestLabel(user.getUsername() + "_" + userNo);
				voicebridgeInterest.setInterestValue(String.valueOf(userNo));

				voicebridgeInterests.put(interestNode, voicebridgeInterest);

				VoiceBridgeUserInterest voicebridgeUserInterest = voicebridgeInterest.addUserInterest(vbUser, "true");
				vbUser.addInterest(voicebridgeInterest);

				component.openlinkInterests.put(interestNode + vbUser.getUserNo(), voicebridgeUserInterest);

				if (vbUser.enabled())
				{
					createPubsubNode(user.getUsername() + "@" + component.getDomain());

					createPubsubNode(voicebridgeInterest.getInterestId() + vbUser.getUserNo());
					getInterestSubscriptions(voicebridgeInterest, vbUser.getUserNo());
				}
			}
		}
		catch (Exception e)
		{
	        Log.error( "["+ siteName + "] " +  "Error in getProfiles " + e);
        	e.printStackTrace();
		}
	}

	private String getTelVoiceNumber(Element vCard)
	{
		String telVoiceNumber = null;

		for ( Iterator i = vCard.elementIterator( "TEL" ); i.hasNext(); )
		{
			Element tel = (Element) i.next();
			//Log.debug( "["+ siteName + "] getTelVoiceNumber - tel " + tel.asXML());

			if (tel.element("WORK") != null)
			{
				Element work = tel.element("WORK");
				//Log.debug( "["+ siteName + "] getTelVoiceNumber - work " + work.asXML());

				if (tel.element("WORK") != null && tel.element("VOICE") != null)
				{
					Element number = tel.element("NUMBER");

					if (number != null)
					{
						//Log.debug( "["+ siteName + "] getTelVoiceNumber - number " + number.getText());
						telVoiceNumber = number.getText();
						break;
					}
				}
			}
		}

		return telVoiceNumber;
	}


	public void getUserProfile(VoiceBridgeUser voicebridgeUser) throws NamingException
	{
		Log.info( "["+ siteName + "] Loading user profiles into local cache - " + voicebridgeUser.getUserNo() + " " + voicebridgeUser.getUserId());

		try
		{

		}
		catch (Exception e)
		{
	        Log.error( "["+ siteName + "] " +  "Error in getUserProfile " + e);
		}
	}


	public boolean isLdapAlive()
	{
		try
		{
			ldapAlive = true;
			return ldapAlive;
		}
		catch (Exception e)
		{
			return false;
		}
	}


	public boolean connect()
	{
		boolean rval = true;
		return rval;
	}


//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	public void createPubsubNode(String interestNode)
	{
		Log.debug("["+component.getName()+"] createPubsubNode - " + interestNode);

		String domain = component.getDomain();

		IQ iq1 = new IQ(IQ.Type.set);
		iq1.setFrom(component.getName() + "." + domain);
		iq1.setTo("pubsub." + domain);
		Element pubsub1 = iq1.setChildElement("pubsub", "http://jabber.org/protocol/pubsub");
		Element create = pubsub1.addElement("create").addAttribute("node", interestNode);

		Element configure = pubsub1.addElement("configure");
		Element x = configure.addElement("x", "jabber:x:data").addAttribute("type", "submit");

		Element field1 = x.addElement("field");
		field1.addAttribute("var", "FORM_TYPE");
		field1.addAttribute("type", "hidden");
		field1.addElement("value").setText("http://jabber.org/protocol/pubsub#node_config");

		//Element field2 = x.addElement("field");
		//field2.addAttribute("var", "pubsub#persist_items");
		//field2.addElement("value").setText("1");

		Element field3 = x.addElement("field");
		field3.addAttribute("var", "pubsub#max_items");
		field3.addElement("value").setText("1");

		Log.debug("createPubsubNode " + iq1.toString());
		component.sendPacket(iq1);
	}


	public void getInterestSubscriptions(VoiceBridgeInterest voicebridgeInterest, String userNo)
	{
		String interestNode = voicebridgeInterest.getInterestId() + userNo;
		String domain = component.getDomain();

		Log.debug("["+component.getName()+"] getInterestSubscriptions  - " + interestNode);

		IQ iq2 = new IQ(IQ.Type.get);
		iq2.setFrom(component.getName() + "." + domain);
		iq2.setTo("pubsub." + domain);
		Element pubsub2 = iq2.setChildElement("pubsub", "http://jabber.org/protocol/pubsub#owner");
		Element subscriptions = pubsub2.addElement("subscriptions").addAttribute("node", interestNode);

		Log.debug("subscriptions " + iq2.toString());
		component.sendPacket(iq2);
	}

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	public void removeVoiceBridgeUser(String userName)
	{
		Log.debug("["+component.getName()+"] removeVoiceBridgeUser - looking for " + userName);

		Iterator<VoiceBridgeUser> it = voicebridgeUserTable.values().iterator();

		while( it.hasNext() )
		{
			VoiceBridgeUser voicebridgeUser = (VoiceBridgeUser)it.next();

			if (userName.equals(voicebridgeUser.getUserId()))
			{
				Log.debug("["+component.getName()+"] removeVoiceBridgeUser - found " + userName);

				voicebridgeUser.setEnabled(false);
			}
		}
	}

	public void addVoiceBridgeUser(String userName)
	{
		Log.debug("["+component.getName()+"] addVoiceBridgeUser - looking for " + userName);

		Iterator<VoiceBridgeUser> it = voicebridgeUserTable.values().iterator();

		while( it.hasNext() )
		{
			VoiceBridgeUser voicebridgeUser = (VoiceBridgeUser)it.next();

			if (userName.equals(voicebridgeUser.getUserId()))
			{
				Log.debug("["+component.getName()+"] addVoiceBridgeUser - found " + userName);

				voicebridgeUser.setEnabled(true);

				Iterator<VoiceBridgeInterest> iter2 = voicebridgeUser.getInterests().values().iterator();

				while( iter2.hasNext() )
				{
					VoiceBridgeInterest voicebridgeInterest = (VoiceBridgeInterest)iter2.next();
					getInterestSubscriptions(voicebridgeInterest, voicebridgeUser.getUserNo());
				}
			}
		}
	}
}
