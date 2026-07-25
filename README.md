<p align="center">
  <img src="assets/Snippet Studio.svg" alt="Snippet Studio Logo" width="120" height="120" error="if missing use custom style">
</p>

<h1 align="center">Snippet Studio</h1>

<p align="center">
  <strong>专为开发者与 AI 爱好者打造的高颜值、离线优先 Android 代码片段与 Prompt 工坊</strong>
</p>

<p align="center">
  <a href="https://github.com/echeao/Snippet-Studio-V1/releases"><img src="https://img.shields.io/github/v/release/echeao/Snippet-Studio-V1?style=flat-square&color=6C5CE7" alt="Latest Release"></a>
  <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform"></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"></a>
  <a href="https://www.eclipse.org/jgit/"><img src="https://img.shields.io/badge/Git%20Engine-JGit-F05032?style=flat-square&logo=git&logoColor=white" alt="JGit"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.style=flat-square" alt="License"></a>
</p>

<p align="center">
  <a href="#-核心特性">核心特性</a> •
  <a href="#-应用预览">应用预览</a> •
  <a href="#-技术栈与架构">技术栈</a> •
  <a href="#-快速开始">快速开始</a> •
  <a href="#-配置说明">配置说明</a> •
  <a href="#-路线图">路线图</a>
</p>

---

## 📖 项目简介

**Snippet Studio** 是一款基于 Android 平台构建的现代化代码片段 (Code Snippets) 与 Prompt 提示词管理工具。

在日常开发和 AI 交互中，开发者常需要随时保存、预览和跨设备同步零碎的代码块（HTML/JS/Markdown）或精雕细琢的 AI Prompts。Snippet Studio 将**离线数据安全**与**Git云端同步**完美结合，提供轻量、顺滑且极为优雅的移动端体验。

> 💡 **为什么选择 Snippet Studio？**
> - **无第三方服务绑定**：你的代码存储在你自己的 GitHub/GitLab 私有仓库或本地数据库中。
> - **即时渲染与预览**：HTML/JS 支持实时 Web 渲染预览，Markdown 支持实时富文本渲染。
> - **真正的离线优先**：无网络下依然流畅编辑与离线 Commit，恢复连线后一键 Sync/Push。

---

## ✨ 核心特性

| 模块 | 特性说明 |
| :--- | :--- |
| ⚡ **现代 UI 体验** | 基于 **Jetpack Compose + Material 3** 构建，动态色彩方案，流畅微交互动画。 |
| 🔄 **原生 Git 云同步** | 内置 **JGit 核心引擎**，支持 PAT 鉴权、Push/Pull 双向同步，具备智能合并冲突检测与自动备份机制。 |
| 📝 **多类型片段管理** | 原生支持 **HTML**（网页预览）、**JavaScript**（脚本）、**Markdown**（文档与实时渲染）、**Prompt**（AI 提示词）。 |
| 🏷️ **高效组织与检索** | 支持多标签分类 (Tags)、星标收藏 (Starred)、全局实时搜索以及带安全恢复功能的回收站 (Trash)。 |
| 🌐 **多语言国际化** | 完整支持 **简体中文 (ZH)**、**English (EN)**、**日本語 (JA)** 自适应切换。 |
| 🔒 **数据自主控制** | 本地 Room 数据库 + 沙盒 Git 仓同步，数据完全受你掌控，绝不上传第三方服务器。 |

---

## 📱 应用预览

> *(滑动或点击查看软件实际运行界面)*

| 🏠 首页概览 | 📝 实时编辑与预览 | ⚙️ Git 同步配置 |
| :---: | :---: | :---: |
| <img src="assets/screenshots/home.png" width="240" alt="Home Screen"/> | <img src="assets/screenshots/editor.png" width="240" alt="Editor Screen"/> | <img src="assets/screenshots/git_settings.png" width="240" alt="Git Settings"/> |

---

## 🛠️ 技术栈与架构

Snippet Studio 严格遵循 **Android Modern Development (MAD)** 架构标准与 Clean Architecture 规范：

```mermaid
graph TD
    UI[UI 层: Jetpack Compose + Material 3] --> VM[ViewModel 层: StateFlow / LiveData]
    VM --> Repo[Repository 层: Data Repository]
    Repo --> Local[本地数据源: Room Database / DataStore]
    Repo --> GitEng[Git 引擎: JGit Engine Sandbox]
    GitEng <--> Remote[远程仓库: GitHub / GitLab / Gitee]
```

* **Core Language**: Kotlin 2.x (Coroutines, Flow, KSP)
* **UI Framework**: Jetpack Compose, Material 3, Navigation Compose
* **Architecture**: MVVM / Single-Activity Architecture
* **Local Persistence**: Room Database (ORM), DataStore Preferences
* **Version Control**: Eclipse JGit (Pure Java Git implementation)
* **Testing Framework**: JUnit 4, Robolectric, Espresso, Roborazzi
* **CI/CD**: GitHub Actions (Auto APK Build & Release Workflow)

---

## 📥 快速开始

### 方式 1：下载安装包 (APK)

你可以直接前往 [Releases 页面](https://github.com/echeao/Snippet-Studio-V1/releases) 下载最新版本的 `app-release.apk` 并在 Android 设备上直接安装。

* **最低系统要求**：Android 8.0 (API Level 24) 或更高版本
* **推荐系统版本**：Android 13.0+

### 方式 2：从源码构建

构建项目需要安装 **Android Studio Ladybug (或更高版本)** 以及 **JDK 17+**。

1. **克隆仓库**
   ```bash
   git clone https://github.com/echeao/Snippet-Studio-V1.git
   cd Snippet-Studio-V1
   ```

2. **配置环境变量 (可选)**
   复制 `.env.example` 并重命名为 `.env`：
   ```bash
   cp .env.example .env
   ```

3. **编译并运行**
   使用 Gradle 命令行编译 Debug 包：
   ```bash
   # Windows (PowerShell)
   .\gradlew assembleDebug

   # macOS / Linux
   ./gradlew assembleDebug
   ```

---

## ⚙️ 配置说明

### 🔑 配置 Git 远程仓库同步

1. 打开应用，进入 **设置 (Settings)** 页面；
2. 填入你的远程 Git 仓库信息：
   - **Repository URL**: 例如 `https://github.com/yourname/my-snippets-repo.git`
   - **Branch**: 例如 `main` 或 `master`
   - **Personal Access Token (PAT)**: 输入具有 `repo` 读写权限的个人访问令牌
3. 点击 **测试连接**。校验成功后即可在主界面进行 Pull/Push 同步操作。

---

## 🛣️ 路线图 (Roadmap)

- [x] **v1.0.0 核心发布**
  - [x] 多类型 Snippet (HTML/JS/Markdown/Prompt) 创建与管理
  - [x] Jetpack Compose Material 3 基础 UI 框架
  - [x] 基于 JGit 的基础 Commit & Push / Pull 云同步
  - [x] 自动合并冲突处理与本地安全备份
  - [x] 多语言支持 (ZH / EN / JA)
  - [x] GitHub Actions CI/CD 打包发布
- [ ] **v1.1.0 演进计划 (进行中)**
  - [ ] 集成 Monaco Editor / CodeMirror 网页端高级代码编辑器（支持代码高亮与自动补全）
  - [ ] 代码片段标签图谱与多维筛选过滤
  - [ ] 导出/导入 JSON 与 Zip 压缩包本地备份
  - [ ] 支持 Gist / GitHub GraphQL API 快捷同步

---

## 🤝 参与贡献

欢迎对 Snippet Studio 提交 Issue 或 Pull Request！

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的修改 (`git commit -m 'feat: 增加动态代码高亮支持'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 发起 Pull Request

> ⚠️ **提交规范**：请确保 Commit Message 使用**简体中文**编写，且代码符合 Kotlin 官方编码规范。

---

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 协议开源，你可以自由使用、修改与分发。

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/echeao">echeao</a> & Antigravity AI
</p>
