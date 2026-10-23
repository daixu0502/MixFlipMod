# MixFlipMod

小米 MIX Flip / MIX Flip 2 外屏增强模块

支持 HyperOS 3.0 及以上版本，需要配合支持 libxposed API 101 的 Xposed 框架使用

应用包名为 `vip.chener.mixflipmod`。与原包名版本属于不同应用，安装后需要在 LSPosed 中重新启用并设置作用域，原版本的设置不会自动迁移；请避免同时启用两个版本。

## 构建与性能验证

使用 JDK 17 和 Android SDK 37 构建。在 Windows 上执行：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleUnminified
.\gradlew.bat :app:assembleRelease
```

`debug` 为可调试版本，未启用 R8；`unminified` 为不可调试且未启用 R8 的性能验证版本，使用调试签名；`release` 保持启用 R8。产物分别位于 `app/build/outputs/apk/` 下的同名目录。比较 R8 对流畅度的影响时，应使用 `unminified` 与 `release`，避免混入 Compose 调试开销。

主界面的应用信息在后台加载并复用，列表入口独立懒加载，图标使用有容量上限的内存缓存。仅保留实际使用的三个 Material 扩展图标源码，避免未启用 R8 时打包完整扩展图标库。

## 功能

隐藏外屏状态：

- 恢复部分系统应用默认样式

系统框架：

- 解除外屏应用限制
- 所有应用合盖继续使用
- 自定义外屏输入法（设置为 Gboard 后，可使用 Magisk 或 KernelSU + mountify 刷入 MixFlipGboard 模块启用外屏优化适配）
- 自定义外屏应用渲染模式（全屏/不缩放/自定义缩放比例）

> 系统应用推荐缩放比例：
>
> | 应用 | 缩放比例 |
> | --- | --- |
> | 电话 | 70% |
> | 短信 | 80% |
> | 相册 | 80% |
> | 时钟 | 70% |
> | 日历 | 70% |
> | 计算器 | 70% |

系统界面：

- 恢复外屏通知长按菜单
- 恢复外屏控制中心样式
- 隐藏外屏状态栏时钟
- 调整外屏通知图标最大数量

外屏桌面：

- 屏蔽特殊应用外屏启动页
- 最近任务样式（纵向/横向）
- 最近任务长按功能（纵向显示功能按钮，横向打开应用信息）
- 应用桌面长按菜单（增加应用信息选项）

智能助理：

- 支持添加外屏小部件

搜狗输入法小米版：

- 恢复外屏工具栏
- 恢复外屏剪贴板候选词

## 权限说明

项目会读取已安装应用列表，用于显示相关应用的名称、图标，并为需要按应用选择的功能提供应用列表

## 致谢

项目实现参考了以下开源项目的一些思路：

- [KernelSU](https://github.com/tiann/KernelSU)
- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler)
- [AndroidX Compose Material Icons](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/material/material-icons-extended/)：三个本地图标保留原始 Apache-2.0 许可证声明。

## 许可

本项目使用 AGPL-3.0 许可证，详见 [LICENSE](LICENSE)
