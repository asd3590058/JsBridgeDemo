package com.example.jsbridgedemo.jsbridge;

import android.content.Context;
import android.util.Log;

import com.tencent.smtt.sdk.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BridgeUtil {
    public final static String YY_OVERRIDE_SCHEMA = "yy://";
    /**
     * 格式为   yy://return/{function}/returncontent
     */
    public final static String YY_RETURN_DATA = YY_OVERRIDE_SCHEMA + "return/";
    private final static String YY_FETCH_QUEUE = YY_RETURN_DATA + "_fetchQueue/";
    private final static String EMPTY_STR = "";
    public final static String UNDERLINE_STR = "_";
    public final static String SPLIT_MARK = "/";
    public final static String CALLBACK_ID_FORMAT = "JAVA_CB_%s";
    public final static String JS_FETCH_QUEUE_FROM_JAVA =
            "javascript:WebViewJavascriptBridge._fetchQueue();";
    public static final String JAVA_SCRIPT = "WebViewJavascriptBridge.js";
    public final static String JS_HANDLE_MESSAGE_FROM_JAVA = "javascript:WebViewJavascriptBridge._handleMessageFromNative('%s');";

    /**
     * 例子 javascript:WebViewJavascriptBridge._fetchQueue(); --> _fetchQueue
     *
     * @param jsUrl url
     * @return 返回字符串，注意获取的时候判断空
     */
    public static String parseFunctionName(String jsUrl) {
        return jsUrl.replace("javascript:WebViewJavascriptBridge.", "").replaceAll("\\(.*\\);", "");
    }

    /**
     * js 文件将注入为第一个script引用
     *
     * @param view WebView
     * @param url  url
     */
    public static void webViewLoadJs(WebView view, String url) {
        String js = "var newscript = document.createElement(\"script\");";
        js += "newscript.src=\"" + url + "\";";
        js += "document.scripts[0].parentNode.insertBefore(newscript,document.scripts[0]);";
        view.loadUrl("javascript:" + js);
    }


    public static String getDataFromReturnUrl(String url) {
        if (url.startsWith(YY_FETCH_QUEUE)) {
            Log.d("chromium data", "处理_fetchQueue:" + url);
            return url.replace(YY_FETCH_QUEUE, EMPTY_STR);
        }

        String temp = url.replace(YY_RETURN_DATA, EMPTY_STR);
        String[] functionAndData = temp.split(SPLIT_MARK);

        if (functionAndData.length >= 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < functionAndData.length; i++) {
                sb.append(functionAndData[i]);
            }
            return sb.toString();
        }
        return null;
    }

    public static String getFunctionFromReturnUrl(String url) {
        String temp = url.replace(YY_RETURN_DATA, EMPTY_STR);
        String[] functionAndData = temp.split(SPLIT_MARK);
        if (functionAndData.length >= 1) {
            return functionAndData[0];
        }
        return null;
    }

    /**
     * 这里只是加载lib包中assets中的 WebViewJavascriptBridge.js
     *
     * @param view webview
     * @param path 路径
     */
    public static void webViewLoadLocalJs(WebView view, String path) {
        String jsContent = assetFile2Str(view.getContext(), path);
        view.loadUrl("javascript:" + jsContent);
    }


    /**
     * 解析assets文件夹里面的代码,去除注释,取可执行的代码
     *
     * @param c      context
     * @param urlStr 路径
     * @return 可执行代码
     */
    public static String assetFile2Str(Context c, String urlStr) {
        InputStream in = null;
        try {
            in = c.getAssets().open(urlStr);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line;
            StringBuilder sb = new StringBuilder();
            do {
                line = bufferedReader.readLine();
                // 去除注释
                if (line != null && !line.matches("^\\s*\\/\\/.*")) {
                    sb.append(line);
                }
            } while (line != null);

            bufferedReader.close();
            in.close();

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
