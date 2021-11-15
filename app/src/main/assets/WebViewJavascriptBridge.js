//notation: js file can only use this kind of comments
//since comments will cause error when use in webview.loadurl,
//comments will be remove by java use regexp
//function 是指定义一个函数
(function() {
    //
    if (window.WebViewJavascriptBridge) {
        return;
    }

    var bizMessagingIframe;
    var sendMessageQueue = [];
    var receiveMessageQueue = [];
    var messageHandlers = {};

    //这个是自定义scheme协议
    var CUSTOM_PROTOCOL_SCHEME = 'yy';
    //队列消息
    var QUEUE_HAS_MESSAGE = '__QUEUE_MESSAGE__/';

    var responseCallbacks = {};
    var uniqueId = 1;
    var lastCallTime = 0;
    var stoId = null;
    var FETCH_QUEUE_INTERVAL = 20;

    //创建消息体队列iframe
    function _createQueueReadyIframe4biz(doc) {
        bizMessagingIframe = doc.createElement('iframe');
        bizMessagingIframe.style.display = 'none';
        doc.documentElement.appendChild(bizMessagingIframe);
    }

    //set default messageHandler  初始化默认的消息线程
    function init(messageHandler) {
        if (WebViewJavascriptBridge._messageHandler) {
            throw new Error('WebViewJavascriptBridge.init called twice');
        }
        console.log('初始化成功');
        WebViewJavascriptBridge._messageHandler = messageHandler;
        var receivedMessages = receiveMessageQueue;
        receiveMessageQueue = null;
        for (var i = 0; i < receivedMessages.length; i++) {
            console.log('init 调用 _dispatchMessageFromNative'+receivedMessages[i]);
            _dispatchMessageFromNative(receivedMessages[i]);
        }
    }

    // 发送 js单独调用send方法 通知 原生
    function send(data, responseCallback) {
    console.log('调用send方法');
        _doSend('send',{
            data: data
        }, responseCallback);
    }

    // 注册线程 往数组里面添加值
    function registerHandler(handlerName, handler) {
        messageHandlers[handlerName] = handler;
        console.log('调用registerHandler，注册方法 handlerName = '+handlerName);
    }

    // 调用线程
    function callHandler(handlerName, data, responseCallback) {

            // 如果方法不需要参数，只有回调函数，简化JS中的调用
            if (arguments.length == 2 && typeof data == 'function') {
             console.log('调用callHandler');
    			responseCallback = data;
    			data = null;
    		}
    		 console.log('调用callHandler中的 _doSend');
         _doSend(handlerName,{
                   handlerName: handlerName,
                   data: data
               }, responseCallback);
    }

    //sendMessage add message, 触发native处理 sendMessage
    function _doSend(handlerName,message, responseCallback) {
     console.log('handlerName : '+handlerName);
     console.log('message : '+JSON.stringify(message));
      console.log('responseCallback : '+responseCallback);
        var callbackId;
        if(typeof responseCallback === 'string'){
            callbackId = responseCallback;
        } else if (responseCallback) {
            callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
            responseCallbacks[callbackId] = responseCallback;
            message.callbackId = callbackId;
        }else{
            callbackId = '';
        }
        try {
             var fn = eval(window.android[handlerName]);
         } catch(e) {
             console.log(e);
         }
         if (typeof fn === 'function'){
             var responseData ;
             if(handlerName == "send"){
                 console.log(handlerName);
                 responseData = android.send(JSON.stringify(message), callbackId);
             }else if(handlerName == "response"){
                 console.log(handlerName);
                 responseData = android.response(JSON.stringify(message), callbackId);
             }else{
                responseData= android[handlerName](JSON.stringify(message), callbackId);
             }

             console.log('response message: '+ responseData);
             if(responseData){
                 responseCallback = responseCallbacks[callbackId];
                 if (!responseCallback) {
                     return;
                  }
                 responseCallback(responseData);
                 delete responseCallbacks[callbackId];
             }
         }
        console.log('message : '+JSON.stringify(message));
        //存入队列
        sendMessageQueue.push(message);
         //触发原生shouldOverrideUrlLoading方法
        bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;
        console.log('触发触发原生shouldOverrideUrlLoading方法，bizMessagingIframe.src : '+bizMessagingIframe.src);
    }

    // 提供给native调用,该函数作用:获取sendMessageQueue返回给native,
    // 由于android不能直接获取返回的内容,所以使用url shouldOverrideUrlLoading 的方式返回内容
    function _fetchQueue() {
        console.log('_fetchQueue : '+JSON.stringify(sendMessageQueue));
        // 空数组直接返回
        if (sendMessageQueue.length === 0) {
          return;
        }

        // _fetchQueue 的调用间隔过短，延迟调用
        if (new Date().getTime() - lastCallTime < FETCH_QUEUE_INTERVAL) {
          if (!stoId) {
            stoId = setTimeout(_fetchQueue, FETCH_QUEUE_INTERVAL);
          }
          return;
        }
        lastCallTime = new Date().getTime();
        stoId = null;
        var messageQueueString = JSON.stringify(sendMessageQueue);
        sendMessageQueue = [];
        //android can't read directly the return data, so we can reload iframe src to communicate with java
        bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://return/_fetchQueue/' + encodeURIComponent(messageQueueString);
        console.log('触发触发原生shouldOverrideUrlLoading方法，bizMessagingIframe.src : '+bizMessagingIframe.src);
    }


  //提供给native使用
    function _dispatchMessageFromNative(messageJSON) {
        setTimeout(function() {
        console.log('_dispatchMessageFromNative messageJSON:= '+ JSON.stringify(messageJSON));
            var message = JSON.parse(messageJSON);
            var responseCallback;
            console.log('dispatchMessage message.responseId:= '+ message.responseId + "，message.callbackId:="+message.callbackId);
            //java call finished, now need to call js callback function
            if (message.responseId) {
            console.log('message.responseId:= '+ message.responseId);
                responseCallback = responseCallbacks[message.responseId];
                if (!responseCallback) {
                    return;
                }
                responseCallback(message.responseData);
                delete responseCallbacks[message.responseId];
            } else {
                //直接发送
                if (message.callbackId) {
                 console.log('直接发送调用response方法，responseId = ' + message.callbackId);
                    var callbackResponseId = message.callbackId;
                    responseCallback = function(responseData) {
                        _doSend('response',{
                            responseId: callbackResponseId,
                            responseData: responseData
                        });
                    };
                }

                var handler = WebViewJavascriptBridge._messageHandler;
                if (message.handlerName) {
                 console.log('handlerName='+ message.handlerName);
                    handler = messageHandlers[message.handlerName];
                }
                //查找指定handler
                try {
                    handler(message.data, responseCallback);
                } catch (exception) {
                    if (typeof console != 'undefined') {
                        console.log("WebViewJavascriptBridge: WARNING: javascript handler threw.", message, exception);
                    }
                }
            }
        });
    }

    //原生调用loadUrl方法通知js的时候触发此方法。receiveMessageQueue 在会在页面加载完后赋值为null。
    function _handleMessageFromNative(messageJSON) {
        console.log('_handleMessageFromNative:'+messageJSON);
        if (receiveMessageQueue) {
            //添加到队列中
             console.log('_handleMessageFromNative:添加到队列中');
            receiveMessageQueue.push(messageJSON);
        }
         console.log('调用_dispatchMessageFromNative');
        _dispatchMessageFromNative(messageJSON);

    }

    var WebViewJavascriptBridge = window.WebViewJavascriptBridge = {
        init: init,
        send: send,
        registerHandler: registerHandler,
        callHandler: callHandler,
        _fetchQueue: _fetchQueue,
        _handleMessageFromNative: _handleMessageFromNative
    };
 console.log('调用WebViewJavascriptBridge成功');
    var doc = document;
    _createQueueReadyIframe4biz(doc);
    var readyEvent = doc.createEvent('Events');
    readyEvent.initEvent('WebViewJavascriptBridgeReady');
    readyEvent.bridge = WebViewJavascriptBridge;
    doc.dispatchEvent(readyEvent);
})();
