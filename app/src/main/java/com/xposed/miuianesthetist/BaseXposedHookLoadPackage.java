package com.xposed.miuianesthetist;

import de.robv.android.xposed.IXposedHookLoadPackage;

import static com.xposed.miuianesthetist.XposedHelpersWraper.setTAG;


public abstract class BaseXposedHookLoadPackage implements IXposedHookLoadPackage {
    protected Boolean iib;
    protected ClassLoader classLoader;

    static {
        setTAG("miuianesthetist");
    }
}
