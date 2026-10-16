# DiPlay 上游同步

`develop` 保存 dwcoffee 的车型适配、文案和图标。原项目 `shihabal3amri/DiPlay` 的 `main` 是上游来源。同步采用 Git 合并，保留双方提交历史，不用上游文件直接覆盖本项目。

## 自动检查

[同步工作流](../.github/workflows/sync-upstream.yml)每天检查一次上游，也可从 GitHub Actions 手动运行。发现新提交后，它在临时分支合并 `upstream/main`，先执行完整 Android 源码测试、Lint 和构建。通过后，工作流更新 `sync/diplay-upstream` 分支并向 `develop` 创建 PR。它不会自动合并或发布 APK。

如有合并冲突或检查失败，工作流失败且不会更新 PR。需要人工解决冲突，再运行同一套检查。合并 PR 前还需复核 BYD 与 GWM WEY 的功能和车型文案；自动检查不能代替实车回归。

`origin` 为 `wakamda6/dwcoffee`，`upstream` 为只读的 DiPlay 原仓库。启用 GitHub 定时运行时，须让工作流文件存在于仓库默认分支，并在仓库的 Actions 设置中允许工作流创建 PR。完整检查在同步工作流的只读 `verify` 任务中运行；由 `GITHUB_TOKEN` 创建的 PR 若触发额外的 PR 工作流，GitHub 会要求有写入权限的成员批准该工作流运行。

## 本地手动同步

```sh
git checkout develop
git fetch upstream main
git switch -c sync/diplay-manual
git merge --no-ff upstream/main
```

解决冲突后运行 [Android CI 命令](../.github/workflows/android.yml)，确认应用 ID 仍为 `com.wakamda.dwcoffee`、Coffee 车型功能仍可见，再将同步分支合并到 `develop`。

源代码同步与应用内更新是两件事：应用更新检查指向 `wakamda6/dwcoffee` 的 GitHub Releases，不会把上游 DiPlay APK 当作本项目更新。每次发布须上传签名的 dwcoffee APK 和对应的 `SHA256SUMS.txt`；新版本的前三段跟随上游，第四段为本项目修订号。
