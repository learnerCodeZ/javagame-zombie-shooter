# 04 代码精读 —— 收尾(音效 / 排行榜 / admin)

| 项 | 内容 |
|---|---|
| 目标 | 读完项目最后三块:**纯代码合成音效**(独立亮点)、排行榜 UI、admin 后台 |
| 配套 | [01](./01_代码精读_入口与登录切片.md) · [02](./02_代码精读_注册改密忘记密码.md) · [03](./03_代码精读_游戏核心.md) · [00 总纲](./00_答辩准备总纲_项目通览与学习路线.md) |
| 进度 | 收尾 ✅ · **全项目代码精读完成 🎉**(见 §4) |

> 本站 §1(音效)值得深读——是项目里最"硬核"的独立亮点;§2/§3(排行榜/admin)是 UI 模式,你 01/02 已学,点到即止。

---

# §1 `SoundUtil` —— 纯代码合成音效 ⭐⭐(独立亮点)

> 文件:[`util/SoundUtil.java`](../../src/main/java/com/game/util/SoundUtil.java)

**为什么是亮点**:别人用 `.wav`/`.mp3` 文件播音效;这个**用代码现场算 PCM 采样喂给扬声器**,零外部音频资源。一个文件串起 Java Sound API、数字音频原理(采样/波形/包络)、多线程(生产者-消费者)、缓冲机制——答辩讲这个很能加分。

## 1.1 播放模型:单线路 + 队列 + 守护线程

> 📍 L76 `sharedLine` · L81 `playQueue` · L83-88 守护线程 · L93-106 `playLoop` · L111-128 `ensureLine`

```
shoot()/hit()/...  ──offer任务──▶  playQueue(BlockingQueue)
                                        │ take()
                                        ▼
                              sound-player 守护线程
                                        │ ensureLine() 拿/开 sharedLine
                                        ▼
                              task.run() → buildTone 合成 PCM → line.write
                                        │
                                        ▼
                                   扬声器
```

- **全局只开一条 `SourceDataLine`**(`sharedLine`,懒加载复用)。早期版本每音效开一条线 → 连射时**耗尽混音器线路** → 抛 `LineUnavailableException`(被静默吞) → "有时候没声音"。复用一条从根本上避免。
- **`BlockingQueue` + 单消费线程**:所有音效把"合成+写入"任务丢进队列,由 `sound-player` **守护线程**串行处理 → 对共享线路的访问不会并发冲突。
- **为什么不直接在调用线程播音**:`line.write` 会阻塞(缓冲满时),如果在 EDT/游戏线程调,会**卡界面/卡游戏**。丢给专用线程,主线程立刻返回。

## 1.2 三道防线防"连射堆爆"

> 📍 L69 `SHOOT_DEBOUNCE_MS=40` · L71 `MAX_PENDING=2` · L134-147 `shoot()`

- **防抖**(`shoot` L139):两次开火音至少间隔 40ms,太近就跳过——连射不会每次都堆一个任务。
- **队列上限**(`shoot`/`hit` L143/L156):`playQueue.size() >= 2` 时丢弃本次——不让队列积压成"停火后还在响"。
- **小缓冲**(`LINE_BUFFER_BYTES` L66,≈50ms):见 1.4。

## 1.3 合成原理:PCM 是怎么算出来的 ⭐

> 📍 L52 `SAMPLE_RATE=44100` · L60 `FORMAT` · L227-244 `buildTone` · L256-275 `buildSweep` · L309-319 `appendSample`

**PCM(脉冲编码调制)**= 把声波按固定频率"采样",每个采样点记一个振幅数值,扬声器照着这些数值震动发声。

- **采样参数**(L52/L60):44100Hz(每秒采 44100 个点)/ 16bit(每点用 2 字节存)/ 单声道 / 有符号小端序。
- **`buildTone(freq, ms, decay, square)`**(L227)合成一段固定频率的音:

```java
for (int i = 0; i < samples; i++) {
    double t = i / SAMPLE_RATE;                              // 当前时间(秒)
    double env = Math.exp(-decay * t);                        // 指数衰减包络(声音逐渐变小)
    double atk = t < ATTACK_SEC ? (t/ATTACK_SEC) : 1.0;       // 开头淡入(attack)
    double rel = (remaining < RELEASE_SEC) ? remaining/RELEASE_SEC : 1.0;  // 结尾淡出(release)
    double s = Math.sin(2 * Math.PI * freq * t);              // 正弦波
    appendSample(buffer, i, s * amp * env * atk * rel);       // 叠起来 → 写进缓冲
}
```

一段音 = **波形(正弦) × 衰减包络 × 起音淡入 × 释音淡出 × 音量**。

- **`buildSweep`**(L256)频率扫描(hurt 的下滑音):用**相位累加** `phase += 2π·freq·dt`(而非直接 `sin(2π·freq·t)`)——因为频率在变,直接算会在变频率处**相位跳变 → 咔哒声**;累加相位保证连续。
- **`appendSample`**(L309):把 double 振幅 → 钳到 short 范围 → 拆成**小端序两字节**(低字节在前)写进缓冲。

**四种音**:

| 方法 | 合成 | 听感 |
|---|---|---|
| `shoot()`(L134) | `buildTone(600, 40ms, decay=25)` | 短促"pew" |
| `hit()`(L152) | `buildTone(150, 60ms, decay=12)` | 低沉击打 |
| `hurt()`(L165) | `buildSweep(400→200, 120ms)` | 下滑鸣音(痛感) |
| `gameOver()`(L175) | `buildNotes(660,520,392,262)` | 递降旋律"完蛋了" |

## 1.4 两个修过的坑(连射乱响 / 松手还在响)⭐

> 详见 [`扩展_音效修复`](../v1.0_审计修复/扩展_音效修复_射击干脆与松手即停.md)

**坑 A:松开鼠标还在响**(根因在 `SourceDataLine` 的缓冲机制):
- `line.write()` **只在缓冲满时才阻塞**。默认大缓冲(几百 ms)→ 消费线程把队列里的音**以远快于实时的速度**全灌进缓冲 → 音频被"囤积" → 松手后缓冲里的还在吐。
- **修法**(L66 + L120):`l.open(FORMAT, LINE_BUFFER_BYTES)` **开小缓冲(≈50ms)** → `write()` 在缓冲接近满时阻塞 → 消费线程被**节流到实时** → 音频几乎不囤积、松手即停。50ms 延迟人耳察觉不到,又够大避免欠载爆音。

**坑 B:连按乱响/爆音**:
- 每个音**只有开头淡入(attack)、没结尾淡出** → 末尾包络还在 ~37% 振幅 → 相邻音衔接处振幅突变 → 咔哒/爆音。
- **修法**(L58 `RELEASE_SEC=0.005` + buildTone/buildSweep 的 `rel`):末尾 5ms 振幅线性降到 0,与开头淡入对称 → 衔接处归零、无突变 → 连射不咔哒。

## 1.5 不阻塞 / 不崩原则

- 消费线程是**守护线程**(`setDaemon(true)`,L86)→ JVM 退出时不会被拖住。
- 整段 `try/catch`(L95-105):无音频设备 / `LineUnavailableException` 一律**静默吞掉**——音效是锦上添花,绝不让游戏崩。
- `shouldSkip()`(L187):静音或音量 0 直接 return,不入队。

## 1.6 想改 X 动哪里 / 自测

- 想加"换弹音":新增 `reload()` 方法调 `buildTone(...)` + 在合适处调。
- 想换音调:`shoot` 的 600Hz 改成别的频率。
- 想用真实音频文件替代合成:换成 `AudioSystem.getAudioInputStream` 读 wav(但要打包音频资源)。
- **Q**:`line.write()` 什么时候阻塞?为什么默认大缓冲会导致"松手还在响"?
- **Q**:为什么 `hurt` 的频率扫描用相位累加,不直接 `sin(2π·freq·t)`?

<details><summary>答案</summary>

`line.write()` **只在内部缓冲满时阻塞**。默认大缓冲 → 消费线程把队列抽干进缓冲的速度远快于实时播放 → 音频囤积在缓冲 → 松手后缓冲里囤的继续播 → "还在响"。开小缓冲(50ms)让 write 在接近满时阻塞,把消费线程节流到实时 → 不囤积。

频率扫描若直接 `sin(2π·freq·t)`,当 freq 逐帧变化时,`2π·freq·t` 的相位会**跳变**(因为 freq 变了但 t 连续)→ 波形不连续 → 咔哒声。相位累加 `phase += 2π·freq·dt` 保证相位**连续**,波形平滑。

</details>

---

# §2 `LeaderboardFrame` —— 排行榜 UI

> 文件:[`ui/LeaderboardFrame.java`](../../src/main/java/com/game/ui/LeaderboardFrame.java)

UI 模式和 01/02 一样(JTable + 按钮 + DAO),不重复讲套路,只看**它特有的状态驱动刷新**。

## 2.1 两个状态变量 → 四种视图

> 📍 L45 `currentDifficulty`(默认 EASY) · L47 `mineMode`(默认 false) · L93-95 按钮改状态

```java
easyButton → currentDifficulty=EASY, mineMode=false
hardButton → currentDifficulty=HARD, mineMode=false
mineButton → mineMode = !mineMode(开关)
```

两个状态组合 → `reload()` 据此查不同数据:

| mineMode | currentDifficulty | 查什么 | 显示 |
|---|---|---|---|
| false | EASY | `topN(50, "EASY")` | 简单榜(全部玩家、按分数) |
| false | HARD | `topN(50, "HARD")` | 困难榜(全部玩家、按分数) |
| true | (任意) | `mine(userId)` | 我的记录(自己的全部战绩、按时间) |

## 2.2 `reload()` —— 状态驱动刷新

> 📍 [`reload()` 第 128–157 行](../../src/main/java/com/game/ui/LeaderboardFrame.java#L128) · 📍 L150 `diffLabel(r.getDifficulty())`(难度列)

- 据 `mineMode` 选 `mine` 或 `topN` → 清空表格 → 遍历填行(每行带 `diffLabel` 把 EASY/HARD 转简单/困难)→ `updateViewLabel()`。
- **`diffLabel()`**(L181):`Difficulty.valueOf(diff).label` 把 DB 串转中文;空/未知兜底。

## 2.3 `updateViewLabel()` —— 顶部标签

> 📍 L162-171

`mineMode` → 「当前:我的记录(全部 · 按时间倒序)」;否则 → 「当前:简单榜/困难榜」。颜色随难度(简单绿/困难红)。

## 2.4 一个踩坑:注释会随重构过时 ⚠️

写本篇时发现:`LeaderboardFrame` 的**类 javadoc** 还写着旧设计(「我的记录是叠加只看自己」),但代码早改成「全部战绩」了——**注释和代码不一致**。这就是 **code-comment drift**(注释漂移):重构改了行为,忘了同步注释。

> 教训:**注释也是代码的一部分**,改行为要回头同步注释;读代码时如果注释和代码矛盾,**以代码为准**(代码是真相,注释可能过时)。本篇已顺手把这个过时注释修了。

## 2.5 想改 X 动哪里

- 想加"只看困难我的记录":`mineMode` 之外再加个过滤(目前我的记录=全部难度)。
- 想改榜单显示前 N 名:`reload` 里 `topN(50, ...)` 的 50。

---

# §3 `AdminFrame` —— admin 后台

> 文件:[`ui/AdminFrame.java`](../../src/main/java/com/game/ui/AdminFrame.java)

admin 专用窗,**双 JTable + 5 按钮**。UI 套路不重复,看它怎么把"选中行"映射回对象。

## 3.1 结构

> 📍 L43-49 `userModel`(ID/手机号/昵称/角色/注册时间) · L51-57 `requestModel`(手机号/昵称/申请时间) · L63-66 `users`/`pending` 两个 List

- **两张表 + 两个 List**:`users`(所有用户)和 `pending`(待审申请)两个 `List` 与表格行**一一对应**。
- 关键设计:表格只显示数据,但每行对应的**原始对象**存在 List 里 → 操作时能取到完整对象(id/userId 等)。

## 3.2 选中行 → 对象(关键套路)

> 📍 [`selectedRequest()` L306-312](../../src/main/java/com/game/ui/AdminFrame.java#L306) · [`selectedUser()` L319-325](../../src/main/java/com/game/ui/AdminFrame.java#L319)

```java
private User selectedUser() {
    int row = userTable.getSelectedRow();          // 表格选中行号
    if (row < 0 || users == null || row >= users.size()) return null;
    return users.get(row);                          // 行号 → List 里的对象
}
```

**为什么这么设计**:JTable 只存"显示用"的字符串,但删除/审核需要 `id`/`userId`。所以用一个并行 List 存原始对象,靠**行号**把表格行映射回对象。这是 Swing 表格操作的常用套路。

## 3.3 三个操作(都调 02 讲过的 DAO)

> 📍 [`doApprove()` L194-220](../../src/main/java/com/game/ui/AdminFrame.java#L194) · [`doReject()` L225-249](../../src/main/java/com/game/ui/AdminFrame.java#L225) · [`doDeleteUser()` L254-291](../../src/main/java/com/game/ui/AdminFrame.java#L254)

| 按钮 | 调 DAO | 关键点 |
|---|---|---|
| 通过 | `resetDao.approve(id, userId)` | 事务(02 §3.4 讲过);成功→"已重置为 123456";admin 不可重置→"已置拒绝" |
| 拒绝 | `resetDao.reject(id)` | 置 rejected + 记处理时间 |
| 删除用户 | `userDao.deleteUser(id)` | **二次确认** + admin 不可删(UI 先拦,DAO 兜底) + 三表删除事务([审计修复②](../v1.0_审计修复/扩展_审计修复_数据与玩法.md)) |

每个操作后都 `doRefresh()`(L296)重载两张表。

## 3.4 想改 X 动哪里

- 想加"批量删除":`selectedUser` 改成支持多选 + 循环 deleteUser。
- 想看用户战绩:选中用户 → 加按钮查 `RecordDao.mine(userId)` 弹窗显示。

---

# §4 全项目代码精读完成 🎉

四站走完,你现在能把整个项目从入口到收尾讲清楚:

| 站 | 文件 | 你学到了 |
|---|---|---|
| [01](./01_代码精读_入口与登录切片.md) | GameApp / LoginFrame / UserDao / DBUtil / MD5Util / User | EDT、三层架构、PreparedStatement 防注入、JDBC 连接、MD5、POJO |
| [02](./02_代码精读_注册改密忘记密码.md) | RegisterFrame / ChangePasswordDialog / LoginFrame.doResetRequest / UserDao / ResetRequestDao | UI→DAO 模式、查重、WHERE 带旧密码校验、跨 DAO 申请流程、事务 |
| [03](./03_代码精读_游戏核心.md) | GameObject / Player / Zombie / Bullet / Difficulty / GameController / GamePanel | OOP 多态、游戏主循环、圆形碰撞、刷怪递增、javax.swing.Timer、分层渲染 |
| **04** | SoundUtil / LeaderboardFrame / AdminFrame | 纯代码合成 PCM、单线路+队列+守护线程、状态驱动刷新、表格选中行映射对象 |

## 你现在能做的

- ✅ **看懂**:从 `main` 启动 → 登录 → 游戏(主循环/碰撞/计分) → 存档 → 排行榜,全链路能讲。
- ✅ **能改**:知道每个改动该动哪个文件(每站的「想改 X 动哪里」表)。
- ✅ **能答**:OOP 体现、防注入、事务、EDT、多态、难度系统、音效合成——答辩高频题都有底。

## 下一步建议

1. **对着 [00 总纲 §6 的 26 问](./00_答辩准备总纲_项目通览与学习路线.md) 自测**——能答上八成就 ready。
2. **走一遍 [00 §8.1 演示路径](./00_答辩准备总纲_项目通览与学习路线.md)** 实机演练(注册→登录→游戏→排行榜→admin 审核)。
3. **挑 2-3 个亮点深讲**(推荐:难度系统 + 音效合成 + 事务)——答辩时主动展示。
4. 有不懂的随时回来问;代码有想改的也随时说。

> 答辩一句话总结:「项目用三层架构(UI/游戏/数据),`GameObject` 接口体现多态,`Difficulty` 枚举参数化难度,`PreparedStatement` 防注入、事务保原子性,音效用代码现场合成 PCM;经历了 8 阶段开发 + 扩展 + 审计修复,文档代码一致。」

**祝答辩顺利!** 🎓

---

*相关文件:[`util/SoundUtil`](../../src/main/java/com/game/util/SoundUtil.java) · [`ui/LeaderboardFrame`](../../src/main/java/com/game/ui/LeaderboardFrame.java) · [`ui/AdminFrame`](../../src/main/java/com/game/ui/AdminFrame.java)。*
