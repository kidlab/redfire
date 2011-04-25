package com.ifsoft.iftalk.plugin.voicebridge;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.io.*;

import com.ifsoft.iftalk.plugin.tsc.*;

public class VoiceBridgeInterest extends AbstractInterest  {

	private String interestType 			= "";
	private String interestValue			= "";
	private String callset 					= null;
	private String interestLabel			= "";
	private String siteName					= "";
	private String defaultInterest 			= "false";
	private Map<String, VoiceBridgeUserInterest> voicebridgeUserInterests 	= new HashMap();


//-------------------------------------------------------
//
//
//
//-------------------------------------------------------

	public String getDefault() {
		return defaultInterest;
	}

	public void setDefault(String defaultInterest) {
		this.defaultInterest = defaultInterest;
	}

	public String getInterestId() {
		return interestType + interestValue;
	}

	public String getInterestType() {
		return interestType;
	}

	public void setInterestType(String interestType) {
		this.interestType = interestType;
	}

	public String getInterestLabel() {
		return interestLabel;
	}

	public void setInterestLabel(String interestLabel) {
		this.interestLabel = interestLabel;
	}

	public String getInterestValue() {
		return interestValue;
	}

	public void setInterestValue(String interestValue) {
		this.interestValue = interestValue;
	}

	public String getCallset() {
		return callset;
	}

	public void setCallset(String callset) {
		this.callset = callset;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public VoiceBridgeUserInterest addUserInterest(VoiceBridgeUser voicebridgeUser, String defaultInterest)
	{
		VoiceBridgeUserInterest voicebridgeUserInterest = null;

		if (!voicebridgeUserInterests.containsKey(voicebridgeUser.getUserNo()))
		{
			voicebridgeUserInterest = new VoiceBridgeUserInterest();
			voicebridgeUserInterest.setUser(voicebridgeUser);
			voicebridgeUserInterest.setInterest(this);
			voicebridgeUserInterest.setDefault(defaultInterest);

			this.voicebridgeUserInterests.put(voicebridgeUser.getUserNo(), voicebridgeUserInterest);

		} else {

			voicebridgeUserInterest = voicebridgeUserInterests.get(voicebridgeUser.getUserNo());
		}

		return voicebridgeUserInterest;
	}

	public Map<String, VoiceBridgeUserInterest> getUserInterests()
	{
		return voicebridgeUserInterests;
	}

	public void setInterests(Map<String, VoiceBridgeUserInterest> voicebridgeUserInterests)
	{
		this.voicebridgeUserInterests = voicebridgeUserInterests;
	}
}
