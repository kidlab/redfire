package com.salesbuilder.dao
{
    	import mx.collections.ArrayCollection;
    	import mx.controls.Alert;
    	import mx.utils.ObjectUtil;
    	import com.salesbuilder.model.Contact;

	import org.igniterealtime.xiff.core.*;
	import org.igniterealtime.xiff.data.*;
	
	import com.moneyserve.xiff.data.sql.SQLExtension;
	import com.moneyserve.control.ConnectionManager;
	import org.igniterealtime.xiff.data.IQ;
	import flash.xml.XMLNode;	
	
	public class ContactDAO
	{		
		public function ContactDAO()
		{

		}
		
		public function getContact(contactId:int, resultHandler:Function, faultHandler:Function = null):void
		{
			sendSQL(1, "select * from contacts where serialno=" + contactId + "", function (resultIQ:IQ):void
			{
				resultHandler.call(null, getContactResult(resultIQ));
			});				

		}
		
		public function getContacts(resultHandler:Function, faultHandler:Function = null):void
		{
			sendSQL(1, "select count(*) from contacts", function (resultIQ:IQ):void
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
							sendSQL(count, "select * from contacts", function (resultIQ:IQ):void
							{
								resultHandler.call(null, getContactResults(resultIQ));
							});											
						}			   	
					}
				}
			});			

		}
		
		public function getContactsByAccount(accountId:int, resultHandler:Function, faultHandler:Function = null):void
		{
			sendSQL(32, "select * from contacts where custserialno=" + accountId + "", function (resultIQ:IQ):void
			{
				resultHandler.call(null, getContactResults(resultIQ));
			});				

		}

		public function getContactsByName(name:String, resultHandler:Function, faultHandler:Function = null):void
		{
			sendSQL(32, "select * from contacts where (firstname like '%" + name + "%') or (lastname like '%" + name + "%')", function (resultIQ:IQ):void
			{
				resultHandler.call(null, getContactResults(resultIQ));
			});		

		}

		public function getChanges(resultHandler:Function, faultHandler:Function = null):void
		{

		}

		public function updateContact(contact:Object, resultHandler:Function = null, faultHandler:Function = null):void
		{

		}
		
		public function createContact(contact:Object, resultHandler:Function = null, faultHandler:Function = null):void
		{
			sendSQL(0, "INSERT INTO CONTACTS (CUSTSERIALNO, MGRSERIALNO, FIRSTNAME, LASTNAME, STREET, CITY, STATE, ZIPCODE, TELEPHONE, CELLPHONE, EMAIL) VALUES (" + contact.accountId + "," + contact.managerId + ",'" + contact.firstName + "','" + contact.lastName + "','" + contact.address1 + "','" + contact.city + "','" + contact.state + "','" + contact.zip + "','" + contact.officePhone + "','" + contact.cellPhone + "','" + contact.email + "')", null);					
		}

		public function unflagChanges(resultHandler:Function = null, faultHandler:Function = null):void
		{

		}

		private function sendSQL(size:int, sql:String, resultHandler:Function):void
		{
			var iq:IQ = new IQ(new EscapedJID("moneyserve1." + ConnectionManager.connection.server), IQ.TYPE_GET, XMPPStanza.generateID("get_contacts_"), resultHandler);
			iq.addExtension(new SQLExtension(sql, 0, size));
			ConnectionManager.connection.send( iq ); 						
		}
		
		public function getContactResults(resultIQ:IQ):ArrayCollection
		{		
			var iqNode:XMLNode = resultIQ.getNode();			
			var children:Array = iqNode.childNodes;
			var contacts:ArrayCollection = new ArrayCollection();			
			
			for (var i:String in children) 
			{						
				if (children[i].nodeName == "moneyserve-sql") 
				{
					var rows:Array = children[i].childNodes;			

					for (var j:String in rows) 
					{			
					   if (rows[j].nodeName == "row") 
					   {			   			   
						var contact:Contact = _processContactRow(rows[j]);

						if (contact.contactId > 0)
						{
							contacts.addItem(contact);			
						}			   	
					   }
					}
				}
			}
			return contacts;
		}
		
		public function getContactResult(resultIQ:IQ):Contact
		{		
			var contacts:ArrayCollection = getContactResults(resultIQ); 
			return contacts[0];
		}
		
		private function _processContactRow(iqNode:XMLNode):Contact 
		{		
			var columns:Array = iqNode.childNodes;
			var contact:Contact = new Contact();
			
			contact.contactId = 0;

			for (var j:String in columns) 
			{
				if (columns[j].nodeName.toLowerCase() == "serialno")				
					contact.contactId = Number(columns[j].firstChild.nodeValue);

				if (columns[j].nodeName.toLowerCase() == "custserialno")				
					contact.accountId = Number(columns[j].firstChild.nodeValue);

				if (columns[j].nodeName.toLowerCase() == "mgrserialno")				
					contact.managerId = Number(columns[j].firstChild.nodeValue);

				if (columns[j].nodeName.toLowerCase() == "firstname")
					contact.firstName = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "lastname")
					contact.lastName = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "street")
					contact.address1 = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "city")
					contact.city = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "zipcode" && columns[j].firstChild)
					contact.zip = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "state" && columns[j].firstChild)
					contact.state = columns[j].firstChild.nodeValue;

				if (columns[j].nodeName.toLowerCase() == "telephone" && columns[j].firstChild)
					contact.officePhone = columns[j].firstChild.nodeValue;
					
				if (columns[j].nodeName.toLowerCase() == "cellphone" && columns[j].firstChild)
					contact.cellPhone = columns[j].firstChild.nodeValue;												

				if (columns[j].nodeName.toLowerCase() == "email" && columns[j].firstChild)
					contact.email = columns[j].firstChild.nodeValue;
					
					
			}
			
			return contact;
		}
		

		private function typeObject(o:Object):Contact
		{
			var c:Contact = new Contact();
			c.accountId = o.ACCOUNT_ID;
			c.address1 = o.ADDRESS1;
			c.address2 = o.ADDRESS2;
			c.cellPhone = o.CELL_PHONE;
			c.city = o.CITY;
			c.contactId = o.CONTACT_ID;
			c.email = o.EMAIL;
			c.fax = o.FAX;
			c.firstName = o.FIRST_NAME;
			c.lastName = o.LAST_NAME;
			c.lastUpdated = o.LAST_UPDATED;
			c.managerId = o.MANAGER_ID;
			c.notes = o.NOTES;
			c.officePhone = o.OFFICE_PHONE;
			c.offlineOperation = o.OFFLINE_OPERATION;
			c.owner = o.OWNER;
			c.state = o.STATE;
			c.title = o.TITLE;
			c.zip = o.ZIP;
			c.priority = o.priority;
			return c;
		}

	}
}