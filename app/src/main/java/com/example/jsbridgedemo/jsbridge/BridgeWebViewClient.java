package com.example.jsbridgedemo.jsbridge;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * 如果要自定义WebViewClient必须要集成此类
 * @author  by bruce on 10/28/15.
 */
public class BridgeWebViewClient extends WebViewClient {

    private final OnLoadJsListener mListener;
    private final Context mContext;
    private final BridgeWebView mWebView;

    public BridgeWebViewClient(BridgeWebView webView ,OnLoadJsListener listener, Context context) {
        this.mWebView = webView;
        this.mListener = listener;
        this.mContext = context;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Log.d("chromium data", "url:" + url);

        if (url.startsWith("tel:")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            mContext.startActivity(intent);
            return true;
        }
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.d("chromium data", "decode后的url:" + url);
        // 如果是返回数据
        if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) {
            mWebView.handlerReturnData(url);
            Log.d("chromium data", "返回数据:" + url);
            return true;
        } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) {
            mWebView.flushMessageQueue();
            Log.d("chromium data", "刷新队列:" + url);
            return true;
        }
        view.loadUrl(url);
        return true;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
        String url = request.getUrl().toString();
        Log.d("chromium data", "request:" + url);

        if (url.startsWith("tel:")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            mContext.startActivity(intent);
            return true;
        }
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.d("chromium data", "decode后的url:" + url);

        // 如果是返回数据
        if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) {
            mWebView.handlerReturnData(url);
            Log.d("chromium data", "返回数据:" + url);
            return true;
        } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) {
            Log.d("chromium data", "刷新队列:" + url);
            mWebView.flushMessageQueue();
            return true;
        }else {
            webView.loadUrl(url);
            return true;
        }
    }

    @Override
    public void onPageStarted(WebView webView, String s, Bitmap bitmap) {
        super.onPageStarted(webView, s, bitmap);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        mListener.onLoadStart();
        BridgeUtil.webViewLoadLocalJs(view, BridgeUtil.JAVA_SCRIPT);
        mListener.onLoadFinished();
    }

    public interface OnLoadJsListener {

        /**
         * 开始加载
        */
        void onLoadStart();
        /**
         * 加载结束
         */
        void onLoadFinished();
    }
}