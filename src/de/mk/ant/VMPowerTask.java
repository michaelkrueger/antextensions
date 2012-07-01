package de.mk.ant;

import java.net.URL;
import java.util.Arrays;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;


public class VMPowerTask extends Task {

	String url;
	String username;
	String password;
	String vmname;
	String operation;
	
	
	public void execute() throws BuildException {
		ServiceInstance si = null;
		try {
			si = new ServiceInstance(
					new URL(getUrl()), getUsername(), getPassword() , true);

			Folder rootFolder = si.getRootFolder();
			if ("list".equalsIgnoreCase(getOperation())) {
				listRootFolder(rootFolder);
			}
			VirtualMachine vm = (VirtualMachine) new InventoryNavigator(
					rootFolder).searchManagedEntity("VirtualMachine", vmname);

			if(vm==null) {
				System.out.println("No VM " + vmname + " found");
				si.getServerConnection().logout();
				throw new BuildException("No VM " + vmname + " found");
			}

			Operations operation = Operations.valueOf(getOperation());
			if (operation==null) throw new BuildException("Operation "+getOperation()+" unknown! Valid: "+Arrays.asList(Operations.values()));

			operation.execute(vm);
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

	private void listRootFolder(Folder rootFolder) throws Exception {
		ManagedEntity[] vms = new InventoryNavigator(
				rootFolder).searchManagedEntities("VirtualMachine");
		
		for(ManagedEntity e : vms) {
			VirtualMachine vm = (VirtualMachine)e;
			System.out.println("VM: Name "+vm.getName());
		}
	}
	
	private enum Operations {
		reboot { 
			public void execute(VirtualMachine vm)  throws Exception {
				System.out.println("Reboot VM: "+vm.getName());
				vm.rebootGuest();
			}
		},poweron{ 
			public void execute(VirtualMachine vm)  throws Exception {
				System.out.println("Power on VM: "+vm.getName());
				vm.powerOnVM_Task(null);
			}
		},poweroff{ 
			public void execute(VirtualMachine vm)  throws Exception {
				System.out.println("Power off VM: "+vm.getName());
				vm.powerOffVM_Task();
			}
		},reset{ 
			public void execute(VirtualMachine vm)  throws Exception {
				System.out.println("Reset VM: "+vm.getName());
				vm.resetVM_Task();
			}
		},standby{ 
			public void execute(VirtualMachine vm)  throws Exception {
				System.out.println("Standby VM: "+vm.getName());
				vm.standbyGuest();
			}
		},suspend{ 
			public void execute(VirtualMachine vm)  throws Exception {
				System.out.println("Suspend VM: "+vm.getName());
				vm.suspendVM_Task();
			}
		},shutdown{ 
			public void execute(VirtualMachine vm)  throws Exception {
				System.out.println("Shutdown VM: "+vm.getName());
				vm.shutdownGuest();
			}
		};
		
		abstract public void execute(VirtualMachine vm) throws Exception ;
	};
}
