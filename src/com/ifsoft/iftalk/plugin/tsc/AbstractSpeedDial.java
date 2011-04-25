package com.ifsoft.iftalk.plugin.tsc;

import java.io.*;
import org.apache.log4j.Logger;

public abstract class AbstractSpeedDial
{
    protected Logger Log = Logger.getLogger(getClass().getName());

	private String abstractSDNumber			= null;
	private String abstractSDLabel			= null;
	private String abstractExternalAccess	= null;
	private String abstractCanonicalNumber	= null;
	private String abstractDialableNumber	= null;

	public String getLabel() {
		return abstractSDLabel;
	}

	public void setLabel(String abstractSDLabel) {
		this.abstractSDLabel = abstractSDLabel;
	}

	public String getNumber() {
		return abstractSDNumber;
	}

	public void setNumber(String abstractSDNumber) {
		this.abstractSDNumber = abstractSDNumber;
	}

	public String getExternalAccess() {
		return abstractExternalAccess;
	}

	public void setExternalAccess(String abstractExternalAccess) {
		this.abstractExternalAccess = abstractExternalAccess;
	}

	public String getCanonicalNumber() {
		return abstractCanonicalNumber;
	}

	public void setCanonicalNumber(String abstractCanonicalNumber) {
		this.abstractCanonicalNumber = abstractCanonicalNumber;
	}

	public String getDialableNumber() {
		return abstractDialableNumber;
	}

	public void setDialableNumber(String abstractDialableNumber) {
		this.abstractDialableNumber = abstractDialableNumber;
	}
}

