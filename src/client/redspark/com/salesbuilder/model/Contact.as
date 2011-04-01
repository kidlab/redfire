package com.salesbuilder.model
{
	import com.salesbuilder.dao.AccountDAO;
	
	[Bindable]
	public class Contact
	{
		public var contactId:int;
		public var accountId:int;
		public var managerId:int;
		public var firstName:String;
		public var lastName:String;
		public var title:String;
		public var owner:int;
		public var officePhone:String;
		public var cellPhone:String;
		public var email:String;
		public var fax:String;
		public var address1:String;
		public var address2:String;
		public var city:String;
		public var state:String;
		public var zip:String;
		public var notes:String;
		public var priority:int;
		public var lastUpdated:Number = 0;
		public var offlineOperation:String;

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