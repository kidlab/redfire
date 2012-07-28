package com.ifsoft.redfire.servlets;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.http.HttpServlet;
import org.apache.log4j.Logger;
import com.ifsoft.jmdns.*;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.TaskEngine;

import org.red5.server.webapp.voicebridge.*;
import com.sun.voip.server.*;

public class CumulusServlet extends HttpServlet implements CumulusAudioHandler
{
	private Logger Log = Logger.getLogger(getClass().getName());

    private CumulusThread cumulusThread;
    private ServerSocket serverSocket;
    private Socket clientSocket = null;
    private DataOutputStream out = null;
	private DataInputStream in = null;
	private boolean running = false;
	private int fromCounter = 0;
	private int toCounter = 0;

	public void init()
	{
        try
        {
			Log.info("Init CumulusServlet Starting Cumulus RTMFP Server");

			Thread thread = new Thread(new Runnable() {
				public void run() {
					try {
						startServer();
					}
					catch (Exception e) {
						Log.error(e.getMessage(), e);
					}
				}
			}, "Cumulus Connector");

			thread.start();

			RtmfpParticipant.handler = this;

			String sPort = JiveGlobals.getProperty("httpbind.port.plain", "7070");
			int port = Integer.parseInt(sPort);
			InetAddress addr = InetAddress.getLocalHost();

			Log.info("Init CumulusServlet - reporting jMDNS for " + addr.getHostName() + " on  port " + port);
        }
        catch (Exception e)
        {
            Log.error("CumulusServlet init failed", e);
        }
	}


	public void destroy()
	{
        try
        {
			Log.info("Exit CumulusServlet stopping cumulus connector");

			if (out != null) {
				out.flush();
				out.close();
			}
			if (in != null) {
				in.close();
			}
			if (clientSocket != null) {
				clientSocket.close();
			}

            if (serverSocket != null) {
                serverSocket.close();
            }

			Log.info("Exit CumulusServlet stopping cumulus service");
			running = false;
			cumulusThread.stop();

        }
        catch (Exception e)
        {
            Log.error("CumulusServlet destroy failed", e);
        }
	}

    // ------------------------------------------------------------------------
    //
    // Cumulus Connector Listener
    //
    // ------------------------------------------------------------------------

    private void startServer() throws Exception
    {
        int port = JiveGlobals.getIntProperty("cumulus.port", 5555);

        try {
            // Listen on a specific network interface if it has been set.
            String interfaceName = JiveGlobals.getXMLProperty("network.interface");
            InetAddress bindInterface = null;

            if (interfaceName != null) {
                if (interfaceName.trim().length() > 0) {
                    bindInterface = InetAddress.getByName(interfaceName);
                }
            }
            serverSocket = new ServerSocket(port, -1, bindInterface);

			cumulusThread = new CumulusThread();
			cumulusThread.start(JiveGlobals.getProperty("cumulus.path", JiveGlobals.getProperty("cumulus.path.default")));

            Log.info("Cumulus connector is listening on " + interfaceName + " on port " + port);

        }
        catch (IOException e) {
            Log.error("Could not listen on port: " + port, e);
            return;
        }

		try {
			clientSocket = serverSocket.accept();
			clientSocket.setSoTimeout(0);

			out = new DataOutputStream(clientSocket.getOutputStream());
			in = new DataInputStream(clientSocket.getInputStream());

			TaskEngine.getInstance().scheduleAtFixedRate(new PingTask(), 0, 5000);

			running = true;

			while (running)
			{
				read(in);
			}
		}
		catch (IOException e) {
			if (XMPPServer.getInstance().isShuttingDown()) {
				running = false;
			}
			Log.error(e.getMessage(), e);
		}
		finally {

		}

    }


    private void read(DataInputStream in)
    {
		byte[] msgType = new byte[2];

        try {

			if (in.read(msgType, 0, 2) == 2)
			{
				String msgId = new String(msgType);

				if ("01".equals(msgId)) readAudioData(in);

			}
        }
        catch (Exception e) {
            Log.debug("Exception (read): " + e);
        }
    }

    private void readAudioData(DataInputStream in)
    {
		byte[] msgLen = new byte[4];
		byte[] timestamp = new byte[6];
		byte[] stream = new byte[36];
		byte[] message = new byte[161];

        try {

			if (in.read(msgLen, 0, 4) == 4)
			{
				int msgSize = Integer.parseInt(new String(msgLen));
				int streamSize = msgSize - 46;

				//Log.info("Got Packet size " + msgSize);

				message = new byte[msgSize - 46];

				in.read(timestamp, 0, 6);
				int timeStamp = Integer.parseInt(new String(timestamp));

				in.read(stream, 0, 36);

				if (in.read(message, 0, streamSize) == streamSize)
				{
					processAudioData((new String(stream)).trim(), message, timeStamp);
				}
			}

        }
        catch (Exception e) {
            Log.debug("Exception (readAudioData): " + e);
        }
    }

    private class PingTask extends TimerTask
    {
        @Override
        public void run()
        {
			//Log.info("PingTask " + new Date());

        }
	}


    // ------------------------------------------------------------------------
    //
    // Cumulus Audio Messages Handlers
    //
    // ------------------------------------------------------------------------


    private synchronized void processAudioData(String stream, byte[] audioData, int timestamp)
    {
		if (RTMFPCallAgent.rtmfpReceivers.containsKey(stream))
		{
			RtmfpParticipant rtmfpParticipant = RTMFPCallAgent.rtmfpReceivers.get(stream);
			rtmfpParticipant.push(audioData, (short)timestamp);

			if (fromCounter < 10)
				Log.info("Audio data from Cumulus push to bridge " + stream + " " + timestamp + " " + audioData);

			fromCounter++;

		}
	}


 	public synchronized void handleAudioData(String stream, byte[] audioData, int timestamp)
 	{
        if (out != null)
        {
			try {
				int msgSize = audioData.length;
				String msgLen = String.format("%04d", msgSize);
				String timeStamp = String.format("%06d", timestamp);

				out.write(("01" + msgLen + timeStamp + pad(stream, 36)).getBytes());
				out.write(audioData, 0, audioData.length);
				out.flush();

				if (toCounter < 10)
					Log.info("Audio data to Cumulus " + stream + " " + timestamp + " " + audioData);

				toCounter++;

			} catch (Exception e) {
				Log.error("handleAudioData " + e);
			}
		}

	}

    public void startPublisher(String stream)
    {
		Log.info("startPublisher to Cumulus " + stream);
		fromCounter = 0;
		toCounter = 0;

	}

    public void stopPublisher(String stream)
    {
		Log.info("stopPublisher to Cumulus " + stream);
		fromCounter = 0;
		toCounter = 0;

	}

    public void setRtmpParticipant(RtmpParticipant rtmpParticipant)
    {

	}

    public Red5AudioHandler createRed5AudioHandler()
    {
		return null;
	}

	private String pad(String s, int n)
	{
		return String.format("%1$-" + n + "s", s);
	}

}
