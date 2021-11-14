package com.example.jsbridgedemo;

/**
 * 创建者: wwd
 * 创建日期:2019-12-04
 * 类的功能描述:
 */
public class PluginBean {

  /**
   * pluginname : hello
   * funname : sayHello
   * params : jsonobject
   */

  private String pluginname;
  private String funname;

  public String getParams() {
    return params;
  }

  public void setParams(String params) {
    this.params = params;
  }

  private String params;

  public String getPluginname() {
    String first = pluginname.substring(0, 1);
    String after = pluginname.substring(1);
    first = first.toUpperCase();
    return first + after;
  }

  public void setPluginname(String pluginname) {
    this.pluginname = pluginname;
  }

  public String getFunname() {
    return funname;
  }

  public void setFunname(String funname) {
    this.funname = funname;
  }
}
