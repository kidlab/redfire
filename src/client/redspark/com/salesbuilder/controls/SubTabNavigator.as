package com.salesbuilder.controls
{
	import mx.controls.ButtonBar;
	import mx.containers.TabNavigator;
	import flash.events.MouseEvent;

	public class SubTabNavigator extends Repeater
	{
		public function SubTabNavigator()
		{
			addEventListener(MouseEvent.MOUSE_OVER, mouseOverHandler);
			addEventListener(MouseEvent.MOUSE_OUT, mouseOutHandler);
			addEventListener(MouseEvent.MOUSE_DOWN, mouseDownHandler);
		}
		
		private function mouseOverHandler(event:MouseEvent):void
		{
			event.target.setStyle("color", 0xFF0000);
		}
		private function mouseOutHandler(event:MouseEvent):void
		{
			event.target.setStyle("color", 0xFFFF00);
		}
		private function mouseDownHandler(event:MouseEvent):void
		{
			event.target.setStyle("color", 0xFEFEFE);
		}
	}
}