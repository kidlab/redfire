package com.moneyserve.control
{
	import org.igniterealtime.xiff.core.*;
	import org.igniterealtime.xiff.data.*;
	import org.igniterealtime.xiff.events.*;		
	
	public class ConnectionManager
	{
		private static var _connection:XMPPConnection;
		public static var httpServer:String;
		public static var httpPort:String;

		public static function set connection(conn:XMPPConnection):void
		{
			_connection = conn;
			
			_connection.addEventListener(ConnectionSuccessEvent.CONNECT_SUCCESS, function(evt:ConnectionSuccessEvent):void 
			{

			});


			_connection.addEventListener(LoginEvent.LOGIN, function( event:LoginEvent):void 
			{

			});

			_connection.addEventListener(XIFFErrorEvent.XIFF_ERROR, function(event:XIFFErrorEvent):void 
			{

			});

			_connection.addEventListener(DisconnectionEvent.DISCONNECT, function(event:DisconnectionEvent):void 
			{

			});			
		}

		public static function get connection():XMPPConnection
		{
			return _connection;
		}		
	}
}