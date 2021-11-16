package com.example.jsbridgedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.jsbridgedemo.jsbridge.BridgeWebView;
import com.example.jsbridgedemo.jsbridge.MainJavascriptInterface;
import com.example.jsbridgedemo.jsbridge.OnBridgeCallback;
import com.google.gson.Gson;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
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
        webView = findViewById(R.id.webView);

        button = findViewById(R.id.button);

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
        webView.setGson(new Gson());
        MainJavascriptInterface  mainJavascriptInterface = new MainJavascriptInterface(webView.getResponseCallbacks(), webView,MainActivity.this);
        webView.addJavascriptInterface(mainJavascriptInterface, "android");
        webView.loadUrl("file:///android_asset/demo.html");
        /*原生注册submitFromWeb方法供JS调用，然后通过回调将数据返回给JS*/
        webView.registerHandler("submitFromWeb", (data, function) -> {
            Log.d("chromium", "handler = submitFromWeb, data from web = " + data);
            /*
             *  1.此处可以封装，然后使用反射来调用，但是高版本禁止反射，还需要考虑一下。
             * 2.建议通过一个公共的方法进行传递，然后通过反射的方法来对数据进行处理，回调。
             * 3.JS调用原生的情况，原生通过submitFromWeb方法，携带json类型的参数data，解析json获取对应的信息和数据，反射执行。
             * 4.最后通过全局的callback将数据通过json的方式返回给JS。
             * 5.使用Json的优势就是数据可塑造性比较强。也不用封装那么多方法，进行维护。这样通过反射的方式可以用类的方式去维护.
             *
             * */

            try {
                //绕过高版本进制反射

                DemoPluginBean demoPluginBean = new Gson().fromJson(String.valueOf(data), DemoPluginBean.class);
                // 使用反射获取要解析的类
                String funname = demoPluginBean.getFunname();
                Log.d("chromium", "funname = " + funname);
                Class<?> cls = Class.forName(getPackageName() + "."+demoPluginBean.getPluginname());
                Method getter = cls.getDeclaredMethod(demoPluginBean.getFunname(), String.class, MainActivity.class);
                getter.setAccessible(true);
                Object object=getter.invoke(cls.newInstance(), demoPluginBean.getParams(), this);
                callback = function;
                callback.onCallBack(object);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });

        User user = new User();
        Location location = new Location();
        location.address = "SDU";
        user.location = location;
        user.name = "大头鬼";
        Log.d("chromium", "原生调用callHandler发送functionInJs,data：" + new Gson().toJson(user));
        /*JS注册functionInJs方法供原生调用，原生调用成功之后，通过回调获取data数据。*/
        webView.callHandler("functionInJs", new Gson().toJson(user), data -> Log.d("chromium", "原生接收的回调，onCallBack: " + data));
        Log.d("chromium", "原生调用sendToWeb发送 hello");
        /*调用JS，通过桥给js发送数据*/
        webView.sendToWeb(null, "hello");


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