package com.salesbuilder.util
{
	import com.adobe.images.JPGEncoder;
	
	import flash.display.Bitmap;
	import flash.display.BitmapData;
	import flash.display.DisplayObject;
	import flash.display.Loader;
	import flash.events.Event;
	import flash.events.MouseEvent;
	import flash.net.URLRequest;
	import flash.utils.ByteArray;
	
	import mx.formatters.DateFormatter;
	
	public class DragBitmap
	{
		private var _handle:DisplayObject;
		
		public var data:DisplayObject;
		
		private var dateFormatter:DateFormatter;
		
		public var dragIcon:BitmapData;
		
		public function DragBitmap()
		{
			var loader:Loader = new Loader();
            loader.contentLoaderInfo.addEventListener(Event.COMPLETE, 
				function ():void
				{
					dragIcon = Bitmap(loader.content).bitmapData;
				}
			);
			loader.load(new URLRequest("assets/icon_image.png"));
			dateFormatter = new DateFormatter();
			dateFormatter.formatString = "YYYY-MM-DD-HH-NN-SS";
		} 
		
		public function set handle(handle:DisplayObject):void
		{
			_handle = handle;
			_handle.addEventListener(MouseEvent.MOUSE_MOVE, startDragging);
		}
		
		private function startDragging(event:MouseEvent):void
		{
			
		}
			
		private function getBitmapData():BitmapData
		{
			var bd:BitmapData = new BitmapData(data.width, data.height);
			bd.draw(data);
			return bd;
		}


		private function encodeJPG():ByteArray
		{
			var jpgEncoder:JPGEncoder = new JPGEncoder();
			var bytes:ByteArray = jpgEncoder.encode(getBitmapData());
			return bytes;
		}
		
	}
}