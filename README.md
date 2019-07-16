# MIUI麻醉师
对MIUI进行外科手术式打击。此模块致力于解除MIUI的各种功能限制，让你对MIUI开启上帝模式。

[For English users](#EN)

## 功能特性
禁止MIUI系统启动时检查自身完整性，防止删除系统应用后出现循环重启问题（如小米应用商店、系统更新等）。

解除MIUI系统部分系统应用、组件不能被冻结的限制，并且避免被冻结的应用在重启后自动解冻。（全球上网必须在MConnService(VsimCore.apk)冻结之后才能成功冻结）

禁止MIUI系统将小米应用商店自动设置为默认应用商店劫持Google Play应用商店。~~（其实就是去你大爷的小米应用商店）~~  感谢&启发自：跟悟空扯关系@酷安网 [去除MIUI强制调用小米应用商店（非改build）](https://www.coolapk.com/feed/8492730?shareKey=MjM2ODkyMTI5Zjg4NWNlZDJhMzI~)、ccat3z@github 我不要小米应用市场

解除MIUI系统对于未认证的第三方桌面的限制。感谢&启发自：tianma8023@github [糙米](https://github.com/neoblackxt/FuckMiui) 

开启 安全中心 -> 应用管理 的停用系统应用的功能。

解除无法从非官方渠道安装系统应用的限制。

## 使用方法
安装并启用此Xposed模块，重启系统。

用任何你喜欢的工具/方式冻结你需要冻结的应用。即使安装系统更新，应用也会保持被冻结的状态。

使用release中的[MIUI麻醉师助手Magisk模块](https://github.com/neoblackxt/MIUIAnesthetist/releases/download/v2.0/MIUIAnesthetistHelperMagiskModule.zip)（以下简称助手模块）移除你想要移除的系统应用，并安装[mm管理器](https://github.com/Magisk-Modules-Repo/mm)。即使安装系统更新，应用也会保持被移除的状态。<br>
此助手模块默认只移除小米应用商店。修改此模块中的`install.sh`文件，将你想要移除的应用的所在目录填写在`REPLACE=""`中，一行一个，格式参照文件中上面几行的例子。修改完成后在Magisk Manager中安装它或在recovery模式下刷入它。<br>
你可以将你修改过的并且测试过没有问题的助手模块分享给你网络社区中的朋友们 :)


更多信息：[Magisk模块指南](https://topjohnwu.github.io/Magisk/guides.html#remove-folders)

## 注意事项
工具链是这样的：Magisk -> (Riru-Core) -> (Riru-Ed)Xposed -> MIUI Anesthetist

通过recovery模式升级系统后，应立即刷入Magisk，然后才能开机，以确保工具链有效。

一旦因误操作致使工具链断裂，此模块功能失效，手机可能无法开机进入系统。解决方案见下方的**救援**部分。<br>
可能致使工具链断裂的情况有：<br>
①禁用此Xposed模块<br>
②禁用Xposed框架<br>
③禁用Xposed框架依附的Magisk<br>
④重新安装/版本升级此模块后，Xposed Installer没检测到此模块更新（此时必须手动关闭再打开才能再次启用）

有一些系统应用是安卓的核心程序，这些应用是绝对**不可以**被冻结或者删除的。如果你不了解某个应用，你应该先谷歌一下，三思而后行。

## 救援
如果因冻结系统应用而无法开机，删除或重命名`/data/system/users/0/package-restrictions.xml`文件可以重置应用冻结设置。<br>
操作方法：<br>
①在TWRP recovery的 高级 -> 文件管理 中找到该文件删除。<br>
②在recovery终端中执行命令`rm /data/system/users/0/package-restrictions.xml`。<br>
③用电脑连接recovery模式中的手机，在`cmd`中执行`adb shell`，然后`rm /data/system/users/0/package-restrictions.xml`。<br>
刷机**不会**有任何帮助，擦除`/data`分区是可行的但没必要，因为那样会丢失除了`/sdcard`之外的所有数据。

如果因删除系统应用无法开机，在recovery模式用mm管理器禁用师助手模块即可恢复所有系统应用。你最好提阅读它的[使用说明](https://github.com/Magisk-Modules-Repo/mm/blob/master/README.md)。

如果你没有使用助手模块而是用文件管理器彻底删掉了某个系统应用，可通过刷机恢复（**不必**清除`/data`分区）。

**高级：**<br>
①`package-restrictions.xml`中有关冻结的元素属性是`enabled`，`enabled=3`为冻结，删除`enabled=3`即解冻。<br>
②Magisk配置模块是否启用的方式是查看`/data/adb/modules/<module-id>/disable`文件是否存在，存在为禁用，反之为启用（旧版Magisk不支持此方式）<br>
③(Ed)Xposed启用模块的配置文件为`/data/user_de/0/<package-name>/conf/modules.list`，配置格式为Xposed模块apk文件的绝对路径，每行一个，形如`/data/app/<Xposed-package-name>-<random-string>/base.apk`<br>


**其他：**<br>
[小米应用商店下载](http://apkpure.co/xiaomi-market-com-xiaomi-market/)（此网站中国大陆用户可能无法访问）


# <a id="EN"></a>MIUI Anesthetist
Carry out surgical strikes against MIUI.

## FEATURES
Disable MIUI integrity check when boot to prevent boot-loop, so that you can safely force delete System Updater and other system apps.

Remove limit for disabling some packages and prevent some package auto re-enable when boot.

Prevent MI Market from hijacking Google Play on MIUI China ROMs.

Remove limit for third-party launcher on MIUI China ROMs. Thanks to tianma8023@github [FuckMiui](https://github.com/neoblackxt/FuckMiui)

Allow users to use Security - Manage apps to disable system apps.

Remove limit for installing system apps from unofficial channels on MIUI China ROMs.

## USAGE
Install and enable this Xposed module then reboot.

Freely disable the packages which you don't need.

If you want to remove system apps, I suggest you use [Magisk module](https://github.com/neoblackxt/MIUIAnesthetist/releases/download/v2.0/MIUIAnesthetistHelperMagiskModule.zip) in release to remove them systemless-ly, 
so that if something goes wrong, use [mm](https://github.com/Magisk-Modules-Repo/mm) disable this Magisk module, then all removed apps will come back again. 

Modify `install.sh` file in this Magisk module: add the directories path which app you want to remove locate in to `REPLACE=""`, one path per line, there is a good example on a couple of lines above.
This Magisk module only remove MiuiSuperMarket by default, it's useless to MIUI global edition user, remove that line.

Learn more: [Magisk guides](https://topjohnwu.github.io/Magisk/guides.html#remove-folders)

## NOTICE
If this module disabled by accident, its magic will disappear and then your phone will boot-loop. So remember to reverse your operation on system apks before you disable this module.

Some system apps are Android core programs, they should never be disabled or removed. If you have no idea about a system app, you should Google it first. 

## RESCUE
Flash ROM (**NOT** need to wipe /data) if you have really removed some system apps files from ROM.

Remove `/data/system/users/0/package-restrictions.xml` file if you just disabled some system apps.

## MISCELLANEOUS
Help me translate or optimize this readme if you like.
