# OpenCarLink

OpenCarLink 是基于 [DiPlay](https://github.com/shihabal3amri/DiPlay) 的通用 Android 车机 CarPlay 接收器开发项目。当前处于项目初始化阶段：保留 DiPlay 0.2.20 的接收、连接、音视频和现有车型代码，以长城拿铁 DHT 2022 为第一个新增适配目标。**目前尚未实现或验证拿铁仪表地图及 HUD 输出。**

## 当前基线

- 上游：DiPlay，起点提交 `969f871433b76f6c59f3c93619f7cb5872fdd15d`。
- 主应用模块：`:mobile`；库模块：`:common`、`:shared`。
- 应用 ID：`org.opencarlink.receiver`；调试版：`org.opencarlink.receiver.hudtest`。两者与原 DiPlay 可并存。
- 本地开发分支：`develop`；`upstream` 指向 DiPlay 原仓库。
- 当前更改只涉及项目身份、应用间 action 命名空间和初始文档；尚未改动 CarPlay 协议核心。

## 从哪里开始

完整目标、证据、架构、阶段任务和验收标准见 [初始开发方案](docs/INITIAL_DEVELOPMENT_PLAN.md)。上游原版说明保存在 [DiPlay README](docs/DIPLAY_UPSTREAM_README.md)，现有上游功能和说明仍可作为代码参考，但不能当作 OpenCarLink 的已验证能力。

构建主 APK：`gradlew.bat :mobile:assembleDebug`。源码构建默认不含 CarPlay 运行身份材料，因而 APK 能安装并不代表能与 iPhone 完成 CarPlay 连接。构建环境和本地车测方式见 [构建说明](docs/BUILD.md)。

## 来源与许可证

保留上游 Git 历史、`LICENSE` 和 [第三方声明](docs/THIRD_PARTY_NOTICES.md)。接收器主体为 GPL-3.0；部分界面代码来自 AGPL-3.0；另有单独授权的图片及第三方组件。开发阶段可以仅在本地使用。分发 APK 前须核对相应许可证、提供对应源码，并处理非商业图标、商标素材和实验性配件身份的分发边界。水果互联仅作为已安装软件的行为研究对象，不向本仓库复制其反编译代码或资源。
