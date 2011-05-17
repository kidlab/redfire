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

import java.util.*;
import org.jivesoftware.smack.packet.*;

public class RedfireExtension implements PacketExtension {

    public static final String elementName = "redfire-invite";
    public static final String namespace = "http://redfire.4ng.net/xmlns/redfire-invite";

    private String sessionID;


    public String getElementName() {
        return elementName;
    }

    public String getNamespace() {
        return namespace;
    }

    private Map<String, String> map;


    public String toXML() {
        StringBuffer buf = new StringBuffer();

        buf.append("<").append(elementName).append(" xmlns=\"").append(namespace).append("\">");

        for (Iterator i=getNames(); i.hasNext(); ) {
            String name = (String)i.next();
            String value = getValue(name);

            buf.append("<").append(name).append(">").append(value).append("</").append(name).append(">");
        }

        buf.append("</").append(elementName).append(">");
        return buf.toString();
    }

    public synchronized Iterator getNames() {

        if (map == null) {
            return Collections.EMPTY_LIST.iterator();
        }
        return Collections.unmodifiableMap(new HashMap<String, String>(map)).keySet().iterator();
    }


    public synchronized String getValue(String name) {

        if (map == null) {
            return null;
        }
        return map.get(name);
    }

    public synchronized void setValue(String name, String value) {

        if (map == null) {
            map = new HashMap <String, String>();
        }

        map.put(name, value);
    }
}