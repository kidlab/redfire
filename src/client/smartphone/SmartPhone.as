
// ActionScript file
//include "login.as"
//include "utils.as"

import flash.events.Event;
import flash.media.Sound;
import flash.media.SoundChannel;
import flash.media.SoundMixer;
import flash.net.NetStream;

[Embed(source="sample.mp3")]
public var soundClass:Class;
public	var smallSound:Sound = new soundClass() as Sound;
public var channel:SoundChannel;
public var playingRingtone:Boolean = false;
private var eventTimerCount:int = 0;

private var nstream:NetStream;

// Mic related settings and UI changes.

private var index:Number= 0;
private var active_mic:Microphone = Microphone.getMicrophone();
private var m_testingCompleted:Boolean = true;
private var maxGain:Number = 50;
private var silenceLevel:Number = 10;
private var bAutoGain:Boolean = true;
private var myTimer:Timer = new Timer(50, 0);

public function onInit():void
{
	// UI related initial settings
	cleanUp();
	acceptBtn.visible = false;
	activityLevel_pb.mode = "manual";
	activityLevel_pb.label = "";
	activityLevel_pb.setStyle("themeColor", "0xFF0000");
	active_mic.setSilenceLevel(10, 500);
	active_mic.setUseEchoSuppression(true);
	
	// Enable Microphone Auto Gain control by default
	default_ckbox.selected = true;
	bAutoGain = true;
}


public function cleanUp():void
{
	indication_box.text = "";	
	leg_state_box.text = "";
	callerID_lbl.text = "";
	callbtn.visible= true;
	call_box.editable = true;
	acceptBtn.visible=false;
	talk_ckbox.visible = false;
	gain_value_box.visible = false;
	activityLevel_pb.visible = false;
	gain_label.visible = false;
	max_gain_stepper.visible = false;
	default_ckbox.visible = false;
	max_gain_stepper.value = maxGain;
	SoundMixer.stopAll();


	videoObj.video.clear();
	videoPanel.visible=false;
	videoObj.visible=true;
	videoObj.initialize();
	
	fps.text="0";
	videoBitrate.text="0";
	audioBitrate.text="0";
	buffer.text="0";
	videoheight.text="0";
	videowidth.text="0";
	time.text="0";
 
}

private function onSignOut():void
{

}

private function onMaxGainChange():void
{
	maxGain = max_gain_stepper.value;
	active_mic.gain = maxGain;
}

private function onDefaultSettings():void
{
	if(!default_ckbox.selected){
		bAutoGain = false;
	}else{
		bAutoGain = true;				
	}	
}

private function onDigitClick(digit:String):void
{
	call_box.text += digit;
}

private function onHangupStatus():void
{
	cleanUp();				
}

private function onClear():void
{
	status_log2.text = "";
}

private function onAlwaysTalk():void
{
	if(talk_ckbox.selected){
		active_mic.gain=maxGain;
	}else{
		active_mic.gain=0;
	}
}

private function onAccept():void
{

}

private function onCall():void
{

}