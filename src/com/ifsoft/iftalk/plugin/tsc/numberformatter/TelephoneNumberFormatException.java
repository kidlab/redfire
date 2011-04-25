package com.ifsoft.iftalk.plugin.tsc.numberformatter;

public class TelephoneNumberFormatException extends Exception{

	public TelephoneNumberFormatException(String message) {
		super(message);
	}

 	public TelephoneNumberFormatException(Throwable cause){
		super(cause.toString());
		super.initCause(cause);
	}

	public TelephoneNumberFormatException(String message, Throwable cause){
		super(message);
		super.initCause(cause);
	}

}

