package com.example.jsbridgedemo;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import com.tencent.smtt.export.external.TbsCoreSettings;
import com.tencent.smtt.sdk.QbSdk;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.util.HashMap;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核。
        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {
            @Override
            public void onViewInitFinished(boolean arg0) {
                // x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                Log.d("TAG", "onViewInitFinished is "+arg0);
            }

            @Override
            public void onCoreInitFinished() {
            }
        };
        // x5内核初始化接口
        QbSdk.initX5Environment(this, cb);

        HashMap<String, Object> map = new HashMap<>(4);
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
        QbSdk.initTbsSettings(map);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("");
        }
    }
}
