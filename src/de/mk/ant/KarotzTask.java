package de.mk.ant;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.peripheralware.karotz.action.KarotzAction;
import org.peripheralware.karotz.action.multimedia.PlayMultimediaAction;
import org.peripheralware.karotz.action.tts.SpeakAction;
import org.peripheralware.karotz.client.KarotzClient;
import org.peripheralware.karotz.publisher.KarotzActionPublisher;

public class KarotzTask extends Task  {

	String apiKey;
	String secretKey;
	String installId;
	List<KarotzActionFactory> actions = new ArrayList<KarotzActionFactory>(); 
	
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

	public KarotzActionFactory createKarotzSpeak() {
		return new KarotzSpeakAction();
	}

	public void addKarotzSpeak(KarotzActionFactory a) {
		actions.add(a);
	}
	
	public KarotzActionFactory createKarotzPlay() {
		return new KarotzPlayAction();
	}

	public void addKarotzPlay(KarotzActionFactory a) {
		actions.add(a);
	}


	@Override
	public void execute() throws BuildException {

		Logger.getLogger(KarotzTask.class).addAppender(new ConsoleAppender());
		
		
		KarotzClient client = new KarotzClient(apiKey,secretKey,installId);
		KarotzActionPublisher karotzActionPublisher = new KarotzActionPublisher(client);
		try {
			client.startInteractiveMode();
			
			for(KarotzActionFactory a : actions) {
				try {
					karotzActionPublisher.performAction(a.action());
					
					 
				} catch (Exception e) {
					e.printStackTrace();
					throw new BuildException(e);
				}
			}
			 
			client.stopInteractiveMode();
		} catch(Exception e) {
			throw new BuildException(e);
		}
		
	}	
	
	interface KarotzActionFactory {
		KarotzAction action();
	}
	
	public class KarotzSpeakAction implements KarotzActionFactory {
		
	    private String text;
	    private String language = "EN";

		public String getText() {
			return text;
		}

		public void setText(String textToSpeak) {
			this.text = textToSpeak;
		}

		public String getLanguage() {
			return language;
		}

		public void setLanguage(String language) {
			this.language = language;
		}

		public KarotzAction action() {
			return new SpeakAction(getText(), getLanguage());
		}
	}
	
	public class KarotzPlayAction implements KarotzActionFactory {
		
		String url;
		
		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public KarotzAction action() {
			return new PlayMultimediaAction(getUrl());
		}
	}
}
