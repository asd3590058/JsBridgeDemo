# JsBridge原理
### 调用流程  1.原生调用H5

1.  MainActivity中通过webView调用callHandler方法，调用H5的functionInJs方法，最终调用queueMessage，将数据存放队列startupMessage中。
2.  页面加载结束之后回调onPageFinish方法加载WebViewJavascriptBridge.js文件，加载完毕回调OnLoadJsListener的onLoadFinished方法。其中
    实现是在BridgeWebView 中然后轮询startupMessage，将之前存储在queueMessage数据取出来，调用dispatchMessage方法通过loadUrl加载
    WebViewJavascriptBridge._handleMessageFromNative的方法处理数据。
3.  先注入WebViewJavascriptBridge.js文件，然后进行初始化，创建WebViewJavascriptBridge对象挂载在window上，初始化一些其他事情可以看下
    注释。
4.  调用_handleMessageFromNative方法，将进行数据存储在接受消息的队列中，然后调用_dispatchMessageFromNative方法。
5.  _dispatchMessageFromNative方法中由于是原生发起调用没有responseId，只有callbackId，所以走else逻辑，由于没有responseData，所以不
    执行_deSend方法,而却查找指定handler执行，但是没有指定的handler，所以抛异常。此时java发起调用，经过jsBridge桥，进入到js文件。
6.  demo.html中进行初始化connectWebViewJavascriptBridge方法中进行初始化bridge。调用init方法执行dispatchMessageFromNative(receivedMessages[i])，
    调用registerHandler方法进行注册指定方法，名称handlerName为functionInJs，并将responseData通过responseCallback返回。
7.  _dispatchMessageFromNative(receivedMessages[i]里没有responseId，执行else逻辑，由于H5通过responseCallback携带参数回调，所以执行
    _doSend('response',{ responseId: callbackResponseId, responseData: responseData });
8.  _doSend方法中主要是将返回的数据进行处理，然后存入到sendMessageQueue的队列中。
9.  通过 bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;通过自定义的scheme协议触发原生shouldOverrideUrlLoading方法，
    通知原生去_fetchQueue中取数据。
10. 原生在shouldOverrideUrlLoading方法，执行flushMessageQueue，通过自定义loadUrl(url,callback)执行WebViewJavascriptBridge.js中_fetchQueue方法来获取
    sendMessageQueue中的数据，同时将请求的方法_fetchQueue和callback作为一条数据存储到原生的responseCallbacks里。
11. WebViewJavascriptBridge.js执行_fetchQueue方法，从sendMessageQueue取出数据，清空队列，然后通过bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME + 
    '://return/_fetchQueue/' + encodeURIComponent(messageQueueString); 再次触发shouldOverrideUrlLoading方法。
12. 原生收到请求之后，调用handlerReturnData方法处理返回数据，同时从本地responseCallbacks中拿到callback，执行回调，会进入loadUrl(url,callback)的回调方法中。
13. 对数据进行处理和判断，具体情况看注释。原生请求H5最后会执行 if (!TextUtils.isEmpty(responseId)) {｝ 里面的逻辑。同时通过function.onCallBack(responseData)
    方法最后在MainActivity里进行回调，H5将返回的数据传到MainActivity进行使用。至此，原生调用H5就结束了。

### 调用流程  2.H5调用原生

1.  原生通过registerHandler注册一个handlerName为submitFromWeb的方法，传入回调，等待跟H5桥通了之后返回数据。
2.  demo.html中点击调用Native方法，调用callHandler方法，传入handlerName为submitFromWeb，参数为{'pluginname' : 'hello','funname' : 'sayHello'}，
    回调为responseCallback， 等待原生返回数据。
3.  WebViewJavascriptBridge.js执行callHandler方法，接着执行_doSend方法，由于是有responseCallback的，所以会生成一个callbackId，将callback塞进H5传过来的数据中，
    将responseCallback传入responseCallbacks中，其中key为callbackId。将数据存入sendMessageQueue队列中，然后bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME
    + '://' + QUEUE_HAS_MESSAGE;通知原生去_fetchQueue中取数据。
4.  原生接收到请求后，跟之前交互一样，通过自定义loadUrl(url,callback)执行WebViewJavascriptBridge.js中_fetchQueue方法来获取sendMessageQueue中的数据。同时将请求
    的方法_fetchQueue和callback作为一条数据存储到原生的responseCallbacks里。     
5.  WebViewJavascriptBridge.js执行_fetchQueue方法，从sendMessageQueue取出数据，清空队列，然后通过bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME +
    '://return/_fetchQueue/' + encodeURIComponent(messageQueueString); 再次触发shouldOverrideUrlLoading方法。 
6.  原生收到请求之后，调用handlerReturnData方法处理返回数据，同时从本地responseCallbacks中拿到callback，执行回调，会进入loadUrl(url,callback)的回调方法中。    
7.  因为有callbackId，所以会执行有callbackId的方法，然后继续向下执行，最后执行handler.handler(message.data, responseFunction)回调MainActivity中的回调方法。
8.  原生接收到回调之后，进行反射处理，然后通过callback.onCallBack(object)，执行flushMessageQueue有callbackId的回调的逻辑，调用queueMessage方法，然后调用
    dispatchMessage分发数据，然后执行javascript:WebViewJavascriptBridge._handleMessageFromNative(data)方法。
9.  执行_dispatchMessageFromNative方法中有responseId的逻辑，直接去responseCallbacks队列中取之前保存的回调，直接将数据返回给H5，同时移除之前保存的responseCallback，
    通过function(responseData) {｝ 方法受到回调的数据，将数据显示出来。至此，H5调用原生就结束了。

### 调用流程  3.单个调用-> H5调用原生或原生调用H5 项目用的不多简单说说

1.  单个调用也是差不多，H5调用原生将数据存到_fetchQueue中，通知原生去取，然后通过回调回传。
    原生调用的话将数据存入到receivedMessages中，等到H5初始化开始调用数据，然后继续通过来回通知调用，通过回调交互。

2.  本质上的交互就是通过shouldOverrideUrlLoading拦截，通过loadUrl回传。
    
