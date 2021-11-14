package com.example.jsbridgedemo.jsbridge;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BridgeJsonHelper {

    private final static String CALLBACK_ID_STR = "callbackId";
    private final static String RESPONSE_ID_STR = "responseId";
    private final static String RESPONSE_DATA_STR = "responseData";
    private final static String DATA_STR = "data";
    private final static String HANDLER_NAME_STR = "handlerName";

    public static JSONObject messageToJsonObject(WebJsMessage message) {
        JSONObject jo = new JSONObject();
        try {
            if (message.callbackId != null) {
                jo.put(CALLBACK_ID_STR, message.callbackId);
            }
            if (message.data != null) {
                jo.put(DATA_STR, message.data);
            }
            if (message.handlerName != null) {
                jo.put(HANDLER_NAME_STR, message.handlerName);
            }
            if (message.responseId != null) {
                jo.put(RESPONSE_ID_STR, message.responseId);
            }
            if (message.responseData != null) {
                jo.put(RESPONSE_DATA_STR, message.responseData);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }

    public static WebJsMessage JsonObjectToMessage(JSONObject jo) {
        WebJsMessage message = new WebJsMessage();
        try {
            if (jo.has(CALLBACK_ID_STR)) {
                message.callbackId = jo.getString(CALLBACK_ID_STR);
            }
            if (jo.has(DATA_STR)) {
                message.data = jo.get(DATA_STR);
            }
            if (jo.has(HANDLER_NAME_STR)) {
                message.handlerName = jo.getString(HANDLER_NAME_STR);
            }
            if (jo.has(RESPONSE_ID_STR)) {
                message.responseId = jo.getString(RESPONSE_ID_STR);
            }
            if (jo.has(RESPONSE_DATA_STR)) {
                message.responseData = jo.get(RESPONSE_DATA_STR);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return message;
    }


    public static WebJsMessage toObject(String jsonStr) {
        WebJsMessage m = new WebJsMessage();
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            m.handlerName = (jsonObject.has(HANDLER_NAME_STR) ? jsonObject.getString(HANDLER_NAME_STR) : null);
            m.callbackId = (jsonObject.has(CALLBACK_ID_STR) ? jsonObject.getString(CALLBACK_ID_STR) : null);
            m.responseData = (jsonObject.has(RESPONSE_DATA_STR) ? jsonObject.getString(RESPONSE_DATA_STR) : null);
            m.responseId = (jsonObject.has(RESPONSE_ID_STR) ? jsonObject.getString(RESPONSE_ID_STR) : null);
            m.data = (jsonObject.has(DATA_STR) ? jsonObject.getString(DATA_STR) : null);
            return m;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return m;
    }

    public static List<WebJsMessage> toArrayList(Object jsonStr) {
        List<WebJsMessage> list = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonStr.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                WebJsMessage m = new WebJsMessage();
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                m.handlerName = (jsonObject.has(HANDLER_NAME_STR) ? jsonObject.getString(HANDLER_NAME_STR) : null);
                m.callbackId = (jsonObject.has(CALLBACK_ID_STR) ? jsonObject.getString(CALLBACK_ID_STR) : null);
                m.responseData = (jsonObject.has(RESPONSE_DATA_STR) ? jsonObject.getString(RESPONSE_DATA_STR) : null);
                m.responseId = (jsonObject.has(RESPONSE_ID_STR) ? jsonObject.getString(RESPONSE_ID_STR) : null);
                m.data = (jsonObject.has(DATA_STR) ? jsonObject.getString(DATA_STR) : null);
                list.add(m);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

}
