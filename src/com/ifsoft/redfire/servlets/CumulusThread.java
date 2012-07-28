package com.ifsoft.redfire.servlets;

import java.net.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

public class CumulusThread implements Runnable {

	protected Logger Log = Logger.getLogger(getClass().getName());

	private Thread thread = null;
	private Process cumulusProcess = null;
	private BufferedReader input = null;


	public CumulusThread()
	{

	}


	public void start(String path) {

		stopThread();

		try {
			cumulusProcess = Runtime.getRuntime().exec(path);
			Log.info("Started Cumulus");

      		input = new BufferedReader (new InputStreamReader(cumulusProcess.getInputStream()));
			Log.info("Started Cumulus Reader");

		} catch (Exception e) {
			Log.info("Started Cumulus exception " + e);
		}


		// All ok: start a receiver thread
		thread = new Thread(this);
		thread.start();
	}

	public void run() {
		Log.info("Start run()");

		// Get events while we're alive.
		while (thread != null && thread.isAlive()) {

			try {

				String line = input.readLine();

			  	while (line != null) {
					Log.debug("Console: " + line);
					line = input.readLine();
			  	}

     		  	Thread.sleep(500);

			} catch (Throwable t) {

			}

		}
	}

	public void stop() {

		Log.info("Stopped Cumulus");

		cumulusProcess.destroy();
		stopThread();
	}

	public void stopThread() {
		Log.info("In stopThread()");

		// Keep a reference such that we can kill it from here.
		Thread targetThread = thread;

		thread = null;

		// This should stop the main loop for this thread.
		// Killing a thread on a blcing read is tricky.
		// See also http://gee.cs.oswego.edu/dl/cpj/cancel.html
		if ((targetThread != null) && targetThread.isAlive()) {

			targetThread.interrupt();

			try {

				// Wait for it to die
				targetThread.join(500);
			}
			catch (InterruptedException ignore) {
			}

			// If current thread refuses to die,
			// take more rigorous methods.
			if (targetThread.isAlive()) {

				// Not preferred but may be needed
				// to stop during a blocking read.
				targetThread.stop();

				// Wait for it to die
				try {
					targetThread.join(500);
				}
				catch (InterruptedException ignore) {
				}
			}

			Log.info("Stopped thread alive=" + targetThread.isAlive());

		}
	}

}
