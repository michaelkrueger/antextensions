package de.mk.ant;

import java.util.Arrays;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;

public class StringTask extends Task {

	java.lang.String property = null;
	java.lang.String value = null;
	java.lang.String operation = "substring";
	java.lang.String index = null;
	java.lang.String start = null;
	java.lang.String end   = null;
	
	public StringTask() {
		setTaskName("string");
		setDescription("String-Modification");
		
	}

	public void execute() throws BuildException {
		
		Operations op = Operations.valueOf(getOperation());
		if (op==null) throw new BuildException("Operation "+getOperation()+" unknown! Available operations:"+Arrays.asList(Operations.values()));
		
		op.execute(this);
	}

	public java.lang.String getOperation() {
		return operation;
	}

	// The Operation on value
	public void setOperation(java.lang.String operation) {
		this.operation = operation;
	}

	public java.lang.String getValue() {
		return value;
	}

	// The value to be changed
	public void setValue(java.lang.String value) {
		this.value = value;
	}

	public java.lang.String getProperty() {
		return property;
	}

	//The propery to store the value in
	public void setProperty(java.lang.String property) {
		this.property = property;
	}
	
	public java.lang.String getIndex() {
		return index;
	}

	//Parameter for the operation
	public void setIndex(java.lang.String index) {
		this.index = index;
	}

	public java.lang.String getStart() {
		return start;
	}

	//Parameter for the operation
	public void setStart(java.lang.String startindex) {
		this.start = startindex;
	}

	public java.lang.String getEnd() {
		return end;
	}

	//Parameter for the operation
	public void setEnd(java.lang.String endindex) {
		this.end = endindex;
	}


	private void setNewValue(java.lang.String v) {
		java.lang.String n = getProperty();
		PropertyHelper ph = PropertyHelper.getPropertyHelper(getProject());
		Class [] parameters = {java.lang.String.class, Object.class };
		try {
			log("setNewValue:PropertyHelper:"+ph.getClass()+" newValue-Method"+Arrays.asList(ph.getClass().getMethods()));
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		log("setNewValue:name="+n+" value="+v);
		ph.setProperty(null, n, v, true);
//		if (ph.getUserProperty(n) == null) {
//        } else {
//        	log("Override ignored for " + n, Project.MSG_VERBOSE);
//        	ph.setInheritedProperty(n, v);
//        }
	}


	private enum Operations {
		//from 0 to index
		start {
			public void execute(StringTask v) {
				java.lang.String value = v.getValue();
				
				int ende = value.length();
				if (v.getIndex() !=null) {
					try {
						ende = Integer.parseInt(v.getIndex());
					} catch(Exception ex) {
						throw new BuildException("Index is not a Number!",ex);
					}
				} else {
					throw new BuildException("Index is not a Number!");
				}
				
				java.lang.String newValue = value.substring(0, ende);
				v.setNewValue(newValue);
			}
		},
		//from index to end
		end {
			public void execute(StringTask v) {
				java.lang.String value = v.getValue();
				
				int start = value.length();
				if (v.getIndex() !=null) {
					try {
						start = Integer.parseInt(v.getIndex());
					} catch(Exception ex) {
						throw new BuildException("Index is not a Number!",ex);
					}
				} else {
					throw new BuildException("Index is not a Number!");
				}
				
				java.lang.String newValue = value.substring(start);
				v.setNewValue(newValue);
			}
		},
		//
		substring {
			public void execute(StringTask v) {
				java.lang.String value = v.getValue();
				
				int start = value.length();
				if (v.getStart() !=null) {
					try {
						start = Integer.parseInt(v.getStart());
					} catch(Exception ex) {
						throw new BuildException("Startindex is not a Number!",ex);
					}
				} else {
					throw new BuildException("Startindex is not a Number!");
				}
				
				int ende = value.length();
				if (v.getEnd() !=null) {
					try {
						ende = Integer.parseInt(v.getEnd());
					} catch(Exception ex) {
						throw new BuildException("Endindex is not a Number!",ex);
					}
				} else {
					throw new BuildException("Endindex is not a Number!");
				}
				
				java.lang.String newValue = value.substring(start,ende);
				v.setNewValue(newValue);
			}
		};
		
		abstract public void execute(StringTask v);
	}
}
