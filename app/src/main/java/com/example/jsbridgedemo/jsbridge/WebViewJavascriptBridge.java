package com.example.jsbridgedemo.jsbridge;

public interface WebViewJavascriptBridge {
	
	void sendToWeb(String handlerName,Object data);

	void sendToWeb(String handlerName,Object data, OnBridgeCallback responseCallback);
}
