package gcm.custom;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONObject;

public abstract class AbstractPushServerReciver {

	private static final String DELIMITER = "\n\r";
	private Socket socket;
	private boolean closed = false;
	private OutputStreamWriter osw;
	private ConcurrentLinkedQueue<JSONObject> pushNotifications = new ConcurrentLinkedQueue<>();

	private String pushToken;

	private String senderId, stringIdentifier;

	public AbstractPushServerReciver(String host, int port, String senderId, String stringIdentifier)
			throws UnknownHostException, IOException {
		socket = new Socket(host, port);
		this.senderId = senderId;
		this.stringIdentifier = stringIdentifier;
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(socket.getInputStream());

		new Thread() {
			public void run() {
				while (true) {
					try {
						String nextLine = sc.nextLine();
						if (nextLine.matches("\\s*")) {
							continue;
						}
						JSONObject code = decode(nextLine);
						String type = code.optString("type", null);
						if ("unregestrationComplete".equals(type)) {
							pushToken = "";
						} else if ("regestrationComplite".equals(type)) {
							pushToken = code.getString("token");
						} else if ("push".equals(type)) {
							pushNotifications.add(code.getJSONObject("push"));
						} else if ("echoResponse".equals(type)) {
							onEcho(code.optString("text"));
						} else if ("regestrationError".equals(type)) {
							onRegistrationError(code.getString("message"));
						} else if ("unregestrationError".equals(code)) {
							onUnRigestrationError(code.getString("message"));
						} else {
							onUnsupportedMessage(code.toString());
						}
					} catch (NoSuchElementException e) {
						onUnsupportedMessage(e.getMessage());
						return;
					} catch (IllegalStateException e) {
						onUnsupportedMessage(e.getMessage());
						return;
					} catch (Exception e) {
						if (!closed) {
							onUnsupportedMessage(e.getMessage());
						}
					}
				}
			}
		}.start();
		osw = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
	}

	public void close() throws IOException {
		closed = true;
		socket.close();
	}

	public boolean isOpen() {
		return socket != null && !closed;
	}

	/**
	 * Callback if a registration error was send.
	 * 
	 * @param message
	 *            A description of the error
	 */
	public abstract void onRegistrationError(String message);

	/**
	 * Callback if a unregistration error was send.
	 * 
	 * @param message
	 *            A description of the error
	 */
	public abstract void onUnRigestrationError(String message);

	/**
	 * if a echo response was received
	 * 
	 * @param message
	 */
	public abstract void onEcho(String message);

	/**
	 * if a message was received which could not be parsed or interpret
	 * 
	 * @param message
	 *            the received message
	 */
	public abstract void onUnsupportedMessage(String message);

	public void sendEchoMessage(String message) throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("cmd", "echo");
		obj.put("text", message);
		sendCommand(obj);
	}

	public String getSenderId() {
		return senderId;
	}

	public String getStringIdentifier() {
		return stringIdentifier;
	}

	public void sendCommand(JSONObject obj) throws IOException {
		osw.write(encode(obj));
		osw.flush();
	}

	private String encode(JSONObject obj) {
		return Base64.getEncoder().encodeToString(obj.toString().getBytes(StandardCharsets.UTF_8)) + DELIMITER;
	}

	private JSONObject decode(String res) {
		String asSeting = new String(Base64.getDecoder().decode(res), StandardCharsets.UTF_8);
		return new JSONObject(asSeting);
	}

	public void clearQueue() {
		this.pushNotifications.clear();
	}

	public String getPushToken(long timeOut) throws InterruptedException, IOException {
		if (this.pushToken != null && !this.pushToken.isEmpty()) {
			return this.pushToken;
		} else {
			JSONObject cmd = new JSONObject();
			cmd.put("cmd", "registerWithoutOverride");
			cmd.put("senderId", senderId);
			cmd.put("stringIdentifier", stringIdentifier);

			sendCommand(cmd);

			long waitet = 0;
			while (waitet < timeOut) {
				if (this.pushToken != null && !this.pushToken.isEmpty()) {
					return this.pushToken;
				}
				Thread.sleep(10);
				waitet += 10;

			}
			throw new PushServerTimeOutException("No Push token received in " + timeOut + "ms");
		}

	}

	public JSONObject getPush(long timeOut) throws InterruptedException {
		long waitet = 0;
		while (waitet < timeOut) {
			JSONObject m = pushNotifications.poll();

			if (m != null) {
				return m;
			} else {
				Thread.sleep(10);
				waitet += 10;
			}
		}
		throw new PushServerTimeOutException("Push received in " + timeOut + "ms");
	}

}