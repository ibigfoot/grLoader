package com.force.aus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.FieldType;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.UpsertResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

public class ObjectMapping {

	
	private Map<String, String> fieldMappings;
	private String objectName;
	private DescribeSObjectResult describeResult;
	
	private String[] fieldNames;
	private List<String[]> dataLines;
	
	/**
	 * Construct this mapping object. 
	 * The name of the object is the Salesfoce API name and should be specified
	 * in the ObjectMapping.xml file.
	 * 
	 * @param objectName
	 */
	public ObjectMapping(String objectName) {
		this.objectName = objectName;
		fieldMappings = new HashMap<String, String>();
	}
	
	/**
	 * Adds a single field mapping 
	 * @param sfdcName
	 * @param csvName
	 */
	public void addFieldMapping(String sfdcName, String csvName) {
		fieldMappings.put(sfdcName, csvName);
	}
	
	/**
	 * Get Map of field mappings defined in the mapping xml file
	 * @return
	 */
	public Map<String, String> getFieldMappings() {
		return fieldMappings;
	}
	
	/**
	 * Get collection of all Salesforce Fields defined in the mapping file.
	 * @return
	 */
	public Set<String> getSFDCFieldNames() {
		return fieldMappings.keySet();
	}
	/**
	 * get collection of all the CSV fields defined in the mapping file.
	 * @return
	 */
	public Collection<String> getCSVFieldNames() {
		return fieldMappings.values();
	}
	/**
	 * Get the Salesforce API name of the object this mapping is defined for.
	 * @return
	 */
	public String getObjectName() {
		return  objectName;
	}
	
	/**
	 * Load data and validate mapping.
	 * @param filename
	 */
	public void loadCSVData (String filename) {
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
			String line = reader.readLine();
			validateCSVMapping(line);
			dataLines = new ArrayList<String[]>();
			
			while((line = reader.readLine()) != null) {
				dataLines.add(line.split(","));
			}
			reader.close();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			throw new RuntimeException();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	private void validateCSVMapping(String line) {
		fieldNames = line.split(",");
		Collection<String> values = getCSVFieldNames();
		Set<String> headerFields = new HashSet<String>();
		
		Set<String> unmappedHeaders = new HashSet<String>();
		for(int i=0 ; i<fieldNames.length ; i++) {
			headerFields.add(fieldNames[i]);
		}
		
		Iterator<String> mappedFields = values.iterator();
		while(mappedFields.hasNext()) {
			String field = mappedFields.next();
			if(!headerFields.contains(field))
				unmappedHeaders.add(field);
		}
		
		
		if(!unmappedHeaders.isEmpty()) {
			Iterator<String> unmapped = unmappedHeaders.iterator();
			String error = "The fields ";
			while(unmapped.hasNext()) {
				error = error + "[" + unmapped.next() + "]";
			}
			error = error + " have been incorrectly mapped in the XML file but doesn't exist in the data file.";
		}
	}

	/**
	 * Add the Salesforce describe result for this object.
	 * 
	 * @param describeResult
	 */
	public void setDescribeResult(DescribeSObjectResult describeResult) throws MappingValidationException {
		this.describeResult = describeResult;
		validate();
	}
	
	/**
	 * Validate that fields mapped in the xml exist in the Salesforce object.
	 * 
	 * @throws MappingValidationException
	 */
	private  void validate() throws MappingValidationException {
		
		if(describeResult == null) 
			throw new NullPointerException("There is no DescribeSObjectResult defined in the object mapping for ["+getObjectName()+"]");
		
		Field[] fields = describeResult.getFields();
		Iterator<String> sfdcFields = getSFDCFieldNames().iterator();
		
		Set<String> describeFields = new HashSet<String>();
		for(int i=0 ; i<fields.length ; i++) {
			describeFields.add(fields[i].getName());
		}
		Set<String> unmappedFields = new HashSet<String>();
		while(sfdcFields.hasNext()) {
			String mappedField = sfdcFields.next();
			if(!describeFields.contains(mappedField)) {
				unmappedFields.add(mappedField);
			}
		}
		
		if(!unmappedFields.isEmpty()) {
			Iterator<String> it = unmappedFields.iterator();
			String error = "The mapped fields [";
			while(it.hasNext()) {
				error = error + "][" + it.next();
			}
			error = error + "] are not visible in the describe object ["+getObjectName()+"]. Check your mapping XML";
			throw new MappingValidationException(error);
		}
	}
	
	/**
	 * Takes object mapping, creates SObject array and uses the Upsert API call to insert into Salesforce.
	 * Will look for an External ID field on the object using the MetaData API and attempt to use this as the upsert value
	 * @param pc
	 * @param errorFile
	 * @throws IOException
	 * @throws ConnectionException
	 */
	public void loadDataIntoSalesforce(PartnerConnection pc, File errorFile) throws IOException, ConnectionException {
		
		int line = 2;
		List<SObject> objects = new ArrayList<SObject>();
		String externalId = "";
		for(String [] dataLine : dataLines) {
			//System.out.println("Object ["+ line++ +"] - "+getObjectName());
			try {
				SObject sobj = new SObject();
				sobj.setType(getObjectName());
				// load data fields
				for(int i=0 ; i<dataLine.length ; i++) {
					String data = dataLine[i];
					String csvLabel = fieldNames[i];
					String sfdcFieldName = getSFDCFieldName(csvLabel);
			
					if(sfdcFieldName != null) {
						Field f = getField(sfdcFieldName);
						if(f.getExternalId()) {
							externalId = f.getName();
						}

						sobj.setField(sfdcFieldName, data);
					}
					
				}
				
				objects.add(sobj);
			} catch (ArrayIndexOutOfBoundsException aiobe) {
				FileWriter writer = new FileWriter(errorFile);
				writer.write("Line ["+line+"] is in error, skiping...\n");
				writer.close();
			}
		}
		System.out.println("Attempting upsert of ["+objects.size()+"] records ["+getObjectName()+"] ");
		List<SObject> objs = new ArrayList<SObject>();
		List<UpsertResult> allResults = new ArrayList<UpsertResult>();
		for (int i=0 ; i<objects.size() ; i++) {
			
			objs.add(objects.get(i));
			if(i % 200 == 0) {
				System.out.println("Attempting upsert of "+objs.size()+" "+getObjectName()+" objects");
				UpsertResult[] results = pc.upsert(externalId, objs.toArray(new SObject[]{}));
				allResults.addAll(Arrays.asList(results));
				objs = new ArrayList<SObject>();
			}
		}
		
		if(!allResults.isEmpty()) {
			FileWriter writer  = new FileWriter(errorFile, true);
			for(UpsertResult r : allResults) {
				if(!r.isSuccess()) {
					Error[] errors = r.getErrors();
					for(Error e : errors) {
						System.out.print("Error ["+e.getMessage()+"] -");
						writer.write("Error ["+e.getMessage()+"] -:");
					}
					System.out.print("\n");
					writer.write("\n");
				}
				
			}
			writer.close();
		}
		
	}
	
	private Field getField(String fieldName) {
		for(Field f : describeResult.getFields()) {
			if(f.getName().equalsIgnoreCase(fieldName))
				return f;
		}
		return null;
	}
	
	private String getSFDCFieldName(String csvFieldName) {
		
		Iterator<String> it = getSFDCFieldNames().iterator();
		while(it.hasNext()) {
			String sfdcFieldName = it.next();
			String name = fieldMappings.get(sfdcFieldName);
			if(name.equalsIgnoreCase(csvFieldName))
				return sfdcFieldName;
		}
		return null;
	}
}
