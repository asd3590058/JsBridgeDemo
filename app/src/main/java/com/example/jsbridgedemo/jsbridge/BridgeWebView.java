package com.example.jsbridgedemo.jsbridge;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.example.jsbridgedemo.BuildConfig;
import com.google.gson.Gson;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 武泓吉
 * @date 2021/11/5 15:31
 * @description X5webView
 */
public class BridgeWebView extends WebView implements BridgeWebViewClient.OnLoadJsListener, WebViewJavascriptBridge {

    private Gson mGson;
    /**
     * 存储请求消息
     */
    private List<WebJsMessage> startupMessage = new ArrayList<>();
    /**
     * 管理返回数据回调
     */
    private final Map<String, OnBridgeCallback> responseCallbacks = new HashMap<>();
    /**
     * 管理注册数据请求
     */
    private final Map<String, BridgeHandler> messageHandlers = new HashMap<>();
    /**
     * 创建双端符合的callbackId和responseId
     */
    private long uniqueId = 0;

    private Object data;


    @SuppressLint("SetJavaScriptEnabled")
    public BridgeWebView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        BridgeWebViewClient mClient = new BridgeWebViewClient(this, this, getContext());
        this.setWebViewClient(mClient);
        initWebViewSettings();
        this.getView().setClickable(true);
    }

    public BridgeWebView(Context arg0) {
        super(arg0);
        setBackgroundColor(85621);
    }

    public Map<String, OnBridgeCallback> getResponseCallbacks() {
        return responseCallbacks;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewSettings() {
        WebSettings webSettings = getSettings();
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setTextZoom(100);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setLoadsImagesAutomatically(true);
        if (BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true);
        }
    }

    public void setGson(Gson gson) {
        mGson = gson;
    }

    /**
     * 获取消息list集合
     *
     * @return 集合
     */
    public List<WebJsMessage> getStartupMessage() {
        return startupMessage;
    }

    /**
     * 设置消息，注意目前在onProgressChanged方法中调用
     */
    public void setStartupMessage(List<WebJsMessage> startupMessage) {
        this.startupMessage = startupMessage;
    }

    public void handlerReturnData(String url) {
        String functionName = BridgeUtil.getFunctionFromReturnUrl(url);
        OnBridgeCallback callback = responseCallbacks.get(functionName);
        String data = BridgeUtil.getDataFromReturnUrl(url);
        if (callback != null) {
            callback.onCallBack(data);
            responseCallbacks.remove(functionName);
        }
    }

    @Override
    public void onLoadStart() {
    }

    @Override
    public void onLoadFinished() {
        if (getStartupMessage() != null) {
            for (WebJsMessage m : getStartupMessage()) {
                //分发message 必须在主线程才分发成功
                dispatchMessage(m);
            }
            setStartupMessage(null);
        }
    }

    /**
     * register handler,so that javascript can call it
     * 注册处理程序,以便javascript调用它
     *
     * @param handlerName handlerName
     * @param handler     BridgeHandler
     */
    public void registerHandler(String handlerName, BridgeHandler handler) {
        if (handler != null) {
            // 添加至 Map<String, BridgeHandler>
            messageHandlers.put(handlerName, handler);
        }
    }

    /**
     * call javascript registered handler
     *
     * @param handlerName
     * @param data
     * @param callBack
     */
    public void callHandler(String handlerName, String data, OnBridgeCallback callBack) {
        doSend(handlerName, data, callBack);
    }

    /**
     * unregister handler
     *
     * @param handlerName
     */
    public void unregisterHandler(String handlerName) {
        if (handlerName != null) {
            messageHandlers.remove(handlerName);
        }
    }

    @Override
    public void sendToWeb(String handlerName, Object data) {
        sendToWeb(handlerName, data, null);
    }

    @Override
    public void sendToWeb(String handlerName, Object data, OnBridgeCallback responseCallback) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            doSend(handlerName, data, responseCallback);
        } else {
            if (getHandler() == null) {
                return;
            }
            getHandler().post(() -> doSend(handlerName, data, responseCallback));
        }
    }

    /**
     * 保存message到消息队列
     *
     * @param handlerName      handlerName
     * @param data             data
     * @param responseCallback OnBridgeCallback
     */
    private void doSend(String handlerName, Object data, OnBridgeCallback responseCallback) {
        if (!(data instanceof String) && mGson == null) {
            return;
        }
        WebJsMessage request = new WebJsMessage();
        if (data != null) {
            request.data = (data instanceof String ? (String) data : mGson.toJson(data));
        }
        if (responseCallback != null) {
            String callbackId = String.format(BridgeUtil.CALLBACK_ID_FORMAT, (++uniqueId) + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
            responseCallbacks.put(callbackId, responseCallback);
            request.callbackId = callbackId;
        }
        if (!TextUtils.isEmpty(handlerName)) {
            request.handlerName = handlerName;
        }
        queueMessage(request);
    }

    /**
     * list<message> != null 添加到消息集合否则分发消息
     *
     * @param message Message
     */
    private void queueMessage(WebJsMessage message) {
        if (startupMessage != null) {
            startupMessage.add(message);
        } else {
            dispatchMessage(message);
        }
    }

    /**
     * 分发message 必须在主线程才分发成功
     *
     * @param message Message
     */
    public void dispatchMessage(WebJsMessage message) {
        if (mGson == null) {
            return;
        }
        String messageJson = BridgeJsonHelper.messageToJsonObject(message).toString();
        try {
            //escape special characters for json string  为json字符串转义特殊字符
            messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
            messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
            messageJson = messageJson.replaceAll("(?<=[^\\\\])(\')", "\\\\\'");
            messageJson = messageJson.replaceAll("%7B", URLEncoder.encode("%7B", "UTF-8"));
            messageJson = messageJson.replaceAll("%7D", URLEncoder.encode("%7D", "UTF-8"));
            messageJson = messageJson.replaceAll("%22", URLEncoder.encode("%22", "UTF-8"));
            messageJson = messageJson.replaceAll("%", URLEncoder.encode("%", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        if (javascriptCommand.length() >= 2097152) {
            Log.d("chromium", "回传的数据成功+evaluateJavascript:" + javascriptCommand);
            this.evaluateJavascript(javascriptCommand, null);
        } else {
            Log.d("chromium", "回传的数据成功+loadUrl：" + javascriptCommand);
            this.loadUrl(javascriptCommand);
        }
    }


    public Object getData() {
        return data;
    }

    /**
     * 刷新消息队列
     */
    public void flushMessageQueue() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, data -> {
                // deserializeMessage 反序列化消息
                List<WebJsMessage> list;
                try {
                    list = BridgeJsonHelper.toArrayList(data);
                    Log.d("chromium", "原生发送flushMessageQueue：list为："+list.toString());

                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                if (list.isEmpty()) {
                    return;
                }
                for (int i = 0; i < list.size(); i++) {
                    WebJsMessage message = list.get(i);
                    String responseId = message.responseId;
                    // 是否是response  CallBackFunction
                    if (!TextUtils.isEmpty(responseId)) {
                        OnBridgeCallback function = responseCallbacks.get(responseId);
                        Object responseData = message.responseData;
                        function.onCallBack(responseData);
                        responseCallbacks.remove(responseId);
                    } else {
                        OnBridgeCallback responseFunction;
                        // if had callbackId 如果有回调Id
                        final String callbackId = message.callbackId;
                        if (!TextUtils.isEmpty(callbackId)) {
                            responseFunction = data1 -> {
                                Log.d("chromium", "flushMessageQueue：" + data);
                                WebJsMessage responseMsg = new WebJsMessage();
                                responseMsg.responseId = callbackId;
                                responseMsg.responseData = data1;
                                queueMessage(responseMsg);
                            };
                        } else {
                            responseFunction = data2 -> {
                                // do nothing
                            };
                        }
                        // BridgeHandler执行
                        BridgeHandler handler;
                        if (!TextUtils.isEmpty(message.handlerName)) {
                            handler = messageHandlers.get(message.handlerName);
                        } else {
                            handler = new DefaultHandler();
                        }
                        if (handler != null) {
                            handler.handler(message.data, responseFunction);

                        }
                    }
                }
            });
        }
    }

    public void loadUrl(String jsUrl, OnBridgeCallback returnCallback) {
        this.loadUrl(jsUrl);
        // 添加至 Map<String, CallBackFunction>
        responseCallbacks.put(BridgeUtil.parseFunctionName(jsUrl), returnCallback);
    }

    public static abstract class BaseJavascriptInterface {
        private Map<String, OnBridgeCallback> mCallbacks;

        public BaseJavascriptInterface(Map<String, OnBridgeCallback> callbacks) {
            mCallbacks = callbacks;
        }

        /**
         * web -> Native 从web层直接调用Native方法
         *
         * @param data       传递的数据
         * @param callbackId web需要回调的responseId
         * @return 直接带返回值的方法
         */
        @JavascriptInterface
        public void send(String data, String callbackId) {
            Log.d("chromiumSend",
                    data + ", callbackId: " + callbackId + " " + Thread.currentThread().getName());
              sendImpl(data, callbackId);
        }

        @JavascriptInterface
        public void response(String data, String responseId) {
            Log.d("chromium", data + ", responseId: " + responseId + " " + Thread.currentThread().getName());
            if (!TextUtils.isEmpty(responseId)) {
                OnBridgeCallback function = mCallbacks.remove(responseId);
                if (function != null) {
                    function.onCallBack(data);
                }
            }
        }

        /**
         * web - > Native 从web过来的数据分发到具体的实现类
         *
         * @param data          web传递过来的数据
         * @param webResponseId web 需要回调的id
         * @date 2021/11/9 11:48
         * @author 武泓吉
         */
        public abstract void sendImpl(String data, String webResponseId);
    }

}
