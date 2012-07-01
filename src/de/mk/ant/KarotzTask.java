package de.mk.ant;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.peripheralware.karotz.action.KarotzAction;
import org.peripheralware.karotz.client.KarotzClient;
import org.peripheralware.karotz.publisher.KarotzActionPublisher;

public class KarotzTask extends Task  {

	String apiKey;
	String secretKey;
	String installId;
	List<AntKarotzAction> actions = new ArrayList<AntKarotzAction>(); 
	
	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getInstallId() {
		return installId;
	}

	public void setInstallId(String installId) {
		this.installId = installId;
	}

	public AntKarotzAction createKarotzAction() {
		return new AntKarotzAction();
	}

	public void addKarotzAction(AntKarotzAction a) {
		actions.add(a);
	}
	


	@Override
	public void execute() throws BuildException {

		KarotzClient client = new KarotzClient(apiKey,secretKey,installId);
		KarotzActionPublisher karotzActionPublisher = new KarotzActionPublisher(client);
		try {
		client.startInteractiveMode();
		} catch(Exception e) {
			throw new BuildException(e);
		}
		for(AntKarotzAction a : actions) {
			try {
				karotzActionPublisher.performAction(a.action());
			} catch (Exception e) {
				e.printStackTrace();
				throw new BuildException(e);
			}
		}
		
	}	
	
	public class AntKarotzAction {
		
		String type;
		String text;
		
		
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public KarotzAction action() {
			return null;
		}
	}
}
