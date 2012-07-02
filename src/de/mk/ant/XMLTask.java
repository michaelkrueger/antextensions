package de.mk.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;

public class XMLTask extends Task {
	
	File xmlFile;
	java.lang.String element;
	java.lang.String attribute;
	java.lang.String property;
	
	public java.lang.String getElement() {
		return element;
	}
	public void setElement(java.lang.String element) {
		this.element = element;
	}
	public java.lang.String getAttribute() {
		return attribute;
	}
	public void setAttribute(java.lang.String attribute) {
		this.attribute = attribute;
	}
	public File getXmlFile() {
		return xmlFile;
	}
	public void setXmlFile(File xmlFile) {
		this.xmlFile = xmlFile;
	}
	public java.lang.String getProperty() {
		return property;
	}
	public void setProperty(java.lang.String property) {
		this.property = property;
	}
	
	private void setNewValue(java.lang.String v) {
		java.lang.String n = getProperty();
		PropertyHelper ph = PropertyHelper.getPropertyHelper(getProject());
		Class [] parameters = {java.lang.String.class, Object.class };
//		try {
//			log("setNewValue:PropertyHelper:"+ph.getClass()+" newValue-Method"+Arrays.asList(ph.getClass().getMethods()));
//		} catch (SecurityException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} 
		log("String:setNewValue:name="+n+" value="+v);
		ph.setProperty(null, n, v, true);
//		if (ph.getUserProperty(n) == null) {
//        } else {
//        	log("Override ignored for " + n, Project.MSG_VERBOSE);
//        	ph.setInheritedProperty(n, v);
//        }
	}
	
	public void execute() throws BuildException {
		
		//Lese die Datei ein.
		java.lang.StringBuffer buffer = new StringBuffer();
		try  {
			BufferedReader reader = new BufferedReader(new FileReader(getXmlFile()));
			while (reader.ready()) {
				buffer.append(reader.readLine());
				buffer.append('\n');
			}
			reader.close();
		} catch (Exception ex) {
			throw new BuildException(ex);
		}
		//Suche nach dem ersten auftreten des Elements

		java.lang.String value = null;
		int index_element = buffer.indexOf(getElement());
		if (index_element>=0) {
			//hole das attribut
			int index_attrpos = buffer.indexOf(getAttribute(), index_element+getElement().length());
			if (index_attrpos>=0) {
				int start = buffer.indexOf("\"", index_attrpos+getAttribute().length())+1;
				int end = buffer.indexOf("\"", start+1);
					
				value = buffer.substring(start, end);
			}			
		} 
		
		
		//schreibe es in die Property
		if (value!=null) {
			setNewValue(value);
		} else {
			throw new BuildException("Value of "+getElement()+"."+getAttribute()+" not found in "+getXmlFile());
		}
		
	}
	

}
