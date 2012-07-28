package org.red5.server.webapp.redfire;

import java.io.*;
import java.util.*;

import java.nio.IntBuffer;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagWriter;
import org.red5.io.ITagReader;
import org.red5.io.flv.impl.FLVService;
import org.red5.io.flv.impl.FLV;
import org.red5.io.flv.impl.FLVReader;
import org.red5.io.flv.impl.Tag;
import org.red5.io.IoConstants;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPClient;
import org.red5.server.net.rtmp.INetStreamEventHandler;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.ClientExceptionHandler;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.net.rtmp.event.SerializeUtils;
import org.red5.server.stream.AbstractClientStream;
import org.red5.server.stream.IStreamData;
import org.red5.server.stream.message.RTMPMessage;

import org.red5.server.webapp.voicebridge.RtmpParticipant;
import org.red5.server.webapp.voicebridge.Red5AudioHandler;

import org.apache.log4j.Logger;


public class Red5Participant extends RTMPClient implements Red5AudioHandler, INetStreamEventHandler, ClientExceptionHandler, IPendingServiceCallback {

	protected Logger Log = Logger.getLogger(getClass().getName());

    public boolean createdPlayStream = false;
    public boolean startPublish = false;
    public Integer playStreamId;
    public Integer publishStreamId;
    public RtmpParticipant rtmpParticipant;
    private String publishName;
    private String playName;
    private RTMPConnection conn;
    private ITagWriter writer;
    private ITagReader reader;

    private int kt = 0;
    private short kt2 = 0;
    private IoBuffer buffer;

    private static final int ULAW_CODEC_ID = 130;
    private static final int ULAW_AUDIO_LENGTH = 161;

	private final byte[] byteBuffer = new byte[ULAW_AUDIO_LENGTH];

	private long startTime = System.currentTimeMillis();


    public void setRtmpParticipant(RtmpParticipant rtmpParticipant)
    {
		loggerdebug("setRtmpParticipant");
		this.rtmpParticipant = rtmpParticipant;
	}

    // ------------------------------------------------------------------------
    //
    // Overide
    //
    // ------------------------------------------------------------------------

    @Override
    public void connectionOpened( RTMPConnection conn, RTMP state ) {

        loggerdebug( "Red5Participant - connection opened" );
        super.connectionOpened( conn, state );
        this.conn = conn;
    }


    @Override
    public void connectionClosed( RTMPConnection conn, RTMP state ) {

        loggerdebug( "connection closed" );
        super.connectionClosed( conn, state );
    }


    @Override
    protected void onInvoke( RTMPConnection conn, Channel channel, Header header, Notify notify, RTMP rtmp ) {

        super.onInvoke( conn, channel, header, notify, rtmp );

        try {
            ObjectMap< String, String > map = (ObjectMap) notify.getCall().getArguments()[ 0 ];
            String code = map.get( "code" );

            if ( StatusCodes.NS_PLAY_STOP.equals( code ) ) {
                loggerdebug( "onInvoke, code == NetStream.Play.Stop, disconnecting" );
                disconnect();
            }
        }
        catch ( Exception e ) {

        }

    }


    // ------------------------------------------------------------------------
    //
    // Public
    //
    // ------------------------------------------------------------------------

  	public void startStream(String publishName, String playName )
    {
        loggerdebug( "Red5Participant startStream" );

        String host = "localhost";
        String app = "xmpp";
        int port = 1935;

		if (publishName == null || playName == null)
		{
			loggererror( "Red5Participant startStream stream names invalid " + publishName + " " + playName);

		} else {

			this.publishName = publishName;
			this.playName = playName;

			createdPlayStream = false;
			startPublish = false;

			kt = 0;
			kt2 = 0;
         	startTime = System.currentTimeMillis();

			try {
				connect( host, port, app, this );

			}
			catch ( Exception e ) {
				loggererror( "Red5Participant startStream exception " + e );
			}
		}
    }


    public void stopStream() {

        loggerdebug( "Red5Participant stopStream" );

		kt = 0;
		kt2 = 0;

        try {
            disconnect();
        }
        catch ( Exception e ) {
            loggererror( "Red5Participant stopStream exception " + e );
        }

    }


    // ------------------------------------------------------------------------
    //
    // Implementations
    //
    // ------------------------------------------------------------------------

	public void handleException(Throwable throwable)
	{
			Log.error( throwable.getCause() );
	}


    public void onStreamEvent( Notify notify ) {

        loggerdebug( "onStreamEvent " + notify );

        ObjectMap map = (ObjectMap) notify.getCall().getArguments()[ 0 ];
        String code = (String) map.get( "code" );

        if ( StatusCodes.NS_PUBLISH_START.equals( code ) ) {
            loggerdebug( "onStreamEvent Publish start" );
            startPublish = true;
        }
    }


    public void resultReceived( IPendingServiceCall call ) {

        loggerdebug( "service call result: " + call );

        if ( "connect".equals( call.getServiceMethodName() ) ) {
            createPlayStream( this );

        }
        else if ( "createStream".equals( call.getServiceMethodName() ) ) {

            if ( createdPlayStream ) {
                publishStreamId = (Integer) call.getResult();
                loggerdebug( "createPublishStream result stream id: " + publishStreamId );
                loggerdebug( "publishing video by name: " + publishName );
                publish( publishStreamId, publishName, "live", this );
            }
            else {
                playStreamId = (Integer) call.getResult();
                loggerdebug( "createPlayStream result stream id: " + playStreamId );
                loggerdebug( "playing video by name: " + playName );
                play( playStreamId, playName, -2000, -1000 );

                createdPlayStream = true;
                createStream( this );
            }
        }
    }


	public void pushAudio(byte[] pcmBuffer, int ts)
	{
		try {

			if ( buffer == null ) {
				buffer = IoBuffer.allocate( 1024 );
				buffer.setAutoExpand( true );
			}

			buffer.clear();
			buffer.put( pcmBuffer );
			buffer.flip();

			AudioData audioData = new AudioData( buffer );
			audioData.setTimestamp( ts );

			kt++;

			if ( kt < 10 ) {
				loggerdebug("Red5Participant - Send audio data to Red5 " + audioData);
			}

			RTMPMessage rtmpMsg = new RTMPMessage();
			rtmpMsg.setBody( audioData );
			publishStreamData( publishStreamId, rtmpMsg );

		} catch (Exception e) {

            loggererror( "Red5Participant pushAudio exception " + e );
		}

	}

   	public int[] normalize(int[] audio)
   	{
	    // Scan for max peak value here
	    float peak = 0;
		for (int n = 0; n < audio.length; n++)
		{
			int val = Math.abs(audio[n]);
			if (val > peak)
			{
				peak = val;
			}
		}

		// Peak is now the loudest point, calculate ratio
		float r1 = 32768 / peak;

		// Don't increase by over 500% to prevent loud background noise, and normalize to 75%
		float ratio = Math.min(r1, 5) * .75f;

		for (int n = 0; n < audio.length; n++)
		{
			audio[n] *= ratio;
		}

		return audio;

   	}

    // ------------------------------------------------------------------------
    //
    // Privates
    //
    // ------------------------------------------------------------------------

    private void loggerdebug( String s ) {

        Log.info( s );
    }

    private void loggererror( String s ) {

        Log.error( "[ERROR] " + s );
    }

    private void createPlayStream( IPendingServiceCallback callback ) {

        loggerdebug( "create play stream" );
        IPendingServiceCallback wrapper = new CreatePlayStreamCallBack( callback );
        invoke( "createStream", null, wrapper );
    }

    private class CreatePlayStreamCallBack implements IPendingServiceCallback {

        private IPendingServiceCallback wrapped;


        public CreatePlayStreamCallBack( IPendingServiceCallback wrapped ) {

            this.wrapped = wrapped;
        }


        public void resultReceived( IPendingServiceCall call ) {

       		loggerdebug( "CreatePlayStreamCallBack  resultReceived");

            Integer streamIdInt = (Integer) call.getResult();

            if ( conn != null && streamIdInt != null ) {
                PlayNetStream stream = new PlayNetStream();
                stream.setConnection( conn );
                stream.setStreamId( streamIdInt.intValue() );
                conn.addClientStream( stream );
            }
            wrapped.resultReceived( call );
        }

    }

    private class PlayNetStream extends AbstractClientStream implements IEventDispatcher {

        public void close() {

        }


        public void start() {

        }


        public void stop() {

        }


        public void dispatchEvent( IEvent event )
        {

            if ( !( event instanceof IRTMPEvent ) ) {
                loggerdebug( "skipping non rtmp event: " + event );
                return;
            }

            IRTMPEvent rtmpEvent = (IRTMPEvent) event;

            //if ( logger.isDebugEnabled() ) {
                // loggerdebug("rtmp event: " + rtmpEvent.getHeader() + ", " +
                // rtmpEvent.getClass().getSimpleName());
            //}

            if ( !( rtmpEvent instanceof IStreamData ) ) {
                loggerdebug( "skipping non stream data" );
                return;
            }

            if ( rtmpEvent.getHeader().getSize() == 0 ) {
                loggerdebug( "skipping event where size == 0" );
                return;
            }

            if ( rtmpEvent instanceof VideoData ) {
                // videoTs += rtmpEvent.getTimestamp();
                // tag.setTimestamp(videoTs);

            }
            else if ( rtmpEvent instanceof AudioData ) {
                //audioTs += rtmpEvent.getTimestamp();

                IoBuffer audioData = ( (IStreamData) rtmpEvent ).getData().asReadOnlyBuffer();

				audioData.rewind();
				audioData.position(audioData.position());
				audioData.get(byteBuffer);

				if (rtmpParticipant != null )
				{
					rtmpParticipant.push(byteBuffer);

					if ( kt2 < 10 ) {
              			loggerdebug("Red5Participant - Got audio data from Red5 at " + rtmpEvent.getTimestamp() + " " + byteBuffer.length);
					}

					kt2++;
				}
			}
        }
    }
}
