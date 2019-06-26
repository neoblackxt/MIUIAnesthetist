# MIUIAnesthetist
MIUI麻醉师Xposed模块，麻醉MIUI，以便对小米应用商店等癌组织进行无痛切除

[For English users](#MIUIAnesthetist_EN)

## 功能特性
禁止MIUI系统启动时检查自身完整性，防止在删除小米应用商店后无法开机进入系统。理论上支持其他的开机自检的系统应用，如系统更新等。

解除MIUI系统部分系统应用、组件不能被冻结的限制，并且避免被冻结的应用在重启后自动解冻。（全球上网必须在MConnService(VsimCore.apk)冻结之后才能成功冻结）

禁止MIUI系统将小米应用商店自动设置为默认应用商店劫持Google Play应用商店。~~（其实就是去你大爷的小米应用商店）~~ 如无效请恢复默认应用设置： 安全中心->应用管理->右上竖排三点->默认应用设置->恢复默认 鸣谢：跟悟空扯关系@酷安网coolapk.com [去除MIUI强制调用小米应用商店（非改build）](https://www.coolapk.com/feed/8492730?shareKey=MjM2ODkyMTI5Zjg4NWNlZDJhMzI~)、ccat3z@github 我不要小米应用市场

## 食用方法
安装并启用此Xposed模块，重启系统。

用任何你喜欢的工具/方式冻结你需要冻结的应用。

如果需要移除系统应用建议使用release中的[Magisk模块](https://github.com/neoblackxt/MIUIAnesthetist/releases/download/v2.0/MIUIAnesthetistHelperMagiskModule.zip)，一旦出现异常情况，用[mm管理器](https://github.com/Magisk-Modules-Repo/mm)禁用此magisk模块即可恢复。

此Magisk模块默认只移除MiuiSuperMarket。修改此模块中的`install.sh`文件，将你想要移除的应用的所在目录填写在`REPLACE=""`中，一行一个，格式参照上面几行的例子。

更多信息：[Magisk模块指南](https://topjohnwu.github.io/Magisk/guides.html)

## 注意事项
禁用此Xposed模块 或 禁用Xposed框架 或 禁用Xposed框架依附的magisk 或 重新安装/版本升级此模块后，xposed installer没检测到此模块更新（此时可能需手动关闭再打开才能再次启用），都会导致此模块功能失效。所以如果你没有使用Magisk而是直接删掉了`/system/app/MiuiSuperMarket/MiuiSuperMarket.apk`，并且万一因误操作致使此模块被禁用，手机会无法开机进入系统。

## 救援
如果因冻结系统应用而无法开机，只能删除`/data/system/users/0/package-restrictions.xml`文件后重启系统。可以在TWRP recovery的 高级->文件管理器 中操作。或在recovery终端中执行命令`rm /data/system/users/0/package-restrictions.xml`。或者用电脑连接recovery模式中的手机，在`cmd`中执行`adb shell`，然后`rm /data/system/users/0/package-restrictions.xml`。刷机不会有任何帮助，擦除`/data`分区是可行的但没必要，因为那样会丢失除了`/sdcard`之外的所有数据。

如果因删除小米应用商店而无法开机，将[小米应用商店](http://apkpure.co/xiaomi-market-com-xiaomi-market/)文件放到`/data/app/xiaomimarket/xiaomimarket.apk`,或者刷机（不必清除`/data`分区）。

# MIUIAnesthetist_EN
Cut out those cancer apps on MIUI painlessly with the help of this Xposed module.

## FEATURE
Disable MIUI integrity check when boot to prevent boot-loop, so that you can safely force delete System Updater and other system apps.

Remove limit for disabling some packages and prevent some package auto re-enable when boot.

Prevent MI Market from hijacking Google Play on MIUI China ROMs.

## USAGE
Install and enable this Xposed module then reboot.

Freely disable the packages which you don't need.

If you want to remove system apps, I suggest you use [Magisk module](https://github.com/neoblackxt/MIUIAnesthetist/releases/download/v2.0/MIUIAnesthetistHelperMagiskModule.zip) in release to remove them systemless-ly, 
so that if something goes wrong, use [mm](https://github.com/Magisk-Modules-Repo/mm) disable this Magisk module, then all removed apps will come back again. 

Modify `install.sh` file in this Magisk module: add the directories path which app you want to remove locate in to `REPLACE=""`, one path per line, there is a good example on a couple of lines above.
This Magisk module only remove MiuiSuperMarket by default, it's useless to MIUI global edition user, remove that line.

Learn more: [Magisk guides](https://topjohnwu.github.io/Magisk/guides.html)

## NOTICE
If this module disabled by accident, its magic will disappear and then your phone will boot-loop. So remember to reverse your operation on system apks before you disable this module.

## RESCUE
Flash ROM (NOT need to wipe /data) if you have really removed some system apps files from ROM.

Remove `/data/system/users/0/package-restrictions.xml` file if you just disabled some system apps.

## MISCELLANEOUS
Help me translate or optimize this readme if you like.
