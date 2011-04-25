package com.ifsoft.iftalk.plugin.tsc;

import org.apache.log4j.Logger;

public abstract class AbstractUser
{
    protected Logger Log = Logger.getLogger(getClass().getName());

	public abstract void setUserId(String userId);

	public abstract String getUserNo();
}
