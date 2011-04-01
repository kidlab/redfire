package org.red5.server.webapp.sip;

import java.io.File;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Config {

	private String codecsPrecedence = "18;111;8;0";
	private int startSIPPort = 5070;
	private int startRTPPort = 3000;
	private int SIPPortNum = 30;
	private int SIPPortStep = 2;
	private int RTPPortNum = 30;
	private int RTPPortStep = 2;
	private Boolean normalizeVolume = true;

	private static Config singletonConfig;

	private Config() {

		String appPath = System.getProperty("user.dir");
		String configFile = appPath + File.separator + ".." + File.separator + "plugins" + File.separator + "redfire" + File.separator + "phone" + File.separator + "red5phone.xml";

		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new File(configFile));

			// Read codecsPrecedence TAG
			Element tagCodecsPrecedence = (Element) doc.getElementsByTagName("codecsPrecedence").item(0);
			if ((tagCodecsPrecedence.getTextContent() != null) && (tagCodecsPrecedence.getTextContent().length() > 0)) {
				codecsPrecedence = tagCodecsPrecedence.getTextContent();
			}

			// Read SIPPorts TAG
			Element tagSIPPorts = (Element) doc.getElementsByTagName("SIPPorts").item(0);
			if ((tagSIPPorts.getTextContent() != null) && (tagSIPPorts.getTextContent().length() > 0)) {
				startSIPPort = Integer.parseInt(tagSIPPorts.getTextContent());
			}

			// Read RTPPorts TAG
			Element tagRTPPorts = (Element) doc.getElementsByTagName("RTPPorts").item(0);
			if ((tagRTPPorts.getTextContent() != null) && (tagRTPPorts.getTextContent().length() > 0)) {
				startRTPPort = Integer.parseInt(tagRTPPorts.getTextContent());
			}

			// Read SIPPortNum TAG
			Element tagSIPPortNum = (Element) doc.getElementsByTagName("SIPPortNum").item(0);
			if ((tagSIPPortNum.getTextContent() != null) && (tagSIPPortNum.getTextContent().length() > 0)) {
				SIPPortNum = Integer.parseInt(tagSIPPortNum.getTextContent());
			}

			// Read SIPPortStep TAG
			Element tagSIPPortStep = (Element) doc.getElementsByTagName("SIPPortStep").item(0);
			if ((tagSIPPortStep.getTextContent() != null) && (tagSIPPortStep.getTextContent().length() > 0)) {
				SIPPortStep = Integer.parseInt(tagSIPPortStep.getTextContent());
			}

			// Read RTPPortNum TAG
			Element tagRTPPortNum = (Element) doc.getElementsByTagName("RTPPortNum").item(0);
			if ((tagRTPPortNum.getTextContent() != null) && (tagRTPPortNum.getTextContent().length() > 0)) {
				RTPPortNum = Integer.parseInt(tagRTPPortNum.getTextContent());
			}

			// Read RTPPortStep TAG
			Element tagRTPPortStep = (Element) doc.getElementsByTagName("RTPPortStep").item(0);
			if ((tagRTPPortStep.getTextContent() != null) && (tagRTPPortStep.getTextContent().length() > 0)) {
				RTPPortStep = Integer.parseInt(tagRTPPortStep.getTextContent());
			}

			// Read normalizeVolume TAG
			Element tagNormalizeVolume = (Element) doc.getElementsByTagName("normalizeVolume").item(0);
			if ((tagNormalizeVolume.getTextContent() != null) && (tagNormalizeVolume.getTextContent().length() > 0)) {
				normalizeVolume = Integer.parseInt(tagNormalizeVolume.getTextContent()) == 1 ? true : false ;
			}

			System.out.println(String.format("Read config file: %s", configFile));

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static Config getInstance() {

		if (singletonConfig == null) {

			singletonConfig = new Config();
		}

		return singletonConfig;
	}

	public String getCodecsPrecedence() {
		return codecsPrecedence;
	}

	public int getStartSIPPort() {
		return startSIPPort;
	}

	public int getStartRTPPort() {
		return startRTPPort;
	}

	public int getSIPPortNum() {
		return SIPPortNum;
	}

	public int getSIPPortStep() {
		return SIPPortStep;
	}

	public int getRTPPortNum() {
		return RTPPortNum;
	}

	public int getRTPPortStep() {
		return RTPPortStep;
	}

	public Boolean getNormalizeVolume() {
		return normalizeVolume;
	}

}