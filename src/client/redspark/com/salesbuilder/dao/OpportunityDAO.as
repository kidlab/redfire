package com.salesbuilder.dao
{
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;

	import com.salesbuilder.model.Opportunity;
	import com.salesbuilder.model.Account;
	
	import org.igniterealtime.xiff.core.*;
	import org.igniterealtime.xiff.data.*;
	
	import com.moneyserve.xiff.data.sql.SQLExtension;
	import com.moneyserve.control.ConnectionManager;
	import org.igniterealtime.xiff.data.IQ;
	import flash.xml.XMLNode;	

	public class OpportunityDAO
	{		
		public function OpportunityDAO()
		{
		}
		
		public function getOpportunity(serialId:int, resultHandler:Function, faultHandler:Function = null):void
		{
			sendSQL(1, "select * from transaction where serialno=" + serialId, function (resultIQ:IQ):void
			{
				resultHandler.call(null, getTransferResult(resultIQ));
			});				
		
 		}

		public function getOpportunities(resultHandler:Function, faultHandler:Function = null):void
		{
			sendSQL(1, "select count(*) from transaction", function (resultIQ:IQ):void
			{
				var iqNode:XMLNode = resultIQ.getNode();			
				var children:Array = iqNode.childNodes;

				for (var i:String in children) 
				{						
					if (children[i].nodeName == "moneyserve-sql") 
					{
						var rows:Array = children[i].childNodes;			
						
					   	if (rows[0].nodeName == "row") 
					   	{			   
							var count:Number = Number(rows[0].childNodes[0].firstChild.nodeValue);
							sendSQL(count, "select * from transaction", function (resultIQ:IQ):void
							{
								resultHandler.call(null, getTransferResults(resultIQ));
							});											
						}			   	
					}
				}
			});			
		
		}
		
		public function getOpportunitiesByAccount(accountId:int, resultHandler:Function, faultHandler:Function = null):void
		{
/*		
			sendSQL(32, "select * from transaction where custserialno=" + accountId + "", function (resultIQ:IQ):void
			{
				resultHandler.call(null, getTransferResults(resultIQ));
			});
*/			
			if (accountId == 92) getOpportunities(resultHandler);
		
		}

		public function getOpportunitiesByName(name:String, resultHandler:Function, faultHandler:Function = null):void
		{
			sendSQL(32, "select * from transaction where (sfirstname like '%" + name + "%') or (slastname like '%" + name + "%') or (rfirstname like '%" + name + "%') or (rlastname like '%" + name + "%')", function (resultIQ:IQ):void
			{
				resultHandler.call(null, getTransferResults(resultIQ));
			});		
		}

		public function getChanges(resultHandler:Function, faultHandler:Function = null):void
		{
		}

		public function updateOpportunity(opportunity:Object, resultHandler:Function = null, faultHandler:Function = null):void
		{
		}
		
		public function createOpportunity(opportunity:Object, resultHandler:Function = null, faultHandler:Function = null):void
		{
		}

		public function unflagChanges(resultHandler:Function = null, faultHandler:Function = null):void
		{
		}

		private function sendSQL(size:int, sql:String, resultHandler:Function):void
		{
			var iq:IQ = new IQ(new EscapedJID("moneyserve1." + ConnectionManager.connection.server), IQ.TYPE_GET, XMPPStanza.generateID("get_transfers_"), resultHandler);
			iq.addExtension(new SQLExtension(sql, 0, size));
			ConnectionManager.connection.send( iq ); 						
		}
		
		public function getTransferResults(resultIQ:IQ):ArrayCollection
		{		
			var iqNode:XMLNode = resultIQ.getNode();			
			var children:Array = iqNode.childNodes;
			var transfers:ArrayCollection = new ArrayCollection();			
			
			for (var i:String in children) 
			{						
				if (children[i].nodeName == "moneyserve-sql") 
				{
					var rows:Array = children[i].childNodes;			

					for (var j:String in rows) 
					{			
					   if (rows[j].nodeName == "row") 
					   {			   			   
						var opp:Opportunity = _processTransferRow(rows[j]);

						if (opp.opportunityId > 0)
						{
							transfers.addItem(opp);			
						}			   	
					   }
					}
				}
			}
			return transfers;
		}
		
		public function getTransferResult(resultIQ:IQ):Opportunity
		{		
			var transfers:ArrayCollection = getTransferResults(resultIQ); 
			return transfers[0];
		}
		
		private function _processTransferRow(iqNode:XMLNode):Opportunity 
		{		
			var columns:Array = iqNode.childNodes;
			var opp:Opportunity = new Opportunity();
			var sFirstName:String = "";
			var sLastName:String = "";
			var rFirstName:String = "";
			var rLastName:String = "";
			
			opp.accountId = 92;

			for (var j:String in columns) 
			{
			
				if (columns[j].nodeName.toLowerCase() == "serialno")				
					opp.opportunityId = Number(columns[j].firstChild.nodeValue);

				if (columns[j].nodeName.toLowerCase() == "custserialno" && columns[j].firstChild)				
					opp.accountId = Number(columns[j].firstChild.nodeValue);

				if (columns[j].nodeName.toLowerCase() == "sfirstname" && columns[j].firstChild)
					sFirstName = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "slastname" && columns[j].firstChild)
					sLastName = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "rfirstname" && columns[j].firstChild)
					rFirstName = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "rlastname" && columns[j].firstChild)
					rLastName = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "totalamount" && columns[j].firstChild)
					opp.expectedAmount = Number(columns[j].firstChild.nodeValue);

				if (columns[j].nodeName.toLowerCase() == "transferfee" && columns[j].firstChild)
				{
					opp.probability = Number(columns[j].firstChild.nodeValue);
				}

				if (columns[j].nodeName.toLowerCase() == "transferdate" && columns[j].firstChild)
				{
					var date:String = columns[j].firstChild;
					var year:int = Number(date.substring(0,4));
					var month:int = Number(date.substring(5,7));
					var day:int = Number(date.substring(8,10));
					
					opp.expectedCloseDate = new Date( year, month, day, 0, 0, 0, 0);
				}
					
					
			}
			
			opp.name = sFirstName + " " + sLastName + "=>" + rFirstName + " " + rLastName
			return opp;
		}
		
		public function typeObject(o:Object):Opportunity
		{
			var opp:Opportunity = new Opportunity();
			opp.opportunityId = o.OPPORTUNITY_ID;
			opp.accountId = o.ACCOUNT_ID;
			opp.name = o.NAME;
			opp.owner = o.OWNER;
			opp.owner = o.OWNER;
			opp.expectedCloseDate = new Date(o.EXPECTED_CLOSE_DATE);
			//opp.expectedCloseDate.time = o.EXPECTED_CLOSE_DATE;
			opp.expectedAmount = o.EXPECTED_AMOUNT;
			opp.probability = o.PROBABILITY;
			//opp.status = o.STATUS;
			opp.leadSource = o.LEAD_SOURCE;
			opp.notes = o.NOTES;
			opp.lastUpdated = o.LAST_UPDATED;
			opp.offlineOperation = o.OFFLINE_OPERATION;
			if (o.ACCOUNT_NAME)
			{
				opp.account = new Account();
				opp.account.name = o.ACCOUNT_NAME;
			}
			return opp;
		}
		
	}
}