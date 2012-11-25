package de.mk.ant;

import java.net.URL;
import java.util.Arrays;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NetworkSummary;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineNetworkInfo;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VirtualPCNet32;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.EnvironmentBrowser;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;



public class VMChangeTask extends Task {

	String host;
	String username;
	String password;
	String operation;
	String datacenter;
	String vmbasis;
	Long   memory;
	Integer cpus;
	Integer cores;
	String diskName;
	Long  diskSizeMB;
	String nicName;
	String networkName;
	
	
	
	
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

			System.out.println("ServiceInstance found");
			
			Folder rootFolder = si.getRootFolder();
			if ("list".equalsIgnoreCase(getOperation())) {
				listRootFolder(rootFolder);
				return;
			}
			VirtualMachine vm = (VirtualMachine) new InventoryNavigator(
					rootFolder).searchManagedEntity("VirtualMachine", vmbasis );

			Datacenter dc = (Datacenter) si.getSearchIndex().findByInventoryPath(getDatacenter());
			
			if(vm==null || dc==null) {
				System.out.println("No VM " + vmbasis + " found or datacenter "+getDatacenter()+" found!");
				si.getServerConnection().logout();
				throw new BuildException("No VM " + vmbasis + " found");
			}


			VirtualMachineConfigSpec changeSpec = operation.execute(vm,dc,this);
			
			com.vmware.vim25.mo.Task task = vm.reconfigVM_Task(changeSpec);
			System.out.println("Launching the VM clone task. It might take a while. Please wait for the result ...");

			String status = 	task.waitForMe();
			if(status==com.vmware.vim25.mo.Task.SUCCESS)
			{
				System.out.println("Virtual Machine got cloned successfully.");
			}
			else
			{
				System.out.println("Failure -: Virtual Machine cannot be cloned");
			}
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

	public String getVmbasis() {
		return vmbasis;
	}

	public void setVmbasis(String vmname) {
		this.vmbasis = vmname;
	}
	
	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getDatacenter() {
		return datacenter;
	}

	public void setDatacenter(String datacenter) {
		this.datacenter = datacenter;
	}

	public Long getMemory() {
		return memory;
	}

	public void setMemory(Long memory) {
		this.memory = memory;
	}

	public Integer getCpus() {
		return cpus;
	}

	public void setCpus(Integer cpus) {
		this.cpus = cpus;
	}

	public Integer getCores() {
		return cores;
	}

	public void setCores(Integer cores) {
		this.cores = cores;
	}

	public String getDiskName() {
		return diskName;
	}

	public void setDiskName(String diskName) {
		this.diskName = diskName;
	}

	public Long getDiskSizeMB() {
		return diskSizeMB;
	}

	public void setDiskSizeMB(Long diskSizeMB) {
		this.diskSizeMB = diskSizeMB;
	}

	public String getNicName() {
		return nicName;
	}

	public void setNicName(String nicName) {
		this.nicName = nicName;
	}

	public String getNetworkName() {
		return networkName;
	}

	public void setNetworkName(String networkName) {
		this.networkName = networkName;
	}



	private void listRootFolder(Folder rootFolder) throws Exception {
		ManagedEntity[] vms = new InventoryNavigator(
				rootFolder).searchManagedEntities("VirtualMachine");
		
		for(ManagedEntity e : vms) {
			VirtualMachine vm = (VirtualMachine)e;
			System.out.println("VM: Name "+vm.getName());
		}
	}
	
	private enum Operations {
		cpu { 
			public VirtualMachineConfigSpec execute(VirtualMachine vm, Datacenter dc, VMChangeTask changeTask)  throws Exception {
				System.out.println("CPU: "+vm.getName()+" ");
				
				VirtualMachineConfigSpec changeSpec = new VirtualMachineConfigSpec();
				
				changeSpec.setNumCPUs(changeTask.getCpus());
				changeSpec.setNumCoresPerSocket(changeTask.getCores());
				
				
				return changeSpec;
				
			}
		}, 
		memory { 
			public VirtualMachineConfigSpec execute(VirtualMachine vm, Datacenter dc, VMChangeTask changeTask)  throws Exception {
				System.out.println("Memory: "+vm.getName()+ " change to :"+changeTask.getMemory()+" MB");
				
				VirtualMachineConfigSpec changeSpec = new VirtualMachineConfigSpec();
				
				changeSpec.setMemoryMB(changeTask.getMemory());
				
				return changeSpec;
				
			}
		}, 
		adddisk { 
			public VirtualMachineConfigSpec execute(VirtualMachine vm, Datacenter dc, VMChangeTask changeTask)  throws Exception {
				System.out.println("Add Disk: "+vm.getName());
				VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
		         VirtualDeviceConfigSpec vdiskSpec = changeTask.getDiskDeviceConfigSpec(vm, "Add");
		         if(vdiskSpec != null) 
		         {
		            VirtualDeviceConfigSpec [] vdiskSpecArray = {vdiskSpec};         
		            vmConfigSpec.setDeviceChange(vdiskSpecArray);
		         }
		          
		         return vmConfigSpec;
			}
		}, 
		removedisk { 
			public VirtualMachineConfigSpec execute(VirtualMachine vm, Datacenter dc, VMChangeTask changeTask)  throws Exception {
				System.out.println("Remove Disk: "+vm.getName());
				VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
		         VirtualDeviceConfigSpec vdiskSpec = changeTask.getDiskDeviceConfigSpec(vm, "Remove");
		         if(vdiskSpec != null) 
		         {
		            VirtualDeviceConfigSpec [] vdiskSpecArray = {vdiskSpec};         
		            vmConfigSpec.setDeviceChange(vdiskSpecArray);
		         }
		          
		         return vmConfigSpec;
			}
		}, 	
		addnic { 
			public VirtualMachineConfigSpec execute(VirtualMachine vm, Datacenter dc, VMChangeTask changeTask)  throws Exception {
				System.out.println("Add NIC: "+vm.getName());
				VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
				VirtualDeviceConfigSpec nicSpek = changeTask.getNICDeviceConfigSpec(vm, "Add");
				
				if(nicSpek != null) 
		         {
		            VirtualDeviceConfigSpec [] vdiskSpecArray = {nicSpek};         
		            vmConfigSpec.setDeviceChange(vdiskSpecArray);
		         }
				
				return vmConfigSpec;
				
			}
		},
		removenic { 
			public VirtualMachineConfigSpec execute(VirtualMachine vm, Datacenter dc, VMChangeTask changeTask)  throws Exception {
				System.out.println("Add NIC: "+vm.getName());
				VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
				VirtualDeviceConfigSpec nicSpek = changeTask.getNICDeviceConfigSpec(vm, "Remove");
				
				if(nicSpek != null) 
		         {
		            VirtualDeviceConfigSpec [] vdiskSpecArray = {nicSpek};         
		            vmConfigSpec.setDeviceChange(vdiskSpecArray);
		         }
				
				return vmConfigSpec;
				
			}
		},		
		list { 
				public VirtualMachineConfigSpec execute(VirtualMachine vm, Datacenter datacenter, VMChangeTask task)  throws Exception {
					System.out.println("List the VMs");
					
					return null;
				}
		};
		
		abstract public VirtualMachineConfigSpec execute(VirtualMachine vm, Datacenter datacenter, VMChangeTask task) throws Exception ;
	};
	
	private VirtualDeviceConfigSpec getDiskDeviceConfigSpec(VirtualMachine vm, String ops) throws Exception
	   {
	      VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();      
	      VirtualMachineConfigInfo vmConfigInfo = (VirtualMachineConfigInfo)vm.getConfig();
	      
	      if(ops.equalsIgnoreCase("Add")) 
	      { 
	         VirtualDisk disk =  new VirtualDisk();
	         VirtualDiskFlatVer2BackingInfo diskfileBacking = new VirtualDiskFlatVer2BackingInfo();    
	         String dsName  = getDataStoreName(vm, getDiskSizeMB());         
	         
	         int ckey = 0;
	         int unitNumber = 0;
	     
	         VirtualDevice [] test = vmConfigInfo.getHardware().getDevice();
	         for(int k=0;k<test.length;k++)
	         {
	            if(test[k].getDeviceInfo().getLabel().equalsIgnoreCase("SCSI Controller 0"))
	            {
	               ckey = test[k].getKey();                                
	            }
	         }
	        
	         unitNumber = test.length + 1;                
	         String fileName = "["+dsName+"] "+ getVmbasis() + "/" + getDiskName() + ".vmdk";
	         
	         diskfileBacking.setFileName(fileName);
	         diskfileBacking.setDiskMode("persistent");
	      
	         disk.setControllerKey(ckey);
	         disk.setUnitNumber(unitNumber);
	         disk.setBacking(diskfileBacking);
	         long size = 1024 * getDiskSizeMB();
	         disk.setCapacityInKB(size);
	         disk.setKey(-1);
	         
	         diskSpec.setOperation(VirtualDeviceConfigSpecOperation.add);           
	         diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.create);           
	         diskSpec.setDevice(disk);                 
	      }
	      else if(ops.equalsIgnoreCase("Remove")) 
	      {                             
	         VirtualDisk disk =  null;
	         VirtualDevice [] test = vmConfigInfo.getHardware().getDevice();
	         for(int k=0;k<test.length;k++)
	         {
	            if(test[k].getDeviceInfo().getLabel().equalsIgnoreCase(getDiskName()))
	            {                             
	               disk = (VirtualDisk)test[k];
	            }
	         }             
	         if(disk != null) 
	         {
	            diskSpec.setOperation(VirtualDeviceConfigSpecOperation.remove);           
	            diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.destroy);           
	            diskSpec.setDevice(disk);                 
	         }
	         else 
	         {
	            System.out.println("No device found " + getDiskName());
	            return null;
	         }
	      }
	      return diskSpec;
	   }
	   
	   private String getDataStoreName(VirtualMachine vm, long size) throws Exception
	   {
	      String dsName = null;
	      Datastore[] datastores = vm.getDatastores();
	      for(int i=0; i<datastores.length; i++) 
	      {
	         DatastoreSummary ds = datastores[i].getSummary(); 
	         if(ds.getFreeSpace() > size) 
	         {
	        	dsName = ds.getName();
	            break;           
	         }
	      }
	      return dsName;
	   }	
	   private  VirtualDeviceConfigSpec getNICDeviceConfigSpec(VirtualMachine vm, String ops) throws Exception 
	   {
	      VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();      
	      VirtualMachineConfigInfo vmConfigInfo  = vm.getConfig();
	      
	      if(ops.equalsIgnoreCase("Add")) 
	      {
	         String networkName = getNetworkName(vm, getNetworkName()); 
	         if(networkName != null) 
	         {
	            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
	            VirtualEthernetCard nic =  new VirtualPCNet32();
	            VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
	            nicBacking.setDeviceName(networkName);
	            nic.setAddressType("generated");
	            nic.setBacking(nicBacking);
	            nic.setKey(4);
	            nicSpec.setDevice(nic);
	         }
	         else 
	         {
	            return null;
	         }
	      }
	      else if(ops.equalsIgnoreCase("Remove")) 
	      {
	         VirtualEthernetCard nic = null;
	         VirtualDevice [] test = vmConfigInfo.getHardware().getDevice();
	         nicSpec.setOperation(VirtualDeviceConfigSpecOperation.remove);
	         for(int k=0;k<test.length;k++)
	         {
	        	 if(test[k].getDeviceInfo().getLabel().equalsIgnoreCase(  getNetworkName()))
	        	 {                             
	        		 nic = (VirtualEthernetCard)test[k];
	        	 }
	         }
	         if(nic != null) 
	         {
	            nicSpec.setDevice(nic);
	         }
	         else 
	         {
	            System.out.println("No device available " + getNetworkName());
	            return null;
	         }
	      }
	      return nicSpec;
	   }
	   
	   private static String getNetworkName(VirtualMachine vm, String searchNetworkName) throws Exception 
	   {
	      String networkName = null;
	      VirtualMachineRuntimeInfo vmRuntimeInfo = vm.getRuntime();
	      
	      EnvironmentBrowser envBrowser = vm.getEnvironmentBrowser();
	      ManagedObjectReference hmor = vmRuntimeInfo.getHost();
	      
	      if(hmor != null) 
	      {       
	         ConfigTarget configTarget = envBrowser.queryConfigTarget(new HostSystem(vm.getServerConnection(), hmor));       
	         if(configTarget.getNetwork() != null) 
	         {
	            for (int i = 0; i < configTarget.getNetwork().length; i++) 
	            {
	               VirtualMachineNetworkInfo netInfo = configTarget.getNetwork()[i];
	               NetworkSummary netSummary = netInfo.getNetwork();
	               if (netSummary.isAccessible()) 
	               {
	                  if(netSummary.getName().equalsIgnoreCase(searchNetworkName)) 
	                  {
	                     networkName = netSummary.getName();
	                     break;
	                  }
	               }
	            }
	            if(networkName == null) 
	            {
	               System.out.println("Specify the Correct Network Name");
	               return null;
	            }
	         }
	         System.out.println("network Name " + networkName);
	         return networkName;
	      }
	      else 
	      {
	         System.out.println("No Host is responsible to run this VM");
	         return null;
	      }
	   }
}
