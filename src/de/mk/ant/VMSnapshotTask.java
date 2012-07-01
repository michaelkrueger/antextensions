package de.mk.ant;

import java.net.URL;
import java.util.Arrays;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualMachineSnapshotInfo;
import com.vmware.vim25.VirtualMachineSnapshotTree;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.VirtualMachineSnapshot;


public class VMSnapshotTask extends Task {

	String url;
	String username;
	String password;
	String vmname;
	String snapshot;
	String operation;
	
	
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

			operation.execute(vm, getSnapshot());
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

	public String getSnapshot() {
		return snapshot;
	}

	public void setSnapshot(String snapshot) {
		this.snapshot = snapshot;
	}

	  static void listSnapshots(VirtualMachine vm)
	  {
	    if(vm==null)
	    {
	      return;
	    }
	    VirtualMachineSnapshotInfo snapInfo = vm.getSnapshot();
	    VirtualMachineSnapshotTree[] snapTree = 
	      snapInfo.getRootSnapshotList();
	    printSnapshots(snapTree);
	  }

	  static void printSnapshots(
	      VirtualMachineSnapshotTree[] snapTree)
	  {
	    for (int i = 0; snapTree!=null && i < snapTree.length; i++) 
	    {
	      VirtualMachineSnapshotTree node = snapTree[i];
	      System.out.println("Snapshot Name : " + node.getName());           
	      VirtualMachineSnapshotTree[] childTree = 
	        node.getChildSnapshotList();
	      if(childTree!=null)
	      {
	        printSnapshots(childTree);
	      }
	    }
	  }
	  
	  static VirtualMachineSnapshot getSnapshotInTree(
		      VirtualMachine vm, String snapName)
		  {
		    if (vm == null || snapName == null) 
		    {
		      return null;
		    }

		    VirtualMachineSnapshotTree[] snapTree = 
		        vm.getSnapshot().getRootSnapshotList();
		    if(snapTree!=null)
		    {
		      ManagedObjectReference mor = findSnapshotInTree(
		          snapTree, snapName);
		      if(mor!=null)
		      {
		        return new VirtualMachineSnapshot(
		            vm.getServerConnection(), mor);
		      }
		    }
		    return null;
		  }

		  static ManagedObjectReference findSnapshotInTree(
		      VirtualMachineSnapshotTree[] snapTree, String snapName)
		  {
		    for(int i=0; i <snapTree.length; i++) 
		    {
		      VirtualMachineSnapshotTree node = snapTree[i];
		      if(snapName.equals(node.getName()))
		      {
		        return node.getSnapshot();
		      } 
		      else 
		      {
		        VirtualMachineSnapshotTree[] childTree = 
		            node.getChildSnapshotList();
		        if(childTree!=null)
		        {
		          ManagedObjectReference mor = findSnapshotInTree(
		              childTree, snapName);
		          if(mor!=null)
		          {
		            return mor;
		          }
		        }
		      }
		    }
		    return null;
		  }
		  
	private enum Operations {
		create { 
			public void execute(VirtualMachine vm, String snapshotname)  throws Exception {
				System.out.println("Create Snapshot in VM "+vm.getName()+": Name:"+snapshotname);
				com.vmware.vim25.mo.Task task = vm.createSnapshot_Task(
						snapshotname, "", false, false);
				if(task.waitForMe()==com.vmware.vim25.mo.Task.SUCCESS)
				{
					System.out.println("Snapshot was created.");
				}
			}
		}, remove { 
			public void execute(VirtualMachine vm, String snapshotname)  throws Exception {
				System.out.println("Remove Snapshot in VM "+vm.getName()+": Name:"+snapshotname);
				com.vmware.vim25.mo.Task task = vm.createSnapshot_Task(
						snapshotname, "", false, false);
				if(task.waitForMe()==com.vmware.vim25.mo.Task.SUCCESS)
				{
					System.out.println("Snapshot was created.");
				}
			}
		}, removeall { 
			public void execute(VirtualMachine vm, String snapshotname)  throws Exception {
				System.out.println("Remove All Snapshots from VM "+vm.getName()+": Name:"+snapshotname);
				com.vmware.vim25.mo.Task task = vm.removeAllSnapshots_Task();      
				if(task.waitForMe()== com.vmware.vim25.mo.Task.SUCCESS) 
				{
					System.out.println("Removed all snapshots");
				}
			}
		}, revert { 
			public void execute(VirtualMachine vm, String snapshotname)  throws Exception {
				System.out.println("Remove All Snapshots from VM "+vm.getName()+": Name:"+snapshotname);
				VirtualMachineSnapshot vmsnap = getSnapshotInTree(
				          vm, snapshotname);
				      if(vmsnap!=null)
				      {
				    	  com.vmware.vim25.mo.Task task = vmsnap.revertToSnapshot_Task(null);
				        if(task.waitForMe()==com.vmware.vim25.mo.Task.SUCCESS)
				        {
				          System.out.println("Reverted to snapshot:" 
				              + snapshotname);
				        }
				      }
			}
		}, list { 
			public void execute(VirtualMachine vm, String snapshotname)  throws Exception {
				listSnapshots(vm);
			}
		};
		
		abstract public void execute(VirtualMachine vm, String snapshotname) throws Exception ;
	};
}
