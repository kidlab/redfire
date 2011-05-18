private var rtmpUrl:String = "rtmp:/oflaDemo";
private var rtmfpUrl:String = "rtmfp://p2p.rtmfp.net/e423fa356c187078552b994c-004820ca784f/";		

private var streamKey:String = "KEY";
private var streamMe:String = "ME";
private var nsMe:NetStream;

private var p2pEnabled:Boolean = true;
private var timer:Timer;
		
private var myCam:Camera; 
private var myMic:Microphone;

private var videoPicQuality:int = 0;
private var videoFps:int = 30;
private var videoBandwidth:int = 256000;
private var micSetRate:int = 22;
		
private var nc:NetConnection = null;
private var group:NetGroup = null;
private var groupspec:GroupSpecifier;



private function setupConnection():void
{					
	// Initialise the connection

	nc = new NetConnection();
	nc.client = this;

	nc.addEventListener(NetStatusEvent.NET_STATUS, function (evt:NetStatusEvent ):void 
	{
		logMessage("NC " + evt.info.code);

		switch(evt.info.code) 
		{
			case "NetGroup.Connect.Success":
				connected();									
				break;

			case "NetConnection.Connect.Failed":
			case "NetConnection.Connect.Rejected":

				p2pEnabled = false;
				timer = new Timer(1000);

				timer.addEventListener(TimerEvent.TIMER, function onTimer(evt:TimerEvent):void
				{
					timer.stop();
					nc.connect(rtmpUrl);															
				});

				timer.start();


			case "NetConnection.Connect.Success":

				if (p2pEnabled)
				{
					setupGroup();	

				} else {

					try {
						connected();
					} catch (e:Error) {}
				}
				break;

			default:
		}		    	
	});


	nc.addEventListener(SecurityErrorEvent.SECURITY_ERROR,  function (event:SecurityErrorEvent):void 
	{		    	

	});

			
	// Open streaming connection, first try RTMFP

	nc.connect(rtmfpUrl);
}

private function handleDisconnected():void
{

	nsMe.attachCamera(null);
	nsMe.attachAudio(null);	
	
	nc.close();
}

private function handleConnected():void
{
	logMessage("Enter...connected");

	// Initialise the local camera and microphone source

	myCam = Camera.getCamera()

	if (myCam != null)
	{
		myCam.setMode(videoWidth,videoHeight,videoFps,true);
		myCam.setQuality(videoBandwidth,videoPicQuality);
	}

	myMic = getMicrophone();
	myMic.rate = micSetRate;

	// Initialise the up-stream NetStream

	if (p2pEnabled)
	{
		nsMe = new NetStream(nc, groupspec.groupspecWithAuthorizations());				

	} else {

		nsMe = new NetStream(nc);
	}

	nsMe.bufferTime = 0;		

	nsMe.addEventListener(NetStatusEvent.NET_STATUS, function (evt:NetStatusEvent ):void 
	{	
		logMessage("PUB " + evt.info.code);

		switch(evt.info.code) 
		{
			case "NetStream.Connect.Success":

			case "NetStream.Play.StreamNotFound":
				break;

			case "NetStream.Play.Failed":	
				break;

			case "NetStream.Play.Start":							
				break;

			case "NetStream.Play.Stop":
				break;

			case "NetStream.Buffer.Full":
				break;

			default:
		}		    	
	});


	nsMe.addEventListener(AsyncErrorEvent.ASYNC_ERROR, function (event:AsyncErrorEvent):void 
	{		    	

	});

	if (myCam != null) nsMe.attachCamera(myCam);

	nsMe.attachAudio(myMic);
	nsMe.publish(streamKey + streamMe,"live");			

}


private function setupGroup():void
{
	groupspec = new GroupSpecifier("e423fa356c187078552b994c-004820ca784f-redfire-spark");

	if (rtmfpUrl == "rtmfp:") 
	{
		groupspec.ipMulticastMemberUpdatesEnabled = true;
		groupspec.addIPMulticastAddress("225.225.0.1:30303");

	} else {

		groupspec.serverChannelEnabled = true;				
	}

	groupspec.routingEnabled = true;				
	groupspec.postingEnabled = true;
	groupspec.multicastEnabled = true;

	group = new NetGroup(nc, groupspec.groupspecWithAuthorizations());

	group.addEventListener(NetStatusEvent.NET_STATUS, function (evt:NetStatusEvent ):void 
	{
	    logMessage("GRP " + evt.info.code);

	    switch(evt.info.code) 
	    {
		case "NetGroup.Connect.Success":
			connected();									
			break;


		case "NetGroup.Connect.Failed":
		case "NetGroup.Connect.Rejected":

			p2pEnabled = false;
			nc.connect(rtmpUrl);
			break;

		case "NetGroup.SendTo.Notify":						

			break;

		case "NetGroup.Posting.Notify":

			break;	

		case "NetGroup.Neighbor.Connect":				
			break;

		case "NetGroup.Neighbor.Disconnect":

			break;

		default:

	  }		    	
	});			
} 


private function initVidDisplay(ns:NetStream, vid:Video, uic:VideoObject, streamId:String):void 
{		
	// Configure the stream	

	if (p2pEnabled)
	{
		ns = new NetStream(nc, groupspec.groupspecWithAuthorizations());				

	} else {

		ns = new NetStream(nc);
	}

	var nsClientObj:Object = new Object();

	nsClientObj.onMetaData = function(infoObject:Object):void
	{					
	};

	nsClientObj.onPlayStatus = function(infoObject:Object):void
	{					
	};


	ns.receiveVideo(true);
	ns.bufferTime = 0;		
	ns.client = nsClientObj;

	// Initialise the NetStreams with the new connection

	ns.addEventListener(NetStatusEvent.NET_STATUS, function (evt:NetStatusEvent ):void 
	{	
		logMessage("SUB " + evt.info.code);

		switch(evt.info.code) 
		{
			case "NetStream.Connect.Success":
				break;

			case "NetStream.Play.StreamNotFound":
				break;

			case "NetStream.Play.Failed":	
				break;

			case "NetStream.Play.Start":							
				break;

			case "NetStream.Play.Stop":
				break;

			case "NetStream.Buffer.Full":
				break;

			default:
		}		    	
	});


	ns.addEventListener(AsyncErrorEvent.ASYNC_ERROR, function (event:AsyncErrorEvent):void 
	{		    	

	});


	// Resize the display

	uic.width = videoWidth;
	uic.height = videoHeight;


	if (streamId == null) 
	{
		uic.visible = false;
		return;
	}

	// Resize the video object

	uic.visible = true;			
	vid.width = videoWidth;
	vid.height = videoHeight;

	// Init the component			
	uic.video = vid;
	
	uic.addEventListener("click", function (evt:Event) :void 
	{

		if (streamId == streamMe)
		{	
			if (uic.mic.text == "audio:on" && uic.cam.text == "video:on") {
				uic.mic.text = ("audio:off");
				uic.cam.text = ("video:on");
			} else

			if (uic.mic.text == "audio:off" && uic.cam.text == "video:on") {
				uic.mic.text = ("audio:on");
				uic.cam.text = ("video:off");
			} else

			if (uic.mic.text == "audio:on" && uic.cam.text == "video:off") {
				uic.mic.text = ("audio:off");
				uic.cam.text = ("video:off");
			} else

			if (uic.mic.text == "audio:off" && uic.cam.text == "video:off") {
				uic.mic.text = ("audio:on");
				uic.cam.text = ("video:on");
			}

			if (uic.cam.text == "video:on") 
			{
				if (myCam != null) nsMe.attachCamera(myCam);
			} else {
				nsMe.attachCamera(null);
			}

			if (uic.mic.text == "audio:on") 
			{
				nsMe.attachAudio(myMic);

			} else {
				nsMe.attachAudio(null);				
			}
		} else {

			if (uic.action.text == "") {
				uic.action.text = "stopped";
				ns.play(false);

			} else

			if (uic.action.text == "stopped") {
				uic.action.text = "";
				ns.play(streamKey + streamId);					
			}
		}
	});

	if (streamId == streamMe)
	{			
		// Play my webcam
		if (myCam != null) uic.webcam.attachCamera(myCam);

		uic.caption.text = streamId;
		uic.cam.text = "video:on";
		uic.mic.text = "audio:on";
		uic.action.text = "";					

	} else {

		// Start streaming

		uic.video.attachNetStream(ns);				
		ns.play(streamKey + streamId);

		uic.caption.text = streamId;
		uic.cam.text = "";
		uic.mic.text = "";
		uic.action.text = "";					
	}

	logMessage("Setup play stream " + streamId);
} 



private function init2WayDisplay(ns:NetStream, vid:Video, uic:VideoObject, streamId:String, myVid:Video):void 
{		
	// Configure the stream	

	if (p2pEnabled)
	{
		ns = new NetStream(nc, groupspec.groupspecWithAuthorizations());				

	} else {

		ns = new NetStream(nc);
	}

	var nsClientObj:Object = new Object();

	nsClientObj.onMetaData = function(infoObject:Object):void
	{					
	};

	nsClientObj.onPlayStatus = function(infoObject:Object):void
	{					
	};


	ns.receiveVideo(true);
	ns.bufferTime = 0;		
	ns.client = nsClientObj;

	// Initialise the NetStreams with the new connection

	ns.addEventListener(NetStatusEvent.NET_STATUS, function (evt:NetStatusEvent ):void 
	{	
		logMessage("SUB " + evt.info.code);

		switch(evt.info.code) 
		{
			case "NetStream.Connect.Success":
				break;

			case "NetStream.Play.StreamNotFound":
				break;

			case "NetStream.Play.Failed":	
				break;

			case "NetStream.Play.Start":							
				break;

			case "NetStream.Play.Stop":
				break;

			case "NetStream.Buffer.Full":
				break;

			default:
		}		    	
	});


	ns.addEventListener(AsyncErrorEvent.ASYNC_ERROR, function (event:AsyncErrorEvent):void 
	{		    	

	});


	// Resize the display

	uic.width = videoWidth;
	uic.height = videoHeight;

	// Resize the video object

	uic.visible = true;			
	vid.width = videoWidth;
	vid.height = videoHeight;
	myVid.width = 160;
	myVid.height = 120;
	
	// Init the component	
	
	uic.video = vid;	
	uic.webcam = myVid;

	uic.addEventListener("click", function (evt:Event) :void 
	{
		if (uic.mic.text == "audio:on" && uic.cam.text == "video:on") {
			uic.mic.text = ("audio:off");
			uic.cam.text = ("video:on");
		} else

		if (uic.mic.text == "audio:off" && uic.cam.text == "video:on") {
			uic.mic.text = ("audio:on");
			uic.cam.text = ("video:off");
		} else

		if (uic.mic.text == "audio:on" && uic.cam.text == "video:off") {
			uic.mic.text = ("audio:off");
			uic.cam.text = ("video:off");
		} else

		if (uic.mic.text == "audio:off" && uic.cam.text == "video:off") {
			uic.mic.text = ("audio:on");
			uic.cam.text = ("video:on");
		}

		if (uic.cam.text == "video:on") 
		{
			if (myCam != null) nsMe.attachCamera(myCam);
		} else {
			nsMe.attachCamera(null);
		}

		if (uic.mic.text == "audio:on") 
		{
			nsMe.attachAudio(myMic);

		} else {
			nsMe.attachAudio(null);				
		}	
	});

	// Play my webcam
	
	if (myCam != null) uic.webcam.attachCamera(myCam);

	// Start streaming

	uic.video.attachNetStream(ns);				
	ns.play(streamKey + streamId);

	uic.caption.text = streamId;
	uic.cam.text = "video:on";
	uic.mic.text = "audio:on";
	uic.action.text = "";					


	logMessage("Setup play stream " + streamId);
} 



private function getMicrophone() :Microphone 
{
	var mic:Microphone = Microphone.getEnhancedMicrophone();	
	mic.setUseEchoSuppression(true);
	mic.setLoopBack(false);
	mic.setSilenceLevel(10, 20000);
	mic.gain = 60;
	return mic;
}

private function logMessage(message:String):void
{
	DebugLabel.text = message;
	trace(message);
}

		