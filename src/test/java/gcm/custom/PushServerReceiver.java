package gcm.custom;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

import gcm.custom.AbstractPushServerReciver;

public class PushServerReceiver extends AbstractPushServerReciver {

	public PushServerReceiver(String host, int port, String senderId, String stringIdentifier)
			throws UnknownHostException, IOException {
		super(host, port, senderId, stringIdentifier);
	}

	private ConcurrentLinkedQueue<String> registrationErrors = new ConcurrentLinkedQueue<>();

	@Override
	public void onRegistrationError(String message) {
		System.out.println(message);
		registrationErrors.add(message);

	}

	private ConcurrentLinkedQueue<String> unRsegistrationErrors = new ConcurrentLinkedQueue<>();

	@Override
	public void onUnRigestrationError(String message) {
		System.out.println(message);
		unRsegistrationErrors.add(message);

	}

	private ConcurrentLinkedQueue<String> echos = new ConcurrentLinkedQueue<>();

	@Override
	public void onEcho(String message) {
		System.out.println(message);
		echos.add(message);

	}

	private ConcurrentLinkedQueue<String> other = new ConcurrentLinkedQueue<>();

	@Override
	public void onUnsupportedMessage(String message) {
		System.out.println(message);
		other.add(message);

	}

	public ConcurrentLinkedQueue<String> getRegistrationErrors() {
		return registrationErrors;
	}

	public ConcurrentLinkedQueue<String> getUnRsegistrationErrors() {
		return unRsegistrationErrors;
	}

	public ConcurrentLinkedQueue<String> getEchos() {
		return echos;
	}

	public ConcurrentLinkedQueue<String> getOther() {
		return other;
	}
	
	

}
