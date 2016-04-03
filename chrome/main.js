var tcpServer;
var commandWindow;

/**
 * Listens for the app launching then creates the window
 * 
 * @see https://developer.chrome.com/apps/app_runtime
 * @see https://developer.chrome.com/apps/app_window
 */
chrome.app.runtime.onLaunched.addListener(function() {
	if (commandWindow && !commandWindow.contentWindow.closed) {
		commandWindow.focus();
	} else {
		chrome.app.window.create('index.html', {
			id : "mainwin",
			innerBounds : {
				width : 500,
				height : 309,
				left : 0
			}
		}, function(w) {
			commandWindow = w;
		});
	}

	chrome.gcm.onMessage.addListener(function(message) {
		console.log('chrome.gcm.onMessage', message);
		var push = JSON.stringify(message);
		log.output(push);
		broeadcast({
			type : 'push',
			push : message
		});
	});

});
var registerNewClient = "register_new_client";
var unregisterClient = "unregister_client";
var registrationToken = '';
var senderId = '';
var stringIdentifier = '';

// event logger
var log = (function() {
	var logLines = [];
	var logListener = null;

	var output = function(str) {
		if (str.length > 0 && str.charAt(str.length - 1) != '\n') {
			str += '\n'
		}
		logLines.push(str);
		if (logListener) {
			logListener(str);
		}
	};

	var addListener = function(listener) {
		logListener = listener;
		// let's call the new listener with all the old log lines
		for (var i = 0; i < logLines.length; i++) {
			logListener(logLines[i]);
		}
	};

	return {
		output : output,
		addListener : addListener
	};
})();

function onAcceptCallback(tcpConnection, socketInfo) {
	var info = "[" + socketInfo.peerAddress + ":" + socketInfo.peerPort
			+ "] Connection accepted!";
	log.output(info);
	console.log(socketInfo);
	tcpConnection.buffer = '';
	tcpConnection.addDataReceivedListener(function(data) {
		tcpConnection.buffer += data;
		var idx = tcpConnection.buffer.indexOf('\n\r');
		if (idx != -1) {
			var part = tcpConnection.buffer.substring(0, idx);
			tcpConnection.buffer = tcpConnection.buffer.substring(idx + 1);

			var obj = JSON.parse(window.atob(part));
			log.output(JSON.stringify(obj));

			var re = {};

			if (obj.cmd == 'echo') {
				re = {
					type : 'echoResponse',
					text : obj.text
				};
				var msg = window.btoa(JSON.stringify(re));
				tcpConnection.sendMessage(msg);
			} else if (obj.cmd == 'register') {
				senderId = obj.senderId;
				stringIdentifier = obj.stringIdentifier;
				isRegistered(function(registered) {
					if (registered) {
						unregister();
					}
					register();
				});

			} else if (obj.cmd == 'registerWithoutOverride') {

				senderId = obj.senderId;
				stringIdentifier = obj.stringIdentifier;
				isRegistered(function(registered) {
					if (!registered) {
						register();
					} else {
						re = {
							type : 'regestrationComplite',
							token : registrationToken
						};
						var msg = window.btoa(JSON.stringify(re));
						tcpConnection.sendMessage(msg);
					}

				});

			} else if (obj.cmd == 'unregister') {
				isRegistered(function(registered) {
					if (registered) {
						unregister();
					}
				});
			}
		}
	});
};

function startServer(addr, port) {
	if (tcpServer) {
		tcpServer.disconnect();
	}
	tcpServer = new TcpServer(addr, port);
	tcpServer.listen(onAcceptCallback);

	isRegistered(function(registered) {
		if (registered) {
			handleAlreadyRegistered();
		} else {
			log.output('not registered');
		}
	});
}

function stopServer() {
	if (tcpServer) {
		tcpServer.disconnect();
		tcpServer = null;
	}
}

function getServerState() {
	if (tcpServer) {
		return {
			isConnected : tcpServer.isConnected(),
			addr : tcpServer.addr,
			port : tcpServer.port
		};
	} else {
		return {
			isConnected : false
		};
	}
}

function broeadcast(msg) {
	var msg = window.btoa(JSON.stringify(msg));
	// tcpConnection.sendMessage(msg);
	if (tcpServer) {
		var con = tcpServer.getAllConnection();
		for (var i = 0; i < con.length; i++) {
			con[i].sendMessage(msg);
		}
	}

}

/**
 * Call cb with `true` if the client is registered, false otherwise.
 */
function isRegistered(cb) {
	chrome.storage.local.get('registered', function(result) {
		cb(result['registered']);
	});
}

/**
 * Handles the case when the client is already registered.
 */
function handleAlreadyRegistered() {
	chrome.storage.local.get('regToken', function(result) {
		registrationToken = result['regToken'];
		log.output('Already registered. Registration token:\n'
				+ registrationToken);
	});
}

/**
 * Called when GCM server responds to an unregistration request.
 */
function unregisterCallback() {

	if (chrome.runtime.lastError) {
		log.output('FAILED: ' + chrome.runtime.lastError.message);
		broeadcast({
			type : 'unregestrationError',
			message : 'FAILED: ' + chrome.runtime.lastError.message
		});
	}

	log.output('Unregistration SUCCESSFUL');
	chrome.storage.local.remove([ 'registered', 'regToken' ]);

	// Notify the app server about this unregistration
	unregisterFromAppServer(function(succeed, err) {
		if (succeed) {
			log.output('Unregistration with the app server SUCCESSFUL.');
			registrationToken = '';
			broeadcast({
				type : 'unregestrationComplete',
				token : ''
			});
		} else {
			log.output('Unregistration with app server FAILED: ' + err);
			broeadcast({
				type : 'unregestrationError',
				message : 'Unregistration with app server FAILED: ' + err
			});
		}
	});
}

/**
 * Called when GCM server responds to a registration request.
 */
function registerCallback(regToken) {

	if (chrome.runtime.lastError) {
		log.output('FAILED: ' + chrome.runtime.lastError.message);
	}

	log.output('Registration SUCCESSFUL. Registration ID:\n' + regToken);

	registrationToken = regToken;

	// Notify the app server about this new registration
	registerWithAppServer(registrationToken, function(succeed, err) {
		if (succeed) {
			log.output('Registration with app server SUCCESSFUL.');
			chrome.storage.local.set({
				registered : true
			});
			chrome.storage.local.set({
				regToken : registrationToken
			});

			broeadcast({
				type : 'regestrationComplite',
				token : registrationToken
			});
		} else {
			log.output('Registration with app server FAILED: ' + err);
			broeadcast({
				type : 'regestrationError',
				message : 'Registration with app server FAILED: ' + err
			});
		}
	});
}

/**
 * Calls the GCM API to unregister this client.
 */
function unregister() {
	chrome.gcm.unregister(unregisterCallback);
	log.output('Unregistering...');
}

/**
 * Calls the GCM API to register this client if not already registered.
 */
function register() {
	isRegistered(function(registered) {
		// If already registered, bail out.
		if (registered)
			return handleAlreadyRegistered();

		if (!senderId) {
			log.output('Please provide a valid sender ID.');
		}
		chrome.gcm.register([ senderId ], registerCallback);
		log.output('Registering...');
	});
}

/**
 * Register a GCM registration token with the app server.
 */
function registerWithAppServer(regToken, cb) {
	var data = {
		action : registerNewClient,
		registration_token : regToken
	};
	if (stringIdentifier)
		data.stringIdentifier = stringIdentifier;

	var message = buildMessagePayload(data);
	chrome.gcm.send(message, function(messageId) {
		if (chrome.runtime.lastError) {
			cb(false, chrome.runtime.lastError);
		} else {
			cb(true);
		}
	});
}

/**
 * Unregister a registration token from the app server.
 */
function unregisterFromAppServer(cb) {
	var message = buildMessagePayload({
		action : unregisterClient,
		registration_token : registrationToken
	});

	chrome.gcm.send(message, function(messageId) {
		if (chrome.runtime.lastError) {
			cb(false, chrome.runtime.lastError);
		} else {
			cb(true);
		}
	});
}

/**
 * Returns a message payload sent using GCM.
 */
function buildMessagePayload(data) {
	return {
		// This is not generating strong unique IDs, which is what you probably
		// want in a production application.
		messageId : new Date().getTime().toString(),
		destinationId : senderId + '@gcm.googleapis.com',
		data : data
	};
}