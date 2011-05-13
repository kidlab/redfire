//-------------------------------------------------------
//
//	Initialise Toolbar
//
//-------------------------------------------------------

var redspark;
var username;

//-------------------------------------------------------
//
//	Initialise Toolbar
//
//-------------------------------------------------------

function startApp(username, password)
{
	this.username = username;
	
	var dom = '<div id="footpanel">' + 
	      '<ul id="mainpanel">' +	
		'<li id="alertpanel"></li>' + 
		'<li id="chatpanel"></li>' + 
	      '</ul>' + 
	   '</div><div id="redsparkDIV"></div>' 
	
	
	jQuery('body').append(dom);

	addXMPPConnection(username, password);
	
	addServicesPanel();	
	addAlertPanel();
	addChatPanel();	
	addRoomPanels();
	
	//Adjust panel height
	
	$.fn.adjustPanel = function(){
		jQuery(this).find("ul, .subpanel").css({ 'height' : 'auto'}); //Reset subpanel and ul height

		var windowHeight = jQuery(window).height(); //Get the height of the browser viewport
		var panelsub = jQuery(this).find(".subpanel").height(); //Get the height of subpanel
		var panelAdjust = windowHeight - 100; //Viewport height - 100px (Sets max height of subpanel)
		var ulAdjust =  panelAdjust - 25; //Calculate ul size after adjusting sub-panel (27px is the height of the base panel)

		if ( panelsub >= panelAdjust ) {	 //If subpanel is taller than max height...
			jQuery(this).find(".subpanel").css({ 'height' : panelAdjust }); //Adjust subpanel to max height
			jQuery(this).find("ul").css({ 'height' : ulAdjust}); //Adjust subpanel ul to new size
		}
		else if ( panelsub < panelAdjust ) { //If subpanel is smaller than max height...
			jQuery(this).find("ul").css({ 'height' : 'auto'}); //Set subpanel ul to auto (default size)
		}
	};

	//Execute function on load
	
	jQuery("#chatpanel").adjustPanel(); //Run the adjustPanel function on #chatpanel
	jQuery("#alertpanel").adjustPanel(); //Run the adjustPanel function on #alertpanel

	//Each time the viewport is adjusted/resized, execute the function
	
	jQuery(window).resize(function () {
		jQuery("#chatpanel").adjustPanel();
		jQuery("#alertpanel").adjustPanel();
		jQuery("#roompanel").adjustPanel();		
	});

	//Click event on Chat Panel + Alert Panel
	
	jQuery("#chatpanel a:first, #alertpanel a:first, .chat").click(function() 
	{ 
	
		//If clicked on the first link of #chatpanel and #alertpanel...
	
		if(jQuery(this).next(".subpanel").is(':visible')){ //If subpanel is already active...
			jQuery(this).next(".subpanel").hide(); //Hide active subpanel
			jQuery("#footpanel li a").removeClass('active'); //Remove active class on the subpanel trigger
		}
		else { //if subpanel is not active...
			jQuery(".subpanel").hide(); //Hide all subpanels
			jQuery(this).next(".subpanel").toggle(); //Toggle the subpanel to make active
			jQuery("#footpanel li a").removeClass('active'); //Remove active class on all subpanel trigger
			jQuery(this).toggleClass('active'); //Toggle the active class on the subpanel trigger
		}
		return false; //Prevent browser jump to link anchor
	});

	//Click event outside of subpanel
	
	jQuery(document).click(function() { //Click anywhere and...
		jQuery(".subpanel").hide(); //hide subpanel
		jQuery("#footpanel li a").removeClass('active'); //remove active class on subpanel trigger
	});
	jQuery('.subpanel ul').click(function(e) {
		e.stopPropagation(); //Prevents the subpanel ul from closing on click
	});

	//Delete icons on Alert Panel
	
	jQuery("#alertpanel li").hover(function() {
		jQuery(this).find("a.delete").css({'visibility': 'visible'}); //Show delete icon on hover
	},function() {
		jQuery(this).find("a.delete").css({'visibility': 'hidden'}); //Hide delete icon on hover out
	});
}


function addServicesPanel()
{
	  var dom = '<li><a id="displayName" class="home">&nbsp;<small>&nbsp;</small></a></li>' +
	  '<li><a onclick="alert(1)" class="profile">View Profile <small>View Profile</small></a></li>' + 
	  '<li><a onclick="alert(1)" class="editprofile">Edit Profile <small>Edit Profile</small></a></li>' + 
	  '<li><a onclick="alert(1)" class="contacts">Contacts <small>Contacts</small></a></li>' +
	  '<li><a onclick="alert(1)" class="messages">Messages (10) <small>Messages</small></a></li>' + 
	  '<li><a onclick="alert(1)" class="playlist">Play List <small>Play List</small></a></li>' + 
	  '<li><a onclick="alert(1)" class="videos">Videos <small>Videos</small></a></li>'
	  
	jQuery('#mainpanel').append(dom); 
}


function addAlertPanel()
{

        var dom = '<a  class="alerts">Alerts</a>' +
            	  '<div class="subpanel">' + 
            	  '<h3><span> &ndash; </span>Notifications</h3>' + 
            	  '<ul>' +
		  '<li class="view"><a >View All</a></li>'
		  
	for (var i=0; i<10; i++)
	{
	   dom = dom + '<li><a  class="delete">X</a><p><a >' + i + '</a> abico quod duis odio tation luctus eu ad <a >lobortis facilisis</a>.</p></li>'
	}

	dom = dom + '</ul></div>'
	
	jQuery('#alertpanel').append(dom); 
}

function addChatPanel()
{
	var dom = '<a  class="chat">Friends (<strong>18</strong>) </a>' +
            	  '<div class="subpanel">' +
            	  '<h3><span> &ndash; </span>Friends Online</h3>' + 
            	  '<ul>' + 
            	  '<li><span>Family Members</span></li>' 

	for (var i=0; i<5; i++)
	{
            	dom = dom + '<li><a ><img src="images/chat-thumb.gif" alt="" /> Your Relative</a></li>	'
	}
	
	dom = dom + '<li><span>Friends</span></li>' 

	for (var i=0; i<10; i++)
	{
            	dom = dom + '<li><a ><img src="images/chat-thumb.gif" alt="" /> Your Friend</a></li>	'
	}            	  

	dom = dom + '</ul></div>'
	
	jQuery('#chatpanel').append(dom); 
}

function addRoomPanels()
{
	for (var i=0; i<2; i++)
	{
		addRoomPanel('Contact ' + i, i);
	}
	
}


function addRoomPanel(contact, id)
{
	var dom =   '<li id="roompanel' + id + '"><a  class="chat">' + contact + '</a>' +
		'<div class="subpanel">' +
		'<h3><span> &ndash; </span>' + contact + '</h3>'

	for (var i=0; i<10; i++)
	{
		dom = dom + '&nbsp;<p/>'
	}

	dom = dom + '</div></li>'    

	jQuery('#mainpanel').append(dom); 
}



function addXMPPConnection(username, password)
{	
	var fo = new SWFObject("swf/redspark.swf?username=" + username + "&password=" + password + "&connectionType=rtmfp", "redspark", "0", "0", "10");
	fo.addParam("swLiveConnect", "true");
	fo.addParam("name", "redspark");
	fo.write("redsparkDIV");
}



//-------------------------------------------------------
//
//	Initialise XMPP
//
//-------------------------------------------------------


function ready()
{
	redspark = document.getElementById("redspark");
	redspark.login();
}


function stopApp()
{
	redspark.logout();
}

function getPort()
{
	return window.location.port;
}

function getDomain()
{
	return "btg199251";
}

function getHostname()
{
	return window.location.hostname;
}

function getResource()
{
	return "redspark";
}

//-------------------------------------------------------
//
//	Callbacks
//
//-------------------------------------------------------

function displayName(displayName, firstName, lastName)
{	
	jQuery('#displayName').html(username + '<small>' + displayName + '&nbsp;</small>'); 

}


//-------------------------------------------------------
//
//	Utility
//
//-------------------------------------------------------

function error(errorCondition, errorMessage)
{
	alert("Error " + errorCondition + " " + errorMessage);
}


function getParameter(string, parm, delim) {

     if (string.length == 0) {
     	return '';
     }

	 var sPos = string.indexOf(parm + "=");

     if (sPos == -1) {return '';}

     sPos = sPos + parm.length + 1;
     var ePos = string.indexOf(delim, sPos);

     if (ePos == -1) {
     	ePos = string.length;
     }

     return unescape(string.substring(sPos, ePos));
}

function getPageParameter(parameterName, defaultValue) {

	var s = self.location.search;

	if ((s == null) || (s.length < 1)) {
		return defaultValue;
	}

	s = getParameter(s, parameterName, '&');

	if ((s == null) || (s.length < 1)) {
		s = defaultValue;
	}

	return s;
}
