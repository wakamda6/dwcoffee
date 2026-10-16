# dwcoffee

dwcoffee（DiPlay With Coffee）基于 [DiPlay](https://github.com/shihabal3amri/DiPlay)，保留原有 CarPlay 核心功能，补充 GWM WEY 拿铁车型支持和说明。拿铁实车已验证仪表地图及 HUD 导航内容可见；媒体、通话、车道信息与跨固件行为仍需逐项验证。

## 当前基线

- 上游：DiPlay，起点提交 `969f871433b76f6c59f3c93619f7cb5872fdd15d`。
- 主应用模块：`:mobile`；库模块：`:common`、`:shared`。
- 应用 ID：`com.wakamda.dwcoffee`；调试版：`com.wakamda.dwcoffee.hudtest`。两者与原 DiPlay 可并存。
- 开发分支：`develop`；`origin` 指向 [dwcoffee](https://github.com/wakamda6/dwcoffee)，`upstream` 指向 DiPlay 原仓库。
- 车型补充、品牌文案和图标位于独立开发分支；CarPlay 协议核心沿用上游实现。

## 从哪里开始

完整目标、证据、架构、阶段任务和验收标准见 [初始开发方案](docs/INITIAL_DEVELOPMENT_PLAN.md)。上游原版说明保存在 [DiPlay README](docs/DIPLAY_UPSTREAM_README.md)。[上游同步流程](docs/UPSTREAM_SYNC.md)会定期生成待审查的合并 PR，通过完整源码检查后仍须人工合并。

构建主 APK：`gradlew.bat :mobile:assembleDebug`。源码构建默认不含 CarPlay 运行身份材料，因而 APK 能安装并不代表能与 iPhone 完成 CarPlay 连接。构建环境和本地车测方式见 [构建说明](docs/BUILD.md)。

## 来源与许可证

保留上游 Git 历史、`LICENSE` 和 [第三方声明](docs/THIRD_PARTY_NOTICES.md)。接收器主体为 GPL-3.0；部分界面代码来自 AGPL-3.0；另有单独授权的图片及第三方组件。开发阶段可以仅在本地使用。分发 APK 前须核对相应许可证、提供对应源码，并处理非商业图标、商标素材和实验性配件身份的分发边界。水果互联仅作为已安装软件的行为研究对象，不向本仓库复制其反编译代码或资源。
