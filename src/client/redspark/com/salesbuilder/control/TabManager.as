package com.salesbuilder.control
{
	import mx.core.Container;
	import mx.containers.TabNavigator;
	import mx.events.ChildExistenceChangedEvent;
	import com.salesbuilder.model.Account;
	import com.salesbuilder.model.Contact;
	import com.salesbuilder.model.Opportunity;
	import com.salesbuilder.view.AccountPanel;
	import com.salesbuilder.view.ContactPanel;
	import com.salesbuilder.view.OpportunityPanel;
	import com.salesbuilder.dataviz.Dashboard;
	import com.salesbuilder.dao.OpportunityDAO;
	import com.salesbuilder.dao.ContactDAO;
	import com.salesbuilder.dao.AccountDAO;
	
	public class TabManager
	{
		private static var tabNav:TabNavigator;
		
		private static var uniqueTabs:Object = new Object();

		public static function set tabNavigator(tabNavigator:TabNavigator):void
		{
			tabNav = tabNavigator;
			tabNav.addEventListener(ChildExistenceChangedEvent.CHILD_REMOVE,
				function (event:ChildExistenceChangedEvent):void
				{
					for (var uniqueId:String in uniqueTabs)
					{
						if (uniqueTabs[uniqueId] == event.relatedObject)
						{
							uniqueTabs[uniqueId] = null;
							return;
						}
					}
				});
		}
		
		public static function openTab(tabClass:Class, uniqueId:String=null):Container
		{
			if (uniqueId && uniqueTabs[uniqueId])
			{
				tabNav.selectedChild = uniqueTabs[uniqueId];
				return uniqueTabs[uniqueId];
			}

			var tab:Container = new tabClass();

			if (uniqueId)
			{
				uniqueTabs[uniqueId] = tab;
			}
			
			tabNav.addChild(tab);
			
			tabNav.selectedChild = tab;

			return tab;
		}
		
		public static function setUniqueTab(uniqueId:String, tab:Container):void
		{
			uniqueTabs[uniqueId] = tab;		
		}

		public static function openAccount(account:Account):void
		{
			//var tab:AccountPanel = TabManager.openTab(AccountPanel, "ACCOUNT:"+account.accountId) as AccountPanel;
			//tab.account = account;
			//return tab;
			
			openAccountById(account.accountId);
		}

		public static function openAccountById(accountId:int):void
		{
			var accountDAO:AccountDAO = new AccountDAO();
			accountDAO.getAccount(accountId, function(data:Account):void
			{
				var tab:AccountPanel = TabManager.openTab(AccountPanel, "ACCOUNT:"+accountId) as AccountPanel;
				tab.account = data;	
			});
		}		
		
		public static function openContact(contact:Contact):void
		{
			//var tab:ContactPanel = TabManager.openTab(ContactPanel, "CONTACT:"+contact.contactId) as ContactPanel;
			//tab.contact = contact;
			//return tab;
			
			openContactById(contact.contactId);
		}

		public static function openContactById(contactId:int):void
		{
			var contactDAO:ContactDAO = new ContactDAO();
			contactDAO.getContact(contactId, function(data:Contact):void
			{
				var tab:ContactPanel = TabManager.openTab(ContactPanel, "CONTACT:"+contactId) as ContactPanel;	
				tab.contact = data;	
			});
		}
		
		public static function openOpportunity(opportunity:Opportunity):OpportunityPanel
		{
			var tab:OpportunityPanel = TabManager.openTab(OpportunityPanel, "OPPORTUNITY:"+opportunity.opportunityId) as OpportunityPanel;	
			tab.opportunity = opportunity;	
			return tab;
		}
		
		
		public static function openOpportunityById(opportunityId:int):void
		{
			var opportunityDAO:OpportunityDAO = new OpportunityDAO();
			opportunityDAO.getOpportunity(opportunityId,
				function(data:Opportunity):void
				{
					var tab:OpportunityPanel = TabManager.openTab(OpportunityPanel, "OPPORTUNITY:"+opportunityId) as OpportunityPanel;	
					tab.opportunity = data;	
				});
		}
		
		public static function openDashboard():Dashboard
		{
			return openTab(Dashboard, "DASHBOARD") as Dashboard;
		}

		public static function removeTab(tab:Container):void
		{	
			/*	
			for (var uniqueId:String in uniqueTabs)
			{
				if (uniqueTabs[uniqueId] == tab)
				{
					uniqueTabs[uniqueId] = null;
					return;
				}
			}
			*/
			tabNav.removeChild(tab);
		}
		
		public static function getUniqueTab(uniqueId:String):Container
		{
			return uniqueTabs[uniqueId];
		}

	}
}