package gcm.custom;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONObject;

public class PushServerReciver {

	private static final String DELIMITER = "\n\r";
	private Socket socket;
	private OutputStreamWriter osw;
	private ConcurrentLinkedQueue<JSONObject> pushNotifications = new ConcurrentLinkedQueue<>();

	private String pushToken;

	private String senderId, stringIdentifier;

	public PushServerReciver(String host, int port, String senderId, String stringIdentifier)
			throws UnknownHostException, IOException {
		socket = new Socket(host, port);
		this.senderId = senderId;
		this.stringIdentifier = stringIdentifier;
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(socket.getInputStream());

		new Thread() {
			public void run() {
				while (true) {
					JSONObject code = decode(sc.nextLine());
					String type = code.optString("type", null);
					if ("unregestrationComplete".equals(type)) {
						pushToken = "";
					} else if ("regestrationComplite".equals(type)) {
						pushToken = code.getString("token");
					} else if ("push".equals(type)) {
						pushNotifications.add(code.getJSONObject("push"));
					} else if ("echoResponse".equals(type)) {
						System.out.println(code);
					} else if ("regestrationError".equals(type)) {
						System.out.println(code);
					} else if ("unregestrationError".equals(code)) {
						System.out.println(code);
					} else {
						System.out.println("Can not interpret " + code);
					}
				}
			}
		}.start();
		osw = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
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
			throw new PushServerTimeOutException("No Push received in " + timeOut + "ms");
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
		throw new PushServerTimeOutException("Push recived in " + timeOut + "ms");
	}

}