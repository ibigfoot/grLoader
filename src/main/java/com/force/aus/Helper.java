package com.force.aus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Helper {

	public static Properties loadProperties (String propertiesFileName) {
		
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(new File(Constants.APP_PROPS_FILE)));
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException();
		}
		return props;
	}
}
