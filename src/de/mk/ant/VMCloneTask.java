package de.mk.ant;

import java.net.URL;
import java.util.Arrays;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;



public class VMCloneTask extends Task {

	String host;
	String username;
	String password;
	String operation;
	String vmbasis;
	String vmclone;
	String datacenter;
	
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


			operation.execute(vm,dc, getVmclone());
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

	public String getVmclone() {
		return vmclone;
	}

	public void setVmclone(String vmclone) {
		this.vmclone = vmclone;
	}

	public String getDatacenter() {
		return datacenter;
	}

	public void setDatacenter(String datacenter) {
		this.datacenter = datacenter;
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
		clone { 
			public void execute(VirtualMachine vm, Datacenter dc, String cloneName)  throws Exception {
				System.out.println("Clone VM: "+vm.getName());
				Folder vmFolder = dc.getVmFolder();
				
				   VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
				   cloneSpec.setLocation(new VirtualMachineRelocateSpec());
				   cloneSpec.setPowerOn(false);
				   cloneSpec.setTemplate(false);

//				   cloneSpec.customization.identity.
				   
				   com.vmware.vim25.mo.Task task = vm.cloneVM_Task(vmFolder, cloneName, cloneSpec);
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

			}
		}, 
		list { 
				public void execute(VirtualMachine vm, Datacenter datacenter, String cloneName)  throws Exception {
					System.out.println("List the VMs");
				}
		};
		
		abstract public void execute(VirtualMachine vm, Datacenter datacenter, String cloneName) throws Exception ;
	};
}
