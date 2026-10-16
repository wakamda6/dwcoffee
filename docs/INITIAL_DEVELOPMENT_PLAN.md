# dwcoffee 初始开发方案

状态：项目基线已建立；拿铁显示原型已由用户在实车验证仪表地图与 HUD 导航内容可见。日期：2026-10-15。

## 1. 目标与边界

目标是把 DiPlay 改造成可扩展的 Android 车机 CarPlay 接收器，而非以比亚迪为默认前提的单车型应用。保留现有 CarPlay 连接、鉴权接口、iAP2/AirPlay、音视频解码、输入和诊断能力，新增按车机能力选择的仪表、HUD、媒体、通话及车辆数据输出。第一个适配目标是长城拿铁 DHT 2022。通用运行路径不调用 BYD/FCE 服务或车机命令；迁移期间先隔离原实现并保持编译，待通用替代路径和回归检查成立后删除项目中的车型专属功能。

当前基线：DiPlay `969f871433b76f6c59f3c93619f7cb5872fdd15d`（0.2.20），`develop` 为本地开发分支。`upstream` 指向原项目。项目名为 dwcoffee，应用 ID 为 `com.wakamda.dwcoffee`。

初始化验证（2026-10-15）：在 Windows、Temurin JDK 21、Android SDK Platform 37.0 和 NDK 28.2.13676358 下，上游 `AGENTS.md` 规定的完整 CI 命令通过：`:shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest :mobile:lintDebug :home:lintDebug :maphost:lintDebug :mobile:assembleDebug :home:assembleDebug :maphost:assembleDebug`。本机须先设置 `DIPLAY_TEST_SHELL` 为 Git for Windows 的 `sh.exe`；未设置时 `UsbPermissionSetupTest` 的 4 个 shell 夹具测试因找不到 `sh` 而失败。当时生成的主调试 APK 包名经 `aapt dump badging` 核验为 `org.opencarlink.receiver.hudtest`。这些结果只证明当时的源码构建与本地测试通过，尚未在车机和 iPhone 上验证新包。

首个拿铁原型：`GwmLatteDisplays` 仅在 Presentation 列表同时存在 Display 3（1920×384）与 Display 4（800×480）时选择它们。仪表复用 DiPlay 的第二流 111，仅在流激活时打开 Presentation；HUD 使用独立开关，在 Display 4 绘制 CarPlay 导航、媒体、通话及车道信息，并提供布局设置和透明叠加原车 HUD 模式。用户实车测试报告：仪表显示高德导航页面、HUD 显示导航参数；导航未开启时 HUD 开关也可立即投屏，设置期间持续显示，透明叠加模式正常。尚缺媒体、通话、车道信息逐项验证，以及断连、原厂告警优先级和跨固件回归记录。

本地装车测试构建（2026-10-15）：从工作区提供的 DiPlay 0.2.19 APK 中取出其两项运行时鉴权材料，仅存入 Git 忽略的 `.private/runtime-assets/`，用 `DIPLAY_AUTH_ASSETS_DIR` 构建 `:mobile:assembleStandaloneDebug`。核对输出 APK 内两项资源与输入逐字节哈希一致；完整 CI 同时通过。这个结果不证明该身份在当前 iPhone/车机上可用，也不授予公开分发该身份的权利。

## 2. 事实与证据

| 项目 | 已确认事实 | 尚待验证 |
| --- | --- | --- |
| 拿铁显示拓扑 | Android 9 车机有五个逻辑 Display；Display 3 为 1920×384 仪表弹窗层，Display 4 为 800×480 HUD 层；均声明 Presentation 能力 | 固件更新、休眠/唤醒后编号和可见性是否变化 |
| 水果互联仪表 | 实车上普通应用在 Display 3 创建全屏 Presentation，SurfaceFlinger 观察到不透明 1920×384 buffer，原厂窗口仍在下层 | 断连、崩溃和原厂高优先级告警时的恢复行为 |
| 水果互联 HUD | REA 静态代码显示独立 Presentation，布局含导航、车道、媒体、通话控件；实车日志曾观察到 Display 4 窗口建立 | 目标车的 HUD 光学屏是否实际显示，以及内容层级和可读性 |
| DiPlay 仪表视频 | 上游已有第二视频流 111、仪表显示配置和 Surface 解码路径；当前显示选择偏向 BYD | 拿铁适配后 iPhone 是否稳定提供第二流、画面尺寸与帧率 |
| DiPlay 导航数据 | 上游已有 iAP2 导航引导解析和 `ClusterTurnGuidance` 数据 | 地图 App/iOS 版本对 HUD 字段的提供程度 |

本仓库之外的工作区资料：`../../docs/SHUIGUO_REA_FEATURE_ANALYSIS.md`、`../../docs/SHUIGUO_REALCAR_REA_SUPPLEMENT.md`、`../../docs/LATTE_DHT_2022_PLATFORM_PROFILE.md`。关键 REA 证据包括 `ev_a62166e8ca2d26993a08d0c654826cdb743617747313c5890d2c42b23eb6e1cf`（视频流分路）、`ev_12d94da54ee6a8fc3a39b87a08f05da25650ce0e12eacac2e6bfd7f95b0f8bca`（仪表 Presentation）、`ev_6bb3196b44e838ae96ccdd46218c9bc13b4852e47965106fff04a36877b73e84`（HUD 布局）。REA 静态结果不能代替实车显示验证。

## 3. 拟定架构

```text
iPhone / CarPlay
    │
    ├─ 主画面、音频、输入 ── 现有 :shared / :common 核心
    ├─ 第二视频流 111 ───── 仪表视频输出接口 ── 车型 Display 适配器
    └─ 导航/媒体/通话数据 ── 结构化状态 ─────── HUD/原厂数据适配器
                                                     │
                                BYD / GWM Latte / 通用副屏 / 无副屏
```

能力模型应明确报告 `clusterVideo`、`hudGuidance`、`media`、`calls`、`vehicleData` 是否可用及其验证等级。车型识别只用于推荐配置，最终目标 Display 必须在运行时检查 `isValid`、尺寸、名称/系统属性和权限结果；不能把逻辑 ID 3/4 当作所有固件的常量。没有匹配或匹配歧义时保持原厂显示，诊断页给出原因。

建议优先抽取以下边界，具体接口形态以代码改动时的调用链为准：

- `VehicleProfile`：探测固件与显示能力，提供候选屏和安全区域。
- `ClusterVideoOutput`：接收 CarPlay 第二流的 Surface，负责 Presentation 生命周期、显示变化及首帧状态。
- `GuidanceOutput`：消费现有导航数据，分别支持 HUD Presentation 或经验证的原厂数据通道。
- `OutputCoordinator`：处理连接、断连、超时、后台、Display 移除及用户开关；输出故障时立即释放副屏窗口。

先以 [水果互联显示链路 REA](FRUIT_DISPLAY_REA.md) 确定输出行为和字段，再复用 DiPlay 的 [仪表窗口](../common/src/main/java/com/shilapi/xcertplay/ClusterMapPresentation.kt)、[主界面编排](../common/src/main/java/com/shilapi/xcertplay/CarPlayHostActivity.kt)、[视频配置](../shared/src/main/java/com/shilapi/xcertplay/airplay/CarPlayClusterDisplay.kt) 和已有导航解析能力。先从启动、流控制和显示选择中剥离 BYD 依赖，再删除专属 Binder、广播、屏幕名、ADB 命令、设置和资源。原厂签名权限和车辆控制接口不作为第一版依赖。

## 4. 分阶段交付

### P0：可重复的独立基线

- 保留上游历史、许可证、第三方声明；建立独立应用 ID、显示名称和应用间 action。
- 跑通源码 APK 构建及上游 CI 检查，记录工具链版本和结果。先不宣称 CarPlay 实车功能已回归。
- 用现有 DiPlay 和新包并存安装，确认调试包的启动、连接、主画面与音频基线；运行身份材料始终放在仓库外。

### P0.5：REA 还原两条显示链路

- 针对已提供的水果互联 APK，追踪仪表 Display 选择、分辨率协商、第二流 111、Surface/解码器、窗口显示和释放。
- 追踪 HUD 的导航/车道、媒体、通话事件，从原生回调到解析、状态、Presentation 渲染和退出清理；为每段记录证据、字段和未知项。
- 形成可实现的行为规格，不以反编译文本充当原始源码。原生库内的握手细节和真实 HUD 可见性要分别用运行记录及静止实车验证。

### P1：通用能力层与 BYD 剥离

- 抽取车型能力与显示发现接口；通用模式不启动 BYD/FCE 输出。按依赖顺序移除 BYD/FCE 专属模块，保持 CarPlay 核心接口可编译、可回归。
- 设置页按能力展示车型相关功能；没有仪表/HUD 时不显示无效开关。
- 诊断输出候选 Display、选择理由、视频流 111 状态、Surface 生命周期和退出原因。

### P2：拿铁仪表地图

- 以当前车机的 Display 3（1920×384）为候选，仅在实际拓扑匹配且用户启用时创建 Presentation。
- 复用第二视频流与解码器；按 5:1 屏幕比例设置流尺寸和安全区域，不直接套用 BYD 的 8:3 参数。
- 首帧前不长期覆盖原厂仪表；无帧、断连、后台、显示移除或异常时释放窗口。提供清晰的关闭入口和诊断记录。
- 停车验证启动、旋转/分辨率、导航开始/结束、睡眠唤醒、进程退出与告警叠层。

### P3：拿铁 HUD 信息

- 独立在 HUD Display 4 上渲染简洁的转向、距离、道路信息；根据实测再考虑车道、媒体与通话。
- 使用 DiPlay 的结构化数据，不复用水果互联代码或资源。字段缺失时隐藏对应元素，不生成臆测值。
- 实测 HUD 光学画面、亮度/昼夜、遮挡、断连清理与原厂提示优先级。若 Presentation 不可见，再研究原厂导航桥接的可公开调用接口。

### P4：车型扩展与发布准备

- 清理残留 BYD/FCE 设置、资源和服务；用能力接口接入未来车型，不把拿铁的 Display ID 写成通用常量。
- 整理第三方素材授权，替换非商业 BYD 导航图标及需重新授权的品牌素材；审查应用商标和实验性配件身份。
- 发布 APK 时同步提供准确的对应源码、构建说明和许可证声明。

## 5. 验收标准与风险

首个拿铁原型的最低验收：新包能与现有 DiPlay 并存；主 CarPlay 不退化；仪表在导航时显示第二流且原厂重要提示仍可见；HUD 在实体投影上显示正确导航信息；断连/崩溃/熄火恢复后不遗留覆盖窗口。每项都应记录 Android/车机固件、iOS 与地图 App 版本、显示拓扑及测试视频/日志摘要。行驶信息屏的首次测试只在停车或台架进行。

最大风险是全屏 Presentation 覆盖原厂仪表内容；水果互联的实车证据证明它能够覆盖，但这不等于适合长期驾驶使用。第一版应优先限制区域和持续时间，验证原厂告警与速度信息的可见性，失败时回到原厂显示。HUD 可见性、iPhone 第二流和固件兼容性目前均不能承诺。

## 6. 许可证与来源规则

本项目沿用上游 GPL-3.0 历史；`DiPlayActivity.kt` 等特定代码另有 AGPL-3.0 声明。保留 [第三方声明](THIRD_PARTY_NOTICES.md) 和原许可文本。只在本地开发、个人使用不要求公开仓库；分发 APK 时按适用许可证向接收者提供对应源码和构建材料。独立私有开发仓库可以作为工作区，不能把私有仓库本身当作向 APK 接收者履行源码义务。

水果互联 APK 的逆向结果仅用于确定功能行为、显示通道和字段需求；不复制其 DEX/原生代码、图片、布局或授权/支付实现。原 DiPlay 公开 APK 中的实验性配件身份、Apple 图标与非商业 BYD 图标也不自动取得新项目的分发授权。发布决策另行做素材和凭据审查。
