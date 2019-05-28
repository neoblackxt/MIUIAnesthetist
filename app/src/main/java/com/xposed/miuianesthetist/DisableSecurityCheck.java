package com.xposed.miuianesthetist;

import java.lang.reflect.Field;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.content.Intent;

public class DisableSecurityCheck implements IXposedHookLoadPackage {
    private static final Object lock = new Object();
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android")) {
            try {
                /* 禁用开机检查自身完整性
                 com.miui.server.SecurityManagerService中的checkSystemSelfProtection方法过短，
                 无法插入跳转指令进行hook，故hook其方法内创建的匿名内部类的run方法 */
                findAndHookMethod("com.miui.server.SecurityManagerService$1"
                        , lpparam.classLoader, "run", XC_MethodReplacement.DO_NOTHING);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
            try {
                /* 禁用检查应用是否可以被禁用 */
                findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector"
                        , lpparam.classLoader, "isAllowedDisable",
                        String.class, int.class, XC_MethodReplacement.returnConstant(true));
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            /* 弃用
            try {
                *//* 禁用小米应用商店劫持其他应用商店 来自 ccat3z@github [我不要小米应用市场] *//*
                findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector"
                        , lpparam.classLoader, "getMarketResolveInfo",
                        List.class, XC_MethodReplacement.returnConstant(null));
            } catch (Throwable t) {
                XposedBridge.log(t);
            }*/

            try {
                /* 禁用小米应用商店劫持其他应用商店 来自 跟悟空扯关系@酷安网coolapk.com
                去除MIUI强制调用小米应用商店（非改build）
                https://www.coolapk.com/feed/8492730?shareKey=MjM2ODkyMTI5Zjg4NWNlZDJhMzI~ */
                Field IS_INTERNATIONAL_BUILD = XposedHelpers.findField(XposedHelpers.findClass("miui.os.Build", lpparam.classLoader), "IS_INTERNATIONAL_BUILD");
                boolean iib = IS_INTERNATIONAL_BUILD.getBoolean(null);
                if (!iib) {
                    XposedBridge.log("Get MIUI Build: NOT international build");
                    Class<?> pmsClazz = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpparam.classLoader);
                    findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector"
                            , lpparam.classLoader, "checkMiuiIntent",
                            pmsClazz, Intent.class, String.class, int.class, List.class, int.class, new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                    Object result;
                                    synchronized (lock){
                                        IS_INTERNATIONAL_BUILD.setBoolean(null,true);
                                        XposedBridge.log("Set MIUI Build to international build");
                                        result = XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                                        IS_INTERNATIONAL_BUILD.setBoolean(null,false);
                                        XposedBridge.log("Set MIUI Build back to NOT international build");
                                    }
                                    return result;
                                }
                            });
                }

            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            /*
            static ResolveInfo checkMiuiIntent(PackageManagerService pms, Intent intent, String resolvedType, int flags, List<ResolveInfo> query, int userId) {
                ResolveInfo ri;
                if (intent != null && !Build.IS_INTERNATIONAL_BUILD) {
                    if (intent.getCategories() == null || !intent.getCategories().contains("android.intent.category.HOME")) {
                        if ("android.intent.action.ASSIST".equals(intent.getAction()) || "android.intent.action.VOICE_COMMAND".equals(intent.getAction())) {
                            return getSystemResolveInfo(pms, query);
                        }
                        if (("http".equals(intent.getScheme()) || "https".equals(intent.getScheme())) && "android.intent.action.VIEW".equals(intent.getAction())) {
                            ri = UrlResolver.checkMiuiIntent(pms.mContext, pms, intent, resolvedType, flags, query, userId);
                            if (ri != null) {
                                return ri;
                            }
                            if (VERSION.SDK_INT < 23 && intent.getComponent() == null && intent.getType() == null) {
                                ri = getBrowserResolveInfo(pms, query);
                                if (ri != null) {
                                    return ri;
                                }
                            }
                        } else if ("mimarket".equals(intent.getScheme()) || ("market".equals(intent.getScheme()) && "android.intent.action.VIEW".equals(intent.getAction()))) {
                            Uri uri = intent.getData();
                            if (uri != null) {
                                String host = uri.getHost();
                                if (host != null && (host.equals("details") || host.equals("search"))) {
                                    ResolveInfo ri2 = getMarketResolveInfo(query);
                                    if (ri2 != null) {
                                        return ri2;
                                    }
                                }
                            }
                        } else if (PACKAGE_MIME_TYPE.equals(intent.getType()) && "android.intent.action.VIEW".equals(intent.getAction())) {
                            String realPkgName;
                            if (Build.IS_INTERNATIONAL_BUILD || AppOpsUtils.isXOptMode() || Build.IS_TABLET) {
                                realPkgName = PACKAGE_INSTALLER_NAME;
                                synchronized (pms.mPackages) {
                                    if (pms.mSettings.getRenamedPackageLPr(PACKAGE_INSTALLER_NAME) != null) {
                                        realPkgName = pms.mSettings.getRenamedPackageLPr(PACKAGE_INSTALLER_NAME);
                                    }
                                }
                            } else {
                                realPkgName = MIUI_INSTALLER_PACKAGE;
                            }
                            intent.setPackage(realPkgName);
                            return pms.resolveIntent(intent, resolvedType, flags, userId);
                        }
                    } else if (Build.getUserMode() == 1) {
                        intent.setClassName("com.jeejen.family.miui", "com.jeejen.home.launcher.Launcher");
                    } else {
                        intent.setClassName("com.miui.home", "com.miui.home.launcher.Launcher");
                    }
                    if (intent.getComponent() != null) {
                        return pms.resolveIntent(intent, resolvedType, flags, userId);
                    }
                } else if (!(intent == null || !Build.IS_INTERNATIONAL_BUILD || AppOpsUtils.isXOptMode())) {
                    if ((PACKAGE_MIME_TYPE.equals(intent.getType()) && "android.intent.action.VIEW".equals(intent.getAction())) || "android.intent.action.INSTALL_PACKAGE".equals(intent.getAction())) {
                        intent.setPackage(PACKAGE_INSTALLER_NAME);
                        return pms.resolveIntent(intent, resolvedType, flags, userId);
                    } else if (isOpenByGooglePlayStore(intent, pms)) {
                        ri = getGlobalMarketResolveInfo(query);
                        if (ri != null) {
                            return ri;
                        }
                    }
                }
                return pms.mResolveInfo;
            }


             */
        }
    }
}
