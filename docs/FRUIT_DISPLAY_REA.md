# 水果互联仪表与 HUD 显示链路：REA 工作记录

状态：静态链路已追踪到 Android 渲染入口；原生实现、运行时帧和光学 HUD 效果待验证。日期：2026-10-15。

## 目标与制品

分析对象是工作区 `../../analysis/shuiguo/runtime/shuiguo-runtime.apk`，由用户提供的水果互联 1.204 APK 与运行时恢复的业务 DEX 组成，仅作离线分析。原 APK 和载体哈希、提取限制见 `../../docs/SHUIGUO_REA_FEATURE_ANALYSIS.md`。本记录中的类名是混淆后名称，REA/JADX 输出是反编译推断，不是项目源码，也不能保证还原原生库的所有分支。

实施目标是复现可观察的显示行为和输入输出契约。OpenCarLink 不复制水果互联的 DEX、原生库、布局、图片或授权机制；CarPlay 连接与鉴权沿用本项目现有接口。

## 链路 A：仪表地图

```text
DisplayManager 枚举 → 仪表目标选择 → 副屏尺寸/区域配置
→ CarPlay 第二视频流 111 → 压缩帧 → MediaCodec → Surface
→ 仪表 Display 上的 Presentation/TextureView
```

| 环节 | 静态结论 | REA 证据 |
| --- | --- | --- |
| 显示选择 | `CarplayService.onCreate/v` 读取仪表设置并从非默认 Display 中选择目标；`A` 用显示尺寸和可选区域调用 `setAltResolutions`。 | `ev_e55de701a9f3b059478f667ac09dd15d050ea5a87663147a9c8afeaa3c71805a`、`ev_242a3f14f3be8d3c4eb8cc4b86789a41628c1442384e0c79f50c3c722f006ba2`、`ev_ebbf271ae1c8be3206fb9e23ad910ed7e88d6ec2fe138d7ee215b8e5e106bf45` |
| 窗口与解码 | `p` 在仪表 Display 创建 `a3.n0` Presentation，布局含全屏 TextureView；Surface 送给副屏解码器。 | `ev_12d94da54ee6a8fc3a39b87a08f05da25650ce0e12eacac2e6bfd7f95b0f8bca`、`ev_2966e85cb9cb631f0bd2e2da7d26c769370b37096f3f19125e1fcc1317ee548a`、`ev_bd00183c0e24e30cf9fa0619a848f914db9bd73e630435d0a09e1f01293d2eec` |
| 视频来源 | `CarplayNative.onVideoDataCallback` 将 110 分给主画面、111 分给副屏；副屏经 H.264/HEVC MediaCodec 直接输出 Surface。它不是主屏截图镜像。 | `ev_a62166e8ca2d26993a08d0c654826cdb743617747313c5890d2c42b23eb6e1cf`、`ev_064076bb45a9642371c9c9b163fd67808a7df55a13601b38e18e18ff110c6526` |
| 退出清理 | 服务销毁时撤销 DisplayListener 并 dismiss 仪表 Presentation。 | `ev_b88ad58e99eb93a962f26b4915928628eeac5bcc0400f2bc8b99cdfce199ac83` |

拿铁实车历史记录显示 Display 3 为 1920×384，水果互联能在其上建立全屏层；这只证明 Android 合成层可覆盖原厂窗口，不能证明原厂安全告警优先级或长期驾驶安全。OpenCarLink 已有第二流 111 和 `ClusterMapPresentation`，需解除其 DiLink 尺寸、显示名与原厂投屏模式假设。

## 链路 B：HUD 信息

```text
iAP2/原生回调：转向、路线、车道、媒体、通话
→ Java Handler/导航字段解析 → 结构化状态
→ HUD Display 上的独立 Presentation → 文本/图标/进度视图
```

| 环节 | 静态结论 | REA 证据 |
| --- | --- | --- |
| 目标屏与窗口 | `CarplayService.w` 选择非默认 HUD Display，`q` 建立 `a3.i` Presentation 并将其交给导航解析器。HUD 不接第二视频流。 | `ev_bedbacb6d2d7138993fbf40419cb322d5ebf8f97f0141554850ee4f224cc88fc`、`ev_a48857c2a5144a33831d09a05039ae423e6c1f4e140aa1a52a1cdc25b2616b84` |
| 导航回调 | `CarplayNative` 的转向、路线、车道回调分别投递 Handler 消息 1、2、3；`e3.o.a/b/c` 解析长度/字段编号组成的记录。路线按 DisplayComponentID 和 index 关联转向及车道信息。 | `ev_85df05c6cc0384f2dbf0951192ff80a5f8164eb7be3d69a3a3d0b262f2d15ff9`、`ev_f3b0184b427f700f7f027e1281fd23f49984dcedbfd90545f4feb06f04e1b779`、`ev_1706891a54fc6cfc0ea9f348516e33cebc23ce298a83a7dd2ec41f7e9777f5bc`、`ev_0cdbeef7f187d0a57243e55f3d214c1647dab739b2e35fe1f78312b6db1f385c`、`ev_891dfa69e823123706d4855d245d2b5c1014456b1c144b6da3097b1f61142fc7`、`ev_c7813a80c2a0d47aca6fb31a8c0e70969469e547e9653ab9519cc4fb1a63a7b1` |
| 导航渲染 | 路线状态可清空或显示到达；活动导航显示下一动作图标、道路名、距离、预计到达、车道图。状态经 UI 线程投递。 | `ev_dd242f1ec197bc10ece9fb829cb40fc88c53a1e06018269c4463a569a473ba00`（case 17/18）、`ev_6bb3196b44e838ae96ccdd46218c9bc13b4852e47965106fff04a36877b73e84` |
| 媒体与通话 | 媒体回调经服务 Handler 更新标题、艺人、时长、进度；通话回调更新短暂的状态文字。 | `ev_ff3cd79aa1107532173c1753739446ce1e192a1d5d82fbb35a4c88f06912a189`、`ev_dcf175492a5d0c9ed3ace9e178532cf70bacd1a5b4056487ab95b2b4d89021e3`（case 18）、`ev_4b8e22544f608a8461dfb75eff163b7576a7b3a94aacbd3d023e442d3bae36e2`、`ev_9978f7d71e104fed6557899318daadfb55ac601ea24fadacb9bab2098ad8988a` |
| 退出清理 | 服务销毁时 dismiss HUD Presentation。 | `ev_b88ad58e99eb93a962f26b4915928628eeac5bcc0400f2bc8b99cdfce199ac83` |

`f3.g` 还向 `AUTONAVI_STANDARD_BROADCAST_SEND` 发送特定状态广播；这是一条厂商/地图应用桥接路径，与独立 HUD Presentation 不同。通用输出不使用该广播，证据为 `ev_6211fa5a698002d956eeff0ca0b3d659b8795f29897e4e65f51069021edd33ee`。

## 对 OpenCarLink 的实现约束

1. 第二流和 HUD 状态分开建模。仪表输出持有视频 Surface；HUD 输出消费导航、媒体、通话状态，不依赖仪表 Surface 是否存在。
2. 通用 Display 发现依据运行时能力、尺寸、唯一性和显式用户选择；拿铁 3/4 是候选配置，不是通用固定 ID。歧义时不占用原厂屏。
3. 先从启动与帧分发中解耦 `BydNavigationOutputs`，将导航状态发布成车厂无关接口；BYD/FCE 桥接不得随通用模式启动。按依赖图移除专属模块，保留 iAP2 解析、第二流、主屏、音频、输入和诊断接口。
4. 无帧、断连、Display 移除、后台退出和异常均应释放输出。HUD 字段缺失即隐藏；仪表首帧未到时不持续盖住原厂信息。
5. 使用本项目自己的布局与图标。水果互联反编译值仅用于确定字段语义，不能把其资源或源码直接纳入仓库。

## 尚未闭合的证据

- JNI 内的 CarPlay 会话协商和全部字段来源无法由当前 Java DEX 证明。DiPlay 已有对应协议解析，但需用接收帧日志核对实际 iPhone 和地图 App 的字段。
- 当前没有已授权、可连接的水果互联实时会话帧记录；静态解析覆盖的是可见 Java 分支，不能声称“完整获取原始代码”。
- Display 4 上建立窗口不等于 HUD 光学投影可见。需静止实车确认 HUD 开关、窗口层级、焦距/亮度及原厂提示优先级。
- BYD 逻辑嵌在 `CarPlayHostActivity`、`BydNavigationOutputs`、`CarPlayClusterDisplay` 与设置页等多处。删除前须先建立通用输出边界并做依赖清单，避免误删第二流和导航解析。

下一实施节点：用本地测试 APK 在停车状态核对拿铁 Display 3/4 的画面、第二流是否出现、HUD 光学可见性、断连清理和原厂告警叠层；再完善通用输出接口与独立 HUD 设置，逐段移除 BYD 调用。当前原型只消费项目已有导航数据，没有复制水果互联实现。
