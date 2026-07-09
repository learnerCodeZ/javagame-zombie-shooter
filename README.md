# 🧟 打僵尸射击游戏 · Zombie Shooter

> 基于 **Java Swing** 的 2D 射击小游戏：鼠标瞄准、点击射击、消灭僵尸；带账号登录、主菜单与基于 MySQL 的战绩排行榜。
> 一个同时覆盖「Java 程序设计」与「数据库原理与应用」的完整项目。

![Java](https://img.shields.io/badge/Java-25-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1)
![Maven](https://img.shields.io/badge/Maven-build-C71A36)
![GUI](https://img.shields.io/badge/GUI-Swing-6DB33F)
![Platform](https://img.shields.io/badge/platform-Windows-lightgrey)
![Status](https://img.shields.io/badge/进度-阶段⑥a完成-brightgreen)

---

## 目录

- [简介](#简介)
- [✨ 功能特性](#-功能特性)
- [🛠 技术栈](#-技术栈)
- [📁 项目结构](#-项目结构)
- [🚀 快速开始](#-快速开始)
- [⚙️ 配置](#️-配置)
- [🗄️ 数据库](#️-数据库)
- [🎮 使用说明](#-使用说明)
- [📜 脚本](#-脚本)
- [🧑‍💻 开发](#-开发)
- [📈 开发路线与进度](#-开发路线与进度)
- [📚 文档](#-文档)
- [📄 许可证](#-许可证)

---

## 简介

本项目由一张 Scratch 卡通射击游戏截图（橙色猫持枪打绿色僵尸）启发，用 **Java** 重写并扩展为一个带账号体系的完整桌面游戏：玩家登录后进入主菜单，开始游戏时用鼠标瞄准并射击不断涌来的僵尸，击杀得分、被撞扣血；游戏结束后战绩写入 **MySQL**，可在排行榜查看。

整个项目按软件工程流程分阶段推进：**需求分析 → 系统设计 → 数据层 → 界面层 → 游戏核心 → 排行榜 → 打磨 → 测试**，每阶段产出可独立验收的文档与代码。

## ✨ 功能特性

- 🔐 **用户系统**：注册 / 登录 / 退出登录，密码 MD5 加密存储
- 🧭 **主菜单**：开始游戏 / 排行榜 / 游戏说明 / 设置 / 退出
- 🎯 **射击玩法**：WASD 移动 + 鼠标瞄准射击、击杀得分、血条 HUD、难度递增、粒子震屏等打击感；局内可"退出本局"结算存档
- 🗄️ **数据持久化**：MySQL 存用户与战绩，JDBC + PreparedStatement（防注入）
- 🏆 **排行榜**：全局榜 + 我的记录，按分数倒序
- 🎨 **Swing 桌面 GUI**：系统原生外观、卡片式登录窗

## 🛠 技术栈

| 层 | 技术 |
|---|---|
| 语言 | Java（开发用 JDK 25，可在 `pom.xml` 调低以兼容 8/11/17） |
| 图形界面 | Java Swing + Java 2D（AWT） |
| 数据库 | MySQL 8.x |
| 数据访问 | JDBC（`mysql-connector-j` 8.0.33） |
| 构建 | Maven（`pom.xml` 管理依赖） |
| 文档/建模 | Markdown + Mermaid（类图 / 流程图 / ER 图） |

## 📁 项目结构

```
Game/
├── pom.xml                         # Maven 配置 + MySQL 驱动依赖
├── run.bat                         # Windows 一键编译运行脚本
├── README.md                       # 本文件
├── AGENTS.md                       # AI 助手工作约定
├── docs/                           # 设计文档
│   ├── 项目计划书.md
│   ├── 需求规格说明书.md
│   ├── 详细设计说明书.md           # UML 类图 / 流程图 / 时序图（Mermaid）
│   └── 数据库设计文档.md           # ER 图 / 表结构 / 数据字典
├── notes/                          # 学习笔记 + 阶段复盘
│   ├── 学习笔记_JDBC与MySQL驱动与依赖管理.md
│   ├── 学习笔记_Agent工作流Workflow.md
│   ├── 学习笔记_Windows批处理bat.md
│   └── v1.0_PhaseSummary/          # 各阶段复盘总结
├── sql/
│   └── schema.sql                  # 建库建表 + 测试数据
├── lib/
│   └── mysql-connector-j.jar       # 仅供 run.bat 命令行运行用
└── src/main/
    ├── java/com/game/
    │   ├── GameApp.java            # 程序入口
    │   ├── TestDao.java            # 数据层验收测试
    │   ├── ui/                     # 界面层：LoginFrame / RegisterFrame / MainFrame
    │   ├── game/                   # 游戏层：Player/Zombie/Bullet/GamePanel/GameController
    │   ├── model/                  # 实体：User / GameRecord
    │   ├── dao/                    # 数据访问：DBUtil / UserDao / RecordDao
    │   └── util/                   # 工具：MD5Util
    └── resources/
        └── db.properties           # 数据库连接配置
```

> `out/` 为编译产物（由 `run.bat` 生成），可不提交。

## 🚀 快速开始

### 环境要求
- **JDK 8+**（开发用 25；课设机器若为 8/11/17，把 `pom.xml` 里 `<release>25</release>` 和 `source/target` 改成对应版本即可）
- **MySQL 8.x** 已安装并运行
- Windows 系统（跨平台运行需自行适配 `run.bat`）

### 方式一：`run.bat`（Windows 最快）
1. 确认 MySQL 已启动，且 `src/main/resources/db.properties` 里的密码改成本机 root 密码。
2. 双击 `run.bat`，或在终端进入项目目录执行 `run.bat`。
3. 登录窗口弹出 → 用测试账号登录。

### 方式二：IntelliJ IDEA（推荐开发用）
1. `File → Open` 选择 `D:\MYCODE\Game`（或直接选 `pom.xml`）。
2. 等待 Maven 自动下载驱动（右下角进度条）。
3. 运行 `com.game.GameApp`（在 `GameApp.java` 的 `main` 上右键 Run）。

### 方式三：Maven 命令行
```bash
mvn compile
mvn -q exec:java -Dexec.mainClass="com.game.GameApp"
```
（需本机已安装 Maven；IDEA 自带 Maven。）

## ⚙️ 配置

数据库连接信息在 [`src/main/resources/db.properties`](src/main/resources/db.properties)（**改密码只改这里，不用动代码**）。

> ⚠️ 仓库里提供的是模板 [`db.properties.example`](src/main/resources/db.properties.example)：首次使用先复制为 `db.properties` 并填入你的 MySQL 密码。`db.properties` 已在 `.gitignore` 中，**不会进仓库，密码安全**。

```properties
url=jdbc:mysql://localhost:3306/game_db?serverTimezone=Asia/Shanghai&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true
user=root
password=你的密码
```

## 🗄️ 数据库

执行 [`sql/schema.sql`](sql/schema.sql) 建库建表并写入测试数据（可重复执行）：

```bash
mysql --default-character-set=utf8mb4 -uroot -p < sql/schema.sql
```

两表关系：`user` **1 — N** `game_record`（外键 `user_id`）。详见 [数据库设计文档](docs/数据库设计文档.md)。

## 🎮 使用说明

启动后用以下任一测试账号登录（密码均为 `123456`）：

| 用户名 | 昵称 |
|---|---|
| `admin` | 管理员 |
| `player1` | 张三 |
| `player2` | 李四 |

也可点「去注册」自行创建账号。

## 📜 脚本

| 脚本 | 作用 |
|---|---|
| `run.bat` | Windows 下：编译全部源码 + 配置 classpath（驱动 jar / db.properties）+ 运行 `GameApp` |

> `lib/` 下的驱动 jar **仅为 `run.bat` 命令行运行服务**；IDEA / Maven 各自管理依赖，不依赖此目录。

## 🧑‍💻 开发

### 构建
```bash
mvn clean compile        # 编译
mvn package              # 打包
```

### 代码规范
- **缩进**：4 空格（锯齿型书写格式），K&R 花括号
- **命名**：类名 PascalCase，方法/变量 camelCase，常量全大写下划线
- **包名**：`com.game.{ui|game|model|dao|util}`
- **注释**：Javadoc + 中文行内注释；文件 UTF-8 编码
- **SQL**：一律 `PreparedStatement`（防注入）；密码一律 MD5

## 📈 开发路线与进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| ① 需求与设计 | 需求/设计/数据库三文档 + schema.sql | ✅ 完成 |
| ② 数据层 | JDBC 连库、DAO、注册/登录跑通 | ✅ 完成 |
| ③ 界面层 | Swing 登录/注册/主菜单 | ✅ 完成 |
| ④ 游戏核心 | 玩家/僵尸/子弹/主循环/碰撞/计分/结算/局内退出 | ✅ 完成 |
| ⑤ 排行榜 | LeaderboardFrame + JOIN 取昵称 | ✅ 完成 |
| ⑥ 打磨 | 6a 玩法+视觉(移动/追踪/难度/血条/打击感/Brute)✅；6b 音效+设置 ⏳ | 🚧 进行中 |
| ⑦ 测试与文档 | 自测、补文档、正式图 | ⏳ 待开始 |
| ⑧ 答辩准备 | 演示、讲解、PPT | ⏳ 待开始 |

## 📚 文档

- 设计文档：[`docs/`](docs/)（计划书 / 需求 / 详细设计 / 数据库设计）
- 阶段复盘：[`notes/v1.0_PhaseSummary/`](notes/v1.0_PhaseSummary/)
- 学习笔记：[`notes/`](notes/)（JDBC 与驱动、Agent 工作流、Windows bat）

## 📄 许可证

本项目为课程设计用途，仅供学习参考。
