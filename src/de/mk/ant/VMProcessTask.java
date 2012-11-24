package de.mk.ant;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	boolean wait=true;
	
	List<VmExec> executes = new ArrayList<VmExec>();
	
	
	public void execute() throws BuildException {
		Operations operation = null;
		try {
			operation = Operations.valueOf(getOperation());
		} catch (IllegalArgumentException ex) {
			throw new BuildException("Operation "+getOperation()+" unknown! Valid: "+Arrays.asList(Operations.values()));			
		}
		
		//Create vmexecute from attribute:
		if (getExecutable()!=null) {
			VmExec v = new VmExec();
			v.setExecutable(getExecutable());
			v.setArguments(getArguments());
			v.setWorkingDirectory(getWorkingDirectory());
			v.setWait(isWait());
			executes.add(v);
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
		if (arguments==null) return "";
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
		if (workingDirectory==null) return ".";
		return workingDirectory;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public boolean isWait() {
		return wait;
	}

	public void setWait(boolean wait) {
		this.wait = wait;
	}

	public VmExec createVmExec() {
		return new VmExec();
	}

	public void addVmExec(VmExec a) {
		executes.add(a);
	}
		
	private void execute(GuestProcessManager gpm, NamePasswordAuthentication npa, VmExec exec) throws Exception {
		GuestProgramSpec spec = exec.getVmWareSpec();
	    long pid = gpm.startProgramInGuest(npa, spec);
	    System.out.println("pid: " + pid);
	    
	    if (exec.isWait()) {
	    	boolean finished = false;
	    	long[] pids = {pid};
	    	
	    	while (!finished) {
	    		Thread.sleep(1000L);
	    		
	    		GuestProcessInfo[] infos = gpm.listProcessesInGuest(npa, pids);
	    		if (infos[0].getEndTime()!=null || infos[0].getExitCode()!=null) {
	    			System.out.println("Process "+pid+" exited: Errorcode:"+infos[0].getExitCode());
	    			finished=true;
	    		}
	    	}
	    }
	}
	
	private enum Operations {
		start { 
			public void execute(ServiceInstance si, VirtualMachine vm, VMProcessTask task)  throws Exception {
				System.out.println("Start Process in VM: "+ vm.getName()+ " "+task.getExecutable()+" "+task.getArguments());
				
			    if(!"guestToolsRunning".equals(vm.getGuest().toolsRunningStatus))
			    {
			      System.out.println("The VMware Tools is not running in the Guest OS on VM: " + vm.getName());
			      System.out.println("Exiting...");
			      return;
			    }
				
				GuestOperationsManager gom = si.getGuestOperationsManager();
				GuestProcessManager gpm = gom.getProcessManager(vm);
				
				GuestAuthManager gam = gom.getAuthManager(vm);
				NamePasswordAuthentication npa = new NamePasswordAuthentication();
				npa.username = task.getGuestUser();
				npa.password = task.getGuestPassword();
				
				//Create VMEXEC from attributes
				for(VmExec exec : task.executes) {					
					task.execute(gpm, npa, exec);
				}
			}
		}, list { 
			public void execute(ServiceInstance si, VirtualMachine vm, VMProcessTask task)  throws Exception {
				System.out.println("List Processes in VM:"+vm.getName());

				if(!"guestToolsRunning".equals(vm.getGuest().toolsRunningStatus))
			    {
			      System.out.println("The VMware Tools is not running in the Guest OS on VM: " + vm.getName());
			      System.out.println("Exiting...");
			      return;
			    }
				
				GuestOperationsManager gom = si.getGuestOperationsManager();
				GuestProcessManager gpm = gom.getProcessManager(vm);
				
				GuestAuthManager gam = gom.getAuthManager(vm);
				NamePasswordAuthentication npa = new NamePasswordAuthentication();
				npa.username = task.getGuestUser();
				npa.password = task.getGuestPassword();
				
				GuestProcessInfo[] infos = gpm.listProcessesInGuest(npa, new long[0]);
				
				for(GuestProcessInfo i : infos) {
					System.out.println("Process: "+i.name+"("+i.owner+") "+i.pid+" CMD> "+i.getCmdLine());
				}
			}
			
// See: http:// communities.vmware.com/servlet/JiveServlet/previewBody/12413-102-4-13370/VMware%20Tools%20-%20Unattended_Install.pdf
// Possible Arguments: Windows: 
//		/S /v"/qn REBOOT=R"
			
		}, installVmWareUtils { 
			
			public void execute(ServiceInstance si, VirtualMachine vm, VMProcessTask task)  throws Exception {
				System.out.println("Install VmWareUtils in VM:"+vm.getName());
				
				vm.mountToolsInstaller();
				System.out.println("Tools mounted ");
				
				vm.upgradeTools_Task(task.getArguments());
				System.out.println("Tools upgrade ");
				
				vm.unmountToolsInstaller();
				System.out.println("Tools unmounted ");
			}
		};
		
		abstract public void execute(ServiceInstance si, VirtualMachine vm, VMProcessTask task) throws Exception ;
	};
	
	public class VmExec {
		String workingDirectory;
		String executable;
		String arguments;
		boolean wait=true;
		
		public String getWorkingDirectory() {
			return workingDirectory;
		}
		public void setWorkingDirectory(String workingDirectory) {
			this.workingDirectory = workingDirectory;
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
		public boolean isWait() {
			return wait;
		}
		public boolean getWait() {
			return wait;
		}
		public void setWait(boolean wait) {
			this.wait = wait;
		}
		
		public GuestProgramSpec getVmWareSpec() {
			GuestProgramSpec spec = new GuestProgramSpec();
		    spec.programPath = getExecutable();
		    spec.arguments = getArguments();
		    spec.workingDirectory = getWorkingDirectory();
		    
		    return spec;
		}
	}
}
