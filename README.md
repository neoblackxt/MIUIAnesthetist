# MIUIAnesthetist
MIUI麻醉师Xposed模块，麻醉MIUI，以便对MIUI商店等癌组织进行无痛切除

## 功能
禁止MIUI系统启动时检查自身完整性，防止在删除MIUI商店后无法开机进入系统。理论上支持其他的开机自检的系统应用，如系统更新等。

## 食用方法
利用magisk的挂载功能，在`/sbin/.magisk/modules/你的模块名/system/app/MiuiSuperMarket/`下建立一个`.replace`文件占位即可，每次开机自动移除垃圾系统应用。
需要恢复系统应用时，删除或重命名`.replace`文件。
你可能需要在`/sbin/.magisk/modules/你的模块名`路径下补充模块的说明文件，可参照其他模块修改。或者直接利用已有的模块，比如magisk自带hosts模块（在 magisk设置->Systemless hosts 处开启）
移除其他系统应用方法同理。

## 注意事项
禁用此模块 或 禁用Xposed框架 或 禁用Xposed框架依附的magisk，都会导致此模块功能失效。所以如果你直接删掉了`/system/app/MiuiSuperMarket/MiuiSuperMarket.apk`而不是用magisk进行“伪删除”，并且因误操作致使此模块被禁用，手机会无法开机进入系统。

## 救援
将[小米应用商店](http://apkpure.co/xiaomi-market-com-xiaomi-market/)文件放到`/data/app/xiaomimarket/xiaomimarket.apk`,或者刷机（不必清除/data分区）。