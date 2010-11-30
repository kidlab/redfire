package org.red5.server.webapp.sip;

import java.util.Enumeration;
import java.util.Vector;

import org.red5.codecs.SIPCodec;
import org.red5.codecs.SIPCodecFactory;
import org.red5.codecs.SIPCodecUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zoolu.sdp.AttributeField;
import org.zoolu.sdp.MediaDescriptor;
import org.zoolu.sdp.MediaField;
import org.zoolu.sdp.SessionDescriptor;

public class SdpUtils {

    protected static Logger log = LoggerFactory.getLogger( SdpUtils.class );
    
    
    /**
     * @return Returns the audio codec to be used on current session.
     */
    public static SIPCodec getNegotiatedAudioCodec( SessionDescriptor negotiatedSDP )
    {
        int payloadId;
        String rtpmap;
        SIPCodec sipCodec = null;
        
        printLog( "getNegotiatedAudioCodec", "Init..." );
        
        rtpmap = negotiatedSDP.getMediaDescriptor( SIPCodec.MEDIA_TYPE_AUDIO ).
                getAttribute( SIPCodec.ATTRIBUTE_RTPMAP ).getAttributeValue();
        
        printLog( "getNegotiatedAudioCodec", "rtpmap = [" + rtpmap + "]." );
        
        if ( rtpmap.length() > 0 ) {
            
            payloadId = Integer.parseInt(
                    rtpmap.substring(0, rtpmap.indexOf(" ")));
            
            printLog( "getNegotiatedAudioCodec", "payloadId = [" + payloadId + "]." );
            
            sipCodec = SIPCodecFactory.getInstance().getSIPAudioCodec( payloadId );
            
            if ( sipCodec == null ) {
                
                printLog( "getNegotiatedAudioCodec", "Error... codec not found." );
            }
            else {
                
                printLog( "getNegotiatedAudioCodec", 
                        "payloadType = " + sipCodec.getCodecId() + 
                        ", payloadName = " + sipCodec.getCodecName() + "." );
            }
        }
        
        printLog( "getNegotiatedAudioCodec", "End..." );
        
        return sipCodec;
    }
    
    
    /**
     * 
     * @param userName
     * @param viaAddress
     * 
     * @return Return the initial local SDP.
     */
    public static SessionDescriptor createInitialSdp( 
            String userName, String viaAddress, int audioPort, 
            int videoPort, String audioCodecsPrecedence ) {
        
        SessionDescriptor initialDescriptor = null;
        
        printLog( "createInitialSdp", "Init..." );
        
        try {
            
            printLog( "createInitialSdp", 
                    "userName = [" + userName + "], viaAddress = [" + viaAddress + 
                    "], audioPort = [" + audioPort + "], videoPort = [" + videoPort + 
                    "], audioCodecsPrecedence = [" + audioCodecsPrecedence + "]." );
            
            int audioCodecsNumber = SIPCodecFactory.getInstance().getAvailableAudioCodecsCount();
            int videoCodecsNumber = SIPCodecFactory.getInstance().getAvailableVideoCodecsCount();
            
            if ( ( audioCodecsNumber == 0 ) && ( videoCodecsNumber == 0 ) ) {
                
                printLog( "createInitialSdp", 
                        "audioCodecsNumber = [" + audioCodecsNumber + 
                        "], videoCodecsNumber = [" + videoCodecsNumber + "]." );
                
                return null;
            }
            
            initialDescriptor = new SessionDescriptor( userName, viaAddress );
            
            if ( initialDescriptor == null ) {
                
                printLog( "createInitialSdp", 
                        "Error instantiating the initialDescriptor!" ); 
                
                return null;
            }
            
            if ( audioCodecsNumber > 0 ) {
                
                SIPCodec[] audioCodecs;
                Vector audioAttributes = new Vector();
                
                if ( audioCodecsPrecedence.length() == 0 ) {
                    
                    audioCodecs = SIPCodecFactory.getInstance().getAvailableAudioCodecs();
                }
                else {
                    
                    audioCodecs = SIPCodecFactory.getInstance().
                            getAvailableAudioCodecsWithPrecedence( audioCodecsPrecedence );
                }
                
                for ( int audioIndex = 0; audioIndex < audioCodecsNumber; audioIndex++ ) {
                    
                    String payloadId = String.valueOf( audioCodecs[audioIndex].getCodecId() );
                    String rtpmapParamValue = payloadId;
                    rtpmapParamValue += " " + audioCodecs[audioIndex].getCodecName();
                    rtpmapParamValue += "/" + audioCodecs[audioIndex].getSampleRate() + "/1";
                    
                    printLog( "createInitialSdp", 
                            "Adding rtpmap for payload [" + payloadId + 
                            "] with value = [" + rtpmapParamValue + "]." );
                    
                    audioAttributes.add( new AttributeField( 
                            SIPCodec.ATTRIBUTE_RTPMAP, rtpmapParamValue ) );
                    
                    String[] codecMediaAttributes = 
                            audioCodecs[audioIndex].getCodecMediaAttributes();
                    
                    if ( codecMediaAttributes != null ) {
                        
                        printLog( "createInitialSdp", 
                                "Adding " + codecMediaAttributes.length + 
                                " audio codec media attributes." );
                        
                        for ( int attribIndex = 0; attribIndex < codecMediaAttributes.length; attribIndex++ ) {
                            
                            printLog( "createInitialSdp", 
                                    "Adding audio media attribute [" + 
                                    codecMediaAttributes[attribIndex] + "]." );
                            
                            AttributeField newAttribute = 
                                    parseAttributeField( codecMediaAttributes[attribIndex] );
                            
                            if ( newAttribute != null ) {
                                
                                audioAttributes.add( newAttribute );
                            }
                        }
                    }
                    else {
                        
                        printLog( "createInitialSdp", 
                                "Audio codec has no especific media attributes." );
                    }
                }
                
                // Calculate the format list to be used on MediaDescriptor creation.
                String formatList = getFormatList( audioAttributes );
                
                for ( Enumeration attributesEnum = audioAttributes.elements(); attributesEnum.hasMoreElements(); ) {
                    
                    AttributeField audioAttribute = (AttributeField) attributesEnum.nextElement();
                    
                    if ( initialDescriptor.getMediaDescriptor( SIPCodec.MEDIA_TYPE_AUDIO ) == null ) {
                        
                        printLog( "createInitialSdp", 
                                "Creating audio media descriptor." );
                        
                        initialDescriptor.addMedia( 
                                new MediaField( SIPCodec.MEDIA_TYPE_AUDIO, audioPort, 0, "RTP/AVP", formatList ), 
                                audioAttribute );
                    }
                    else {
                        
                        printLog( "createInitialSdp", 
                                "Just adding attribute." );
                        
                        initialDescriptor.getMediaDescriptor( SIPCodec.MEDIA_TYPE_AUDIO ).
                                addAttribute( audioAttribute );
                    }
                }
                
                String[] commonAudioMediaAttributes = 
                        SIPCodecFactory.getInstance().getCommonAudioMediaAttributes();
                
                if ( commonAudioMediaAttributes != null ) {
                    
                    printLog( "createInitialSdp", "Adding " + 
                            commonAudioMediaAttributes.length + " common audio media attributes." );
                    
                    for ( int attribIndex = 0; attribIndex < commonAudioMediaAttributes.length; attribIndex++ ) {
                        
                        printLog( "createInitialSdp", 
                                "Adding common audio media attribute [" + 
                                commonAudioMediaAttributes[attribIndex] + "]." );
                        
                        AttributeField newAttribute = 
                                parseAttributeField( commonAudioMediaAttributes[attribIndex] );
                        
                        if ( newAttribute != null ) {
                            
                            initialDescriptor.getMediaDescriptor( SIPCodec.MEDIA_TYPE_AUDIO ).
                                    addAttribute( newAttribute );
                        }
                    }
                }
                else {
                    
                    printLog( "createInitialSdp", "No common audio media attributes." );
                }
            }
            
            if ( videoCodecsNumber > 0 ) {
                
                SIPCodec[] videoCodecs = SIPCodecFactory.getInstance().getAvailableVideoCodecs();
                Vector videoAttributes = new Vector();
                
                for ( int videoIndex = 0; videoIndex < audioCodecsNumber; videoIndex++ ) {
                    
                    String payloadId = String.valueOf( videoCodecs[videoIndex].getCodecId() );
                    String rtpmapParamValue = payloadId;
                    rtpmapParamValue += " " + videoCodecs[videoIndex].getCodecName();
                    rtpmapParamValue += "/" + videoCodecs[videoIndex].getSampleRate() + "/1";
                    
                    printLog( "createInitialSdp", 
                            "Adding rtpmap for payload [" + payloadId + 
                            "] with value = [" + rtpmapParamValue + "]." );
                    
                    videoAttributes.add( new AttributeField( 
                            SIPCodec.ATTRIBUTE_RTPMAP, rtpmapParamValue ) );
                    
                    String[] codecMediaAttributes = videoCodecs[videoIndex].getCodecMediaAttributes();
                    
                    if ( codecMediaAttributes != null ) {
                        
                        printLog( "createInitialSdp", 
                                "Adding " + codecMediaAttributes.length + 
                                " video codec media attributes." );
                        
                        for ( int attribIndex = 0; attribIndex < codecMediaAttributes.length; attribIndex++ ) {
                            
                            printLog( "createInitialSdp", 
                                    "Adding video media attribute [" + 
                                    codecMediaAttributes[attribIndex] + "]." );
                            
                            AttributeField newAttribute = 
                                    parseAttributeField( codecMediaAttributes[attribIndex] );
                            
                            if ( newAttribute != null ) {
                                
                                videoAttributes.add( newAttribute );
                            }
                        }
                    }
                    else {
                        
                        printLog( "createInitialSdp", 
                                "Video codec has no especific media attributes." );
                    }
                }
                
                // Calculate the format list to be used on MediaDescriptor creation.
                String formatList = getFormatList( videoAttributes );
                
                for ( Enumeration attributesEnum = videoAttributes.elements(); attributesEnum.hasMoreElements(); ) {
                    
                    AttributeField videoAttribute = (AttributeField) attributesEnum.nextElement();
                    
                    if ( initialDescriptor.getMediaDescriptor( SIPCodec.MEDIA_TYPE_VIDEO ) == null ) {
                        
                        initialDescriptor.addMedia( 
                                new MediaField( SIPCodec.MEDIA_TYPE_VIDEO, audioPort, 0, "RTP/AVP", formatList ), 
                                videoAttribute );
                    }
                    else {
                        
                        initialDescriptor.getMediaDescriptor( SIPCodec.MEDIA_TYPE_VIDEO ).
                                addAttribute( videoAttribute );
                    }
                }
                
                String[] commonVideoMediaAttributes = 
                        SIPCodecFactory.getInstance().getCommonAudioMediaAttributes();
                
                if ( commonVideoMediaAttributes != null ) {
                    
                    printLog( "createInitialSdp", "Adding " + 
                            commonVideoMediaAttributes.length + " common video media attributes." );
                    
                    for ( int attribIndex = 0; attribIndex < commonVideoMediaAttributes.length; attribIndex++ ) {
                        
                        printLog( "createInitialSdp", 
                                "Adding common video media attribute [" + 
                                commonVideoMediaAttributes[attribIndex] + "]." );
                        
                        AttributeField newAttribute = 
                                parseAttributeField( commonVideoMediaAttributes[attribIndex] );
                        
                        if ( newAttribute != null ) {
                            
                            initialDescriptor.getMediaDescriptor( SIPCodec.MEDIA_TYPE_VIDEO ).
                                    addAttribute( newAttribute );
                        }
                    }
                }
                else {
                    
                    printLog( "createInitialSdp", "No common video media attributes." );
                }
            }
        }
        catch ( Exception exception ) {
            
            printLog( "createInitialSdp", "Failure creating initial SDP: " );
            exception.printStackTrace();
        }
        
        printLog( "createInitialSdp", "End..." );
        
        return initialDescriptor;
    }
    
    
    private static String getFormatList( Vector mediaAttributes ) {
        
        AttributeField mediaAttribute = null;
        String formatList = "";
        
        printLog( "getFormatList", "Init..." );
        
        for ( Enumeration attributeEnum = mediaAttributes.elements(); attributeEnum.hasMoreElements(); ) {
            
            mediaAttribute = (AttributeField) attributeEnum.nextElement();
            
            if ( mediaAttribute.getAttributeName().equalsIgnoreCase( SIPCodec.ATTRIBUTE_RTPMAP ) ) {
                
                if ( formatList.length() > 0 ) {
                    formatList += " ";
                }
                
                formatList += getPayloadIdFromAttribute( mediaAttribute );
            }
        }
        
        printLog( "getFormatList", "formatList = [" + formatList + "]." );
        
        printLog( "getFormatList", "End..." );
        
        return formatList;
    }
    
    
    private static AttributeField parseAttributeField( String codecMediaAttribute ) {
        
        AttributeField newAttribute = null;
        
        printLog( "parseAttributeField", "Init..." );
        
        printLog( "parseAttributeField", 
                "codecMediaAttribute = [" + codecMediaAttribute + "]." );
        
        String attribName = codecMediaAttribute.substring(
                0, codecMediaAttribute.indexOf( ":" ) );
        String attribValue = codecMediaAttribute.substring(
                codecMediaAttribute.indexOf( ":" ) + 1 );
        
        printLog( "parseAttributeField", 
                "attribName = [" + attribName + 
                "] attribValue  = [" + attribValue + "]." );
        
        if ( ( attribName.length() > 0 ) && ( attribValue.length() > 0 ) ) {
            
            newAttribute = new AttributeField( attribName, attribValue );
        }
        
        printLog( "parseAttributeField", "End..." );
        
        return newAttribute;
    }
    
    
    /** 
     * We must validate the existence of all remote "rtpmap" attributes 
     * on local SDP.
     * If some exist, we add it to newSdp negotiated SDP result.
     * 
     * @param localSdp
     * @param remoteSdp
     * 
     * @return Returns the new local descriptor as a result of media 
     *         payloads negotiation.
     */
    public static SessionDescriptor makeMediaPayloadsNegotiation( 
            SessionDescriptor localSdp, SessionDescriptor remoteSdp ) {
        
        SessionDescriptor newSdp = null;
        
        printLog( "makeMediaPayloadsNegotiation", "Init..." );
        
        try {
            
            newSdp = new SessionDescriptor( remoteSdp.getOrigin(), remoteSdp.getSessionName(),
                    localSdp.getConnection(), localSdp.getTime() );
            
            Vector remoteDescriptors = remoteSdp.getMediaDescriptors();
            
            for ( Enumeration descriptorsEnum = remoteDescriptors.elements(); descriptorsEnum.hasMoreElements(); ) {
                
                MediaDescriptor remoteDescriptor = (MediaDescriptor) descriptorsEnum.nextElement();
                MediaDescriptor localDescriptor = localSdp.getMediaDescriptor( 
                        remoteDescriptor.getMedia().getMedia() );
                
                if ( localDescriptor != null ) {
                    
                    Vector remoteAttributes = remoteDescriptor.getAttributes( SIPCodec.ATTRIBUTE_RTPMAP );
                    Vector newSdpAttributes = new Vector();
                    
                    for ( Enumeration attributesEnum = remoteAttributes.elements(); attributesEnum.hasMoreElements(); ) {
                        
                        AttributeField remoteAttribute = (AttributeField) attributesEnum.nextElement();
                        
                        String payloadId = getPayloadIdFromAttribute( remoteAttribute );
                        
                        if ( "".equals( payloadId ) ) {
                            
                            printLog( "makeMediaPayloadsNegotiation", 
                                    "Error! Payload id not found on attribute: Name = [" + 
                                    remoteAttribute.getAttributeName() + "], Value = [" + 
                                    remoteAttribute.getAttributeValue() + "]." );
                        }
                        else if ( findAttributeByPayloadId(
                                remoteAttribute.getAttributeName(), payloadId, 
                                localDescriptor ) != null ) {
                            
                            newSdpAttributes.add( remoteAttribute );
                        }
                    }
                    
                    // Calculate the format list to be used on MediaDescriptor creation.
                    String formatList = getFormatList( newSdpAttributes );
                    
                    for ( Enumeration attributesEnum = newSdpAttributes.elements(); attributesEnum.hasMoreElements(); ) {
                        
                        AttributeField mediaAttribute = (AttributeField) attributesEnum.nextElement();
                        
                        if ( newSdp.getMediaDescriptors().size() == 0 ) {
                            
                            newSdp.addMediaDescriptor( new MediaDescriptor(
                                    new MediaField( localDescriptor.getMedia().getMedia(), 
                                            localDescriptor.getMedia().getPort(), 
                                            0, 
                                            localDescriptor.getMedia().getTransport(), 
                                            formatList ), 
                                    localDescriptor.getConnection() ) );
                        }
                        
                        newSdp.getMediaDescriptor( localDescriptor.getMedia().getMedia() ).
                                addAttribute( mediaAttribute );
                    }
                }
            }
        }
        catch ( Exception exception ) {
            
            printLog( "makeMediaPayloadsNegotiation", "Failure creating initial SDP: " );
            exception.printStackTrace();
        }
        
        printLog( "makeMediaPayloadsNegotiation", "End..." );
        
        return newSdp;
    }
    
    
    /**
     * Parameter "newSdp" must be the returning value from method's 
     * "makeMediaPayloadsNegotiation" execution.
     * Here the pending attributes will be negotiated as well.
     * 
     * @param newSdp
     * @param localSdp
     * @param remoteSdp
     * 
     */
    public static void completeSdpNegotiation(
    		SessionDescriptor newSdp, SessionDescriptor localSdp, 
    		SessionDescriptor remoteSdp ) {
        
        printLog( "makeSdpNegotiation", "Init..." );
        
        try {
            
            if ( newSdp.getMediaDescriptors().size() == 0 ) {
                
                // Something is wrong.
                // We should have at least a "audio" media descriptor with 
                // all audio payloads suported.
                
                printLog( "completeSdpNegotiation", 
                        "Error! No media descriptors after \"makeMediaPayloadsNegotiation\"." );
                
                return;
            }
            
            Vector remoteDescriptors = remoteSdp.getMediaDescriptors();
            
            for ( Enumeration descriptorsEnum = remoteDescriptors.elements(); descriptorsEnum.hasMoreElements(); ) {
                
                MediaDescriptor remoteDescriptor = (MediaDescriptor) descriptorsEnum.nextElement();
                MediaDescriptor localDescriptor = localSdp.getMediaDescriptor( 
                        remoteDescriptor.getMedia().getMedia() );
                
                if ( localDescriptor != null ) {
                    
                    // First we make the negotiation of remote attributes with 
                    // local ones to generate the new SDP "newSdp".
                    
                    Vector remoteAttributes = remoteDescriptor.getAttributes();
                    
                    for ( Enumeration atributesEnum = remoteAttributes.elements(); atributesEnum.hasMoreElements(); ) {
                        
                        AttributeField remoteAttribute = (AttributeField) atributesEnum.nextElement();
                        
                        makeAttributeNegotiation( newSdp, localDescriptor, remoteAttribute );
                    }
                    
                    // Now we add to "newSdp" all the local attributes that 
                    // were not negotiated yet.
                    
                    Vector localAttributes = localDescriptor.getAttributes();
                    
                    for ( Enumeration atributesEnum = localAttributes.elements(); atributesEnum.hasMoreElements(); ) {
                        
                        AttributeField localAttribute = (AttributeField) atributesEnum.nextElement();
                        MediaDescriptor newLocalDescriptor = 
                                newSdp.getMediaDescriptor( localDescriptor.getMedia().getMedia() );
                        
                        if ( isPayloadRelatedAttribute( localAttribute ) ) {
                            
                            String payloadId = getPayloadIdFromAttribute( localAttribute );
                            
                            if ( findAttributeByPayloadId( 
                                    localAttribute.getAttributeName(), 
                                    payloadId, 
                                    newLocalDescriptor) == null ) {
                                
                                newLocalDescriptor.addAttribute( localAttribute );
                            }
                        }
                        else if ( newLocalDescriptor.getAttribute( 
                                localAttribute.getAttributeName() ) == null ) {
                            
                            newLocalDescriptor.addAttribute( localAttribute );
                        }
                    }
                }
            }
        }
        catch ( Exception exception ) {
            
            printLog( "completeSdpNegotiation", "Failure creating initial SDP: " );
            exception.printStackTrace();
        }
        
        printLog( "completeSdpNegotiation", "End..." );
    }
    
    
    /**
     * Here we make the negotiation of all attributes besides "rtpmap" (
     * these are negotiated on "makeMediaPayloadsNegotiation" method).
     * 
     * @param newSdp
     * @param localMedia
     * @param remoteAttribute
     */
    private static void makeAttributeNegotiation( 
            SessionDescriptor newSdp, MediaDescriptor localMedia, AttributeField remoteAttribute ) {
        
        printLog( "makeAttributeNegotiation", "Init..." );
        
        try {
            
            printLog( "makeAttributeNegotiation", 
                    "AttributeName = [" + remoteAttribute.getAttributeName() + 
                    "], AttributeValue = [" + remoteAttribute.getAttributeValue() + "].");
            
            if ( remoteAttribute.getAttributeName().equals( SIPCodec.ATTRIBUTE_RTPMAP ) ) {
                
                printLog( "makeAttributeNegotiation", 
                        "\"rtpmap\" attributes were already negotiated." );
            }
            else if ( !isPayloadRelatedAttribute( remoteAttribute ) ) {
                
                // We do nothing with attributes that are not payload 
                // related, like: "ptime", "direction", etc.
                // For now, we consider that they don't demand negotiation.
                
                printLog( "makeAttributeNegotiation", 
                        "Attribute is not payload related. Do not negotiate it..." );
            }
            else {
                
                String payloadId = getPayloadIdFromAttribute( remoteAttribute );
                
                if ( "".equals( payloadId ) ) {
                    
                    printLog( "makeAttributeNegotiation", 
                            "Error! Payload id not found on attribute: Name = [" + 
                            remoteAttribute.getAttributeName() + "], Value = [" + 
                            remoteAttribute.getAttributeValue() + "]." );
                }
                // We must be sure this attribute is related with a payload 
                // already present on newSdp.
                else if ( findAttributeByPayloadId( SIPCodec.ATTRIBUTE_RTPMAP, payloadId, 
                        newSdp.getMediaDescriptor( localMedia.getMedia().getMedia() ) ) != null ) {
                    
                    printLog( "makeAttributeNegotiation", 
                            "Payload " + payloadId + " present on newSdp." );
                    
                    AttributeField localAttribute = findAttributeByPayloadId( 
                            remoteAttribute.getAttributeName(), payloadId, localMedia );
                    
                    SIPCodec sipCodec = SIPCodecFactory.getInstance().getSIPAudioCodec(
                            Integer.valueOf( payloadId ) );
                    
                    if ( sipCodec != null ) {
                        
                        String localAttibuteValue = "";
                        
                        if ( localAttribute != null ) {
                            
                            localAttibuteValue = localAttribute.getAttributeValue();
                        }
                        else {
                            
                            printLog( "makeAttributeNegotiation", 
                                    "Attribute not found on local media." );
                        }
                        
                        String attributeValueResult = sipCodec.codecNegotiateAttribute(
                                remoteAttribute.getAttributeName(), 
                                localAttibuteValue, 
                                remoteAttribute.getAttributeValue() );
                        
                        if ( ( attributeValueResult != null ) && ( !"".equals( attributeValueResult ) ) ) {
                            
                            newSdp.getMediaDescriptor( localMedia.getMedia().getMedia() ).
                                    addAttribute( new AttributeField( 
                                            remoteAttribute.getAttributeName(), attributeValueResult ) );
                        }
                    }
                    else {
                        
                        printLog( "makeAttributeNegotiation", "Codec not found!" );
                    }
                }
            }
        }
        catch ( Exception exception ) {
            
            printLog( "makeAttributeNegotiation", "Failure creating initial SDP: " );
            exception.printStackTrace();
        }
        
        printLog( "makeAttributeNegotiation", "End..." );
    }
    
    
    private static AttributeField findAttributeByPayloadId( 
            String attributeName, String payloadId, MediaDescriptor mediaDescriptor ) {
        
        printLog( "findAttributeByPayloadId", "Init..." );
        
        AttributeField searchingMediaAttribute = null;
        
        printLog( "findAttributeByPayloadId", 
                "attributeName = [" + attributeName + 
                "], payloadId = [" + payloadId + "]." );
        
        Vector mediaAttributes = mediaDescriptor.getAttributes( attributeName );
        
        for ( Enumeration attributesEnum = mediaAttributes.elements(); attributesEnum.hasMoreElements(); ) {
            
            AttributeField mediaAttribute = (AttributeField) attributesEnum.nextElement();
            
            printLog( "findAttributeByPayloadId", 
                    "Validating attribute with name = [" + mediaAttribute.getAttributeName() + 
                    "] and value = [" + mediaAttribute.getAttributeValue() + "]." );
            
            if ( getPayloadIdFromAttribute( mediaAttribute ).equals( payloadId ) ) {
                
                searchingMediaAttribute = mediaAttribute;
                break;
            }
        }
        
        if ( searchingMediaAttribute != null ) {
            
            printLog( "findAttributeByPayloadId", 
                    "Attribute found with name = [" + 
                    searchingMediaAttribute.getAttributeName() + "] and value = [" + 
                    searchingMediaAttribute.getAttributeValue() + "]." );
        }
        else {
            
            printLog( "findAttributeByPayloadId", 
                    "Attribute with name [" + attributeName + 
                    "] and payloadId [" + payloadId + "] was not found." );
        }
        
        printLog( "findAttributeByPayloadId", "End..." );
        
        return searchingMediaAttribute;
    }
    
    
    private static String getPayloadIdFromAttribute( AttributeField attribute ) {
        
        String payloadId = "";
        
        printLog( "getPayloadIdFromAttribute", "Init..." );
        
        printLog( "getPayloadIdFromAttribute", 
                "AttributeName = [" + attribute.getAttributeName() + 
                "], AttributeValue = [" + attribute.getAttributeValue() + "]." );
        
        if ( isPayloadRelatedAttribute( attribute ) ) {
            
            payloadId = attribute.getAttributeValue().substring( 0, 
                    attribute.getAttributeValue().indexOf( " " ) );
        }
        
        printLog( "getPayloadIdFromAttribute", "payloadId = " + payloadId ); 
        
        printLog( "getPayloadIdFromAttribute", "End..." );
        
        return payloadId;
    }
    
    
    private static boolean isPayloadRelatedAttribute( AttributeField attribute ) {
        
        boolean isPayloadAttribute = false;
        
        printLog( "isPayloadRelatedAttribute", "Init..." );
        
        printLog( "isPayloadRelatedAttribute", 
                "AttributeName = [" + attribute.getAttributeName() + 
                "], AttributeValue = [" + attribute.getAttributeValue() + "]." );
        
        if ( ( attribute.getAttributeName().compareToIgnoreCase( SIPCodec.ATTRIBUTE_RTPMAP ) == 0 ) || 
                ( attribute.getAttributeName().compareToIgnoreCase( SIPCodec.ATTRIBUTE_AS ) == 0 ) || 
                ( attribute.getAttributeName().compareToIgnoreCase( SIPCodec.ATTRIBUTE_FMTP ) == 0 ) ) {
            
            isPayloadAttribute = true;
        }
        
        printLog( "isPayloadRelatedAttribute", "isPayloadAttribute = " + isPayloadAttribute ); 
        
        printLog( "isPayloadRelatedAttribute", "End..." );
        
        return isPayloadAttribute;
    }


    private static void printLog( String method, String message ) {
        
        log.debug( "SdpUtils - " + method + " -> " + message );
        System.out.println( "SdpUtils - " + method + " -> " + message );
    }
}
