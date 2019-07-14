package com.xposed.miuianesthetist;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.xposed.miuianesthetist.XposedHelpersWraper.findAndHookConstructor;
import static com.xposed.miuianesthetist.XposedHelpersWraper.findAndHookMethod;
import static com.xposed.miuianesthetist.XposedHelpersWraper.findClass;
import static com.xposed.miuianesthetist.XposedHelpersWraper.getObjectField;
import static com.xposed.miuianesthetist.XposedHelpersWraper.getStaticBooleanField;
import static com.xposed.miuianesthetist.XposedHelpersWraper.invokeOriginalMethod;
import static com.xposed.miuianesthetist.XposedHelpersWraper.log;
import static com.xposed.miuianesthetist.XposedHelpersWraper.setIntField;

public class DisableSecurityCheck extends BaseXposedHookLoadPackage {
    private static volatile MenuItem menu2;

    @Override
    public void initZygote(StartupParam startupParam) {
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        classLoader = lpparam.classLoader;
        if (lpparam.packageName.equals("android")) {
            try {
                handleAndroid();
            } catch (Throwable t) {
                log(t);
            }
        }
        //SecurityCenter.apk
        if (lpparam.packageName.equals("com.miui.securitycenter")) {
            try {
                handleSecurityCenter();
            } catch (Throwable t) {
                log(t);
            }
        }
    }

    private void handleAndroid() {
        //Disable integrity check when boot in services.jar
        Class sms = findClass("com.miui.server.SecurityManagerService"
                , classLoader);
        findAndHookConstructor(sms, Context.class, boolean.class/*onlyCore*/,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        //Set the instance of SecurityManagerService class to be onlyCore mode,
                        //then checkSystemSelfProtection() method will do nothing
                        param.args[1] = true;
                        //Set system apps' states to be not cracked
                        setIntField(param.thisObject, "mSysAppCracked", 0);
                    }
                });
        //Remove the limit for disabling system apps in services.jar
        findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector"
                , classLoader, "isAllowedDisable",
                String.class, int.class, XC_MethodReplacement.returnConstant(true));
        //Prevent some ultra sticky system apps (MiuiDaemon, FindDevice, etc.) from running
        //after have been disabled in services.jar
        findAndHookMethod("com.android.server.am.ActivityManagerServiceInjector",
                classLoader, "shouldAddPersistApp", ApplicationInfo.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        ApplicationInfo info = (ApplicationInfo) param.args[0];
                        if (!info.packageName.equals("com.miui.greenguard") &&
                                info.enabled) {
                            param.setResult(true);
                            return;
                        }
                        //Shorten TAG ActivityManagerServiceInjector
                        log("AMSInjector", "persist app : " + info.packageName +
                                " should not add to start");
                        param.setResult(false);
                    }
                });
        //Check if MIUI is global version
        if (null == iib) {
            iib = getStaticBooleanField(findClass("miui.os.Build", classLoader),
                    "IS_INTERNATIONAL_BUILD");
        }
        //Return an intent before it get processed to avoid being hijacked in services.jar
        if (!iib) {
            Class<?> pmsClazz = findClass("com.android.server.pm.PackageManagerService",
                    classLoader);
            findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector"
                    , classLoader, "checkMiuiIntent",
                    pmsClazz, Intent.class, String.class, int.class, List.class, int.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            Intent intent = (Intent) param.args[1];
                            if (("market".equals(intent.getScheme()) &&
                                    "android.intent.action.VIEW".equals(intent.getAction())) ||
                                    ("http".equals(intent.getScheme()) ||
                                            "https".equals(intent.getScheme())) &&
                                            "android.intent.action.VIEW".equals(intent.getAction())) {
                                //return mResolveInfo of the first arg pms
                                return getObjectField(param.args[0], "mResolveInfo");
                            }
                            return invokeOriginalMethod(param.method, param.thisObject, param.args);
                        }
                    });
        }
    }

    private void handleSecurityCenter() {
        //Allow users to disable system apps using SecurityCenter's manage apps function
        //FIXME menuItem get disabled after selected and icon changed
        Class<?> ada = findClass("com.miui.appmanager.ApplicationsDetailsActivity",
                classLoader);
        XC_MethodHook xc_enable_menuItem = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (param.args.length == 1)
                        //Index: 0 force stop; 1 uninstall/disable/enable; 2 clear data/cache
                        menu2 = ((Menu) param.args[0]).getItem(1);
                    if (menu2 != null)
                        menu2.setEnabled(true);
                } catch (Throwable t) {
                    log(t);
                }
            }
        };
        findAndHookMethod(ada, "onCreateOptionsMenu", Menu.class, xc_enable_menuItem);
        findAndHookMethod(ada, "onPrepareOptionsMenu", Menu.class, xc_enable_menuItem);
        findAndHookMethod(ada, "onResume", xc_enable_menuItem);
        //Allow users to set third-party launcher to be default
        findAndHookMethod("com.miui.securitycenter.provider.ThirdDesktopProvider",
                classLoader, "call", String.class, String.class,
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
                            log(t);
                        }
                    }
                });
    }
}
