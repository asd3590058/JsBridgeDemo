# JsBridge原理 基于X5WebView
### 调用流程  1.原生调用H5

1.  `MainActivity`中通过`webView`调用`callHandler`方法，调用H5的`functionInJs`方法，最终调用`queueMessage`，将数据存放队列`startupMessage`中。
2.  页面加载结束之后回调`onPageFinish`方法加载`WebViewJavascriptBridge.js`文件，加载完毕回调`OnLoadJsListener`的`onLoadFinished`方法。其中
    实现是在BridgeWebView 中然后轮询`startupMessage`，将之前存储在`queueMessage`数据取出来，调用`dispatchMessage`方法通过`loadUrl`加载
    `WebViewJavascriptBridge._handleMessageFromNative`的方法处理数据。
3.  先注入`WebViewJavascriptBridge.js`文件，然后进行初始化，创建`WebViewJavascriptBridge`对象挂载在window上，初始化一些其他事情可以看下
    注释。
4.  调用`_handleMessageFromNative`方法，将进行数据存储在接受消息的队列中，然后调用`_dispatchMessageFromNative`方法。
5.  `_dispatchMessageFromNative`方法中由于是原生发起调用没有`responseId`，只有`callbackId`，所以走else逻辑，由于没有`responseData`，所以不
    执行_deSend方法,而却查找指定handler执行，但是没有指定的handler，所以抛异常。此时java发起调用，经过jsBridge桥，进入到js文件。
6.  `demo.html`中进行初始化`connectWebViewJavascriptBridge`方法中进行初始化bridge。调用init方法执行`dispatchMessageFromNative(receivedMessages[i])`，
    调用`registerHandler`方法进行注册指定方法，名称`handlerName`为`functionInJs`，并将`responseData`通过`responseCallback`返回。
7.  `_dispatchMessageFromNative(receivedMessages[i]`里没有`responseId`，执行else逻辑，由于H5通过`responseCallback`携带参数回调，所以执行
    `_doSend('response',{ responseId: callbackResponseId, responseData: responseData });`
8.  `_doSend`方法中主要是将返回的数据进行处理，然后存入到`sendMessageQueue`的队列中。
9.  通过`bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;`通过自定义的scheme协议触发原生`shouldOverrideUrlLoading`方法，
    通知原生去`_fetchQueue`中取数据。
10. 原生在`shouldOverrideUrlLoading`方法，执行`flushMessageQueue`，通过自定义`loadUrl(url,callback)`执行`WebViewJavascriptBridge.js`中`_fetchQueue`方法来获取
    `sendMessageQueue`中的数据，同时将请求的方法`_fetchQueue`和`callback`作为一条数据存储到原生的`responseCallbacks`里。
11. `WebViewJavascriptBridge.js`执行`_fetchQueue`方法，从`sendMessageQueue`取出数据，清空队列，然后通过`bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME + 
    '://return/_fetchQueue/' + encodeURIComponent(messageQueueString);` 再次触发`shouldOverrideUrlLoading`方法。
12. 原生收到请求之后，调用`handlerReturnData`方法处理返回数据，同时从本地`responseCallbacks`中拿到callback，执行回调，会进入`loadUrl(url,callback)`的回调方法中。
13. 对数据进行处理和判断，具体情况看注释。原生请求H5最后会执行 `if (!TextUtils.isEmpty(responseId)) {｝` 里面的逻辑。同时通过`function.onCallBack(responseData)`
    方法最后在`MainActivity`里进行回调，H5将返回的数据传到`MainActivity`进行使用。至此，原生调用H5就结束了。

### 调用流程  2.H5调用原生

1.  原生通过`registerHandler`注册一个`handlerName`为`submitFromWeb`的方法，传入回调，等待跟H5桥通了之后返回数据。
2.  `demo.html`中点击调用原生方法，调用`callHandler`方法，传入`handlerName`为`submitFromWeb`，参数为`{'pluginname' : 'hello','funname' : 'sayHello'}`，
    回调为`responseCallback`， 等待原生返回数据。
3.  `WebViewJavascriptBridge.js`执行`callHandler`方法，接着执行`_doSend`方法，由于是有`responseCallback`的，所以会生成一个`callbackId`，将`callback`塞进H5传过来的数据中，
    将`responseCallback传入responseCallbacks`中，其中key为`callbackId`。将数据存入`sendMessageQueue`队列中，然后`bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME+ '://' + QUEUE_HAS_MESSAGE;`通知原生去`_fetchQueue`中取数据。
4.  原生接收到请求后，跟之前交互一样，通过自定义`loadUrl(url,callback)`执行`WebViewJavascriptBridge.js`中`_fetchQueue`方法来获取`sendMessageQueue`中的数据。同时将请求
    的方法`_fetchQueue`和`callback`作为一条数据存储到原生的`responseCallbacks`里。     
5.  `WebViewJavascriptBridge.js`执行`_fetchQueue`方法，从`sendMessageQueue`取出数据，清空队列，然后通过`bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME +
    '://return/_fetchQueue/' + encodeURIComponent(messageQueueString)`; 再次触发`shouldOverrideUrlLoading`方法。 
6.  原生收到请求之后，调用`handlerReturnData`方法处理返回数据，同时从本地`responseCallbacks`中拿到`callback`，执行回调，会进入`loadUrl(url,callback)`的回调方法中。    
7.  因为有`callbackId`，所以会执行有`callbackId`的方法，然后继续向下执行，最后执行`handler.handler(message.data, responseFunction)`回调MainActivity中的回调方法。
8.  原生接收到回调之后，进行反射处理，然后通过`callback.onCallBack(object)`，执行`flushMessageQueue`有`callbackId`的回调的逻辑，调用`queueMessage`方法，然后调用
    `dispatchMessage`分发数据，然后执行`javascript:WebViewJavascriptBridge._handleMessageFromNative(data)`方法。
9.  执行`_dispatchMessageFromNative`方法中有`responseId`的逻辑，直接去`responseCallbacks`队列中取之前保存的回调，直接将数据返回给H5，同时移除之前保存的`responseCallback`
    通过`function(responseData) {｝` 方法受到回调的数据，将数据显示出来。至此，H5调用原生就结束了。

### 调用流程  3.单个调用-> H5调用原生或原生调用H5 项目用的不多简单说说

1.  单个调用也是差不多，H5调用原生将数据存到`_fetchQueue`中，通知原生去取，然后通过回调回传。
    原生调用的话将数据存入到`receivedMessages`中，等到H5初始化开始调用数据，然后继续通过来回通知调用，通过回调交互。

2.  本质上的交互就是通过`shouldOverrideUrlLoading`拦截，通过`loadUrl`回传。
    
    
    
    参考的项目：
    
    1.https://github.com/lzyzsd/JsBridge


    2.https://github.com/yangchong211/YCWebView
