package com.xposed.miuianesthetist;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.ArrayMap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findConstructorExact;

public class DisableSecurityCheck implements IXposedHookLoadPackage {

    private static Boolean iib = null;
    private static int count = 0;


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)  {
        if (lpparam.packageName.equals("android")) {

            /* Disable integrity check when boot in services.jar */
            try {
                Class sms = XposedHelpers.findClass("com.miui.server.SecurityManagerService"
                        , lpparam.classLoader);
                findAndHookConstructor(sms, Context.class, boolean.class/*onlyCore*/, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // Set the instance of SecurityManagerService class to be onlyCore mode, then checkSystemSelfProtection() method will do nothing
                        param.args[1] = true;
                        // Set system apps' states to be not cracked
                        XposedHelpers.findField(sms, "mSysAppCracked").setInt(param.thisObject, 0);
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log("miuianesthetist err:");
                XposedBridge.log(t);
            }


            /* Remove the limit for disabling system apps in services.jar */
            try {
                findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector"
                        , lpparam.classLoader, "isAllowedDisable",
                        String.class, int.class, XC_MethodReplacement.returnConstant(true));
            } catch (Throwable t) {
                XposedBridge.log("miuianesthetist err:");
                XposedBridge.log(t);
            }

            // check if MIUI is global version
            if (null == iib) {
                try {
                    iib = XposedHelpers.findField(XposedHelpers.findClass(
                            "miui.os.Build", lpparam.classLoader),
                            "IS_INTERNATIONAL_BUILD").getBoolean(null);
                } catch (Throwable t) {
                    XposedBridge.log("miuianesthetist err:");
                    XposedBridge.log(t);
                }
            }

            /* Return an intent before it get proceeded to avoid being hijacked in services.jar */
            antiHijack:
            {
                if (iib) {
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
                                        //返回第一个参数pms的mResolveInfo属性
                                        return XposedHelpers.findField(pmsClazz, "mResolveInfo").get(param.args[0]);
                                    }
                                    return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                                }
                            });
                } catch (Throwable t) {
                    XposedBridge.log("miuianesthetist err:");
                    XposedBridge.log(t);
                }
            }


            

            /* Dangerous ops: force set a app to be not installed forever, it modify "pkg" element's inst="false" in /data/system/users/0/package-restrictions.xml */
            /*try {
                Class<?> aouClazz = XposedHelpers.findClass("android.miui.AppOpsUtils", lpparam.classLoader);
                Object isXOptMode = XposedHelpers.findMethodExact(aouClazz, "isXOptMode").invoke(null);
                XposedBridge.log("miuianesthetist debug: isOptMode " + (boolean) isXOptMode);
                Class<?> psClazz = XposedHelpers.findClass("com.android.server.pm.PackageSetting", lpparam.classLoader);
                Class<?> pmsClazz = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpparam.classLoader);
                Class<?> sClazz = XposedHelpers.findClass("com.android.server.pm.Settings", lpparam.classLoader);

                findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector"
                        , lpparam.classLoader, "checkPackageInstallerStatus", pmsClazz, sClazz, new XC_MethodHook() {

                            @TargetApi(Build.VERSION_CODES.KITKAT)
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                try {
                                    XposedBridge.log("miuianesthetist debug: checkPackageInstallerStatus after hook");
                                    ArrayMap mPackages = (ArrayMap) XposedHelpers.findField(sClazz, "mPackages").get(param.args[1]);
                                    Object miuiInstaller = psClazz.cast(mPackages.get("com.xiaomi.market"));
                                    XposedHelpers.findMethodBestMatch(psClazz, "setInstalled", boolean.class, int.class).invoke(miuiInstaller, false, 0);
                                } catch (Throwable t) {
                                    XposedBridge.log("miuianesthetist err:");
                                    XposedBridge.log(t);
                                }

                            }
                        });
            } catch (Throwable t) {
                XposedBridge.log("miuianesthetist err:");
                XposedBridge.log(t);
            }*/

        }


        //SecurityCenter.apk
        if (lpparam.packageName.equals("com.miui.securitycenter")) {
            /* Allow user disable system apps WLan network access permission */
            try {
                Class<?> ada = XposedHelpers.findClass("com.miui.appmanager.ApplicationsDetailsActivity", lpparam.classLoader);
                Method[] declaredMethods = ada.getDeclaredMethods();
                for (Method m : declaredMethods) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param)  {
                            try {
                                if(param.thisObject!=null)
                                XposedHelpers.setBooleanField(param.thisObject,"mIsSystem",false);
                            } catch (Throwable t) {
                                XposedBridge.log("miuianesthetist err:");
                                XposedBridge.log(t);
                            }
                        }
                    });
                }

            } catch (Throwable t) {
                XposedBridge.log("miuianesthetist err:");
                XposedBridge.log(t);
            }

            /* Allow user set third-party launcher to be default */
            try {
                XposedHelpers.findAndHookMethod("com.miui.securitycenter.provider.ThirdDesktopProvider", lpparam.classLoader,"call",String.class,String.class,Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param)  {
                        try {
                            Bundle result = (Bundle)param.getResult();
                            //mode=0 can't use unauthorized third-party launcher; mode=1 can't use official launcher; other value no limit
                            result.putInt("mode",-1);
                            param.setResult(result);
                        } catch (Throwable t) {
                            XposedBridge.log("miuianesthetist err:");
                            XposedBridge.log(t);
                        }
                    }
                });
            }catch (Throwable t) {
                XposedBridge.log("miuianesthetist err:");
                XposedBridge.log(t);
            }


        }


    }
}
