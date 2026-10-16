# 水果互联 HUD 编辑器核对

目标是本工作区恢复后的 `analysis/shuiguo/runtime/shuiguo-runtime.apk`，SHA-256 `65ff99eb1b375f524d252c0b89046fb666afff8b9a3b7dad672b5a48de20d321`。REA 识别为 `com.autochips.carplayapp` 1.204。恢复后的 DEX 与原始 APK 区分；以下是静态反编译观察，未声称在实车上逐项操作过该编辑器。

| 用户看到的行为 | 静态证据 |
| --- | --- |
| HUD 编辑画布含转向图、道路、距离、路线、来电、媒体、车道与进度示例 | `setting_hud_dialog.xml` 与 `a3.r0.onCreate`，REA `ev_c8503be5b345bbb67f4084a78d31096f542d4718a403a16f13ed1f438a507dd2` |
| 点选元素，单指拖动，位置限制在画布内 | `a3.r0.a.onTouch`，REA `ev_440f590bd6f84026fd355a4574fff5c2a7a17c81197a865137946a5548c9ee1f` |
| 对当前元素切换颜色、显隐，点击放大/缩小 | `a3.r0.onClick`、`a3.r0.c`，REA `ev_25f396206800045f2a8bd2644b7e8aae3c9bc86b6b175f87679614d470f7cf38`、`ev_fd0d655a0cf093a529d26f68d997fedd6c5e442fa7638d075e662f182dcaacc4` |
| 确定时保存各元素的位置、大小、颜色和显隐，供另一块 HUD Display 使用 | `a3.r0.d`、`a3.i.b`，REA `ev_64cecdd1e6af90c6ccd0f0785802023c00b55f049bf94e274898ea8ed6b6f10d`、`ev_b18a8563961a55b7737fafa2841d062d548e0f0020d2ea71163b33cc047a106b` |

DiPlay With Coffee 的编辑器据此提供同屏示例、点选拖动、缩放、颜色、显隐、保存和取消。三个独立方案是本项目新增设计；方案 1 沿用此前保存的布局。画布只预览元素的相对位置，不表示覆盖整块物理 HUD。水果互联使用图片视图和真实车机窗口；本项目当前运行时 HUD 以文字和简单转向符号绘制，因此不能把两者的图标外观或实车投影效果称作已验证一致。
