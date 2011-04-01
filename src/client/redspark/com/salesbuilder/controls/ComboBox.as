package com.salesbuilder.controls
{
	import mx.controls.ComboBox;
	
	public class ComboBox extends mx.controls.ComboBox
	{
		private var _value:Object;
		
		public var valueField:String = "data";
	
		public function set value(value:Object):void 
		{
			_value = value;
	   		if (dataProvider && dataProvider.length > 0)
	   		{
	   			selectIndex();	
	   		}
		}
	
		override public function set dataProvider(dataProvider:Object):void 
	   	{
			super.dataProvider = dataProvider;
			if (_value)
			{
				selectIndex();
			}
	   	}
	   	
	   	private function selectIndex():void
	   	{
			for (var i:int = 0; i < dataProvider.length; i++) 
			{
				if (_value == dataProvider[i][valueField])
				{
					selectedIndex = i;
					return;
				}
			}
	   	}
	  
		override protected function createChildren():void
		{
			super.createChildren();
			height = 29;
		}
	   	
		override protected function updateDisplayList(unscaledWidth:Number, unscaledHeight:Number):void
		{
			super.updateDisplayList(unscaledWidth, unscaledHeight);
			textInput.y = 5;
		}

	}
}