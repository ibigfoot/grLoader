package com.force.aus;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

/**
 * Hello world!
 *
 */
public class Loader {
	
	public static Properties appProps;
	
    public static void main( String[] args ) {
    	
    	Loader l = new Loader();
    	
		appProps = Helper.loadProperties(Constants.APP_PROPS_FILE);
		
		System.out.println("Here are the properties we are going to run with:");
		System.out.println("Use Sandbox environment? - " + appProps.get(Constants.PROP_USE_SANDBOX));
		System.out.println("Salesforce User - " + appProps.get(Constants.PROP_FORCE_USER));
		System.out.println("Salesforce Pass - " + appProps.get(Constants.PROP_FORCE_PASS));
	    System.out.println("Salesforce Security Token - "+appProps.getProperty(Constants.PROP_FORCE_SECURITY_TOKEN));
		System.out.println("Path to Data Files - " + appProps.get(Constants.PROP_DATA_LOCATION));
		System.out.println("Constituent filename - " + appProps.get(Constants.PROP_CONSTITUENT_FILE_NAME));
		System.out.println("Order Detail filename - " + appProps.get(Constants.PROP_ORDER_FILE_NAME));
		System.out.println("Are these OK? (Yes or No)");
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			String response = reader.readLine();
			if(response.startsWith("Y") || response.startsWith("y")) {
				l.run();
			} else {
				System.out.println("Please adjust the properties file "+Constants.APP_PROPS_FILE+" and reload before running");
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	private void run() {
		System.out.println("We should now be running successfully.");
		
		ConnectorConfig config = new ConnectorConfig();
		config.setAuthEndpoint(appProps.getProperty(Constants.PROP_AUTH_ENDPOINT));
		config.setUsername(appProps.getProperty(Constants.PROP_FORCE_USER));
		config.setPassword(appProps.getProperty(Constants.PROP_FORCE_PASS) + appProps.getProperty(Constants.PROP_FORCE_SECURITY_TOKEN));
		
		config.setCompression(true);
		config.setPrettyPrintXml(true);
		config.setTraceMessage(false);
		
		try {
			// load XML mapping file
			Map<String, ObjectMapping> mappings = loadMappings();
			
			// Connect to Salesforce
			PartnerConnection conn = new PartnerConnection(config);
			
			// Validate that mappings are correct to Salesforce
			Iterator<String> mappingIterator = mappings.keySet().iterator();
			while(mappingIterator.hasNext()) {
				String objectName = mappingIterator.next();
				ObjectMapping mapping = mappings.get(objectName);
				mapping.setDescribeResult(conn.describeSObject(objectName));
				
				if(mapping.getObjectName().equalsIgnoreCase("account")) {
					mapping.loadCSVData(appProps.getProperty(Constants.PROP_DATA_LOCATION) + appProps.getProperty(Constants.PROP_CONSTITUENT_FILE_NAME));
				} 
				if(mapping.getObjectName().equalsIgnoreCase("order_details__c")) {
					mapping.loadCSVData(appProps.getProperty(Constants.PROP_DATA_LOCATION) + appProps.getProperty(Constants.PROP_ORDER_FILE_NAME));
				}
				File errorFile = new File(appProps.getProperty(Constants.PROP_ERROR_FILE));
				System.out.println("Loading "+mapping.getObjectName());
				mapping.loadDataIntoSalesforce(conn, errorFile);
			}
			
			conn.logout();
			
		} catch (ConnectionException ce) {
			ce.printStackTrace();
			throw new RuntimeException();
		} catch (MappingValidationException mve) {
			mve.printStackTrace();
			throw new RuntimeException();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	private Map<String, ObjectMapping> loadMappings() {
		
		File xmlFile = new File(appProps.getProperty(Constants.PROP_MAPPING_LOCATION)+appProps.getProperty(Constants.PROP_MAPPING_FILE));
		Map<String, ObjectMapping> objectMappings = new HashMap<String, ObjectMapping>();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document doc = builder.parse(xmlFile);
			
			doc.getDocumentElement().normalize();
			
			NodeList nodeList = doc.getElementsByTagName("objectMapping");
			
			for(int i=0 ; i<nodeList.getLength(); i++) {
				Node n = nodeList.item(i);
				ObjectMapping mapping = new ObjectMapping(n.getAttributes().getNamedItem("name").getNodeValue());
				processFieldMappings(mapping, n.getChildNodes());
				objectMappings.put(mapping.getObjectName(), mapping);
			}
			
			
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
			throw new RuntimeException();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException();
		} catch (SAXException saxe) {
			saxe.printStackTrace();
			throw new RuntimeException();
		}
		return objectMappings;
	}
	
	private void processFieldMappings(ObjectMapping mapping, NodeList nodes) {
		
		for(int i=0 ; i<nodes.getLength() ; i++) {
			Node n = nodes.item(i);
			if(n.getNodeName().equalsIgnoreCase("fieldMapping")) {
				mapping.addFieldMapping(n.getAttributes().getNamedItem("sfFieldName").getNodeValue(), 
						n.getAttributes().getNamedItem("csvFieldName").getNodeValue());		
			}
		}
 	}

}
