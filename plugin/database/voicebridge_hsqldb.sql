INSERT INTO ofVersion (name, version) VALUES ('voicebridge', 0);

CREATE TABLE ofvoicebridge (
  siteID BIGINT NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  privateHost VARCHAR(50),
  publicHost VARCHAR(50),
  defaultProxy VARCHAR(50),
  defaultExten VARCHAR(50)    
 );
  
CREATE TABLE ofvoicebridgeproxy 
( 
 proxyName VARCHAR(50) NOT NULL PRIMARY KEY,
 displayName VARCHAR(50),
 hostName VARCHAR(50),
 userName VARCHAR(50),
 userAuthName VARCHAR(50),
 password VARCHAR(50),
 realm VARCHAR(50) 
);

 
CREATE TABLE ofcalllog 
(
  callId VARCHAR(255) NOT NULL PRIMARY KEY,
  tscId VARCHAR(50),  
  profileId VARCHAR(255),
  interestId VARCHAR(255),
  state VARCHAR(50),
  direction VARCHAR(50),
  startTimestamp TIMESTAMP NOT NULL,
  duration BIGINT,
  callerName VARCHAR(255),
  callerNumber VARCHAR(255),
  calledName VARCHAR(255),  
  calledNumber VARCHAR(255)
);

CREATE TABLE ofparticipantlog 
(
  callId VARCHAR(255) NOT NULL PRIMARY KEY,
  jid VARCHAR(255) NOT NULL PRIMARY KEY,
  tscId VARCHAR(50),    
  direction VARCHAR(50),
  type VARCHAR(50),
  startTimestamp TIMESTAMP NOT NULL PRIMARY KEY,
  duration BIGINT
);
