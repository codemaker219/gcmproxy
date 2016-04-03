# gcmproxy
A proxy chrome app that sends gcm-notifications to Simple Java Server. 

This is a **quick and dirty** solution if you want to test GCM-push-notifications by google. This project is designed for **testing purpose**.

# Usage 
This Project consists of two parts a Chrome-App to proxy push-notifications and a Java-Server that receives the push-notification form the Chrome-App

# Java-Client
The Java Client connects to the Chrome-App via a tcp connection. For the Client you must specify the url where the Chrome-App is running and its port (9876 is default). Additionally you must specify the Sender Id and the String Identifier in order to receive Push-Notifications.

In this client you have access to the Push-Token and a Push-Notifications. For both you must specify a timeout until the Chrome-App has to response for this informations. 

# Chrome App
Because there is no solution to get GCM-notifications in Java without Android we use this chrome app to receive the Notifications. This app works as a proxy and forwards received notifications to the connected clients.

To install the app you can follow this instructions  https://developer.chrome.com/extensions/getstarted#unpacked.

This app is a mix of https://github.com/googlesamples/gcm-playground and https://github.com/GoogleChrome/chrome-app-samples/tree/master/samples/tcpserver. 
 
# Abstract Workflow

![](https://github.com/codemaker219/gcmproxy/blob/master/Zeichnung.gif)

# Communication

There are a few commands that you can send to the Chrome-App. The commands are always in JSON and encoded in Base64 appended with the delimiter \n\r.
Messages from the app are sent like this also.

Commands are 

1. echo: The app will response the same message. e.g Request: ```{"cmd":"echo","text":"1234"}``` Response: ```{"type":"echoResponse","text":"1234"}```
2. register: The app will register to GCM. This will unregister from GCM if it is already registered: ```{"cmd":"register","senderId":"XXX","stringIdentifier":"XXX"}``` Response: No Response
3. registerWithoutOverride: The app will register to GCM only if it is not already registered. ```{"cmd":"registerWithoutOverride","senderId":"XXX","stringIdentifier":"XXX"}``` Response: No Response
4. unregister: The app will unregister from GCM. ```{"cmd":"unregister"}``` Response: No Response

The Chrome-App will broadcast to every connected Client 

1. if the push-token is change   e.g ```{"type":"regestrationComplite","token":"XXX"}``` 
2. if the app has unregistered from GCM e.q ```{"type":"unregestrationComplete","token":""}```
3. if during the registration-process something gone wrong: e.q ```{"type":"regestrationError","message":"XXX"}```
4. if during the unregistration-process something gone wrong: e.q ```{"type":"unregestrationError","message":"XXX"}```
5. if a push notification was received: e.q ```{"type":"push","push":{XXX}}```


# Example

For a simple test you can look at the PushReceiveTest.java. There is also a simple PushServerReceiver implementation. **Please make sure that the Chrome-App is running**!

```java
String senderId = "yourSenderId";
String key = "yourKey";

//init the reciver
PushServerReceiver reciver = new PushServerReceiver("localhost", 9876, senderId, key);
//int a sender
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

```