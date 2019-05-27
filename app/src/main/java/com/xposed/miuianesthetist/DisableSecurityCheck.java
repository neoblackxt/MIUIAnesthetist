package com.xposed.miuianesthetist;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class DisableSecurityCheck implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android")) {
            try {
                /**com.miui.server.SecurityManagerService中的checkSystemSelfProtection方法过短，
                 无法插入跳转指令进行hook，故hook其方法内创建的匿名内部类的run方法 */
                findAndHookMethod("com.miui.server.SecurityManagerService$1"
                        , lpparam.classLoader, "run", XC_MethodReplacement.DO_NOTHING);

            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }
}
