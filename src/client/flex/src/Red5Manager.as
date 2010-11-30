package {
	
	import com.adobe.crypto.MD5;
	import flash.events.AsyncErrorEvent;
	import flash.events.NetStatusEvent;
	import flash.events.SecurityErrorEvent;
	import flash.external.*;
	import flash.net.NetConnection;
	import flash.net.NetStream;
	
	public class Red5Manager {
			
		[Bindable]
		public  var netConnection:NetConnection = null;
		private var incomingNetStream:NetStream = null;
		private var outgoingNetStream:NetStream = null;
		private var phone:String;
		private var uid:String;
		private var username:String;
		private var password:String;
		private var red5Url:String;
		private var sipRealm:String;
		private var sipServer:String; 
		private var conference:String; 
		private var obproxy:String;
		private var useEncryptedPwd:Boolean = false;
		
		private var isConnected:Boolean = false;
		
		public function Red5Manager(obproxy:String, uid:String, phone:String, username:String, password:String, red5Url:String, sipRealm:String, sipServer:String, conference:String) {	
			this.uid = uid;
			this.phone     = phone;
			this.username  = username;
			this.password  = password;
			this.red5Url   = red5Url;
			this.sipRealm  = sipRealm;
			this.sipServer = sipServer
			this.conference   = conference;
			this.obproxy   = obproxy;
			this.init();
		}
		
		public function setUseEncryptedPwd(useEncryptedPwd:Boolean):void {
			this.useEncryptedPwd = useEncryptedPwd;
		}
		
		private function init():void {
			
			NetConnection.defaultObjectEncoding = flash.net.ObjectEncoding.AMF0;	
			netConnection = new NetConnection();
			netConnection.client = this;
			netConnection.addEventListener( NetStatusEvent.NET_STATUS , netStatus );
			netConnection.addEventListener(SecurityErrorEvent.SECURITY_ERROR, securityErrorHandler);
		}
		
		public function connectRed5():void {
			netConnection.connect(red5Url);
		}
		
		public function closeNetConnection():void {
			netConnection.close();
		}
		
		private function netStatus (evt:NetStatusEvent ):void {		 

			switch(evt.info.code) {
				
				case "NetConnection.Connect.Success":
					dispatchEvent (new Red5MessageEvent(Red5MessageEvent.MESSAGE, Red5MessageEvent.NETSTAUS,  'Connection success'));
					this.doOpen();									
					break;
		
				case "NetConnection.Connect.Failed":
					dispatchEvent (new Red5MessageEvent(Red5MessageEvent.MESSAGE, Red5MessageEvent.NETSTAUS,  'Failed to connect to Red5'));
					break;
					
				case "NetConnection.Connect.Closed":
					dispatchEvent (new Red5MessageEvent(Red5MessageEvent.MESSAGE, Red5MessageEvent.NETSTAUS,  'Failed to connect to Red5'));
					break;
		
				case "NetConnection.Connect.Rejected":
					dispatchEvent (new Red5MessageEvent(Red5MessageEvent.MESSAGE, Red5MessageEvent.NETSTAUS,  'Connection Rejected'));
					break;
		
				case "NetStream.Play.StreamNotFound":
					break;
		
				case "NetStream.Play.Failed":
					this.doStreamStatus("failed");
					break;
					
				case "NetStream.Play.Start":	
					this.doStreamStatus("start");	
					break;
					
				case "NetStream.Play.Stop":			
					this.doStreamStatus("stop");	
					break;
					
				case "NetStream.Buffer.Full":
					break;
					
				default:
					
			}			 
		} 
		
		private function asyncErrorHandler(event:AsyncErrorEvent):void {
           trace("AsyncErrorEvent: " + event);
        }
		
		private function securityErrorHandler(event:SecurityErrorEvent):void {
            trace("securityErrorHandler: " + event);
        }
        
     
        
        //********************************************************************************************
		//			
		//			CallBack Methods from Red5 
		//
		//********************************************************************************************

		public function registrationSucess(msg:String):* {
			dispatchEvent (new Red5MessageEvent(Red5MessageEvent.MESSAGE, Red5MessageEvent.SIP_REGISTER,  "SUCCESS"));
		}
	
		public function registrationFailure(msg:String):* {
			dispatchEvent (new Red5MessageEvent(Red5MessageEvent.MESSAGE, Red5MessageEvent.SIP_REGISTER,  msg));
		}
		
		public function callState(msg:String):* {
			trace("RED5Manager callState " + msg);
			dispatchEvent (new Red5MessageEvent(Red5MessageEvent.MESSAGE, Red5MessageEvent.CALLSTATE,  msg));
			
			if (msg == "onUaCallClosed" ||  msg == "onUaCallFailed" || msg == "onUaCallTrasferred") {
				dispatchEvent (new CallDisconnectedEvent(CallDisconnectedEvent.DISCONNECTED,  msg));
				isConnected = false;
			}
			//missed call
			if (msg == "onUaCallCancelled") {
				dispatchEvent (new MissedCallEvent(MissedCallEvent.CALLMISSED,  msg));
				isConnected = false;
				//if (incomingCall) {
					//SipCallWindow.close();
					//SipMissedCallWindow.show(displayName, incomingURL, "SIP Phone: Missed Call");
				//}
			}
		}
		
		public function incoming(source:String, sourceName:String, destination:String, destinationName:String):*  {		
			dispatchEvent (new IncomingCallEvent(IncomingCallEvent.INCOMING, source,  sourceName, destination, destinationName ));
		}
        
                public function connected(publishName:String, playName:String):* {
			dispatchEvent (new CallConnectedEvent(CallConnectedEvent.CONNECTED, publishName,  playName));
			isConnected = true;
		}
		
		public function mailBoxStatus(isWaitting:Boolean, newMessage:String, oldMessage:String):* {

			dispatchEvent (new MailBoxStatusEvent(MailBoxStatusEvent.MAIBOXSTATUS, isWaitting,  newMessage, oldMessage));

		}
		
		public function mailBoxCount(newMessage:String, oldMessage:String):* {

			dispatchEvent (new MailBoxCountEvent(MailBoxCountEvent.MAIBOXCOUNT, newMessage,  oldMessage));

		}
		
		
		//********************************************************************************************
		//			
		//			SIP Actions
		//
		//********************************************************************************************
		
		
		public function doOpen():void {
			if (useEncryptedPwd) {
				netConnection.call("initialize", null, uid, username, getA1ParamMD5());
				netConnection.call("open", null, obproxy, uid, phone, username, "", sipRealm, sipServer);
			}
			else {
				netConnection.call("open", null, obproxy, uid, phone, username, password, sipRealm, sipServer);
			}
		}
		
		public function doCall(dialStr:String):void {
			netConnection.call("call", null, uid, dialStr);
		}
		public function doTransfer(transferTo:String):void {
			netConnection.call("transfer", null, uid, transferTo);
		}
		
		public function joinConf():void {
			netConnection.call("call", null, uid, this.conference);
		}

		public function addToConf():void {
			netConnection.call("transfer", null, uid, this.conference);
		}
		
		public function doCallChar(chr:String):void {
			if (isConnected) {
				netConnection.call("dtmf", null, uid, chr);
			}
		}
		
		public function doHangUp():void {
			netConnection.call("hangup", null, uid);
			if (isConnected) {
				isConnected = false;
			}
		}
		
		public function doAccept():void {
			netConnection.call("accept", null, uid);			
		}
		
		public function doStreamStatus(status:String):void {
			netConnection.call("streamStatus", null, uid, status);	
		}
		
		public function doClose1():void {
			netConnection.call("unregister", null, uid);	
		}
		
		//********************************************************************************************
		//			
		//			Asterisk Manager Actions
		//
		//********************************************************************************************
		
		public function doMialBoxStatus():void {

			//netConnection.call("vmStatus", null);			

		}
		
		public function doMailBoxCount():void {

			//netConnection.call("vmCount", null;			
		}
		
		/** Calculates A1 value.
         *  A1 = username ":" realm ":" passwd
         */
		public function getA1ParamMD5():String {
		    var A1Param:String;
		    A1Param = username + ":" + sipRealm + ":" + password;

		    return MD5.hash(A1Param);
		}
	}
}