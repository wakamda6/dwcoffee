# DiPlay 上游同步

`develop` 保存 dwcoffee 的车型适配、文案和图标。当前上游基线固定为 DiPlay 0.2.20（`969f871433b76f6c59f3c93619f7cb5872fdd15d`）。项目不定时检查或自动合并原仓库更新；需要更新时再手动决定目标版本，并用 Git 合并保留双方提交历史。

## 以后需要时手动同步

`origin` 为 `wakamda6/dwcoffee`，`upstream` 为只读的 DiPlay 原仓库。确认要更新的上游版本后，在独立分支合并对应提交或标签；不要直接把上游文件覆盖到 `develop`。

```sh
git checkout develop
git fetch upstream main
git switch -c sync/diplay-manual
git merge --no-ff <确认过的上游提交或标签>
```

解决冲突后运行 [本地构建与检查](BUILD.md)，确认应用 ID 仍为 `com.wakamda.dwcoffee`、Coffee 车型功能仍可见，并复核 BYD 与 GWM WEY 的功能和车型文案，再将同步分支合并到 `develop`。

源代码同步与应用内更新是两件事：应用更新检查指向 `wakamda6/dwcoffee` 的 GitHub Releases，不会把上游 DiPlay APK 当作本项目更新。每次发布须上传签名的 dwcoffee APK 和对应的 `SHA256SUMS.txt`；新版本的前三段跟随上游，第四段为本项目修订号。
