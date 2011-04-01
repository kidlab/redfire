package com.salesbuilder.dao
{
    	import mx.collections.ArrayCollection;
    	import mx.controls.Alert;
    	import com.salesbuilder.model.Account;

	import org.igniterealtime.xiff.core.*;
	import org.igniterealtime.xiff.data.*;
	
	import com.moneyserve.xiff.data.sql.SQLExtension;
	import com.moneyserve.control.ConnectionManager;
	import org.igniterealtime.xiff.data.IQ;
	import flash.xml.XMLNode;	
	
	public class AccountDAO
	{
		public function AccountDAO()
		{
		}
		
		public function getAccount(accountId:int, resultHandler:Function, faultHandler:Function = null):void
		{
			sendSQL(1, "select * from clients where serialno=" + accountId + "", function (resultIQ:IQ):void
			{
				resultHandler.call(null, getAccountResult(resultIQ));
			});				
		}

		public function getAccounts(resultHandler:Function, faultHandler:Function = null):void
		{			
			sendSQL(1, "select count(*) from clients", function (resultIQ:IQ):void
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
							sendSQL(count, "select * from clients", function (resultIQ:IQ):void
							{
								resultHandler.call(null, getAccountResults(resultIQ));
							});											
						}			   	
					}
				}
			});			
		}

		public function getTopAccounts(size:int, resultHandler:Function, faultHandler:Function = null):void
		{
			sendSQL(size, "select * from clients", function (resultIQ:IQ):void
			{
				resultHandler.call(null, getAccountResults(resultIQ));
			});							
		}

		public function getAccountsByName(name:String, resultHandler:Function, faultHandler:Function = null):void
		{
			sendSQL(32, "select * from clients where (firstname like '%" + name + "%') or (lastname like '%" + name + "%')", function (resultIQ:IQ):void
			{
				resultHandler.call(null, getAccountResults(resultIQ));
			});		
		}

		public function getChanges(resultHandler:Function, faultHandler:Function = null):void
		{
		}

		public function unflagChanges(resultHandler:Function = null, faultHandler:Function = null):void
		{
 		}
		
		public function updateAccount(account:Object, resultHandler:Function = null, faultHandler:Function = null):void
		{
		}
		
		public function createAccount(account:Object, resultHandler:Function = null, faultHandler:Function = null):void
		{
			sendSQL(0, "INSERT INTO CLIENTS (FIRSTNAME, CUSTOMERTYPE, TELEPHONE, EMAIL, STREET, CITY, STATE, ZIPCODE) VALUES ('" + account.name + "','" + account.type + "','" + account.phone + "','" + account.email + "','" + account.address1 + "','" + account.city + "','" + account.state + "','" + account.zip + "')", null);					
		}
		
		private function sendSQL(size:int, sql:String, resultHandler:Function):void
		{
			var iq:IQ = new IQ(new EscapedJID("moneyserve1." + ConnectionManager.connection.server), IQ.TYPE_GET, XMPPStanza.generateID("get_accounts_"), resultHandler);
			iq.addExtension(new SQLExtension(sql, 0, size));
			ConnectionManager.connection.send( iq ); 						
		}
		
		public function getAccountResults(resultIQ:IQ):ArrayCollection
		{		
			var iqNode:XMLNode = resultIQ.getNode();			
			var children:Array = iqNode.childNodes;
			var accounts:ArrayCollection = new ArrayCollection();			
			
			for (var i:String in children) 
			{						
				if (children[i].nodeName == "moneyserve-sql") 
				{
					var rows:Array = children[i].childNodes;			

					for (var j:String in rows) 
					{			
					   if (rows[j].nodeName == "row") 
					   {			   			   
						var account:Account = _processAccountRow(rows[j]);

						if (account.accountId > 0)
						{
							accounts.addItem(account);			
						}			   	
					   }
					}
				}
			}
			return accounts;
		}
		
		public function getAccountResult(resultIQ:IQ):Account
		{		
			var accounts:ArrayCollection = getAccountResults(resultIQ); 
			return accounts[0];
		}
		
		private function _processAccountRow(iqNode:XMLNode):Account 
		{		
			var columns:Array = iqNode.childNodes;
			var account:Account = new Account();
			var firstName:String = "";
			var lastName:String = "";
			account.accountId = 0;
			
			for (var j:String in columns) 
			{
				if (columns[j].nodeName.toLowerCase() == "serialno")
					account.accountId = Number(columns[j].firstChild.nodeValue);

				if (columns[j].nodeName.toLowerCase() == "firstname")
					firstName = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "lastname")
					lastName = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "city" && columns[j].firstChild)
					account.city = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "telephone" && columns[j].firstChild)
					account.phone = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "street" && columns[j].firstChild)
					account.address1 = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "city" && columns[j].firstChild)
					account.city = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "zipcode" && columns[j].firstChild)
					account.zip = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "state" && columns[j].firstChild)
					account.state = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "customertype" && columns[j].firstChild)
					account.type = columns[j].firstChild.nodeValue;
				
				if (columns[j].nodeName.toLowerCase() == "email" && columns[j].firstChild)
					account.email = columns[j].firstChild.nodeValue;
				
					
			}
			
			account.name = firstName + " " + lastName;
			account.currentYearResults = 50513;
			account.lastYearResults =  28470;
			
			return account;
		}		

		private function processRow(o:Object):Object
		{
			var a:Account = new Account();
			a.accountId = o.ACCOUNT_ID;
			a.annualRevenue = o.ANNUAL_REVENUE;
			a.address1 = o.ADDRESS1;
			a.address2 = o.ADDRESS2;
			a.city = o.CITY;
			a.state = o.STATE;
			a.zip = o.ZIP;
			a.fax = o.FAX;
			a.industry = o.INDUSTRY;
			a.lastUpdated = o.LAST_UPDATED;
			a.name = o.NAME;
			a.notes = o.NOTES;
			a.numberEmployees = o.NUMBER_EMPLOYEES;
			a.owner = o.OWNER;
			a.ownership = o.OWNERSHIP;
			a.phone = o.PHONE;
			a.priority = o.PRIORITY;
			a.rating = o.RATING;
			a.url = o.URL;
			a.ticker = o.TICKER;
			a.type = o.TYPE;
			a.currentYearResults = o.CURRENT_YEAR_RESULTS;
			a.lastYearResults = o.LAST_YEAR_RESULTS;
			a.offlineOperation = o.OFFLINE_OPERATION;
			return a;
		}

	}
}