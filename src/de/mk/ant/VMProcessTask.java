package de.mk.ant;

import java.net.URL;
import java.util.Arrays;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.vmware.vim25.GuestProcessInfo;
import com.vmware.vim25.GuestProgramSpec;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.GuestAuthManager;
import com.vmware.vim25.mo.GuestOperationsManager;
import com.vmware.vim25.mo.GuestProcessManager;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;


public class VMProcessTask extends Task {

	String host;
	String username;
	String password;
	String vmname;
	String operation;
	String workingDirectory;
	String executable;
	String arguments;
	String guestUser;
	String guestPassword;
	
	
	
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


			operation.execute(si, vm, this);
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

	public String getExecutable() {
		return executable;
	}

	public void setExecutable(String executable) {
		this.executable = executable;
	}

	public String getArguments() {
		return arguments;
	}

	public void setArguments(String arguments) {
		this.arguments = arguments;
	}

	public String getGuestUser() {
		return guestUser;
	}

	public void setGuestUser(String guestUser) {
		this.guestUser = guestUser;
	}

	public String getGuestPassword() {
		return guestPassword;
	}

	public void setGuestPassword(String guestPassword) {
		this.guestPassword = guestPassword;
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}


	private enum Operations {
		start { 
			public void execute(ServiceInstance si, VirtualMachine vm, VMProcessTask task)  throws Exception {
				System.out.println("Start Process in VM: "+ vm.getName()+ " "+task.getExecutable()+" "+task.getArguments());
				
				GuestOperationsManager gom = si.getGuestOperationsManager();
				GuestProcessManager gpm = gom.getProcessManager(vm);
				
				GuestAuthManager gam = gom.getAuthManager(vm);
				NamePasswordAuthentication npa = new NamePasswordAuthentication();
				npa.username = task.getGuestUser();
				npa.password = task.getGuestPassword();
				
				GuestProgramSpec spec = new GuestProgramSpec();
			    spec.programPath = task.getExecutable();
			    spec.arguments = task.getArguments();
			    spec.workingDirectory = task.getWorkingDirectory();
			    
			    long pid = gpm.startProgramInGuest(npa, spec);
			    System.out.println("pid: " + pid);
			}
		}, list { 
			public void execute(ServiceInstance si, VirtualMachine vm, VMProcessTask task)  throws Exception {
				System.out.println("List Processes in VM:"+vm.getName());

				GuestOperationsManager gom = si.getGuestOperationsManager();
				GuestProcessManager gpm = gom.getProcessManager(vm);
				
				GuestAuthManager gam = gom.getAuthManager(vm);
				NamePasswordAuthentication npa = new NamePasswordAuthentication();
				npa.username = task.getGuestUser();
				npa.password = task.getGuestPassword();
				
				GuestProcessInfo[] infos = gpm.listProcessesInGuest(npa, new long[0]);
				
				for(GuestProcessInfo i : infos) {
					System.out.println("Process: "+i.name+"("+i.owner+") "+i.pid+" - started: "+i.getStartTime()+"-"+i.getEndTime()+" CMD> "+i.getCmdLine());
				}
			}
		};
		
		abstract public void execute(ServiceInstance si, VirtualMachine vm, VMProcessTask task) throws Exception ;
	};
}
