package com.salesbuilder.model
{
	import com.salesbuilder.dao.AccountDAO;
	
	[Bindable]
	public class Opportunity
	{
		public var opportunityId:int;
		public var accountId:int;
		public var name:String;
		public var owner:int;
		public var expectedCloseDate:Date = new Date();
		public var expectedAmount:Number = 0;
		public var probability:Number = 50;
		public var currencyRecieved:String;
		public var leadSource:String;
		public var notes:String;
		public var lastUpdated:Number = 0;
		public var offlineOperation:String;
		public var type:String;
		
		private var _account:Account = new Account();
		private var accountLoaded:Boolean = false;
		
		// Lazy loading of account
		[Bindable(event="accountChanged")]
		
		public function get account():Account
		{
		
			if (!accountLoaded && accountId > 0)
			{
				var dao:AccountDAO = new AccountDAO();
				dao.getAccount(accountId,
					function (a:Account):void
					{
						_account = a;
						dispatchEvent(new Event("accountChanged"));
					});
				accountLoaded = true;
			}
			
			return _account;
		}
		
		public function set account(account:Account):void
		{
			_account = account;
			accountLoaded = true;
			dispatchEvent(new Event("accountChanged"));
		}
		
	}
}