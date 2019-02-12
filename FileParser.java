package com.file.parsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import au.com.bytecode.opencsv.CSVReader;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.*;

public class FileParser {

	private static final String PROPERTIESFILE = "/fileFeedProperties.properties";
	private static final String SYSTEMPATH = "."; //can define system path
	private static final String COMMA_DELIMITER = ",";
	public static String systemPath;
	private static String fileName = "";
	private static Properties props = null;
	private static File propsFile = null;
	public HashMap map;
	static FileOutputStream fos;
	static File file;
	static String outFilename = "";
	static final String header=StringUtils.rightPad("REFERENCE",10)+ StringUtils.rightPad("DESCRIPTION",40)+StringUtils.rightPad("REASON",30);
	static String trailer=StringUtils.rightPad("COUNT",10);
	
	/**
	 * 
	 * @throws FileNotFoundException
	 */
	public FileParser() throws FileNotFoundException {
		systemPath = System.getProperty(SYSTEMPATH);
		if (systemPath == null)
			systemPath = ".";
		propsFile = new File(systemPath + PROPERTIESFILE);
		load();
		outFilename = props.getProperty("OutputFile");
		file = new File(outFilename);
	}
/**
 * @load default property files 
 */
	private static void load() {
		try {
			FileInputStream fi = new FileInputStream(propsFile);
			props = new Properties();
			props.load(fi);
			fi.close();
			} catch (Exception x) {
			}
		if (props == null)
			props = new Properties();
	}
/**
 * 
 * @param path
 * @return List
 */
	public ArrayList<String> parseXmlFile(String path) {

		ArrayList<String> outputList = new ArrayList<String>();
		try {
			HashMap<String, String> map = new HashMap<String, String>();
			HashSet<String> failedSet = new HashSet<String>();
			String line = "";
			String reason=null;
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(path);
			doc.getDocumentElement().normalize();
			NodeList record_list = doc.getElementsByTagName("record");
			if (record_list != null) {
				String reference = null;
				String description = null;
				double startBalance=0;
				double mutation=0;
				double endBalance=0;
				for (int i = 0; i < record_list.getLength(); i++) {

					Node recordNode = record_list.item(i);
					
					if (recordNode.getNodeType() == Node.ELEMENT_NODE) {
						Element resource = (Element) recordNode;
						if (resource != null) {
							reference = resource.getAttribute("reference");
							description = resource.getElementsByTagName("description").item(0).getTextContent();
							startBalance=Double.parseDouble(resource.getElementsByTagName("startBalance").item(0).getTextContent());
							mutation=Double.parseDouble(resource.getElementsByTagName("mutation").item(0).getTextContent());
							endBalance=Double.parseDouble(resource.getElementsByTagName("endBalance").item(0).getTextContent());
						}
						if (!map.containsKey(reference)) {
							map.put(reference, description);
						} else {
							failedSet.add(reference);
							reason="Duplicate Record";
							line = StringUtils.rightPad(reference,10)+ StringUtils.rightPad(description,40)+StringUtils.rightPad(reason,30);
							outputList.add(line);
						}
						if(round2Decimal2s(startBalance+mutation) != endBalance){
							reason="End Balance not macthed";
							line = StringUtils.rightPad(reference,10)+ StringUtils.rightPad(description,40)+StringUtils.rightPad(reason,30);
							outputList.add(line);
							
						}
					}
				}
				Iterator itr = failedSet.iterator();
				String key = null;
				while (itr.hasNext()) {
					key = (String) itr.next();
					description = map.get(key);
					reason="Duplicate Record";
					line = StringUtils.rightPad(reference,10)+ StringUtils.rightPad(description,40)+StringUtils.rightPad(reason,30);
					outputList.add(line);

				}
			}

		} catch (Exception e) {
			
			e.printStackTrace();
		}
		return outputList;

	}
/**
 * 
 * @param path
 * @return ArrayList
 */
	public  ArrayList<String> parseCSVFile(String path) {
		BufferedReader br = null;
		String line = "";
		String reason=null;
		ArrayList<String> outputList = new ArrayList();
		try {

			CSVReader reader = new CSVReader(new FileReader(path), ',', '\'', 1);
			String[] nextLine;
			String ref = null;
			String desc = null;
			double startBalance=0;
			double mutation=0;
			double endBalance=0;
			
			HashMap<String, String> map = new HashMap<String, String>();

			HashSet<String> dupSet = new HashSet();
			
			while ((nextLine = reader.readNext()) != null) {
				for (int i = 0; i < nextLine.length; i++) {
					if (i == 0) {
						ref = nextLine[i];
					}
					if (i == 2) {
						desc = nextLine[i];
					}
					if (i == 3) {
						startBalance = Double.parseDouble(nextLine[i]);
					}
					if (i == 4) {
						mutation =  Double.parseDouble(nextLine[i]);
					}
					if (i == 5) {
						endBalance =  Double.parseDouble(nextLine[i]);
					}

				}
				if (!map.containsKey(ref)) {
					map.put(ref, desc);
				} else {
					reason="Duplicate Record";
					dupSet.add(ref);
					line = StringUtils.rightPad(ref,10)  +  StringUtils.rightPad(desc,40)+StringUtils.rightPad(reason,30);
					outputList.add(line);
				}
				if(round2Decimal2s(startBalance+mutation) != endBalance){
					reason="End Balance not matched Record";
					line = StringUtils.rightPad(ref,10)  +  StringUtils.rightPad(desc,40)+StringUtils.rightPad(reason,30);
					outputList.add(line);
					
				}
			}
			Iterator itr = dupSet.iterator();
			String key = null;
			while (itr.hasNext()) {
				key = (String) itr.next();
				desc = map.get(key);
				reason="Duplicate Record";
				line = StringUtils.rightPad(key,10) + StringUtils.rightPad(desc,40)+StringUtils.rightPad(reason,30);
				outputList.add(line);

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return outputList;
	}
/**
 * 
 * @param text
 * generate output file 
 */
	public static void write(List<String> text) {
		try {
			Integer count=0;
			fos.write(header.getBytes());
			fos.write(System.getProperty("line.separator").getBytes());
			for (String printText : text) {
				fos.write(printText.getBytes());
				fos.write(System.getProperty("line.separator").getBytes());
				count++;
			}
			trailer=StringUtils.rightPad(trailer,10)+StringUtils.right(count.toString(),10);
			fos.write(trailer.getBytes());
			

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public static double round2Decimal2s(double a)
	{
        BigDecimal bg = new BigDecimal(String.valueOf(Math.round(a*100.0)/100.0));
        return bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	public static void main(String args[]) throws Exception {
		FileParser fp=new FileParser();
		fos = new FileOutputStream(file);
		String dirname = null;
		String type = null;
		ArrayList<String> dupList = new ArrayList();

		dirname = props.getProperty("dirname");
		File dir = new File(dirname);
		String[] filelist = dir.list();
		
		if (filelist.length > 0) {
			Path filePath;
			for (int i = 0; i < filelist.length; i++) { //iterate all files in the directory
				String filename = filelist[i];
				filePath = Paths.get(dirname, filename);
				fileName = filename;
				type = Files.probeContentType(filePath); //determine type of file
				
				if (type.contains("xml")) {
					dupList.addAll(fp.parseXmlFile(dirname + "/" + filename));
					
				} else if (type.contains("ms-excel")) {
					dupList.addAll(fp.parseCSVFile(dirname + "/" + filename));
				}
				
			}
			write(dupList);
		} else {
			System.out.println("No files found in " + dirname);
		}
		
		System.out.println("File parsed and created FailedRecord.txt File"); 
	}
}
