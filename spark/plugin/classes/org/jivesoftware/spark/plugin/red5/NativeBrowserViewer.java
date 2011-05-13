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

import java.awt.BorderLayout;
import java.net.MalformedURLException;
import java.net.URL;
import org.jdesktop.jdic.browser.*;
import org.jivesoftware.Spark;
import org.jivesoftware.spark.util.log.Log;
import org.jivesoftware.spark.component.browser.*;


class NativeBrowserViewer extends BrowserViewer  implements WebBrowserListener
{
    private WebBrowser browser;
    public static final long serialVersionUID = 24362462L;

    public void initializeBrowser()
    {
        BrowserEngineManager bem = BrowserEngineManager.instance();

        if(Spark.isWindows())
            bem.setActiveEngine(BrowserEngineManager.IE);
        else
            bem.setActiveEngine(BrowserEngineManager.MOZILLA);

        browser = new WebBrowser();
        setLayout(new BorderLayout());
        add(browser, "Center");
        browser.addWebBrowserListener(this);
    }

    public void loadURL(String url)
    {
        try
        {
            browser.setURL(new URL(url));
        }
        catch(MalformedURLException e)
        {
            Log.error(e);
        }
    }

    public void goBack()
    {
        browser.back();
    }

    public void downloadStarted(WebBrowserEvent webbrowserevent)
    {
    }

    public void downloadCompleted(WebBrowserEvent event)
    {
        if(browser == null || browser.getURL() == null)
        {
            return;

        } else {

            String url = browser.getURL().toExternalForm();
            documentLoaded(url);
            return;
        }
    }

    public void downloadProgress(WebBrowserEvent webbrowserevent)
    {
    }

    public void downloadError(WebBrowserEvent webbrowserevent)
    {
    }

    public void documentCompleted(WebBrowserEvent webbrowserevent)
    {
    }

    public void titleChange(WebBrowserEvent webbrowserevent)
    {
    }

    public void statusTextChange(WebBrowserEvent webbrowserevent)
    {
    }

    public void initializationCompleted(WebBrowserEvent webbrowserevent)
    {
    }
}
