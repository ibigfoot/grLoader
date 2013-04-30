package com.force.aus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class Cleaner {

	public static Properties appProps;
	
	
	public static void main(String [] args) throws Exception {
		Cleaner c = new Cleaner();
		appProps = Helper.loadProperties(Constants.APP_PROPS_FILE);
		
		System.out.println("Here are the properties we are going to run with:");
		System.out.println("Use Sandbox environment? - " + appProps.get(Constants.PROP_USE_SANDBOX));
		System.out.println("Salesforce User - " + appProps.get(Constants.PROP_FORCE_USER));
		System.out.println("Salesforce Pass - " + appProps.get(Constants.PROP_FORCE_PASS));
		System.out.println("Path to Data Files - " + appProps.get(Constants.PROP_DATA_LOCATION));
		System.out.println("Constituent filename - " + appProps.get(Constants.PROP_CONSTITUENT_FILE_NAME));
		System.out.println("Order Detail filename - " + appProps.get(Constants.PROP_ORDER_FILE_NAME));
		System.out.println("Are these OK? (Yes or No)");
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			String response = reader.readLine();
			if(response.startsWith("Y") || response.startsWith("y")) {
				c.run();
			} else {
				System.out.println("Please adjust the properties file "+Constants.APP_PROPS_FILE+" and reload before running");
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	private void run() throws Exception {
		String fileName = appProps.getProperty(Constants.PROP_DATA_LOCATION) + appProps.getProperty(Constants.PROP_CONSTITUENT_FILE_NAME);
		BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
		FileWriter writer = new FileWriter(new File("cleanFile.csv"));
		String line = reader.readLine();
		writer.write(line);
		
		while((line = reader.readLine()) != null) {
			String[] data = line.split(",");
			int i=0;
			for(String s : data) {
				i++;
				if(s.contains("T") && s.endsWith(":"))
					s = s + "00";
				writer.write(s);
				if(i != data.length) 
					writer.write(",");
				else 
					writer.write("\n");
			}
		}
		
		reader.close();
		writer.close();
		System.out.println("Done....");
	}
}
