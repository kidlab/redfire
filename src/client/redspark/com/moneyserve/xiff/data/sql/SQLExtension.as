package com.moneyserve.xiff.data.sql {
	
	import flash.xml.XMLNode;	
	import org.igniterealtime.xiff.data.Extension;
	import org.igniterealtime.xiff.data.IExtension;
	import org.igniterealtime.xiff.data.ISerializable;
		
	 
	public class SQLExtension extends Extension implements IExtension, ISerializable
	{
		public static var NS:String = "http://moneyserve.com/protocol/sql";
		public static var ELEMENT:String = "moneyserve-sql";
		private var sql:String;
		private var start:String;
		private var count:String;
		
		public function SQLExtension(sql:String, start:int, count:int)
		{
			super(null);
			this.sql = sql;
			this.start = String(start);
			this.count = String(count);			
		}
			
		public function getNS():String
		{
			return SQLExtension.NS;
		}
	
		public function getElementName():String
		{
			return SQLExtension.ELEMENT;
		}
			
		public function serialize( parentNode:XMLNode ):Boolean		
		{
			var xmlNode:XMLNode = new XMLNode(1, ELEMENT);			
			xmlNode.attributes.xmlns = NS;			
		
			var sqlNode:XMLNode = new XMLNode(1, 'sql');
			sqlNode.appendChild(new XMLNode(3, this.sql));			
			xmlNode.appendChild(sqlNode);

			var startNode:XMLNode = new XMLNode(1, 'start');
			startNode.appendChild(new XMLNode(3, this.start));			
			xmlNode.appendChild(startNode);

			var countNode:XMLNode = new XMLNode(1, 'count');
			countNode.appendChild(new XMLNode(3, this.count));			
			xmlNode.appendChild(countNode);

			parentNode.appendChild(xmlNode);			
			return true;
		}
	
		public function deserialize( node:XMLNode ):Boolean
		{
			return true;
	
		}
				
	}
}