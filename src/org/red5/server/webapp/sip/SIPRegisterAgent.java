package org.red5.server.webapp.sip;


import local.net.KeepAliveSip;
import org.zoolu.net.SocketAddress;
import org.zoolu.sip.address.*;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.header.*;
import org.zoolu.sip.message.*;
import org.zoolu.sip.transaction.TransactionClient;
import org.zoolu.sip.transaction.TransactionClientListener;
import org.zoolu.sip.authentication.DigestAuthentication;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Vector;


/** Register User Agent.
  * It registers (one time or periodically) a contact address with a registrar server.
  */
public class SIPRegisterAgent implements Runnable, TransactionClientListener
{
   /** The CallerID and CSeq that should be used during REGISTER method */
   	private CallIdHeader	registerCallID;
	private int				registerCSeq;

   /** Max number of registration attempts. */
   static final int MAX_ATTEMPTS=3;

   /** RegisterAgent listener */
   SIPRegisterAgentListener listener;

   /** SipProvider */
   SipProvider sip_provider;

   /** User's URI with the fully qualified domain name of the registrar server. */
   NameAddress target;

   /** User name. */
   String username;

   /** User name. */
   String realm;

   /** User's passwd. */
   String passwd;

   /** Nonce for the next authentication. */
   String next_nonce;

   /** Qop for the next authentication. */
   String qop;

   /** User's contact address. */
   NameAddress contact;

   /** Expiration time. */
   int expire_time;

   /** Renew time. */
   int renew_time;
   //change by lior
   int orig_renew_time;

   int minRenewTime=20;
   int regFailRetryTime=15;

   /** Whether keep on registering. */
   boolean loop;
   //changed by Lior
   boolean lastRegFailed=false;
   boolean regInprocess=false;

   /** Whether the thread is running. */
   boolean is_running;
   
   protected String A1ParamMD5;

   /** Event logger. */
    private static Logger log = Logger.getLogger(SIPRegisterAgent.class.getName());


   /** Number of registration attempts. */
   int attempts;

   /** KeepAliveSip daemon. */
   KeepAliveSip keep_alive;


   /** Creates a new RegisterAgent. */
   public SIPRegisterAgent(SipProvider sip_provider, String target_url, String contact_url, SIPRegisterAgentListener listener)
   {  init(sip_provider,target_url,contact_url,listener);
   }


   /** Creates a new RegisterAgent with authentication credentials (i.e. username, realm, and passwd). */
   public SIPRegisterAgent(SipProvider sip_provider, String target_url, String contact_url, String username, String realm, String passwd, SIPRegisterAgentListener listener)
   {  init(sip_provider,target_url,contact_url,listener);
      // authentication
      this.username=username;
      this.realm=realm;
      this.passwd=passwd;
   }
   
   /** Sets A1 digest parameter */
   public void setA1Parameter(String A1ParamMD5) {
	   this.A1ParamMD5 = A1ParamMD5;
   }

   /** Inits the RegisterAgent. */
   private void init(SipProvider sip_provider, String target_url, String contact_url, SIPRegisterAgentListener listener)
   {  this.listener=listener;
      this.sip_provider=sip_provider;
      this.target=new NameAddress(target_url);
      this.contact=new NameAddress(contact_url);
      //this.expire_time=SipStack.default_expires;
      this.expire_time=600;
      // changed by Lior
      this.renew_time=600;
      this.orig_renew_time=this.renew_time;
      this.is_running=false;
      this.keep_alive=null;
      // authentication
      this.username=null;
      this.realm=null;
      this.passwd=null;
      this.next_nonce=null;
      this.qop=null;
      this.attempts=0;
      this.minRenewTime=20;
      this.regFailRetryTime=5;

      this.registerCallID = null;
      this.registerCSeq = 0;


   }


   /** Whether it is periodically registering. */
   public boolean isRegistering()
   {  return is_running;
   }


   /** Registers with the registrar server. */
   public void register()
   {  register(expire_time);
   }


   /** Registers with the registrar server for <i>expire_time</i> seconds. */
   public void register(int expire_time)
   {  attempts=0;
      lastRegFailed=false;
      regInprocess=true;
      if (expire_time>0) this.expire_time=expire_time;
      Message req=MessageFactory.createRegisterRequest(sip_provider,target,target,contact);

      /*
	         * MY_FIX: registerCallID contains the CallerID randomly generated in the first REGISTER method.
	         * It will be reused for all successive REGISTER invocations
	        */
	  	  if (this.registerCallID == null)
	  		  this.registerCallID = req.getCallIdHeader();
	  	  else
	  		  req.setCallIdHeader(this.registerCallID);

	        /*
	         * MY_FIX: the registerCSeq must be unique for a given CallerID
	         */
	        this.registerCSeq++;
             req.setCSeqHeader(new CSeqHeader(this.registerCSeq, SipMethods.REGISTER));


      req.setExpiresHeader(new ExpiresHeader(String.valueOf(expire_time)));
      if (next_nonce!=null)
      {  AuthorizationHeader ah=new AuthorizationHeader("Digest");
         SipURL target_url=target.getAddress();
         ah.addUsernameParam(username);
         ah.addRealmParam(realm);
         ah.addNonceParam(next_nonce);
         ah.addUriParam(req.getRequestLine().getAddress().toString());
         ah.addQopParam(qop);
         DigestAuthentication digestAuth = new DigestAuthentication(SipMethods.REGISTER, ah, null, passwd);
         digestAuth.setA1Parameter(A1ParamMD5);
         String response = digestAuth.getResponse();
         ah.addResponseParam(response);
         req.setAuthorizationHeader(ah);
      }
      if (expire_time>0) printLog("Registering contact "+contact+" (it expires in "+expire_time+" secs)");
      else printLog("Unregistering contact "+contact);
      TransactionClient t=new TransactionClient(sip_provider,req,this);
      t.request();
   }


   /** Unregister with the registrar server */
   public void unregister()
   {  register(0);

   }


   /** Unregister all contacts with the registrar server */
   public void unregisterall()
   {  attempts=0;
      NameAddress user=new NameAddress(target);
      Message req=MessageFactory.createRegisterRequest(sip_provider,target,target,null);
      //ContactHeader contact_star=new ContactHeader(); // contact is *
      //req.setContactHeader(contact_star);
      req.setExpiresHeader(new ExpiresHeader(String.valueOf(0)));
      printLog("Unregistering all contacts");
      TransactionClient t=new TransactionClient(sip_provider,req,this);
      t.request();
   }


   /** Periodically registers with the registrar server.
     * @param expire_time expiration time in seconds
     * @param renew_time renew time in seconds */
   public void loopRegister(int expire_time, int renew_time)
   {  this.expire_time=expire_time;
      this.renew_time=renew_time;
      loop=true;
      if (!is_running) (new Thread(this,this.getClass().getName())).start();
   }


   /** Periodically registers with the registrar server.
     * @param expire_time expiration time in seconds
     * @param renew_time renew time in seconds
     * @param keepalive_time keep-alive packet rate (inter-arrival time) in milliseconds */
   public void loopRegister(int expire_time, int renew_time, long keepalive_time)
   {  loopRegister(expire_time,renew_time);
      // keep-alive
      if (keepalive_time>0)
      {  SipURL target_url=target.getAddress();
         String target_host=target_url.getHost();
         int targe_port=target_url.getPort();
         if (targe_port<0) targe_port=SipStack.default_port;
         keep_alive = new KeepAliveSip(sip_provider,new SocketAddress(target_host,targe_port),null,keepalive_time);
      }
   }


   /** Halts the periodic registration. */
   public void halt()
   {  if (is_running) loop=false;
      if (keep_alive!=null) keep_alive.halt();
   }


   // ***************************** run() *****************************

   /** Run method */
   public void run()
   {
      is_running=true;
      try
      {  while (loop)
         {  register();
           //changed by Lior
           long waitCnt=0;
			    	    while (regInprocess)
			    	    {
			    	    	Thread.sleep(1000);
			    	    	waitCnt+=1000;
			    	    }

			    	    if (lastRegFailed)
			    	    {
			    	    	printLog("Failed Registration stop try.");
				    		//Thread.sleep(regFailRetryTime*1000);
				    		halt();
			    	    }
			    	    else
    	    	       Thread.sleep(renew_time*1000-waitCnt);
         }
      }
      catch (Exception e) {  printException(e);  }
      is_running=false;
   }


   // **************** Transaction callback functions *****************

   /** Callback function called when client sends back a failure response. */

   /** Callback function called when client sends back a provisional response. */
   public void onTransProvisionalResponse(TransactionClient transaction, Message resp)
   {  // do nothing..
   }

   /** Callback function called when client sends back a success response. */
   public void onTransSuccessResponse(TransactionClient transaction, Message resp)
   {  if (transaction.getTransactionMethod().equals(SipMethods.REGISTER))
      {  if (resp.hasAuthenticationInfoHeader())
         {  next_nonce=resp.getAuthenticationInfoHeader().getNextnonceParam();
         }
         StatusLine status=resp.getStatusLine();
         String result=status.getCode()+" "+status.getReason();

         // update the renew_time
         //changed by Lior
         int expires=0;
         //int newRenew=0;
         if (resp.hasExpiresHeader())
           {  expires=resp.getExpiresHeader().getDeltaSeconds();
         //{
        //	 newRenew=resp.getExpiresHeader().getDeltaSeconds();
         }
         else if (resp.hasContactHeader())
         // look for the max expires - should be the latest
         {  Vector contacts=resp.getContacts().getHeaders();
            for (int i=0; i<contacts.size(); i++)
            {  int exp_i=(new ContactHeader((Header)contacts.elementAt(i))).getExpires();
            //   if (exp_i>0 && (expires==0 || exp_i<expires)) expires=exp_i;
            //{
		    //       	newRenew=(new ContactHeader((Header)contacts.elementAt(i))).getExpires();
             if (exp_i/2>expires)
            	   expires=exp_i/2;
           }
         }
         //if (expires>0 && expires<renew_time) renew_time=expires;

         //printLog("Registration success: ");
         //if (listener!=null) listener.onUaRegistrationSuccess(this,target,contact,result);
   //   }
   //}
//if (newRenew>0 && newRenew<renew_time)
        if (expires>0 && expires<renew_time)
        {
        	 renew_time=expires;
        	 if (renew_time<minRenewTime)
        	 {
        		 printLog("Attempt to set renew time below min renew. Attempted="+renew_time+" min="+minRenewTime+"\r\nResponse="+resp.toString());
        		 renew_time=minRenewTime;
        	 }
         }
         else if (expires>orig_renew_time)
         {
       		 printLog("Attempt to set renew time above original renew. Attempted="+expires+" origrenew="+orig_renew_time+"\r\nResponse="+resp.toString());
         }

         printLog("Registration success: ");
         regInprocess=false;
         if (listener!=null) listener.onUaRegistrationSuccess(this,target,contact,result);


      }
   }
   /** Callback function called when client sends back a failure response. */
   public void onTransFailureResponse(TransactionClient transaction, Message resp)
   {
      printLog("onTransFailureResponse start: ");

	  if (transaction.getTransactionMethod().equals(SipMethods.REGISTER))
      {  StatusLine status=resp.getStatusLine();
         int code=status.getCode();
         if ((code==401 && attempts<MAX_ATTEMPTS && resp.hasWwwAuthenticateHeader() && resp.getWwwAuthenticateHeader().getRealmParam().equalsIgnoreCase(realm))
       	  || (code==407 && attempts<MAX_ATTEMPTS && resp.hasProxyAuthenticateHeader() && resp.getProxyAuthenticateHeader().getRealmParam().equalsIgnoreCase(realm)))

         {
      		printLog("onTransFailureResponse 401 or 407: ");

			attempts++;
            Message req=transaction.getRequestMessage();
            req.setCSeqHeader(req.getCSeqHeader().incSequenceNumber());
            // * MY_FIX: registerCSeq counter must incremented
            this.registerCSeq++;


         	WwwAuthenticateHeader wah;
         	if (code==401) wah=resp.getWwwAuthenticateHeader();
         	else wah=resp.getProxyAuthenticateHeader();

            String qop_options=wah.getQopOptionsParam();
         	//qop=(qop_options!=null)? "auth" : null;

            // select a new branch - rfc3261 says should be new on each request
            ViaHeader via=req.getViaHeader();
            req.removeViaHeader();
            via.setBranch(SipProvider.pickBranch());
            req.addViaHeader(via);
            qop=(qop_options!=null)? "auth" : null;

            DigestAuthentication digest = new DigestAuthentication(SipMethods.REGISTER,req.getRequestLine().getAddress().toString(),wah,qop,null,username,passwd);
            digest.setA1Parameter(A1ParamMD5);
			AuthorizationHeader ah;
			if (code==401) ah=digest.getAuthorizationHeader();
			else ah=digest.getProxyAuthorizationHeader();

            req.setAuthorizationHeader(ah);
            TransactionClient t=new TransactionClient(sip_provider,req,this);
            t.request();

         }
         else
         {  String result=code+" "+status.getReason();
            lastRegFailed=true;
            regInprocess=false;
            if (listener==null)
            printLog("Registration failure: "+result);
            else
             printLog("Registration failure: "+result);
            if (listener!=null) listener.onUaRegistrationFailure(this,target,contact,result);
         }
      }
   }

   /** Callback function called when client expires timeout. */
   public void onTransTimeout(TransactionClient transaction)
   {  if (transaction.getTransactionMethod().equals(SipMethods.REGISTER))
      {
         if (listener==null)
         printLog("Registration failure: No response from server.");
         else
	 	 printLog("Registration failure: No response from server.");
	       	 lastRegFailed=true;
      	 regInprocess=false;
         if (listener!=null) listener.onUaRegistrationFailure(this,target,contact,"Timeout");
      }
   }


   // ****************************** Logs *****************************


   void printLog(String str) {
   	System.out.println("RegisterAgent: "+str);
   }


   void printException(Exception e)   {
		System.out.println("RegisterAgent Exception: "+e);
   }

}
