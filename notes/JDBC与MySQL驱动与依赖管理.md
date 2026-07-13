# 学习笔记：JDBC、MySQL 驱动、依赖管理（Maven vs 手工 jar）

> 写给：刚跑通阶段②、想彻底搞懂"Java 怎么连数据库"的你
> 关联项目：`D:\MYCODE\Game`（打僵尸游戏）
> 日期：2026-07-09

---

## 一、先搞懂三个概念的关系（最重要，先看这）

一句话：**你的代码 →（用 JDBC 这套标准）→ 驱动（当翻译官）→ MySQL 数据库**。

打比方 🔌：

| 现实世界 | 对应到这里 |
|---|---|
| **USB 接口标准**（定义了形状、针脚、协议） | **JDBC**：Java 连数据库的**标准/规范** |
| **各厂商的 USB 驱动程序**（让电脑认得自家设备） | **MySQL 驱动**：让 Java 认得 MySQL 的**翻译官** |
| 你的电脑（只认 USB 标准，插啥设备都行） | 你的 Java 程序（只认 JDBC，换数据库不用大改） |
| 具体的 U 盘/鼠标/键盘 | 具体的数据库（MySQL / Oracle / SQLServer） |

**关键点**：
- 你的代码**只跟 JDBC 打交道**，不直接碰 MySQL 私有协议。
- JDBC 是**接口**（JDK 自带），驱动是**实现**（第三方给的 jar）。
- 没有 JDBC → Java 没有统一方式连数据库；没有驱动 → JDBC 不知道怎么和 MySQL 说话。

---

## 二、JDBC 是什么

**JDBC = Java DataBase Connectivity**，是 Java 连接数据库的**标准 API**，在 JDK 的 `java.sql` 包里，**装了 JDK 就有**。

它定义了一套统一的类和接口：

| 类/接口 | 干什么的 | 本项目里哪用到 |
|---|---|---|
| `DriverManager` | 管理驱动、获取连接 | `DBUtil.getConnection()` 里 |
| `Connection` | 一个数据库连接 | 每个 DAO 方法的 `try(...)` 里 |
| `PreparedStatement` | 预编译 SQL 语句 | 所有 SQL 都用它 |
| `ResultSet` | 查询返回的结果集 | `login()`、`topN()` 里遍历 |

**JDBC 六步法**（所有数据库操作都是这个套路）：

```java
// 1. 获取连接
Connection conn = DBUtil.getConnection();
// 2. 创建预编译语句（? 是占位符）
PreparedStatement ps = conn.prepareStatement("SELECT * FROM user WHERE username = ?");
// 3. 填参数
ps.setString(1, "admin");
// 4. 执行（查询用 executeQuery，增删改用 executeUpdate）
ResultSet rs = ps.executeQuery();
// 5. 处理结果
while (rs.next()) {
    String name = rs.getString("username");
}
// 6. 关资源（用 try-with-resources 自动关，不用手写）
```

> 👉 JDBC 只规定"怎么连、怎么执行 SQL、怎么取结果"，**不管你用的是哪种数据库**。所以学会 JDBC，连 MySQL、Oracle、SQLServer 套路一样。

---

## 三、MySQL 驱动是什么

**MySQL 驱动 = JDBC 这套接口，针对 MySQL 的具体实现**，是一个第三方提供的 jar 包（`mysql-connector-j`）。

### 它干什么活？
把 JDBC 的标准调用，**翻译成 MySQL 服务器能听懂的网络协议**，再把结果翻译回来。

```
你的代码: ps.executeQuery()
   ↓ (JDBC 标准调用)
驱动 jar: 翻译成 MySQL 私有网络协议
   ↓ (网络)
MySQL 服务器: 执行 SQL，返回数据
   ↓
驱动 jar: 把数据包装成 ResultSet
   ↓
你的代码: rs.getString("username")
```

### 为什么需要它？
因为 Java（JDBC）只定义了**接口**，不知道 MySQL 的**私有通信协议**。必须有人来"翻译"——这就是驱动。每个数据库都有自己的驱动：
- MySQL → `mysql-connector-j`
- Oracle → `ojdbc`
- SQLServer → `mssql-jdbc`

### 它怎么被加载？（JDBC 4.0+ 自动）
老代码里你会看到 `Class.forName("com.mysql.cj.jdbc.Driver")`，那是**手动**告诉 JVM 去加载驱动类。
现在（JDBC 4.0 / Java 6 之后）**不用写了**：只要驱动 jar 在 classpath 上，`DriverManager` 会通过 `ServiceLoader` **自动发现并加载**它。本项目 `DBUtil` 里没写 `Class.forName`，就是因为自动加载。

### ⚠️ 编译 vs 运行（容易混）
| 时刻 | 需要 mysql 驱动 jar 吗？ | 为什么 |
|---|---|---|
| **编译**（javac） | ❌ **不需要** | 代码只 import `java.sql.*`，这些类在 JDK 里 |
| **运行**（java） | ✅ **需要** | `DriverManager` 运行时才去找驱动实现来连库 |

这就是为什么阶段②我能用 `javac` 直接编译通过（没下驱动），但**跑** `TestDao` 时必须先下驱动 jar。

---

## 四、驱动 jar 从哪来、怎么进项目 → 两种方案

驱动 jar 是个外部文件，怎么把它弄进你的 Java 项目？有两种主流做法。

### 方案 A：Maven 工程（本项目用的，推荐）

**Maven 是什么**：Java 的**构建 + 依赖管理工具**。你在一个叫 `pom.xml` 的文件里声明"我需要哪些库"，Maven 自动帮你从网上下载、管理版本、配好 classpath。

**工作流程**：
```
你写 pom.xml 声明依赖
    ↓
Maven 去【中央仓库】(网上的大仓库) 找这个库
    ↓
下载到你的【本地仓库】(C:\Users\你\.m2\repository)
    ↓
自动加到项目 classpath，代码就能 import 了
```

**本项目 pom.xml 长这样**（关键部分）：
```xml
<dependencies>
    <dependency>
        <groupId>com.mysql</groupId>          <!-- 谁做的 -->
        <artifactId>mysql-connector-j</artifactId>  <!-- 叫什么 -->
        <version>8.0.33</version>              <!-- 哪个版本 -->
    </dependency>
</dependencies>
```
> `groupId + artifactId + version` 三件套叫**坐标**，唯一定位一个库。Maven 拿着坐标去中央仓库"取货"。

**优点**：
- ✅ 一行声明，自动下载，不用手动找 jar。
- ✅ 换台电脑/给别人项目，Maven 自动重新下载依赖，**不用拷 jar**。
- ✅ 加新库极方便（再加一个 `<dependency>`）。
- ✅ 业界主流，简历上有加分。

**缺点**：
- ⚠️ 第一次要联网下载（几十秒）。
- ⚠️ 要稍微理解 pom.xml（但其实不用自己手写，工具生成）。

---

### 方案 B：纯手工 jar（传统课设做法）

**做法**：你自己去网上**手动下载** `mysql-connector-j-x.x.x.jar` 这个文件，放进项目的 `lib/` 文件夹，然后在 IDEA 里手动 **Add as Library**（告诉 IDEA "这个 jar 是项目要用的库"）。

**步骤**：
1. 去 MySQL 官网或 Maven 中央仓库，下载 `mysql-connector-j-8.0.33.jar`。
2. 在项目里建 `lib/` 文件夹，把 jar 拷进去。
3. IDEA 里：右键 jar → `Add as Library`。
4. 代码就能 `import java.sql.*` 并连库了。

**优点**：
- ✅ 过程透明，每个 jar 你都看得见。
- ✅ 不依赖 Maven，不联网也能配（只要有 jar）。
- ✅ 很多老课设、老教材就是这么教的。

**缺点**：
- ❌ 要自己找对版本、下对文件（新手容易懵）。
- ❌ 换台电脑要把 jar 一起拷，否则别人编译不过。
- ❌ 加多个库很麻烦，每个都要手动下、手动加。

---

## 五、两种方案对比（一表看清）

| 维度 | Maven 工程 | 纯手工 jar |
|---|---|---|
| 怎么得到驱动 | pom.xml 声明，**自动下载** | 手动下载 jar |
| 怎么配置 | **自动**配 classpath | 手动 Add as Library |
| 换电脑/给别人 | 自动重下，无感 | 要拷 jar，否则挂 |
| 加新库 | 再写一行 dependency | 手动下+手动加 |
| 学习成本 | 要懂一点 pom.xml | 只要点几下鼠标 |
| 联网要求 | 第一次要联网 | 有 jar 就不联网 |
| 业界现状 | **主流** | 老派/教学用 |

**本项目为什么选 Maven**：省心、不用手动折腾下载配置，pom.xml 由工具/我帮你生成。学了不亏。

---

## 六、和本项目对应起来（把概念落到文件）

| 概念 | 本项目对应 |
|---|---|
| JDBC 标准 API | 代码里所有 `java.sql.*` 的 import |
| 用 JDBC 连库的代码 | [DBUtil.java](../src/main/java/com/game/dao/DBUtil.java) 的 `getConnection()` |
| MySQL 驱动 | `pom.xml` 里声明的 `mysql-connector-j`（Maven 自动下） |
| 驱动自动加载 | `DBUtil` 没写 `Class.forName`，靠 JDBC 4 自动发现 |
| 配置（连哪个库） | [db.properties](../src/main/resources/db.properties) |

---

## 六补：`src/main/resources/` 目录是干什么的？

上面说 [`DBUtil`](../src/main/java/com/game/dao/DBUtil.java) 从 classpath 读 `db.properties` 拿连接配置——那 `db.properties` 这个文件**放在项目的哪里**？就在 `src/main/resources/`。

### resources 是什么

Maven 项目约定：**`.java` 源码放 `src/main/java/`；非代码资源放 `src/main/resources/`**。两个目录编译后都进 classpath（`target/classes/`），代码统一用 `getResourceAsStream` 读。

本项目的 resources 里有 4 个文件：

```
src/main/resources/
├── db.properties           ← MySQL 连接配置(含密码,**被 gitignore**)
├── db.properties.example   ← 模板版(占位密码,**公开仓库里有**)
└── images/
    ├── player.png          ← 玩家精灵图(橙猫)
    └── zombie.png          ← 僵尸精灵图(绿僵)
```

| 文件 | 谁读它 | 怎么读 |
|---|---|---|
| `db.properties` | `DBUtil` 静态块 | `getResourceAsStream("db.properties")` → `Properties.load()` |
| `db.properties.example` | 不读（给人看的模板） | 别人 clone 后复制成 db.properties 填密码 |
| `images/player.png` | `Player.sprite()` | `getResourceAsStream("/images/player.png")` → `ImageIO.read()` |
| `images/zombie.png` | `Zombie.sprite()` | 同上 |

> **为什么 db.properties 被gitignore**：里面有你的 MySQL root 密码（`password=123456`）。公开仓库只放 `.example` 模板；别人拿到项目后复制一份、填自己的密码。详见 [README §配置](../README.md)。

> **图片和 JDBC 没关系**，但放一起讲是因为它们**共享同一个机制**：都在 resources 里、都通过 classpath 读。Java 的资源管理就是这么统一——不管配置还是图片，`getResourceAsStream` 一把梭。

---

## 六补₂：`target/` 是什么（Maven 构建产物）

上面说 `src/main/java/` 的 `.java` 和 `src/main/resources/` 的资源编译后都进 classpath——**编译到哪去了？** 就是 `target/`。

### target 是什么

`target/` 是 **Maven 的构建输出目录**。`mvn compile` 把 `.java` 编译成 `.class`、把 resources 原样拷贝，都放进 `target/classes/`：

```
target/
└── classes/                         ← 编译产物(classpath 根目录)
    ├── com/game/                     ← .java 编译出的 .class(按包结构)
    │   ├── GameApp.class
    │   ├── dao/UserDao.class
    │   └── ...
    ├── db.properties                 ← resources 原样拷过来的
    └── images/                       ← resources 里的图片也拷过来
```

> 这就是为什么代码里 `getResourceAsStream("/images/player.png")` 能读到图——编译后图就在 `target/classes/images/` 里，而 `target/classes/` 正是 classpath 根。

### `target/` vs `out/`（两个构建产物目录）

本项目有**两套编译方式**，产出两个不同的目录：

| | `target/` | `out/` |
|---|---|---|
| 谁产生的 | **Maven**（`mvn compile`） | **run.bat**（`javac -d out`） |
| 里面 | `.class` + resources（Maven 结构） | `.class` + resources（javac 结构） |
| 用途 | Maven/IDEA 编译 | run.bat 一键编译运行 |
| gitignore | ✅ 被忽略 | ✅ 被忽略（`out*/`） |
| 能删吗 | ✅ `mvn clean` 删掉，下次编译重新生成 | ✅ 直接删，run.bat 重新生成 |

**两个都是构建产物**——可从源码重新生成，所以都 gitignore、都不提交。你电脑上可能两个都有（IDEA 用 Maven 编到 target，run.bat 编到 out），删哪个都不影响——源码在就能重建。

> **一句话**：`target/` 是 Maven 编译的产物箱、`out/` 是命令行（run.bat）编译的产物箱，都是「从源码生成的、可重建的、不提交的」东西。**源码才是真相，target/out 是衍生品。**

---

## 六补₃：`sql/` 目录是什么（建表与迁移脚本）

上面讲 DBUtil 怎么连数据库、db.properties 配连接——**但数据库本身（表结构、测试数据）从哪来？** 就是 `sql/` 目录里的脚本。

### 四个文件一览

```
sql/
├── schema.sql                              ← 主脚本：建库 + 3 张表 + 种子数据(全新安装用)
├── migrate_v1.1_usermgmt.sql               ← 增量迁移①：加 role 列(用户管理扩展)
├── migrate_v1.2_leaderboard_difficulty.sql ← 增量迁移②：加 difficulty 列 + 复合索引(分难度排行榜)
└── migrate_v1.3_phone.sql                  ← 增量迁移③：username 列改名为 phone(手机号登录)
```

### schema.sql —— 全新安装（一把到位）

```sql
CREATE DATABASE IF NOT EXISTS game_db ...;    -- 建库
DROP TABLE IF EXISTS game_record;             -- 先删旧表(可反复执行)
DROP TABLE IF EXISTS user;
CREATE TABLE user (...);                      -- 建 3 张表
CREATE TABLE game_record (...);
CREATE TABLE password_reset_request (...);
INSERT INTO user(phone, password, ...) VALUES ('00000000000', MD5('123456'), ...);  -- 种子数据
```

**特点**：带 `DROP IF EXISTS`，**可反复执行**——每次跑都先删后建，永远是干净状态。跑法：

```bash
mysql -uroot -p < sql/schema.sql
```

跑完你就有一个完整的 `game_db`，含 3 张表 + 几条测试数据（admin/player1/player2 + 几条战绩），可以立刻注册/登录/游戏。

### 三个 migrate —— 增量迁移（给已有数据库打补丁）

项目开发过程中，表结构会变（加列、改名）。但**已有数据的库不能 DROP 重建**（数据会丢），所以用**增量迁移脚本**：

| 迁移 | 改了什么 | 幂等？ |
|---|---|---|
| `migrate_v1.1` | `user` 表加 `role` 列（admin/user） | ✅ 先查列在不在，在就跳过 |
| `migrate_v1.2` | `game_record` 加 `difficulty` 列 + `idx_diff_score` 索引 | ✅ 先查列/索引在不在 |
| `migrate_v1.3` | `username` 列改名 `phone` + `uk_username` 改名 `uk_phone` | ✅ 先查旧列在不在 |

**「幂等」**= 可重复执行不报错——第二次跑发现列/索引已经有了，就跳过（用 `information_schema` 查 + `IF(...)` 条件判断）。这是数据库迁移脚本的**核心要求**：不能跑第二次就崩。

> **schema vs migrations 的关系**：
> - **全新安装**：直接跑 `schema.sql`（一步到位，3 张表全有最新的列）。
> - **已有老库**：跑对应的 `migrate_v1.x`（只改变化的部分，不动数据）。
> - schema.sql 是**最新状态的全量快照**；migrate 是**从旧状态到新状态的增量补丁**。

### 迁移顺序（重要）

如果从老库升级，必须**按版本号顺序**跑迁移：

```bash
mysql -uroot -p game_db < sql/migrate_v1.1_usermgmt.sql        # 先加 role 列
mysql -uroot -p game_db < sql/migrate_v1.2_leaderboard_difficulty.sql  # 再加 difficulty 列
mysql -uroot -p game_db < sql/migrate_v1.3_phone.sql            # 最后改 phone
```

**不能跳版本、不能倒序**——每个迁移假设前一个已经跑过。但如果你是**全新安装**，直接跑 `schema.sql` 就行（已经内置了所有最新列）。

> ⚠️ v1.3 迁移后**勿再重跑 v1.1**（v1.1 里 `WHERE username='admin'` 引用旧列名，phone 之后列不在了会报错）。全新机直接用 schema.sql，不跑迁移。

---

## 六补₄：`src/main/java/com/game/dao/` 目录是什么（数据访问层）

上面讲完了 DBUtil 怎么连库、配置在哪、编译产物去哪、数据库怎么建——**但谁来执行 SQL？** 就是 `dao/` 包里的类。

### dao 是什么（Data Access Object 模式）

`dao` = **Data Access Object**（数据访问对象）。它是一种**设计模式**：把所有数据库操作（增删改查 SQL）**集中到专门的类里**，让业务逻辑（UI / 游戏逻辑）不用直接写 SQL。

```
UI 层(LoginFrame)  →  DAO 层(UserDao)  →  DBUtil(连接)  →  MySQL
   "登录"             "SELECT ... WHERE phone=?"     getConnection()
   不碰 SQL           不碰界面                        不碰业务逻辑
```

> **分层好处**：换数据库（MySQL→Oracle）只改 DAO + DBUtil，UI 和游戏逻辑一行都不用动。SQL 被关在 DAO 这一层，不泄漏到别处。

### 四个类一览

| 类 | 一句话职责 | 核心方法 |
|---|---|---|
| [`DBUtil`](../src/main/java/com/game/dao/DBUtil.java) | 连接管理（读 db.properties、给连接、关资源） | `getConnection()` / `close()` |
| [`UserDao`](../src/main/java/com/game/dao/UserDao.java) | user 表的增删改查 | `login` / `register` / `findByPhone` / `changePassword` / `deleteUser` |
| [`RecordDao`](../src/main/java/com/game/dao/RecordDao.java) | game_record 表的写入和排行榜查询 | `saveRecord` / `topN(难度)` / `mine(用户)` |
| [`ResetRequestDao`](../src/main/java/com/game/dao/ResetRequestDao.java) | password_reset_request 表的申请/审核 | `requestReset` / `hasPending` / `approve(事务)` / `reject` |

### 每个类的角色

**① `DBUtil` —— 基础设施（不给别的 DAO 写 SQL，只管连接）**
- `static {}` 块：类加载时读 `db.properties`（URL/user/password），只读一次。
- `getConnection()`：`DriverManager.getConnection(...)` 拿连接。连不上抛友好提示。
- `close(conn, stmt, rs)`：倒序关资源（结果集→语句→连接），每步单独 try（一个关失败不影响后面）。
- **不含任何业务 SQL**——它是「水管」，不是「用水的人」。

**② `UserDao` —— 用户增删改查**
- `login(phone, pwd)`：`SELECT ... WHERE phone=? AND password=MD5(?)`，找到返回 User 对象，没找到返回 null。
- `register(user)`：先 `findByPhone` 查重 → `INSERT INTO user(phone, password, nickname) VALUES(?,?,?)`，密码先 MD5。
- `changePassword(userId, oldPwd, newPwd)`：`UPDATE user SET password=? WHERE id=? AND password=?`（旧密码塞进 WHERE 原子校验——一条 SQL 同时校验+改密）。
- `deleteUser(userId)`：**三步删除包事务**（先查 role → 删 game_record → 删 password_reset_request → 删 user，`if(deleted) commit() else rollback()`）。
- 所有方法用 `PreparedStatement`（防 SQL 注入）+ `try-with-resources`（自动关连接）。

**③ `RecordDao` —— 战绩存档 + 排行榜**
- `saveRecord(record)`：`INSERT INTO game_record(user_id, score, kill_count, survive_sec, difficulty) VALUES(?,?,?,?,?)`（结算时调）。
- `topN(n, difficulty)`：`SELECT ... WHERE difficulty=? ORDER BY score DESC LIMIT ?`（简单榜/困难榜）。
- `mine(userId)`：`SELECT ... WHERE user_id=? ORDER BY record_time DESC`（我的记录——全部战绩、按时间倒序）。
- 用 `mapWithUser(rs)` 把 ResultSet 映射成 GameRecord 对象（JOIN user 带出 nickname）。

**④ `ResetRequestDao` —— 忘记密码的申请/审核**
- `requestReset(phone)`：查 userId → `hasPending` 查重 → `INSERT` 一条 pending 申请。
- `approve(requestId, userId)`：**包事务**——`UPDATE user SET password=MD5('123456') WHERE id=? AND role='user'` + `UPDATE 申请 SET status='approved'`，两步要么都成要么都废。
- `reject(requestId)`：`UPDATE 申请 SET status='rejected'`。

### DAO 层的共性套路（看懂一个 = 看懂全部）

每个 DAO 方法都是同一个模式：

```java
public 返回类型 方法名(参数) {
    String sql = "SQL 语句，参数用 ? 占位";          // ① 写 SQL（PreparedStatement 防注入）
    try (Connection conn = DBUtil.getConnection();   // ② 拿连接（try-with-resources 自动关）
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, 值);                          // ③ 填参数
        try (ResultSet rs = ps.executeQuery()) {      // ④ 执行（查用 executeQuery，改用 executeUpdate）
            if (rs.next()) {                           // ⑤ 处理结果（映射成对象）
                User user = new User();
                user.setId(rs.getInt("id"));
                ...
                return user;
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();                           // ⑥ 异常兜底
    }
    return null;                                      // ⑦ 没找到返回 null
}
```

> **记住这个七步套路**，你项目里所有 DAO 方法（login/register/changePassword/topN/mine/approve...）都是这个骨架，只是 SQL 和字段不同。答辩被问「你的 DAO 怎么写的」——把这个套路背出来。

---

## 七、常见疑问 FAQ

**Q1：我代码里没写 `Class.forName(...)`，怎么就连上 MySQL 了？**
A：JDBC 4.0 之后自动加载。只要驱动 jar 在 classpath（Maven 帮你放好了），`DriverManager.getConnection()` 时会自动发现并加载驱动，不用手写。

**Q2：编译报错说找不到 `Connection` 类吗？**
A：不会。`Connection/PreparedStatement/ResultSet` 都在 JDK 的 `java.sql` 里，编译不需要驱动 jar。驱动 jar 只在**运行时**才需要。

**Q3：Maven 下载驱动特别慢/失败怎么办？**
A：默认中央仓库在国外。配一个**阿里云镜像**就快了（在 `C:\Users\你\.m2\settings.xml` 里加 mirror 配置）。需要的话我帮你配。

**Q4：`Statement` 和 `PreparedStatement` 有啥区别？**
A：`Statement` 直接拼 SQL 字符串，**有 SQL 注入风险**；`PreparedStatement` 用 `?` 占位 + `setXxx` 填值，**防注入且更快**。本项目全程用 `PreparedStatement`。

**Q5：JDBC、驱动、Maven 三者是平级的吗？**
A：不是。**JDBC 是标准，驱动是实现，Maven 是搬运工**。JDBC 和驱动是"数据库连接"这个事的两半；Maven 只是帮你把驱动 jar 弄进项目的工具之一（手工 jar 是另一种）。

---

## 八、一句话总结

> **JDBC 是 Java 连数据库的统一标准（JDK 自带）；MySQL 驱动是实现这个标准的翻译官（要单独的 jar）；Maven 是帮你把驱动 jar 自动弄进项目的工具（手工 jar 是它的替代方案）。**

这三者搞懂，Java 连数据库这件事就彻底通了。
