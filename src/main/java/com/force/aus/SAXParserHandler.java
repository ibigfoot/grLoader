package com.force.aus;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXParserHandler extends DefaultHandler{

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		System.out.println("Start ELement URI ["+uri+"] localName["+localName+"] qName ["+qName+"]");
		int numberOfAttributes = attributes.getLength();
		System.out.println("Number of attributes ["+numberOfAttributes+"]");
		for(int i=0 ; i<numberOfAttributes ; i++) {
			System.out.println("Attribute URI ["+attributes.getURI(i)+"] localName ["+attributes.getLocalName(i)+"] qName["+attributes.getQName(i)+"] type ["+attributes.getType(i)+"]");
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		System.out.println("End Element URI ["+uri+"] localName ["+localName+"] qName ["+qName+"]");
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		
		System.out.println("Characters start ["+start+"] length ["+length+"] ");
		for(Character c : ch) 
			System.out.print(c);
		System.out.println();
	}

	
}
