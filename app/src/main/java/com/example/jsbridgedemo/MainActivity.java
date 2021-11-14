package com.example.jsbridgedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.jsbridgedemo.jsbridge.BridgeHandler;
import com.example.jsbridgedemo.jsbridge.BridgeWebView;
import com.example.jsbridgedemo.jsbridge.OnBridgeCallback;
import com.google.gson.Gson;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;

import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "MainActivity";

    BridgeWebView webView;

    Button button;

    int RESULT_CODE = 0;

    ValueCallback<Uri> mUploadMessage;
    OnBridgeCallback callback;

    static class Location {
        String address;
    }


    static class User {
        String name;
        Location location;
        String testStr;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = (BridgeWebView) findViewById(R.id.webView);

        button = (Button) findViewById(R.id.button);

        button.setOnClickListener(this);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String captureType) {
                mUploadMessage = valueCallback;
                pickFile();
            }
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> valueCallback, FileChooserParams fileChooserParams) {
                pickFile();
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/demo.html");
        /*原生注册submitFromWeb方法供JS调用，然后通过回调将数据返回给JS*/
        webView.registerHandler("submitFromWeb", (data, function) -> {
            Log.i("chromium", "handler = submitFromWeb, data from web = " + data);
            /*
             *  此处可以封装，然后使用反射来调用，但是高版本禁止反射，还需要考虑一下。
             *  如果使用反射 可以用下面的例子
             *
             *
             *                 try {
             *                     PluginBean pluginBean =new Gson().fromJson(String.valueOf(data),PluginBean.class);
             *                     new Gson().
             *                     // 使用反射获取要解析的类
             *                     Class<?> cls=Class.forName(getPackageName()+ pluginBean.getPluginname());
             *                     Method getter = cls.getDeclaredMethod(pluginBean.getFunname(), String.class, MainActivity.class);
             *                     getter.setAccessible(true);
             *                     getter.invoke(cls.newInstance(), pluginBean.getParams(), this);
             *                     callback =  function;
             *
             *                 } catch (ClassNotFoundException  e) {
             *                     e.printStackTrace();
             *                 }
             *
             *  将function赋值之后，在处理数据完毕之后，可以调用callback.onCallBack(data)来返回给js。
             * */
            function.onCallBack("submitFromWeb exe, response data 中文 from Java");
        });

        User user = new User();
        Location location = new Location();
        location.address = "SDU";
        user.location = location;
        user.name = "大头鬼";
		Log.d("chromium", "原生发送functionInJs,data："+ new Gson().toJson(user));

        /*JS注册functionInJs方法供原生调用，原生调用成功之后，通过回调获取data数据。*/
        webView.callHandler("functionInJs", new Gson().toJson(user), data -> Log.d("chromium", "原生接收的回调，onCallBack: " + data));

        /*调用JS，通过桥给js发送数据*/
        webView.sendToWeb(null,"hello");


    }
    public void pickFile() {
        Intent chooserIntent = new Intent(Intent.ACTION_GET_CONTENT);
        chooserIntent.setType("image/*");
        startActivityForResult(chooserIntent, RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RESULT_CODE) {
            if (null == mUploadMessage) {
                return;
            }
            Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }
    }

    @Override
    public void onClick(View v) {

    }

}