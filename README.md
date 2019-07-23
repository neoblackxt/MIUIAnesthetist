# MIUI Anesthetist
Carry out surgical strikes against MIUI. This module is committed to remove the limitations of MIUI, allowing you to enter the God mode of MIUI.

## FEATURES
Disable MIUI integrity check during boot to prevent boot-loop, so that you can safely force delete System Updater and other system apps.

Remove limit for disabling some packages and prevent some packages from re-enabling during reboot.

Prevent MI Market from hijacking Google Play on MIUI China ROMs. Thanks to & for the inspiration from: 跟悟空扯关系@CoolApk 去除MIUI强制调用小米应用商店（非改build）、ccat3z@github 我不要小米应用市场

Remove limit for third-party launchers on MIUI China ROMs. Thanks to & for the inspiration from: tianma8023@github FuckMiui

Allow users to use Security - Manage apps to disable system apps.

Remove limit for installing system apps from unofficial channels on MIUI China ROMs.

## USAGE
Install and enable this Xposed module, then reboot.

Freely disable the packages which you don't need. (These packages will be keep disabled after updating system)

To remove system apps, use [MIUI Anesthetist Helper Magisk module](https://github.com/neoblackxt/MIUIAnesthetist/releases/download/v2.0/MIUIAnesthetistHelperMagiskModule.zip)(hereinafter referred to as "helper module") in release, and [Magisk Manager for Recovery Mode](https://github.com/Magisk-Modules-Repo/mm)(hereinafter referred to as "mm") is also needed. (These packages will be kept removed after updating system)<br>
Modify `install.sh` file in helper module: add the directory paths which the apps you want to remove are located in to `REPLACE=""`, one path per line, there is a good example in a couple of lines above in this file. Once finished, you can add it to [Magisk Manager](https://github.com/topjohnwu/Magisk/releases) or install it in recovery mode.<br>
This helper module only removes MiuiSuperMarket by default, it's useless for MIUI global edition users, so remove that line.<br>
You may share your modified and tested helper modules with you friends. :)

Learn more: [Magisk guides](https://topjohnwu.github.io/Magisk/guides.html#remove-folders)

## NOTICE
The toolchain is as: Magisk -> (Riru-Core) -> (Riru-Ed)Xposed -> MIUI Anesthetist

You should flash Magisk immediately as soon as you update system in recovery mode to ensure the toolchain is working.

If this module is disabled by accident, its magic will disappear and then your phone may boot-loop. (See **RESCUE**)<br>
The case can be:<br>
①This Xposed module is disabled<br>
②Xposed framework is disabled<br>
③Magisk which Xposed relies on is disabled<br>
④After reinstalling or updating this Xposed module, Xposed installer doesn't detect its update.(You have to disable it and then re-enable it manually in Xposed module list)

Some system apps are Android core programs, they should **never** be disabled or removed. If you have no idea about a system app, you should Google it first and think twice. 

## RESCUE
If you have disabled some system apps, remove or rename `/data/system/users/0/package-restrictions.xml` file to reset disabling app settings.

If you have removed some system apps, disable the helper module using mm to recover all system apps. BTW, you'd better read its [README](https://github.com/Magisk-Modules-Repo/mm/blob/master/README.md) to know its usage early.

If you have **really** removed some system app files from ROM, flash ROM (**NOT** need to wipe `/data`).

**Advanced:**<br>
①In `/data/system/users/0/package-restrictions.xml` file, the `enabled` attribute determines if a package is disabled. `enabled=3` means it is disabled, and remove `enabled=3` to enable it.<br>
②Magisk determines if a Magisk module is enabled by looking for `/data/adb/modules/<module-id>/disable` file. If it exists, it means the module is disabled.(Some old versions don't support this)<br>
③(Ed)Xposed's config file is `/data/user_de/0/<(Ed)Xposed-installer(manager)-package-name>/conf/modules.list` file, its format is Xposed module apk files' absolute paths, one path per line, like `/data/app/<Xposed-module-package-name>-<random-string>/base.apk`<br>

## MISCELLANEOUS
Help me translate or optimize this README if you like.
