package gcm.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.Test;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

public class PushReceiveTest {

	@Test
	public void test() throws UnknownHostException, IOException, InterruptedException {

		String senderId = "yourSenderId";
		String key = "yourKey";

		PushServerReceiver reciver = new PushServerReceiver("localhost", 9876, senderId, key);

		Sender sender = new Sender(key);
		// wait max 5 sec until the app register
		String pushToken = reciver.getPushToken(5000);

		// Send Push notification
		String value = "Some randomness " + UUID.randomUUID().toString();
		Message message = new Message.Builder().addData("testMessage", value).build();
		Result result = sender.send(message, pushToken, 1);
		assertNotNull("check message id", result.getMessageId());

		// try to receive the push notification
		JSONObject push = reciver.getPush(5000);
		assertEquals("test receiving push notification", value, push.getJSONObject("data").getString("testMessage"));

	}
}
