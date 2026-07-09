# AGENTS.md

> 本文件给在本仓库工作的 AI 助手（Claude Code 等）提供项目约定。开始动手前先读完。

## 项目概述

「打僵尸射击游戏」：Java Swing 2D 射击小游戏 + MySQL 账号/战绩系统。一个同时覆盖 Java 程序设计与数据库原理的课程设计项目。按软件工程流程**分阶段开发**，每阶段产出可独立验收的文档与代码。

## 技术栈与环境

- **语言**：Java（开发机为 JDK 25；可在 `pom.xml` 调低兼容 8/11/17）
- **GUI**：Java Swing + AWT（`javax.swing.Timer` 做游戏主循环）
- **数据库**：MySQL 8.x，库名 `game_db`
- **数据访问**：JDBC（`mysql-connector-j` 8.0.33），全部用 `PreparedStatement`
- **构建**：Maven（`pom.xml` 声明驱动依赖）
- **运行平台**：Windows

## 项目布局（分层，单向依赖）

```
src/main/java/com/game/
├── GameApp.java        # 入口：设系统外观 + 打开 LoginFrame
├── TestDao.java        # 数据层验收测试（带 main）
├── ui/                 # 界面层：LoginFrame / RegisterFrame / MainFrame / LeaderboardFrame
├── game/               # 游戏层：Player / Zombie / Bullet / GamePanel / GameController / GameWindow
├── model/              # 实体（POJO）：User / GameRecord
├── dao/                # 数据访问：DBUtil / UserDao / RecordDao
└── util/               # 工具：MD5Util
```

**依赖方向**：`ui` / `game` → `dao` → `model`；`dao` → `util`、`DBUtil`。**不要反向依赖**（dao 不该 import ui）。

## 构建与运行

- **IDEA**：打开 `pom.xml`，运行 `com.game.GameApp`（Maven 自动下驱动）。
- **命令行（Windows）**：运行 `run.bat`（编译全工程 + 配 classpath + 启动）。
- **手写 javac/java**：编译用 `-encoding UTF-8 -d out -cp lib/*` + 全部源文件；**运行时 classpath 必须同时含** `out`、`lib/*`（驱动 jar）、`src/main/resources`（db.properties），三者缺一不可。
- `lib/` 下的驱动 jar **仅供 `run.bat` 命令行运行**；IDEA/Maven 不依赖它。

## 数据库约定（重要）

- 库名 `game_db`，两表 `user` **1—N** `game_record`（外键 `user_id`）。建表脚本 [`sql/schema.sql`](sql/schema.sql)（可重复执行）。
- 连接配置在 [`src/main/resources/db.properties`](src/main/resources/db.properties)，**改密码只改这里**，不要硬编码到 Java。
- **MD5 必须对齐**：`MD5Util.md5("123456")` 必须等于 schema.sql 里测试数据的 `e10adc3949ba59abbe56e057f20f883e`，否则注册的用户与测试数据对不上、登录失败。改 MD5 实现前务必验证。
- **不要破坏数据层契约**：`UserDao`/`RecordDao` 的方法签名被 UI 层依赖，改动需同步上层调用方。
- MySQL 8 连接串必须带 `serverTimezone`、`characterEncoding=utf8`、`useSSL=false&allowPublicKeyRetrieval=true`。

## 代码规范

- **缩进**：4 空格（锯齿型书写格式），**禁止 Tab**。
- **花括号**：K&R 风格（`{` 不换行）。
- **命名**：类 PascalCase，方法/变量 camelCase，常量全大写下划线；包名全小写 `com.game.*`。
- **注释**：类与 public 方法用 Javadoc；中文行内注释；**文件 UTF-8 编码**。
- **SQL**：一律 `PreparedStatement`（防注入）；密码一律先 `MD5Util.md5`。
- **资源关闭**：`try-with-resources`，不手写 finally。

## 工作约定（分阶段开发）

本项目采用**一次一个阶段**的节奏：

1. **一次只做一个阶段**，做完**停下来**，不要一口气往下冲多个阶段。
2. 每完成一个阶段，在 `notes/v1.0_PhaseSummary/` 写一份复盘总结，命名 `NN_阶段名.md`（如 `01_需求与设计.md`、`02_数据层.md`）。总结面向**复盘学习**：开发流程、关键决策、核心知识点、踩坑、产出速查。
3. 文档与图表：设计文档放 `docs/`，笔记与阶段总结放 `notes/`；**图表用 Mermaid**（`classDiagram` / `flowchart` / `erDiagram` / `sequenceDiagram`）。
4. **跨文档一致性**：数据库文档字段必须与 `schema.sql` 严格一致；UML 类图必须与 `src` 目录结构对应。

## 关键约束与坑

- **JDK 25 很新**：若需在学校机器运行，把 `pom.xml` 的 `release`/`source`/`target` 调低。
- **中文乱码**：`javac`/`java` 加 `-encoding UTF-8` / `-Dfile.encoding=UTF-8`；MySQL 客户端加 `--default-character-set=utf8mb4`；bat 加 `chcp 65001`。
- **连不上库**：`DBUtil` 已抛带友好提示的 `RuntimeException`；UI 层调用 DAO 时应 `try/catch` 兜底（弹"数据库连接失败"），不要让异常崩界面。
- **Swing 线程**：UI 创建/修改必须在 EDT，用 `SwingUtilities.invokeLater`。
- **窗口泄漏**：跳转窗体用 `dispose()` 而非 `setVisible(false)`（JFrame 只隐藏不会被回收）。

## 当前进度

- ✅ 阶段① 需求与设计（docs 三文档 + schema.sql）
- ✅ 阶段② 数据层（DAO 跑通注册/登录/存档/排行榜，`TestDao` 已验证）
- ✅ 阶段③ 界面层（登录/注册/主菜单，`run.bat` 已验证可运行）
- ✅ 阶段④ 游戏核心（Player/Zombie/Bullet/GameController/GamePanel/GameWindow，`TestGameLogic` + `run.bat` 已验证；含局内"退出本局"统一结算）
- ✅ 阶段⑤ 排行榜（LeaderboardFrame + RecordDao JOIN 取昵称，`TestDao` 验证通过）
- ✅ 阶段⑥ 打磨全部完成（6a: WASD移动/僵尸追踪/难度递增/血条HUD/打击感粒子震屏/Brute；6b: 代码合成音效+设置暂停菜单），`TestGameLogic` 验证通过
- ✅ 扩展功能：用户管理与账号安全（admin 角色 / 忘记密码重置申请 / 修改密码；user 加 role 列 + password_reset_request 表；`TestUserMgmt` 15/15 验证通过）
- ⏳ 阶段⑦ 测试与文档（待开始）

下一步：系统自测 + 补正式文档/图，准备答辩（阶段⑦）。
