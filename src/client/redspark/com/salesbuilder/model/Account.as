package com.salesbuilder.model
{
	import mx.collections.ArrayCollection;
	import com.salesbuilder.dao.ContactDAO;
	import com.salesbuilder.dao.OpportunityDAO;
	
	[Bindable]
	public class Account
	{
		public var accountId:int;
		public var name:String;
		public var type:String;
		public var industry:int;
		public var owner:int;
		public var phone:String;
		public var fax:String;
		public var ticker:String;
		public var ownership:String;
		public var numberEmployees:int;
		public var annualRevenue:Number = 0;
		public var priority:int;
		public var address1:String;
		public var address2:String;
		public var city:String;
		public var state:String;
		public var zip:String;
		public var notes:String;
		public var lastUpdated:Number = 0;
		public var offlineOperation:String;
		public var url:String;
		public var email:String;
		public var rating:int;
		public var currentYearResults:Number = 0;
		public var lastYearResults:Number = 0;
		
		private var _contacts:ArrayCollection = new ArrayCollection();
		private var contactsLoaded:Boolean = false;
		
		private var _opportunities:ArrayCollection = new ArrayCollection();
		private var opportunitiesLoaded:Boolean = false;
		
		// Lazy loading of the list of contacts
		[Bindable(event="contactsChanged")]
		public function get contacts():ArrayCollection
		{
		
			if (!contactsLoaded && accountId > 0)
			{
				var contactDAO:ContactDAO = new ContactDAO();
				contactDAO.getContactsByAccount(accountId,
					function(data:ArrayCollection):void
					{
						if (data) _contacts = data;
						dispatchEvent(new Event("contactsChanged"));
					});
				contactsLoaded = true;
			}
			
			return _contacts;
		}
		
		// Lazy loading of the list of opportunities
		[Bindable(event="opportunitiesChanged")]
		public function get opportunities():ArrayCollection
		{
		
			if (!opportunitiesLoaded && accountId > 0)
			{
				var opportunityDAO:OpportunityDAO = new OpportunityDAO();
				opportunityDAO.getOpportunitiesByAccount(accountId,
					function(data:ArrayCollection):void
					{
						if (data) _opportunities = data;
						dispatchEvent(new Event("opportunitiesChanged"));
					});
				opportunitiesLoaded = true;
			}
			
			return _opportunities;
		}
		
	}
}