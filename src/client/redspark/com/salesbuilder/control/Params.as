package com.salesbuilder.control
{
	import mx.rpc.http.HTTPService;
	import mx.rpc.AsyncToken;
	import mx.rpc.Responder;
	import mx.rpc.events.ResultEvent;
	import mx.rpc.events.FaultEvent;
	import mx.controls.Alert;
	import mx.collections.ArrayCollection;
	
	public class Params
	{
		[Bindable]
		public var states:ArrayCollection;
		
		public static var instance:Params;
		
		[Bindable]
		public var accountTypes:ArrayCollection = new ArrayCollection([
			{data: "S", label:"Sender"}, 
			{data: "R", label:"Reciever"}]); 

		[Bindable]
		public var industries:ArrayCollection = new ArrayCollection([
			{data: 0, label:"Select"}, 
			{data: 7, label:"Aerospace"}, 
			{data: 5, label:"Airlines"}, 
			{data: 10, label:"Automobile"}, 
			{data: 4, label:"Biotechnology"}, 
			{data: 2, label:"Computer Hardware"},
			{data: 1, label:"Computer Software"},
			{data: 11, label:"Conglomerates"},
			{data: 8, label:"Financial Services"},
			{data: 9, label:"Food and Beverage"},
			{data: 3, label:"Government"},
			{data: 6, label:"Telecom"}
			]);

		[Bindable]
		public var owners:ArrayCollection = new ArrayCollection([
			{data: 0, label:"Select"}, 
			{data: 1, label:"Dolapo Ogbechie"},
			{data: 2, label:"Didi Ogbechie"}]); 
		
		public function Params()
		{
			var srv:HTTPService = new HTTPService();
			srv.url = "data/states.xml";
			var token:AsyncToken = srv.send();
			token.addResponder(new Responder(
				function (event:ResultEvent):void
				{
					states = event.result.states.state;
				},
				function (event:FaultEvent):void
				{
					Alert.show(event.fault.faultString);
				}
			));
			instance = this;
		}

	}
}