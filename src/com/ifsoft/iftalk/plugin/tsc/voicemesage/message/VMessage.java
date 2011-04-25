package com.ifsoft.iftalk.plugin.tsc.voicemessage.message;

import java.sql.Timestamp;

public class VMessage
{

    private int id;
    private String name;
    private Timestamp creationDate;
    private Timestamp modificationDate;
    private String comment;
    private String path;
    private boolean archived;

    public VMessage(int id, String name, String comment, String path, Timestamp creationDate, Timestamp modifactionDate, boolean archived)
    {
        this.id = -1;
        this.archived = false;

        if(creationDate == null || modifactionDate == null || path == null)
        {
            throw new IllegalArgumentException("Inputs invalid, data and path must be non-null");

        } else {
            this.id = id;
            this.name = name;
            this.comment = comment;
            this.path = path;
            this.creationDate = creationDate;
            modificationDate = modifactionDate;
            this.archived = archived;
            return;
        }
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
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

    public Timestamp getCreationDate()
    {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate)
    {
        this.creationDate = creationDate;
    }

    public Timestamp getModificationDate()
    {
        return modificationDate;
    }

    public void setModificationDate(Timestamp modificationDate)
    {
        this.modificationDate = modificationDate;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public boolean isArchived()
    {
        return archived;
    }

    public void setArchived(boolean archived)
    {
        this.archived = archived;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Message");
        sb.append("{id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", creationDate=").append(creationDate);
        sb.append(", modificationDate=").append(modificationDate);
        sb.append(", comment='").append(comment).append('\'');
        sb.append(", path='").append(path).append('\'');
        sb.append(", archived=").append(archived);
        sb.append('}');
        return sb.toString();
    }

}
