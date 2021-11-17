package com.example.jsbridgedemo.jsbridge;
import android.content.Context;
import java.util.Map;

/**
 * Created on 2019/7/10.
 *
 * @author: bigwang
 * Description:
 * change  武泓吉
 */
public class MainJavascriptInterface extends BridgeWebView.BaseJavascriptInterface {

    private BridgeWebView mWebView;
    private Context mContext;
    private String reponseId;

    public String getReponseId() {
        return reponseId;
    }

    public MainJavascriptInterface(Map<String, OnBridgeCallback> callbacks, BridgeWebView webView,
                                   Context context) {
        super(callbacks);
        mWebView = webView;
        mContext = context;
    }



}
