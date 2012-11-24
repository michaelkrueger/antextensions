package de.mk.ant;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.vmware.vim25.Description;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualPCNet32;
import com.vmware.vim25.VirtualSCSISharing;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;

public class VMCreateTask extends Task {

	String host;
	String username;
	String password;
	String datacenter;
	String datastore;
	String vmName;
	long   memorySizeMB = 500;
	int    cpuCount=1;
	String guestOsId = "sles10Guest";
	String annotation ="Created by AntExtention";
	
	String cdIsoDatastore=null;
	String cdIsoFile=null;
	
	List<DiskSpec> disks = new ArrayList<DiskSpec>();
	List<NicSpec>  nics  = new ArrayList<NicSpec>();
	
	@Override
	public void execute() throws BuildException {
		
		ServiceInstance si = null;
		
		try {
		    String dcName = getDatacenter();
		    String vmName = getVmName();
		    long memorySizeMB = getMemorySizeMB();
		    int cpuCount = getCpuCount();
		    String guestOsId = "sles10Guest";
		    String datastoreName = getDatastore();
		    
//		    long diskSizeKB = 1000000;
//		    // mode: persistent|independent_persistent,
//		    // independent_nonpersistent
//		    String diskMode = "persistent";
//		    String netName = "VM Network";
//		    String nicName = "Network Adapter 1";
		    
		    si = new ServiceInstance(
					new URL(getUrl()), getUsername(), getPassword() , true);

			System.out.println("ServiceInstance found");

		    Folder rootFolder = si.getRootFolder();
		    
		    Datacenter dc = (Datacenter) new InventoryNavigator(
		        rootFolder).searchManagedEntity("Datacenter", dcName);
		    ResourcePool rp = (ResourcePool) new InventoryNavigator(
		        dc).searchManagedEntities("ResourcePool")[0];
		    
		    Folder vmFolder = dc.getVmFolder();

		    // create vm config spec
		    VirtualMachineConfigSpec vmSpec = 
		      new VirtualMachineConfigSpec();
		    vmSpec.setName(vmName);
		    vmSpec.setAnnotation(getAnnotation());
		    vmSpec.setMemoryMB(memorySizeMB);
		    vmSpec.setNumCPUs(cpuCount);
		    vmSpec.setGuestId(guestOsId);
	    
		    // create virtual devices
		    
		    List<VirtualDeviceConfigSpec> specs = new ArrayList<VirtualDeviceConfigSpec>();
		    
		    int cKey = 1000;
		    specs.add(createScsiSpec(cKey));
		    
		    for(DiskSpec disk : disks) {
		    	specs.add(createDiskSpec(datastoreName, cKey, disk.diskSizeKB, disk.diskMode));
		    }
		    for (NicSpec nic : nics) {
		    	specs.add(createNicSpec(nic.netName, nic.nicName));
		    }
		    
		    if (getCdIsoFile()!=null && getCdIsoDatastore()!=null) {
		    	specs.add(createAddCdConfigSpec(null, null, getCdIsoDatastore(), getCdIsoFile()));
		    }
		    
		    vmSpec.setDeviceChange(specs.toArray(new VirtualDeviceConfigSpec[specs.size()]));
		    
		    // create vm file info for the vmx file
		    VirtualMachineFileInfo vmfi = new VirtualMachineFileInfo();
		    vmfi.setVmPathName("["+ datastoreName +"]");
		    vmSpec.setFiles(vmfi);

		    // call the createVM_Task method on the vm folder
		    com.vmware.vim25.mo.Task task = vmFolder.createVM_Task(vmSpec, rp, null);
		    System.out.println("Launching the VM create task. It might take a while. Please wait for the result ...");
			   
		    String status = 	task.waitForMe();
		    if(status==com.vmware.vim25.mo.Task.SUCCESS) 
		    {
		      System.out.println("VM Created Sucessfully");
		    }
		    else 
		    {
		      System.out.println("VM could not be created. ");
		    }
			
			
			
		} catch (Exception ex) {
			throw new BuildException(ex);
		} finally {
			if (si!=null) si.getServerConnection().logout();
		}
	    System.out.println("CreateVM Operation succeeded!");
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
	
	public String getDatacenter() {
		return datacenter;
	}

	public void setDatacenter(String datacenter) {
		this.datacenter = datacenter;
	}

	public String getVmName() {
		return vmName;
	}

	public void setVmName(String vmName) {
		this.vmName = vmName;
	}

	public long getMemorySizeMB() {
		return memorySizeMB;
	}

	public void setMemorySizeMB(long memorySizeMB) {
		this.memorySizeMB = memorySizeMB;
	}

	public int getCpuCount() {
		return cpuCount;
	}

	public void setCpuCount(int cpuCount) {
		this.cpuCount = cpuCount;
	}

	public String getGuestOsId() {
		return guestOsId;
	}

	public void setGuestOsId(String guestOsId) {
		this.guestOsId = guestOsId;
	}

	public String getDatastore() {
		return datastore;
	}

	public void setDatastore(String datastore) {
		this.datastore = datastore;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public String getCdIsoDatastore() {
		return cdIsoDatastore;
	}

	public void setCdIsoDatastore(String cdIsoDatastore) {
		this.cdIsoDatastore = cdIsoDatastore;
	}

	public String getCdIsoFile() {
		return cdIsoFile;
	}

	public void setCdIsoFile(String cdIsoFile) {
		this.cdIsoFile = cdIsoFile;
	}

	public DiskSpec createDiskSpec() {
		return new DiskSpec();
	}

	public void addDiskSpec(DiskSpec a) {
		disks.add(a);
	}

	public NicSpec createNicSpec() {
		return new NicSpec();
	}

	public void addNicSpec(NicSpec a) {
		nics.add(a);
	}

	
	public class DiskSpec {
		long diskSizeKB = 1000000;
	    // mode: persistent|independent_persistent,
	    // independent_nonpersistent
	    String diskMode = "persistent";
		public long getDiskSizeKB() {
			return diskSizeKB;
		}
		public void setDiskSizeKB(long diskSizeKB) {
			this.diskSizeKB = diskSizeKB;
		}
		public void setDiskSizeGB(long diskSizeGB) {
			this.diskSizeKB = diskSizeGB*1024*1024;
		}
		public String getDiskMode() {
			return diskMode;
		}
		public void setDiskMode(String diskMode) {
			this.diskMode = diskMode;
		}
	}
	
	public class NicSpec {
	    String netName = "VM Network";
	    String nicName = "Network Adapter 1";
		public String getNetName() {
			return netName;
		}
		public void setNetName(String netName) {
			this.netName = netName;
		}
		public String getNicName() {
			return nicName;
		}
		public void setNicName(String nicName) {
			this.nicName = nicName;
		}
	}

	static VirtualDeviceConfigSpec createScsiSpec(int cKey)
	  {
	    VirtualDeviceConfigSpec scsiSpec = 
	      new VirtualDeviceConfigSpec();
	    scsiSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
	    VirtualLsiLogicController scsiCtrl = 
	        new VirtualLsiLogicController();
	    scsiCtrl.setKey(cKey);
	    scsiCtrl.setBusNumber(0);
	    scsiCtrl.setSharedBus(VirtualSCSISharing.noSharing);
	    scsiSpec.setDevice(scsiCtrl);
	    return scsiSpec;
	  }
	  
	  static VirtualDeviceConfigSpec createDiskSpec(String dsName, 
	      int cKey, long diskSizeKB, String diskMode)
	  {
	    VirtualDeviceConfigSpec diskSpec = 
	        new VirtualDeviceConfigSpec();
	    diskSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
	    diskSpec.setFileOperation(
	        VirtualDeviceConfigSpecFileOperation.create);
	    
	    VirtualDisk vd = new VirtualDisk();
	    vd.setCapacityInKB(diskSizeKB);
	    diskSpec.setDevice(vd);
	    vd.setKey(0);
	    vd.setUnitNumber(0);
	    vd.setControllerKey(cKey);

	    VirtualDiskFlatVer2BackingInfo diskfileBacking = 
	        new VirtualDiskFlatVer2BackingInfo();
	    String fileName = "["+ dsName +"]";
	    diskfileBacking.setFileName(fileName);
	    diskfileBacking.setDiskMode(diskMode);
	    diskfileBacking.setThinProvisioned(true);
	    vd.setBacking(diskfileBacking);
	    return diskSpec;
	  }
	  
	  static VirtualDeviceConfigSpec createNicSpec(String netName, 
	      String nicName) throws Exception
	  {
	    VirtualDeviceConfigSpec nicSpec = 
	        new VirtualDeviceConfigSpec();
	    nicSpec.setOperation(VirtualDeviceConfigSpecOperation.add);

	    VirtualEthernetCard nic =  new VirtualPCNet32();
	    VirtualEthernetCardNetworkBackingInfo nicBacking = 
	        new VirtualEthernetCardNetworkBackingInfo();
	    nicBacking.setDeviceName(netName);

	    Description info = new Description();
	    info.setLabel(nicName);
	    info.setSummary(netName);
	    nic.setDeviceInfo(info);
	    
	    // type: "generated", "manual", "assigned" by VC
	    nic.setAddressType("generated");
	    nic.setBacking(nicBacking);
	    nic.setKey(0);
	   
	    nicSpec.setDevice(nic);
	    return nicSpec;
	  }
	  VirtualDeviceConfigSpec createAddCdConfigSpec(Integer cdControllerKex, Integer cdUnitNumber,String dsName, String isoName) throws Exception 
	  {
	    VirtualDeviceConfigSpec cdSpec = new VirtualDeviceConfigSpec();

	    cdSpec.setOperation(VirtualDeviceConfigSpecOperation.add);         

	    VirtualCdrom cdrom =  new VirtualCdrom();
	    VirtualCdromIsoBackingInfo cdDeviceBacking = new  VirtualCdromIsoBackingInfo();
//	    DatastoreSummary ds = findDatastoreSummary(vm, dsName);
//	    cdDeviceBacking.setDatastore(ds.getDatastore());
	    cdDeviceBacking.setFileName("[" + dsName +"] "+ isoName );
//	    VirtualDevice vd = getIDEController(vm);          
	    cdrom.setBacking(cdDeviceBacking);                    
	    cdrom.setControllerKey(cdControllerKex);
	    cdrom.setUnitNumber(cdUnitNumber);
	    cdrom.setKey(-1);          

	    cdSpec.setDevice(cdrom);

	    return cdSpec;          
	  }
}
