INSERT INTO ofVersion (name, version) VALUES ('voicebridge', 0);

CREATE TABLE ofvoicebridge 
(
  siteID INTEGER NOT NULL,
  name VARCHAR(255) NOT NULL,
  privateHost VARCHAR(50) NOT NULL,
  publicHost VARCHAR(50) NOT NULL,
  defaultProxy VARCHAR(50),
  defaultExten VARCHAR(50),  
  PRIMARY KEY (siteID)  
 );
  
CREATE TABLE ofvoicebridgeproxy 
( 
 proxyName VARCHAR(50) NOT NULL,
 displayName VARCHAR(50),
 hostName VARCHAR(50),
 userName VARCHAR(50),
 userAuthName VARCHAR(50),
 password VARCHAR(50),
 realm VARCHAR(50),
 PRIMARY KEY (proxyName)   
);


CREATE TABLE ofcalllog (
  callId VARCHAR(255) NOT NULL,
  tscId VARCHAR(50),  
  profileId VARCHAR(255),
  interestId VARCHAR(255),
  state VARCHAR(50),
  direction VARCHAR(50),
  startTimestamp TIMESTAMP,
  duration INTEGER,
  callerName VARCHAR(255),
  callerNumber VARCHAR(255),
  calledName VARCHAR(255),  
  calledNumber VARCHAR(255),
  callSeq BIGSERIAL,
  PRIMARY KEY (callId)  
);

CREATE TABLE ofparticipantlog (
  callId VARCHAR(255) NOT NULL,
  tscId VARCHAR(50),  
  jid VARCHAR(255) NOT NULL,
  direction VARCHAR(50),
  type VARCHAR(50),
  startTimestamp TIMESTAMP,
  duration INTEGER,
  participantSeq BIGSERIAL,
  PRIMARY KEY (callId, jid, startTimestamp)  
);

