package com.xposed.miuianesthetist;

import android.content.Context;
import android.content.Intent;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;


public class DisableSecurityCheck implements IXposedHookLoadPackage {

    private static Boolean iib = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android")) {

            /* 禁用开机检查自身完整性 services.jar */
            try {
                Class sms = XposedHelpers.findClass("com.miui.server.SecurityManagerService"
                        , lpparam.classLoader);
                findAndHookConstructor(sms, Context.class, boolean.class/*onlyCore*/, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // 设置SecurityManagerService类为onlyCore模式，即不做checkSystemSelfProtection()检测
                        param.args[1] = true;
                        // 设置系统应用是否被破解的标识为未被破解
                        XposedHelpers.findField(sms, "mSysAppCracked").setInt(param.thisObject, 0);
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log("miuianesthetist err:");
                XposedBridge.log(t);
            }


            /* 禁用检查应用是否可以被禁用 services.jar */
            try {
                findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector"
                        , lpparam.classLoader, "isAllowedDisable",
                        String.class, int.class, XC_MethodReplacement.returnConstant(true));
            } catch (Throwable t) {
                XposedBridge.log("miuianesthetist err:");
                XposedBridge.log(t);
            }

            // 获取国际版标识
            if (null == iib) {
                iib = XposedHelpers.findField(XposedHelpers.findClass(
                        "miui.os.Build", lpparam.classLoader),
                        "IS_INTERNATIONAL_BUILD").getBoolean(null);
            }

            /* 在intent被MIUI处理之前直接原样返回，避免http/https协议被劫持到小米浏览器，
            同时避免market/http/https协议的应用商店链接被劫持 services.jar */
            antiHijack:
            {
                if (iib) {
                    //是国际版则无需处理
                    break antiHijack;
                }
                try {
                    Class<?> pmsClazz = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpparam.classLoader);
                    findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector"
                            , lpparam.classLoader, "checkMiuiIntent",
                            pmsClazz, Intent.class, String.class, int.class, List.class, int.class, new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                    Intent intent = (Intent) param.args[1];
                                    if (("market".equals(intent.getScheme()) && "android.intent.action.VIEW".equals(intent.getAction())) ||
                                            ("http".equals(intent.getScheme()) || "https".equals(intent.getScheme())) &&
                                                    "android.intent.action.VIEW".equals(intent.getAction())) {
                                        XposedBridge.log("miuianesthetist debug: checkMiuiIntent return original result");
                                        //返回第一个参数pms的mResolveInfo属性
                                        return XposedHelpers.findField(pmsClazz, "mResolveInfo").get(param.args[0]);
                                    }
                                    XposedBridge.log("miuianesthetist debug: checkMiuiIntent return injected result");
                                    return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                                }
                            });
                } catch (Throwable t) {
                    XposedBridge.log("miuianesthetist err:");
                    XposedBridge.log(t);
                }
            }

        }
    }
}
