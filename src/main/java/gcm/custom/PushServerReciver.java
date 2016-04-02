package gcm.custom;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class PushServerReciver extends NanoHTTPD {

	private static final int PORT = 9876;

	public static PushServerReciver instance;

	public static PushServerReciver getInstance() throws IOException {
		return getInstance(PORT);
	}

	public static PushServerReciver getInstance(int port) throws IOException {
		if (instance == null) {
			instance = new PushServerReciver(port);
		}
		return instance;
	}

	public static void main(String[] args) throws IOException {
		PushServerReciver.getInstance();
	}

	private ConcurrentLinkedQueue<JSONObject> pushNotifications = new ConcurrentLinkedQueue<>();

	private String pushToken;

	private PushServerReciver(int port) throws IOException {
		super(port);
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}

	public synchronized Response serve(IHTTPSession session) {
		Map<String, String> files = new HashMap<String, String>();
		Method method = session.getMethod();
		if (Method.POST.equals(method)) {

			try {
				session.parseBody(files);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// get the POST body
			String postBody = files.get("postData");
			JSONObject body = new JSONObject(postBody);
			String type = body.optString("pushToken", null);
			if (type == null) {
				pushNotifications.add(body);
			} else {
				String pushToken = body.getString("pushToken");
				if (!pushToken.equals(this.pushToken)) {
					this.pushToken = pushToken;
				}
			}

		}

		return newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, "ok i am ");
	}

	public void clearQueue() {
		this.pushNotifications.clear();
	}

	public String getPushToken(long timeOut) throws InterruptedException {
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