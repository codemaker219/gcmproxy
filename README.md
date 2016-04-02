# gcmproxy
A proxy chrome app that sends gcm-notifications to Simple Java Server. 

This is a **quik and dirty** solution if you want to test GCM-push-notifications by google. This project is designed for **testing purpose**.

# Usage 
This Project consits of two parts a Chrome-App to proxy push-notifications and a Java-Server that recieves the push-notification form the Chrome-App

# Java-Server
There is a class that starts a lightweight Java-server. Therefore the NanoHTTPD farmework is used (see https://github.com/NanoHttpd/nanohttpd)
The port is 9876 (default). With this class you can access the push token from the chrome app and the recived push-notifications. 

# Chrome App
Because there is no solution to get GCM-notifications in Java without Android we use this chrome app to recive the Notifications. This app works as a proxy and forwards recived notifications to a defined url.

To install the app you can follow this instuctions  https://developer.chrome.com/extensions/getstarted#unpacked.

This app is copied from https://github.com/googlesamples/gcm-playground and extened with a additional field Target url. 

The App will send two Types of Requests:

 1. The Registration-Id of the App. Therefore the app will send a request each 100ms a POST request with the actual Regestration-Id. The Body-content of the request is ```{"pushToken":"<actual token>"}```
 2. The Push-Notification if recived. Therefore a POST reqest will be send to the defined url.
 


  
 