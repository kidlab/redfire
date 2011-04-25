package com.ifsoft.iftalk.plugin.tsc.voicemessage.user;

import java.util.Collection;
import java.util.Iterator;

public class VUser
{

    private String id;
    private String name;
    private boolean disabled;


    public VUser()
    {
        disabled = false;
    }

    public VUser(String id, String name, boolean disabled)
    {
        this();
		this.id = id;
		this.name = name;
		this.disabled = disabled;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }


}
