
package com.ifsoft.iftalk.plugin.voicebridge;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import java.io.*;
import org.jivesoftware.util.Log;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.JiveGlobals;

import com.ifsoft.iftalk.plugin.tsc.*;

public class VoiceBridgeUser extends AbstractUser implements VoiceBridgeConstants, Comparable
{
	private String userType = "VoiceBridge";
	private boolean enabled = true;
	private boolean intercom = false;
	private boolean autoPrivate = false;
	private boolean autoHold = true;
	private String userName;
	private String userId;
	private String userNo;
	private String deviceNo = "0000000000";
	private String handsetNo = "1";
	private int handsets = 1;
	private long   siteID;
	private String siteName;
	private String personalDDI = null;
	private String callset = null;
	private String defaultUser = "false";
	private VoiceBridgeInterest defaultInterest = null;
	private VoiceBridgeUserInterest waitingInterest = null;

	private List<VoiceBridgeGroup> voicebridgeGroups 				= new ArrayList();

	private Map<String, VoiceBridgeInterest> voicebridgeInterests 	= new HashMap();
	public Map<String, String> voicebridgeTrunks 			= new HashMap();

	private String nextStepHandset = "1";
	private String nextStepDDI;
	private String nextStepCallSet;
	private String nextStepLine;
	private String nextStepSpeedial;
	private String nextStepPrivacy;
	private boolean nextStepAutoHold;
	private String nextStepAction = null;

	private String lastPrivacy = null;
	private String lastCallForward = "";
	private String lastCallForwardInterest = "";

	private VoiceBridgeCall currentHS1Call = null;
	private VoiceBridgeCall currentHS2Call = null;
	private VoiceBridgeCall currentICMCall = null;

	private String callback = null;
	private VoiceBridgeCallback phoneCallback = null;
	private boolean callbackActive = false;
	private String vscLine = null;

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------



	public String getProfileName()
	{
		return getUserNo();
	}

	public boolean enabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public boolean autoPrivate()
	{
		return autoPrivate;
	}

	public void setAutoPrivate(boolean autoPrivate)
	{
		this.autoPrivate = autoPrivate;
		this.lastPrivacy = autoPrivate ? "true" : "false";
	}

	public boolean autoHold()
	{
		return autoHold;
	}

	public void setAutoHold(boolean autoHold)
	{
		this.autoHold = autoHold;
	}

	public boolean intercom()
	{
		return intercom;
	}

	public void setIntercom(boolean intercom)
	{
		this.intercom = intercom;
	}

	public int getHandsets()
	{
		return handsets;
	}

	public void setHandsets(int handsets)
	{
		this.handsets = handsets;
	}

	public VoiceBridgeUserInterest getWaitingInterest()
	{
		return waitingInterest;
	}

	public void setWaitingInterest(VoiceBridgeUserInterest waitingInterest)
	{
		this.waitingInterest = waitingInterest;
	}

	public String getLastPrivacy() {
		return lastPrivacy;
	}

	public void setLastPrivacy(String lastPrivacy)
	{
		this.lastPrivacy = lastPrivacy;
		this.autoPrivate = "true".equals(lastPrivacy);
	}

	public VoiceBridgeCall getCurrentHS1Call() {
		return currentHS1Call;
	}

	public void setCurrentHS1Call(VoiceBridgeCall currentHS1Call)
	{
		this.currentHS1Call = currentHS1Call;
	}

	public VoiceBridgeCall getCurrentHS2Call() {
		return currentHS2Call;
	}

	public void setCurrentHS2Call(VoiceBridgeCall currentHS2Call)
	{
		this.currentHS2Call = currentHS2Call;
	}

	public VoiceBridgeCall getCurrentICMCall() {
		return currentICMCall;
	}

	public void setCurrentICMCall(VoiceBridgeCall currentICMCall)
	{
		this.currentICMCall = currentICMCall;
	}

	public String getLastCallForward()
	{
		return lastCallForward;
	}

	public void setLastCallForward(String lastCallForward)
	{
		this.lastCallForward = lastCallForward;
	}

	public String getLastCallForwardInterest()
	{
		return lastCallForwardInterest;
	}

	public void setLastCallForwardInterest(String lastCallForwardInterest)
	{
		this.lastCallForwardInterest = lastCallForwardInterest;
	}

	public String getUserName()
	{
		return userName;
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	public String getUserType()
	{
		return userType;
	}

	public void setUserType(String userType)
	{
		this.userType = userType;
	}

	public String getVSCLine()
	{
		return vscLine;
	}

	public void setVSCLine(String vscLine)
	{
		this.vscLine = vscLine;
	}

	public String getCallset()
	{
		return callset;
	}

	public void setCallset(String callset)
	{
		this.callset = callset;
	}

	public String getDefault()
	{
		return defaultUser;
	}

	public void setDefault(String defaultUser)
	{
		this.defaultUser = defaultUser;
	}

	public VoiceBridgeInterest getDefaultInterest()
	{
		return defaultInterest;
	}

	public void setDefaultInterest(VoiceBridgeInterest defaultInterest)
	{
		this.defaultInterest = defaultInterest;
	}

	public String getPersonalDDI()
	{
		return personalDDI;
	}

	public void setPersonalDDI(String personalDDI)
	{
		this.personalDDI = personalDDI;
	}

	public String getUserId()
	{
		return userId;
	}

	public void setUserId(String userId)
	{
		this.userId = userId;
	}

	public String getUserNo()
	{
		return userNo;
	}

	public void setUserNo(String userNo) {
		this.userNo = String.valueOf(Long.parseLong(userNo));
	}

	public String getDeviceNo()
	{
		if (getPhoneCallback() != null)
			return getPhoneCallback().getVirtualDeviceId();
		else
			return deviceNo;
	}

	public void setDeviceNo(String deviceNo)
	{
		this.deviceNo = deviceNo;
	}

	public String getCallback() {
		return callback;
	}

	public void setCallback(String callback)
	{
		this.callback = callback;
	}

	public VoiceBridgeCallback getPhoneCallback()
	{
		return phoneCallback;
	}

	public void setPhoneCallback(VoiceBridgeCallback phoneCallback)
	{
		this.phoneCallback = phoneCallback;
	}

	public void setCallbackActive(boolean callbackActive)
	{
		this.callbackActive = callbackActive;
	}

	public boolean getCallbackActive()
	{
		return callbackActive;
	}


	public boolean callbackAvailable(VoiceBridgeComponent component)
	{
		if (getPhoneCallback() != null)
		{
			if (!getCallbackActive())
			{
				component.voicebridgeLinkService.activateCallback(getPhoneCallback());
			}

			return true;

		} else return false;
	}

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	public void processConnectedNextSteps(VoiceBridgeComponent component, String line, String turretNo, String handset)
	{
		Log.debug("VoiceBridgeUser - processConnectedNextSteps " + nextStepAction + " " + turretNo + " " + handset);

		if (nextStepAction != null && turretNo.equals(getDeviceNo()) && handset.equals(nextStepHandset))
		{
			if ("speedDial".equals(nextStepAction))
			{
				Log.debug("processConnectedNextSteps " + nextStepAction + " " + nextStepSpeedial + " " + nextStepHandset+ " " + line);

				component.voicebridgeLinkService.dialDigits(line, nextStepSpeedial);

				if (nextStepPrivacy != null && "true".equals(nextStepPrivacy))
				{
					try {
						Thread.sleep(500);
					} catch (Exception e) { }

					component.voicebridgeLinkService.privateCall(turretNo, handset, "true".equals(nextStepPrivacy) ? "Y" : "N");
				}

				resetNextSteps();

			} else if ("privacy".equals(nextStepAction)) {

				if (nextStepPrivacy != null && "true".equals(nextStepPrivacy))
				{
					component.voicebridgeLinkService.privateCall(turretNo, handset, "true".equals(nextStepPrivacy) ? "Y" : "N");
				}

				resetNextSteps();
			}

		}
	}

	public void resetNextSteps()
	{
		nextStepAction 	= null;
	}

	public boolean nextStepsDone()
	{
		return nextStepAction == null;
	}

	public void processConsoleNextSteps(VoiceBridgeComponent component)
	{
		if (nextStepAction != null && !"0000000000".equals(getDeviceNo()))
		{
			if ("selectDDI".equals(nextStepAction))
			{
				//checkAndHoldActiveCall(component, nextStepHandset, nextStepAutoHold);
				component.voicebridgeLinkService.selectDDI(nextStepDDI, getDeviceNo(), nextStepHandset);

				if (nextStepSpeedial != null)
				{
					nextStepAction = "speedDial";
				}

			} else if ("selectCallset".equals(nextStepAction)) {

				//checkAndHoldActiveCall(component, nextStepHandset, nextStepAutoHold);
				component.voicebridgeLinkService.selectCallset(nextStepCallSet, getDeviceNo(), nextStepHandset);

				if (nextStepSpeedial != null)
				{
					nextStepAction = "speedDial";
				}

			} else if ("selectLine".equals(nextStepAction)) {

				//checkAndHoldActiveCall(component, nextStepHandset, nextStepAutoHold);
				component.voicebridgeLinkService.selectLine(nextStepLine, getDeviceNo(), nextStepHandset);
				nextStepAction = "privacy";

			}
		}
	}

	public String getHandsetNo() {
		return handsetNo;
	}

	public String getCurretHandsetNo()
	{
		if (nextStepHandset == null)
			return handsetNo;
		else
			return nextStepHandset;
	}

	public void setHandsetNo(String handsetNo) {
		this.handsetNo = handsetNo;
	}

	public long getSiteID() {
		return siteID;
	}

	public void setSiteID(long siteID) {
		this.siteID = siteID;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public List<VoiceBridgeGroup> getGroups() {
		return voicebridgeGroups;
	}

	public void setGroups(List<VoiceBridgeGroup> voicebridgeGroups) {
		this.voicebridgeGroups = voicebridgeGroups;
	}

	public Map<String, VoiceBridgeInterest> getInterests() {
		return voicebridgeInterests;
	}

	public void addInterest(VoiceBridgeInterest voicebridgeInterest)
	{
		if (!voicebridgeInterests.containsKey(voicebridgeInterest.getInterestId()))
		{
			this.voicebridgeInterests.put(voicebridgeInterest.getInterestId(), voicebridgeInterest);
		}
	}

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


	public void selectLine(VoiceBridgeComponent component, String line, String handset, String privacy, String hold)
	{
		try {
			nextStepPrivacy = privacy == null ? getLastPrivacy() : privacy;
			nextStepAutoHold = hold == null ? autoHold() : "true".equals(hold);

			if (callbackAvailable(component))
			{
				nextStepHandset = getPhoneCallback().getLocalHandset();

			} else {

				handset = handset == null ? getHandsetNo() : handset;
				nextStepHandset = handset;
			}

			if ("0000000000".equals(getDeviceNo()))
			{
				nextStepLine = line;
				nextStepAction = "selectLine";

				component.voicebridgeLinkService.getUserConsole(getUserNo());

			} else {
				nextStepAction = "privacy";
				//checkAndHoldActiveCall(component, handset, nextStepAutoHold);
				component.voicebridgeLinkService.selectLine(line, getDeviceNo(), nextStepHandset);
			}

		}
		catch(Exception e) {
			Log.error("VoiceBridgeUser - selectDDI error: " + e.toString());
		}
	}

	public void selectDDI(VoiceBridgeComponent component, String ddi, String handset, String privacy, String hold, String dialDigvoicebridge)
	{
		Log.debug("VoiceBridgeUser - selectDDI " + ddi + " " + dialDigvoicebridge + " " + handset + " " + privacy);

		try {
			nextStepPrivacy = privacy == null ? getLastPrivacy() : privacy;
			nextStepAutoHold = hold == null ? autoHold() : "true".equals(hold);

			if (callbackAvailable(component))
			{
				nextStepHandset = getPhoneCallback().getLocalHandset();

			} else {

				handset = handset == null ? getHandsetNo() : handset;
				nextStepHandset = handset;
			}

			if ("0000000000".equals(getDeviceNo()))
			{
				nextStepDDI = ddi;
				nextStepCallSet = null;
				nextStepSpeedial = dialDigvoicebridge;
				nextStepAction = "selectDDI";

				component.voicebridgeLinkService.getUserConsole(getUserNo());

			} else {

				if (dialDigvoicebridge == null || "".equals(dialDigvoicebridge))
				{
					nextStepAction = "privacy";

				} else {

					nextStepSpeedial = dialDigvoicebridge;
					nextStepAction = "speedDial";
				}
				//checkAndHoldActiveCall(component, handset, nextStepAutoHold);
				component.voicebridgeLinkService.selectDDI(ddi, getDeviceNo(), nextStepHandset);
			}

		}
		catch(Exception e) {
			Log.error("VoiceBridgeUser - selectDDI error: " + e.toString());
		}
	}

	public void selectCallset(VoiceBridgeComponent component, String callset, String handset, String privacy, String hold, String dialDigvoicebridge)
	{
		Log.debug("VoiceBridgeUser - selectCallset " + callset + " " + dialDigvoicebridge + " " + handset + " " + privacy);

		try {
			nextStepPrivacy = privacy == null ? getLastPrivacy() : privacy;
			nextStepAutoHold = hold == null ? autoHold() : "true".equals(hold);

			if (callbackAvailable(component))
			{
				nextStepHandset = getPhoneCallback().getLocalHandset();

			} else {

				handset = handset == null ? getHandsetNo() : handset;
				nextStepHandset = handset;
			}

			if ("0000000000".equals(getDeviceNo()))
			{
				nextStepAction = "selectCallset";
				nextStepSpeedial = dialDigvoicebridge;
				nextStepCallSet = callset;
				nextStepDDI = null;

				component.voicebridgeLinkService.getUserConsole(getUserNo());

			} else {

				if (dialDigvoicebridge == null || "".equals(dialDigvoicebridge))
				{
					nextStepAction = "privacy";

				} else {

					nextStepSpeedial = dialDigvoicebridge;
					nextStepAction = "speedDial";
				}

				//checkAndHoldActiveCall(component, handset, nextStepAutoHold);
				component.voicebridgeLinkService.selectCallset(callset, getDeviceNo(), nextStepHandset);
			}

		}
		catch(Exception e) {
			Log.error("VoiceBridgeUser - selectDDI error: " + e.toString());
		}
	}

	private void checkAndHoldActiveCall(VoiceBridgeComponent component, String handset, boolean hold)
	{
		try {

			if ((getCurrentHS1Call() != null && handset.equals("1")) || (getCurrentHS2Call() != null && handset.equals("2")))
			{
				if (hold)
				{
					component.voicebridgeLinkService.holdCall(getDeviceNo(), handset);

				} else {

					component.voicebridgeLinkService.clearCall(getDeviceNo(), handset);
				}

				Thread.sleep(500);
			}
		}
		catch(Exception e) {
			Log.error("VoiceBridgeUser - checkAndHoldActiveCall error: " + e.toString());
		}
	}


    public int compareTo(Object object)
    {
        if (object instanceof VoiceBridgeUser) {
            return getUserId().compareTo(((VoiceBridgeUser)object).getUserId());
        }
        return getClass().getName().compareTo(object.getClass().getName());
    }

}
