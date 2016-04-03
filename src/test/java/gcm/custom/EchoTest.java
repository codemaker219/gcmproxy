package gcm.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.Test;

public class EchoTest {

	@Test
	public void test() throws UnknownHostException, IOException, InterruptedException {
		System.out.println("Make shure that the chrome app is running!");
		PushServerReceiver reciever = new PushServerReceiver("localhost", 9876, null, null);
		String message = "Hello World!";
		reciever.sendEchoMessage(message);
		// wait some time
		Thread.sleep(1000);
		String echo = reciever.getEchos().poll();
		assertNotNull("echo message should not be null", echo);
		assertEquals("echo message does not match", message, echo);
	}

}
