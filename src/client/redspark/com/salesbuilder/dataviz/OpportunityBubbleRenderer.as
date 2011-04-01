package com.salesbuilder.dataviz
{
	import mx.core.IDataRenderer;
	import mx.core.UIComponent;
	import flash.events.MouseEvent;
	import mx.graphics.IStroke;
	import flash.display.Graphics;
	import flash.geom.Point;
	import mx.events.FlexEvent;
	import mx.charts.series.BubbleSeries;
	import mx.charts.BubbleChart;
	import mx.charts.DateTimeAxis;
	import mx.charts.LinearAxis;
	import mx.charts.chartClasses.CartesianTransform;
	import mx.charts.chartClasses.CartesianChart;
	import com.salesbuilder.dao.OpportunityDAO;
	import com.salesbuilder.model.Opportunity;
	import com.salesbuilder.control.TabManager;

	public class OpportunityBubbleRenderer extends UIComponent implements IDataRenderer
	{
		private var stageX:Number;
		private var stageY:Number;
		private var dataX:Number;
		private var dataY:Number;
		
		private var hAxis:DateTimeAxis;
		private var vAxis:LinearAxis;
		
		private var hMin:Number;
		private var hMax:Number;
		private var vMin:Number;
		private var vMax:Number;

		private var _over:Boolean = false;
		
		private var _data:Object;
	    
		public function OpportunityBubbleRenderer()
		{
			super();
			doubleClickEnabled = true;
			addEventListener(MouseEvent.ROLL_OVER, rollOverHandler);
			addEventListener(MouseEvent.ROLL_OUT, rollOutHandler);
			addEventListener(MouseEvent.DOUBLE_CLICK, openOpportunity);
			addEventListener(MouseEvent.MOUSE_DOWN, startMoving);
		}
		
	    [Bindable("dataChange")]
	    public function get data():Object 
	    {
	        return _data;
	    }
	    
	    public function set data(value:Object):void 
	    {
	        _data = value;
	        dispatchEvent(new FlexEvent(FlexEvent.DATA_CHANGE));
	    }
		
		private function rollOverHandler(e:MouseEvent):void
		{
			_over = true;
			invalidateDisplayList();						
		}
		
		private function rollOutHandler(e:MouseEvent):void
		{
			_over = false;
			invalidateDisplayList();
		}

		private function startMoving(event:MouseEvent):void
		{
			hAxis = DateTimeAxis(BubbleSeries(parent).getAxis(CartesianTransform.HORIZONTAL_AXIS));
			vAxis = LinearAxis(BubbleSeries(parent).getAxis(CartesianTransform.VERTICAL_AXIS));
			hMin = hAxis.minimum.time;
			hMax = hAxis.maximum.time;
			vMin = vAxis.minimum;
			vMax = vAxis.maximum;

			stageX = event.stageX;
			stageY = event.stageY;

			//dataX = _data.x;
			//dataY = _data.y;
			dataX = _data.item.expectedCloseDate.time;
			dataY = _data.item.probability;

			systemManager.addEventListener(MouseEvent.MOUSE_MOVE, moving, true);
			systemManager.addEventListener(MouseEvent.MOUSE_UP, stopMoving, true);
		}

		private function moving(event:MouseEvent):void
		{
			var expectedCloseDate:Number = dataX + (event.stageX - stageX) * (hMax - hMin) / parent.width;
			if (expectedCloseDate >= hMin && expectedCloseDate <= hMax)
			{
				_data.item.expectedCloseDate = new Date(expectedCloseDate);
			}

			var probability:Number = Math.round(dataY + (stageY - event.stageY) * 100 / parent.height);
			if (probability >= vMin && probability <= vMax)
			{
				_data.item.probability = probability;
			}

			/*
			var mouseData:Array = _data.element.localToData(new Point(dataX + event.stageX - stageX, dataY + event.stageY - stageY)); 
			_data.item.expectedCloseDate=new Date(mouseData[0]);
			_data.item.probability=mouseData[1];
			*/
			
			document.calculatePipeline();
		}
		
		private function stopMoving(event:MouseEvent):void
		{
			systemManager.removeEventListener(MouseEvent.MOUSE_MOVE, moving, true);
			systemManager.removeEventListener(MouseEvent.MOUSE_UP, stopMoving, true);
			
			var dao:OpportunityDAO = new OpportunityDAO();
			_data.item.offlineOperation = "UPDATE"
			dao.updateOpportunity(_data.item);
		}

    	private function openOpportunity(event:MouseEvent):void
		{
			TabManager.openOpportunity(_data.item as Opportunity);
		}
		
		override protected function updateDisplayList(unscaledWidth:Number, unscaledHeight:Number):void
		{
			super.updateDisplayList(unscaledWidth, unscaledHeight);

			var fill:Number;

			/*
			if (_data.item.status==1)
				fill = 0xFF0000;
			else if (_data.item.status==2)
				fill = 0xFF9900;
			else if (_data.item.status==3)
				fill = 0x009900;
			else
				fill = 0xD9D9D9;
			*/
			
			fill = 0xC19021;
			
			var stroke:IStroke = getStyle("stroke");
					
			var w:Number = stroke ? stroke.weight / 2 : 0;
	
			var g:Graphics = graphics;
			g.clear();		
			if (stroke)
			{
				stroke.apply(g);
			}
			g.beginFill(fill, _over ? 1 : 0.8);
			g.drawCircle(unscaledWidth / 2, unscaledHeight / 2, unscaledWidth / 2 - w);
			g.endFill();
		}

	}
}