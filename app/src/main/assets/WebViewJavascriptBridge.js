//notation: js file can only use this kind of comments
//since comments will cause error when use in webView.loadUrl,
//comments will be remove by java use regexp
//function 是指定义一个函数
(function() {
    //
    if (window.WebViewJavascriptBridge) {
        return;
    }

    var bizMessagingIframe;
    // H5发送消息的队列
    var sendMessageQueue = [];
    // H5接收消息的队列
    var receiveMessageQueue = [];
    // 消息处理器
    var messageHandlers = {};

    //这个是自定义scheme协议
    var CUSTOM_PROTOCOL_SCHEME = 'yy';
    //队列消息
    var QUEUE_HAS_MESSAGE = '__QUEUE_MESSAGE__/';
    //H5将消息发送给native，如果H5需要接受native的反馈，就需要提供一个callback
    //responseCallbacks中保存着H5提供的回调，以key-value的形式保存，key对应callbackId,value对应着回调
    var responseCallbacks = {};
    var uniqueId = 1;
    var lastCallTime = 0;
    var stoId = null;
    // fetchQueue中处理数据间隔
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
        //初始化时查看是否已经有原生消息过来了，有的话需要处理
        for (var i = 0; i < receivedMessages.length; i++) {
            console.log('init 调用 _dispatchMessageFromNative'+receivedMessages[i]);
            _dispatchMessageFromNative(receivedMessages[i]);
        }
    }

    // 发送 H5单独调用send方法 通知 原生
    function send(data, responseCallback) {
    console.log('调用send方法');
        _doSend('send',{
            data: data
        }, responseCallback);
    }

    // H5注册消息处理器 原生发送消息来的时候取handlerName对应的handler进行处理
    function registerHandler(handlerName, handler) {
        messageHandlers[handlerName] = handler;
        console.log('调用registerHandler，注册方法 handlerName = '+handlerName);
    }

    // H5调用，指定Handler
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

    //H5 sendMessage add message, 触发native处理 sendMessage
    function _doSend(handlerName,message, responseCallback) {
     console.log('_doSend，handlerName : '+handlerName);
     console.log('_doSend，message : '+JSON.stringify(message));
     console.log('_doSend，responseCallback : '+responseCallback);
        var callbackId;
        if(typeof responseCallback === 'string'){
            callbackId = responseCallback;
            console.log('_doSend，callbackId1 : '+callbackId);
        } else if (responseCallback) {
            callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
            responseCallbacks[callbackId] = responseCallback;
            message.callbackId = callbackId;
            console.log('_doSend，callbackId2 : '+callbackId);
        }else{
            callbackId = '';
        }
        try {
             var fn = eval(window.android[handlerName]);
             console.log('_doSend，fn : '+fn);
         } catch(e) {
             console.log(e);
         }
         if (typeof fn === 'function'){
             var responseData ;
             if(handlerName == "send"){
                 responseData = android.send(JSON.stringify(message), callbackId);
                 console.log('_doSend，responseData，send : '+responseData);
             }else if(handlerName == "response"){
                 responseData = android.response(JSON.stringify(message), callbackId);
                 console.log('_doSend，responseData，response : '+responseData);
             }else{
                responseData= android[handlerName](JSON.stringify(message), callbackId);
                console.log('_doSend，responseData，else : '+responseData);
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
        console.log('触发触发原生shouldOverrideUrlLoading方法，bizMessagingIframe.src : '+CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE.src);
        bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;
    }

    // 提供给native调用,该函数作用:获取sendMessageQueue返回给native
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
        //android不能直接读取返回的数据 所以我们需要通过Iframe方式重新触发android的shouldOverrideUrlLoading处理
        //触发shouldOverrideUrlLoading
        console.log('触发触发原生shouldOverrideUrlLoading方法，bizMessagingIframe.src : '+CUSTOM_PROTOCOL_SCHEME + '://return/_fetchQueue/' + encodeURIComponent(messageQueueString));
        bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://return/_fetchQueue/' + encodeURIComponent(messageQueueString);
    }


    //提供给原生使用 原生通过webView的dispatchMessage方法中的loadUrl加载"javascript:WebViewJavascriptBridge._handleMessageFromNative('%s');"调用
    function _dispatchMessageFromNative(messageJSON) {
        setTimeout(function() {
        console.log('_dispatchMessageFromNative messageJSON:= '+ JSON.stringify(messageJSON));
            var message = JSON.parse(messageJSON);
            var responseCallback;
            console.log('dispatchMessage message.responseId:= '+ message.responseId + "，message.callbackId:="+message.callbackId);
            //如果有responseId，说明是JS调用原生后返回的，可直接回调H5的callback
            if (message.responseId) {
                console.log('message.responseId:= '+ message.responseId);
                // 从responseCallbacks里取出该id对应的callback
                responseCallback = responseCallbacks[message.responseId];
                if (!responseCallback) {
                    return;
                }
                console.log('responseCallback:= '+ responseCallback);
                responseCallback(message.responseData);
                delete responseCallbacks[message.responseId];
            } else {
                //原生直接调用H5
                //如果callbackId不为null，说明要回调原生
                if (message.callbackId) {
                    var callbackResponseId = message.callbackId;
                    //如果H5有返回responseData不为null，执行_doSend方法
                    responseCallback = function(responseData) {
                    console.log('执行H5的responseCallback，responseData = ' + responseData);
                    //调用_doSend将消息返回给native，handler指定为response
                        _doSend('response',{
                            responseId: callbackResponseId,
                            responseData: responseData
                        });
                    };
                }

                var handler = WebViewJavascriptBridge._messageHandler;
                //查询H5注册的handler
                if (message.handlerName) {
                    console.log('handlerName='+ message.handlerName);
                    handler = messageHandlers[message.handlerName];
                }
                //查找指定handler
                try {
                    //有handler的话,回调responseCallback，没有的话抛异常
                    handler(message.data, responseCallback);
                    console.log('handler='+ handler);
                } catch (exception) {
                    if (typeof console != 'undefined') {
                        console.log("WebViewJavascriptBridge: WARNING: javascript handler threw.", message, exception);
                    }
                }
            }
        });
    }

    //原生调用loadUrl方法通知js的时候触发此方法。receiveMessageQueue 在会在页面加载完后赋值为null
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
    //创建WebViewJavascriptBridge对象
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
    //此方法主要为了复制src时 触发webViewClient的shouldOverrideUrlLoading方法
    _createQueueReadyIframe4biz(doc);
    //创建Event的事件，通过document触发事件。
    var readyEvent = doc.createEvent('Events');
    readyEvent.initEvent('WebViewJavascriptBridgeReady');
    readyEvent.bridge = WebViewJavascriptBridge;
    doc.dispatchEvent(readyEvent);
})();
