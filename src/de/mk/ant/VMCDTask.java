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

	String url;
	String username;
	String password;
	String vmname;
	String operation;
	String datastoreName;
	String isoname;
	
	
	public void execute() throws BuildException {
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

			Operations operation = Operations.valueOf(getOperation());
			if (operation==null) throw new BuildException("Operation "+getOperation()+" unknown! Valid: "+Arrays.asList(Operations.values()));

			operation.execute(vm, getDatastoreName(), getIsoname());
		} catch(Exception ex) {
			throw new BuildException(ex);
		} finally {
			if (si!=null) si.getServerConnection().logout();
		}
	    System.out.println("Operation "+getOperation()+" succeeded!");
	}



	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
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

	public String getDatastoreName() {
		return datastoreName;
	}

	public void setDatastoreName(String datastoreName) {
		this.datastoreName = datastoreName;
	}

	public String getIsoname() {
		return isoname;
	}

	public void setIsoname(String isoname) {
		this.isoname = isoname;
	}

	
	private enum Operations {
		mount { 
			public void execute(VirtualMachine vm, String dsName, String isoName)  throws Exception {
				System.out.println("Mount ISO VM:"+vm.getName()+" dsName:"+dsName+" IsoImage:"+isoName);
				VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
				VirtualDeviceConfigSpec cdSpec = createMountCdConfigSpec(vm, dsName, isoName);
				vmConfigSpec.setDeviceChange(new VirtualDeviceConfigSpec[]{cdSpec});
				com.vmware.vim25.mo.Task task = vm.reconfigVM_Task(vmConfigSpec);
				task.waitForMe();
			}
		}, unmount { 
			public void execute(VirtualMachine vm, String dsName, String isoName)  throws Exception {
				System.out.println("Unmount ISO VM:"+vm.getName()+" IsoImage:"+isoName);
				VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
				VirtualDeviceConfigSpec cdSpec = createUnmountCdConfigSpec( vm, isoName);
				vmConfigSpec.setDeviceChange(new VirtualDeviceConfigSpec[]{cdSpec});
				com.vmware.vim25.mo.Task task = vm.reconfigVM_Task(vmConfigSpec);
				task.waitForMe();
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

	    cdSpec.setOperation(VirtualDeviceConfigSpecOperation.add);         

	    VirtualCdrom cdrom =  new VirtualCdrom();
	    VirtualCdromIsoBackingInfo cdDeviceBacking = new  VirtualCdromIsoBackingInfo();
	    DatastoreSummary ds = findDatastoreSummary(vm, dsName);
	    cdDeviceBacking.setDatastore(ds.getDatastore());
	    cdDeviceBacking.setFileName("[" + dsName +"] "+ vm.getName() 
	        + "/" + isoName + ".iso");
	    VirtualDevice vd = getIDEController(vm);          
	    cdrom.setBacking(cdDeviceBacking);                    
	    cdrom.setControllerKey(vd.getKey());
	    cdrom.setUnitNumber(vd.getUnitNumber());
	    cdrom.setKey(-1);          

	    cdSpec.setDevice(cdrom);

	    return cdSpec;          
	  }
	  
	  static VirtualDeviceConfigSpec createUnmountCdConfigSpec(VirtualMachine vm, String cdName) throws Exception 
	  {
	    VirtualDeviceConfigSpec cdSpec = new VirtualDeviceConfigSpec();
	    cdSpec.setOperation(VirtualDeviceConfigSpecOperation.remove);
	    VirtualCdrom cdRemove = (VirtualCdrom)findVirtualDevice(vm.getConfig(), cdName);
	    if(cdRemove != null) 
	    {
	      cdSpec.setDevice(cdRemove);
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
	      if(devices[i].getDeviceInfo().getLabel().equals(name))
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
