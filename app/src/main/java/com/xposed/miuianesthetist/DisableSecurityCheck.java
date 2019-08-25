package com.xposed.miuianesthetist;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.CheckBoxPreference;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.xposed.miuianesthetist.XposedHelpersWraper.findAndHookConstructor;
import static com.xposed.miuianesthetist.XposedHelpersWraper.findAndHookMethod;
import static com.xposed.miuianesthetist.XposedHelpersWraper.findClass;
import static com.xposed.miuianesthetist.XposedHelpersWraper.findField;
import static com.xposed.miuianesthetist.XposedHelpersWraper.findMethodBestMatch;
import static com.xposed.miuianesthetist.XposedHelpersWraper.findMethodExact;
import static com.xposed.miuianesthetist.XposedHelpersWraper.getObjectField;
import static com.xposed.miuianesthetist.XposedHelpersWraper.getStaticBooleanField;
import static com.xposed.miuianesthetist.XposedHelpersWraper.getStaticObjectField;
import static com.xposed.miuianesthetist.XposedHelpersWraper.hookMethod;
import static com.xposed.miuianesthetist.XposedHelpersWraper.invokeOriginalMethod;
import static com.xposed.miuianesthetist.XposedHelpersWraper.log;
import static com.xposed.miuianesthetist.XposedHelpersWraper.setBooleanField;
import static com.xposed.miuianesthetist.XposedHelpersWraper.setIntField;

public class DisableSecurityCheck extends BaseXposedHookLoadPackage {
    private static volatile Resources res;
    private static volatile MenuItem menu2;

    @Override
    public void initZygote(StartupParam startupParam) {
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals("android")) {
            try {
                handleAndroid(lpparam);
            } catch (Throwable t) {
                log(t);
            }
        }
        //SecurityCenter.apk
        if (lpparam.packageName.equals("com.miui.securitycenter")) {
            try {
                handleSecurityCenter(lpparam);
            } catch (Throwable t) {
                log(t);
            }
        }
        //Settings.apk
        if (lpparam.packageName.equals("com.android.settings")) {
            try {
                handleSettings(lpparam);
            } catch (Throwable t) {
                log(t);
            }
        }

        //MiuiInstaller.apk
        if (lpparam.packageName.equals("com.miui.packageinstaller")) {
            try {
                handleInstaller(lpparam);
            } catch (Throwable t) {
                log(t);
            }
        }

    }

    //Remove limit for installing system apps from unofficial channels
    private void handleInstaller(XC_LoadPackage.LoadPackageParam lpparam) {
        ClassLoader classLoader = lpparam.classLoader;
        Class<?> pia = findClass("com.android.packageinstaller.PackageInstallerActivity", classLoader);
        if (pia == null) return;
        List<Method> methods = new LinkedList<>();
        for (Method m : pia.getMethods()) {
            if (Arrays.equals(m.getParameterTypes(), new Class[]{Uri.class})) {
                methods.add(m);
            }
        }
        XC_MethodHook xc_methodHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field[] fields = pia.getDeclaredFields();
                Field P = null;
                for (Field f : fields) {
                    if (f.getType() == List.class) {
                        List sf = (List) getStaticObjectField(pia, f.getName());
                        if (sf.contains("com.android.vending")) {
                            P = f;
                            break;
                        }
                    }
                }
                Uri uri = (Uri) param.args[0];
                String scheme = uri.getScheme();
                if (scheme == null || !scheme.equals("file")) return;
                String path = uri.getPath();
                if (TextUtils.isEmpty(path)) return;
                Class<?> ppc = findClass("com.android.packageinstaller.compat.PackageParserCompat", classLoader);
                Method parsePackage = findMethodBestMatch(ppc, "parsePackage", File.class, int.class);//file, 0
                Method createPackageUserState = findMethodBestMatch(ppc, "createPackageUserState");
                Method generatePackageInfo = findMethodBestMatch(ppc, "generatePackageInfo", Object.class, int[].class, int.class, long.class, long.class, Set.class, Object.class);
                //this.d = PackageParserCompat.generatePackageInfo(v1_1, v2, 0x1080, 0, 0, ((Set)v2), PackageParserCompat.createPackageUserState());
                File file = new File(path);
                Object v1_1 = parsePackage.invoke(null, file, 0);
                int[] v2 = {};
                PackageInfo packageInfo = (PackageInfo) generatePackageInfo.invoke(null, v1_1, v2, 0x1080, 0, 0, new HashSet(), createPackageUserState.invoke(null));
                String packageName = packageInfo.packageName;
                List p = (List<String>) getStaticObjectField(pia, P.getName());
                if (p != null && !p.contains(packageName))
                    p.add(packageName);
            }
        };
        for (Method m : methods) {
            String mname = m.getName();
            findAndHookMethod(pia, mname, Uri.class, xc_methodHook);
        }

    }

    private void handleSettings(XC_LoadPackage.LoadPackageParam lpparam) {
        ClassLoader classLoader = lpparam.classLoader;
        //Try to return a fake result of if an app is a system app, invalid
        Class<?> nshClass = findClass("com.android.settings.notification.NotificationSettingsHelper", classLoader);
        for (Method m : nshClass.getMethods()) {
            if (Arrays.equals(m.getParameterTypes(), new Class[]{int.class})) {
                log(m.getName());
                hookMethod(m, XC_MethodReplacement.returnConstant(false));
                break;
            }
        }
        final Class<?> appRowClass = findClass("com.android.settings.notification.MiuiNotificationBackend$AppRow", classLoader);
        findAndHookMethod("com.android.settings.notification.MiuiNotificationBackend", classLoader, "markAppRowWithBlockables",
                String[].class, appRowClass, String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        log("arg0:" + Arrays.toString((String[]) param.args[0]));
                        log("arg2:" + param.args[2]);
                        param.setResult(null);
                    }
                });

        //Try to allow users disable AppNotificationSettings ,invalid
        Class<?> ans = findClass("com.android.settings.notification.AppNotificationSettings", classLoader);
        findAndHookMethod(ans, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Object o = null;
                try {
                    o = findFirstFieldByExactType(ans, appRowClass).get(param.thisObject);
                } catch (IllegalAccessException e) {
                    log(e);
                }
                setBooleanField(o, "systemApp", false);
                Method findPreference = findMethodBestMatch(ans, "findPreference", String.class);
                CheckBoxPreference block = null;
                try {
                    if (findPreference != null) {
                        findPreference.setAccessible(true);
                        block = (CheckBoxPreference) findPreference.invoke(param.thisObject, "block");
                    }
                } catch (Throwable t) {
                    log(t);
                }
                if (block != null)
                    block.setEnabled(true);
            }
        });
    }

    private void handleAndroid(XC_LoadPackage.LoadPackageParam lpparam) {
        ClassLoader classLoader = lpparam.classLoader;
        //Check if MIUI is global version
        if (null == iib) {
            iib = getStaticBooleanField(findClass("miui.os.Build", classLoader),
                    "IS_INTERNATIONAL_BUILD");
        }

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
                    }
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        //Set system apps' states to be not cracked
                        setIntField(param.thisObject, "mSysAppCracked", 0);
                    }
                });

        //Remove the limit for disabling system apps in services.jar
        findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector"
                , classLoader, "isAllowedDisable",
                String.class, int.class, XC_MethodReplacement.returnConstant(true));

        //Remove the limit for installing WebViewGoogle manually on MIUI China ROM in services.jar
        findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector",
                classLoader, "isAllowedInstall",
                Context.class, File.class, int.class, String.class,
                XC_MethodReplacement.returnConstant(true));

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

        //Allow users disable system apps' notification chanel in framework.jar
        findAndHookConstructor("android.app.NotificationChannel", classLoader, Parcel.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                setBooleanField(param.thisObject, "mBlockableSystem", true);
            }
        });
        findAndHookConstructor("android.app.NotificationChannel", classLoader, String.class, CharSequence.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                setBooleanField(param.thisObject, "mBlockableSystem", true);
            }
        });
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
                                //Return mResolveInfo of the first arg pms
                                return getObjectField(param.args[0], "mResolveInfo");
                            }
                            return invokeOriginalMethod(param.method, param.thisObject, param.args);
                        }
                    });
        }
    }

    private void handleSecurityCenter(XC_LoadPackage.LoadPackageParam lpparam) {
        ClassLoader classLoader = lpparam.classLoader;
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
        //Allow users to revoke system app internet access permission
        //FIXME network control state preview abnormal, title disappear or turn off data, invalid after reboot
        findAndHookMethod(ada, "initData", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    initRes(param);
                    int R_id_am_detail_net = res.getIdentifier("am_detail_net",
                            "id", "com.miui.securitycenter");
                    View am_detail_net = ((Activity) param.thisObject).findViewById(R_id_am_detail_net);
                    am_detail_net.setVisibility(View.VISIBLE);
                } catch (Throwable t) {
                    log(t);
                }
            }
        });
        findAndHookMethod(ada, "onClick", View.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    setBooleanField(param.thisObject, "mIsSystem", false);
                } catch (Throwable t) {
                    log(t);
                }
            }
        });
        //Allow users to set third-party launcher to be default
        findAndHookMethod("com.miui.securitycenter.provider.ThirdDesktopProvider",
                classLoader, "call", String.class, String.class,
                Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (param.args[0].equals("getModeAndList")) {
                                Bundle result = (Bundle) param.getResult();
                                //mode=0 can't use unauthorized third-party launcher;
                                //mode=1 can't use official launcher; other value no limit
                                result.putInt("mode", -1);
                                param.setResult(result);
                            }
                        } catch (Throwable t) {
                            log(t);
                        }
                    }
                });

        //Remove waiting time for applying device admin app
        findAndHookMethod("com.miui.permcenter.install.DeviceManagerApplyActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param)  {
                bypassSuperNotCalledException(param);
            }
        });

        //Remove waiting time for applying adb input
        findAndHookMethod("com.miui.permcenter.install.AdbInputApplyActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param)  {
                try {
                    setProp(classLoader,"persist.security.adbinput","1");
                    bypassSuperNotCalledException(param);
                } catch (Exception e) {
                    log(e);
                }
            }
        });

        //Remove waiting time for applying install verify
        findAndHookMethod("com.miui.permcenter.install.AdbInstallVerifyActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                callPreference(param, "security_adb_install_enable", true);
                setProp(classLoader,"persist.security.adbinstall","1");
                bypassSuperNotCalledException(param);
            }
        });

        //Remove waiting time for applying root access
        findAndHookMethod("com.miui.permcenter.root.RootApplyActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param)  {
                try {
                    grantRootAccess(param);
                } catch (Exception e) {
                    log(e);
                }
            }
        });
    }

    private void grantRootAccess(XC_MethodHook.MethodHookParam param) {
        Activity activity = (Activity) param.thisObject;
        String mPkgName = activity.getIntent().getStringExtra("extra_pkgname");
        if (TextUtils.isEmpty(mPkgName)) {
            bypassSuperNotCalledException(param);
            return;
        }
        String mAppName = getAppName(param,mPkgName);
        Bundle bundle = new Bundle();
        bundle.putLong("extra_permission", 512);
        bundle.putInt("extra_action", 3);
        bundle.putStringArray("extra_package", new String[]{mPkgName});
        bundle.putInt("extra_flags", 0);
        Uri content_uri = Uri.parse("content://com.lbe.security.miui.permmgr");
        activity.getApplicationContext().getContentResolver().call(content_uri, "6", null, bundle);
        Resources resources = activity.getResources();
        int toast_root_apply_accept = resources.getIdentifier("toast_root_apply_accept", "string", "com.miui.securitycenter");
        Toast.makeText(activity, resources.getString(toast_root_apply_accept, mAppName), Toast.LENGTH_SHORT).show();
        bypassSuperNotCalledException(param);
    }

    private String getAppName(XC_MethodHook.MethodHookParam param,String packageName) {
        Activity activity = (Activity) param.thisObject;
        if ("root".equals(packageName)) {
            return "root";
        }
        if ("com.android.shell".equals(packageName)) {
            return "Interactive Shell";
        }
        try {
            PackageManager packageManager = activity.getApplicationContext().getPackageManager();
            return packageManager.getApplicationInfo(packageName,0).loadLabel(packageManager).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private void setProp(ClassLoader classLoader,String key,String value) throws IllegalAccessException, InvocationTargetException {
        Method setProp = findMethodExact("android.os.SystemProperties", classLoader, "set", String.class, String.class);
        setProp.invoke(null,key,value);
    }

    private synchronized void initRes(XC_MethodHook.MethodHookParam param) {
        res = (res == null) ? ((Activity) param.thisObject).getResources() : res;
    }

    private void bypassSuperNotCalledException(XC_MethodHook.MethodHookParam param) {
        Activity activity = (Activity) param.thisObject;
        findField(Activity.class,"mCalled").setAccessible(true);
        setBooleanField(activity,"mCalled",true);
        activity.setResult(Activity.RESULT_OK);
        activity.finish();
        param.setResult(null);
    }
}
