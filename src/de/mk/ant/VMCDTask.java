package de.mk.ant;

import java.net.URL;
import java.util.Arrays;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDeviceConnectInfo;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigOption;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineDatastoreInfo;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.EnvironmentBrowser;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;


public class VMCDTask extends Task {

	String host;
	String username;
	String password;
	String vmname;
	String operation;
	String datastore;
	String isofile;
	
	
	public void execute() throws BuildException {
		Operations operation = null;
		try {
			operation = Operations.valueOf(getOperation());
		} catch (IllegalArgumentException ex) {
			throw new BuildException("Operation "+getOperation()+" unknown! Valid: "+Arrays.asList(Operations.values()));			
		}
		
		ServiceInstance si = null;
		try {
			si = new ServiceInstance(
					new URL(getUrl()), getUsername(), getPassword() , true);

			Folder rootFolder = si.getRootFolder();

			VirtualMachine vm = (VirtualMachine) new InventoryNavigator(
					rootFolder).searchManagedEntity("VirtualMachine", vmname);

			if(vm==null) {
				System.out.println("No VM " + vmname + " found");
				si.getServerConnection().logout();
				throw new BuildException("No VM " + vmname + " found");
			}


			operation.execute(vm, getDatastore(), getIsoFile());
		} catch(Exception ex) {
			throw new BuildException(ex);
		} finally {
			if (si!=null) si.getServerConnection().logout();
		}
	    System.out.println("Operation "+getOperation()+" succeeded!");
	}



	public String getUrl() {
		return "https://"+host+"/sdk";
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getVmname() {
		return vmname;
	}

	public void setVmname(String vmname) {
		this.vmname = vmname;
	}
	
	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getDatastore() {
		return datastore;
	}

	public void setDatastore(String datastore) {
		this.datastore = datastore;
	}

	public String getIsoFile() {
		return isofile;
	}

	public void setIsoFile(String isoname) {
		this.isofile = isoname;
	}

	
	private enum Operations {
		mount { 
			public void execute(VirtualMachine vm, String dsName, String isoName)  throws Exception {
				System.out.println("Mount ISO VM:"+vm.getName()+" dsName:"+dsName+" IsoImage:"+isoName);
				VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
				VirtualDeviceConfigSpec cdSpec = createMountCdConfigSpec(vm, dsName, isoName);
				vmConfigSpec.setDeviceChange(new VirtualDeviceConfigSpec[]{cdSpec});
				com.vmware.vim25.mo.Task task = vm.reconfigVM_Task(vmConfigSpec);
				String r = task.waitForTask();
				System.out.println("Result: "+r);
			}
		}, unmount { 
			public void execute(VirtualMachine vm, String dsName, String isoName)  throws Exception {
				System.out.println("Unmount ISO VM:"+vm.getName()+" IsoImage:"+isoName);
				VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
				VirtualDeviceConfigSpec cdSpec = createUnmountCdConfigSpec( vm, isoName);
				vmConfigSpec.setDeviceChange(new VirtualDeviceConfigSpec[]{cdSpec});
				com.vmware.vim25.mo.Task task = vm.reconfigVM_Task(vmConfigSpec);
				String r = task.waitForTask();
				System.out.println("Result: "+r);
			}
		}, list { 
			public void execute(VirtualMachine vm, String dsName, String isoName)  throws Exception {
				System.out.println("Mount ISO VM:"+vm.getName()+" dsName:"+dsName+" IsoImage:"+isoName);
				//...
			}
		};
		
		abstract public void execute(VirtualMachine vm, String dsName, String isoName) throws Exception ;
	};
	
	  static VirtualDeviceConfigSpec createMountCdConfigSpec(VirtualMachine vm, String dsName, String isoName) throws Exception 
	  {
	    VirtualDeviceConfigSpec cdSpec = new VirtualDeviceConfigSpec();

	    cdSpec.setOperation(VirtualDeviceConfigSpecOperation.edit);   
	    
	    VirtualCdrom cdrom = (VirtualCdrom)findVirtualDevice(vm.getConfig(), "CD/DVD");
//	    VirtualCdrom cdrom =  new VirtualCdrom();
	    VirtualCdromIsoBackingInfo cdDeviceBacking = new  VirtualCdromIsoBackingInfo();
	    DatastoreSummary ds = findDatastoreSummary(vm, dsName);
	    System.out.println("Datastore: "+ds.getName()+" "+ds.getType()+" "+(ds.getCapacity()/1000000)+" MB");
	    cdDeviceBacking.setDatastore(ds.getDatastore());
	    cdDeviceBacking.setFileName("[" + dsName +"] "+ //vm.getName() + 
	        "/" + isoName);
	    cdrom.setBacking(cdDeviceBacking);                    

	    System.out.println("Connectable:"+cdrom.connectable+" "+cdrom.connectable.allowGuestControl+" "+cdrom.connectable.startConnected+" "+cdrom.connectable.connected);
	    //cdrom.connectable = new VirtualDeviceConnectInfo();
	    cdrom.connectable.allowGuestControl = false;
	    cdrom.connectable.startConnected = true;
	    cdrom.connectable.connected = true;
	    
	    cdSpec.setDevice(cdrom);

	    return cdSpec;          
	  }
	  
	  static VirtualDeviceConfigSpec createUnmountCdConfigSpec(VirtualMachine vm, String cdName) throws Exception 
	  {
	    VirtualDeviceConfigSpec cdSpec = new VirtualDeviceConfigSpec();
	    cdSpec.setOperation(VirtualDeviceConfigSpecOperation.edit);
	    VirtualCdrom cdrom = (VirtualCdrom)findVirtualDevice(vm.getConfig(), "CD/DVD");
	    if(cdrom != null) 
	    {
	    	System.out.println("Connectable:"+cdrom.connectable+" "+cdrom.connectable.allowGuestControl+" "+cdrom.connectable.startConnected+" "+cdrom.connectable.connected);
		    //cdrom.connectable = new VirtualDeviceConnectInfo();
		    cdrom.connectable.allowGuestControl = false;
		    cdrom.connectable.startConnected = false;
		    cdrom.connectable.connected = false;
		    
	    	cdSpec.setDevice(cdrom);
	    	return cdSpec;
	    }
	    else 
	    {
	      throw new BuildException("No device available " + cdName);
	    }
	  }

	  private static VirtualDevice findVirtualDevice(
	      VirtualMachineConfigInfo vmConfig, String name)
	  {
	    VirtualDevice [] devices = vmConfig.getHardware().getDevice();
	    for(int i=0;i<devices.length;i++)
	    {
	    	System.out.println("Device-Names: "+devices[i].getDeviceInfo().getLabel());
	      if(devices[i].getDeviceInfo().getLabel().startsWith(name))
	      {                             
	        return devices[i];
	      }
	    }
	    return null;
	  }

	  static DatastoreSummary findDatastoreSummary(VirtualMachine vm, String dsName) throws Exception 
	  {
	    DatastoreSummary dsSum = null;
	    VirtualMachineRuntimeInfo vmRuntimeInfo = vm.getRuntime();
	    EnvironmentBrowser envBrowser = vm.getEnvironmentBrowser(); 
	    ManagedObjectReference hmor = vmRuntimeInfo.getHost();

	    if(hmor == null)
	    {
	      System.out.println("No Datastore found");
	      throw new BuildException("No Datastore found " + dsName);
	    }
	    
	    ConfigTarget configTarget = envBrowser.queryConfigTarget(new HostSystem(vm.getServerConnection(), hmor));
	    VirtualMachineDatastoreInfo[] dis = configTarget.getDatastore();
	    for (int i=0; dis!=null && i<dis.length; i++) 
	    {
	      dsSum = dis[i].getDatastore();
	      if (dsSum.isAccessible() && dsName.equals(dsSum.getName())) 
	      {
	        break;
	      }
	    }
	    return dsSum;
	  }

	  static VirtualDevice getIDEController(VirtualMachine vm) 
	    throws Exception 
	  {
	    VirtualDevice ideController = null;
	    VirtualDevice [] defaultDevices = getDefaultDevices(vm);
	    for (int i = 0; i < defaultDevices.length; i++) 
	    {
	      if (defaultDevices[i] instanceof VirtualIDEController) 
	      {
	        ideController = defaultDevices[i];             
	        break;
	      }
	    }
	    if (ideController==null) throw new BuildException("No IDE Controller found!");
	    return ideController;
	  }

	  static VirtualDevice[] getDefaultDevices(VirtualMachine vm) 
	  throws Exception 
	  {
	    VirtualMachineRuntimeInfo vmRuntimeInfo = vm.getRuntime();
	    EnvironmentBrowser envBrowser = vm.getEnvironmentBrowser(); 
	    ManagedObjectReference hmor = vmRuntimeInfo.getHost();
	    VirtualMachineConfigOption cfgOpt = envBrowser.queryConfigOption(null, new HostSystem(vm.getServerConnection(), hmor));
	    VirtualDevice[] defaultDevs = null;
	    if (cfgOpt != null) 
	    {
	      defaultDevs = cfgOpt.getDefaultDevice();
	      if (defaultDevs == null) 
	      {
	        throw new Exception("No Datastore found in ComputeResource");
	      }
	    }
	    else
	    {
	      throw new Exception("No VirtualHardwareInfo found in ComputeResource");
	    }
	    return defaultDevs;
	  }
}
