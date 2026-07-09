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
