/*
Copyright 2017 yangchong211（github.com/yangchong211）

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.example.jsbridgedemo.jsbridge;

/**
 * <pre>
 *     @author yangchong
 *     blog  : https://github.com/yangchong211
 *     time  : 2019/9/10
 *     desc  : 自定义消息Message实体类
 *     revise: demo地址：https://github.com/yangchong211/YCWebView
 * </pre>
 */
public class WebJsMessage {

    /**
     * callbackId
     * 回调id
     */
    String callbackId = null;
    /**
     * responseId
     * 响应id
     */
    String responseId = null;
    /**
     * responseData
     * 响应内容
     */
    Object responseData = null;
    /**
     * data of message
     * 消息内容
     */
    Object data = null;
    /**
     * name of handler
     * 消息名称
     */
    String handlerName = null;



}
