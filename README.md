# gcmproxy
A proxy chrome app that sends gcm-notifications to Simple Java Server. 

This is a **quik and dirty** solution if you want to test GCM-push-notifications by google. This project is designed for **testing purpose**.

# Usage 
This Project consits of two parts a Chrome-App to proxy push-notifications and a Java-Server that recieves the push-notification form the Chrome-App

# Java-Client
The Java Client connects to the Chrome-App via a tcp connection. For the Client you must specify the url where ths Chrome-App is running and its port (9876 is default). Additionally you must specify the Sender Id and the String Identifier in order to recive Push-Notifications.

In this client you have access to the Push-Token and a Push-Notifications. For both you must specify a timeout untill the Chrome-App has to response for this infomations. 

# Chrome App
Because there is no solution to get GCM-notifications in Java without Android we use this chrome app to recive the Notifications. This app works as a proxy and forwards recived notifications to the connected clients.

To install the app you can follow this instuctions  https://developer.chrome.com/extensions/getstarted#unpacked.

This app is a thrown together from https://github.com/googlesamples/gcm-playground and https://github.com/GoogleChrome/chrome-app-samples/tree/master/samples/tcpserver. 
 
# Abstract Workflow

![](https://github.com/codemaker219/gcmproxy/blob/master/Zeichnung.gif)

  
# Comunictaion

There are a few commands that you can send to the Chrome-App. The commands are always in JSON and encoded in Base64 appended with the delimiter \n\r.
Messages from the App are sended like this also.

Commands are 

1. echo: The app will repsonse the same message. e.g Request: ```{"cmd":"echo","text":"1234"}``` Response: ```{"type":"echoResponse","text":"1234"}```
2. register: The app will register to GCM. This will unregister from GCM if it is already registered: ```{"cmd":"register","senderId":"XXX","stringIdentifier":"XXX"}``` Response: No Response
3. registerWithoutOverride: The app will register to GCM only if it is not already registered. ```{"cmd":"registerWithoutOverride","senderId":"XXX","stringIdentifier":"XXX"}``` Response: No Response
4. unregister: The app will unregister from GCM. ```{"cmd":"unregister"}``` Response: No Response

The Chrome-App will broadcast to every connected Client 
1. if the push-token is change   e.g ```{"type":"regestrationComplite","token":"XXX"}``` 
2. if the app has unregistered from GCM e.q ```{"type":"unregestrationComplete","token":""}```
3. if during the regestration-process something gone wrong: e.q ```{"type":"regestrationError","message":"XXX"}```
4. if during the unregestration-process something gone wrong: e.q ```{"type":"unregestrationError","message":"XXX"}```
5. if a push notification was recived: e.q ```{"type":"push","push":{XXX}}```