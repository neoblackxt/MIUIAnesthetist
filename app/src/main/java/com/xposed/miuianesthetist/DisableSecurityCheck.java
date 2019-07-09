package com.xposed.miuianesthetist;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedBridge.invokeOriginalMethod;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;

public class DisableSecurityCheck implements IXposedHookLoadPackage {

    private static Boolean iib = null;
    private static volatile MenuItem menu2 = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals("android")) {

            // Disable integrity check when boot in services.jar
            try {
                Class sms = findClass("com.miui.server.SecurityManagerService"
                        , lpparam.classLoader);
                findAndHookConstructor(sms, Context.class, boolean.class/*onlyCore*/,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                // Set the instance of SecurityManagerService class to be onlyCore mode,
                                // then checkSystemSelfProtection() method will do nothing
                                param.args[1] = true;
                                // Set system apps' states to be not cracked
                                findField(sms, "mSysAppCracked").setInt(param.thisObject, 0);
                            }
                        });
            } catch (Throwable t) {
                log("miuianesthetist err:");
                log(t);
            }


            // Remove the limit for disabling system apps in services.jar
            try {
                findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector"
                        , lpparam.classLoader, "isAllowedDisable",
                        String.class, int.class, XC_MethodReplacement.returnConstant(true));
            } catch (Throwable t) {
                log("miuianesthetist err:");
                log(t);
            }

            // Prevent some ultra sticky system apps (MiuiDaemon, FindDevice, etc.) from running
            // after have been disabled in services.jar
            try {
                findAndHookMethod("com.android.server.am.ActivityManagerServiceInjector",
                        lpparam.classLoader, "shouldAddPersistApp", ApplicationInfo.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                ApplicationInfo info = (ApplicationInfo) param.args[0];
                                CharSequence GREEN_KID_AGENT_PKG_NAME = (CharSequence) findField(findClass(
                                        "com.miui.server.GreenGuardManagerService",
                                        lpparam.classLoader), "GREEN_KID_AGENT_PKG_NAME").get(null);
                                if (!TextUtils.equals(info.packageName, GREEN_KID_AGENT_PKG_NAME) &&
                                        info.enabled) {
                                    param.setResult(true);
                                    return;
                                }
                                String str = "AMSInjector"; //Shorten TAG ActivityManagerServiceInjector
                                String sb = "persist app : " +
                                        info.packageName +
                                        " should not add to start";
                                Log.d(str, sb);
                                param.setResult(false);
                            }

                        });
            } catch (Throwable t) {
                log("miuianesthetist err:");
                log(t);
            }

            // Check if MIUI is global version
            if (null == iib) {
                try {
                    iib = findField(findClass(
                            "miui.os.Build", lpparam.classLoader),
                            "IS_INTERNATIONAL_BUILD").getBoolean(null);
                } catch (Throwable t) {
                    log("miuianesthetist err:");
                    log(t);
                }
            }

            // Return an intent before it get processed to avoid being hijacked in services.jar
            antiHijack:
            {
                if (iib) {
                    break antiHijack;
                }
                try {
                    Class<?> pmsClazz = findClass("com.android.server.pm.PackageManagerService",
                            lpparam.classLoader);
                    findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector"
                            , lpparam.classLoader, "checkMiuiIntent",
                            pmsClazz, Intent.class, String.class, int.class, List.class, int.class,
                            new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                    Intent intent = (Intent) param.args[1];
                                    if (("market".equals(intent.getScheme()) &&
                                            "android.intent.action.VIEW".equals(intent.getAction())) ||
                                            ("http".equals(intent.getScheme()) ||
                                                    "https".equals(intent.getScheme())) &&
                                                    "android.intent.action.VIEW".equals(intent.getAction())) {
                                        //return mResolveInfo of the first arg pms
                                        return findField(pmsClazz, "mResolveInfo").get(param.args[0]);
                                    }
                                    return invokeOriginalMethod(param.method, param.thisObject, param.args);
                                }
                            });
                } catch (Throwable t) {
                    log("miuianesthetist err:");
                    log(t);
                }
            }
        }

        //SecurityCenter.apk
        if (lpparam.packageName.equals("com.miui.securitycenter")) {

            // Allow users disable system apps using SecurityCenter
            // TODO abstract this three method
            try {
                Class<?> ada = findClass("com.miui.appmanager.ApplicationsDetailsActivity",
                        lpparam.classLoader);
                findAndHookMethod(ada, "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            menu2 = ((Menu) param.args[0]).getItem(1);
                            menu2.setEnabled(true);
                        } catch (Throwable t) {
                            log("miuianesthetist err:");
                            log(t);
                        }
                    }
                });

                findAndHookMethod(ada, "onPrepareOptionsMenu", Menu.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            if (menu2 != null)
                                menu2.setEnabled(true);
                        } catch (Throwable t) {
                            log("miuianesthetist err:");
                            log(t);
                        }
                    }
                });

                findAndHookMethod(ada, "onResume", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            if (menu2 != null)
                                menu2.setEnabled(true);
                        } catch (Throwable t) {
                            log("miuianesthetist err:");
                            log(t);
                        }
                    }
                });
            } catch (Throwable t) {
                log("miuianesthetist err:");
                log(t);
            }

            // Allow users set third-party launcher to be default
            try {
                findAndHookMethod("com.miui.securitycenter.provider.ThirdDesktopProvider",
                        lpparam.classLoader, "call", String.class, String.class,
                        Bundle.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    if (param.args[0].equals("getModeAndList")) {
                                        Bundle result = (Bundle) param.getResult();
                                        // mode=0 can't use unauthorized third-party launcher;
                                        // mode=1 can't use official launcher; other value no limit
                                        result.putInt("mode", -1);
                                        param.setResult(result);
                                    }
                                } catch (Throwable t) {
                                    log("miuianesthetist err:");
                                    log(t);
                                }
                            }
                        });
            } catch (Throwable t) {
                log("miuianesthetist err:");
                log(t);
            }
        }
    }
}
