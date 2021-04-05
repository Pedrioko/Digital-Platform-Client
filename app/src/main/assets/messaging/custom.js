const message = { type: "WPAManifest", manifest: {} };

browser.runtime.sendNativeMessage("browser", message);

// Establish connection with app
let port = browser.runtime.connectNative("browser");
port.postMessage("Hello from WebExtension!");

port.onMessage.addListener(response => {
    // Let's just echo the message back

    if (response.action == "next")
        window.wrappedJSObject.next();

    if (response.action == "back")
        window.wrappedJSObject.back();

    console.log(window.wrappedJSObject);
    port.postMessage(`Received: ${JSON.stringify(response)}`);
});
port.postMessage("Hello from WebExtension!");