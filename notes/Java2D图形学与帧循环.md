# Java 2D 图形学与「帧」循环

| 项 | 内容 |
|---|---|
| 问题 | ① 项目里的画图 / 合成 / 贴图算**计算机图形学**吗?② **「帧」**是怎么用代码实现的? |
| 结合 | 全部用本项目的真实代码讲:[`GamePanel`](../src/main/java/com/game/game/GamePanel.java)、[`GameController`](../src/main/java/com/game/game/GameController.java)、[`Player.draw`](../src/main/java/com/game/game/Player.java#L71)、[`SoundUtil`](../src/main/java/com/game/util/SoundUtil.java) |

---

## 一、这些算计算机图形学吗?(算,但是"应用层")

**算。** 项目里画图用的 `Graphics2D` / `BufferedImage` / `drawImage` / `AlphaComposite` / 坐标 / 像素 / 透明度——全是**计算机图形学的概念和 API**(Java 2D 本身就是一个图形学库)。

但它是**应用层**的图形学:**用人家封装好的库画图**,不是底层原理。你没自己写:
- **光栅化**(把几何图形变成屏幕像素);
- **矩阵投影 / 3D 变换**(那是 3D 图形学);
- **GPU / 着色器**(那是 OpenGL/DirectX 层)。

> 答辩可以说:「我用 **Java 2D** 这个图形学 API 做 2D 渲染——贴精灵图、坐标变换、合成、粒子,都属于计算机图形学的应用。」

### 项目里用到的图形学 API(对照看)

| API / 概念 | 用在哪 | 讲解 |
|---|---|---|
| `Graphics2D`(画笔) | 所有 `draw(Graphics2D g)` 的参数 | 每帧画东西的入口 |
| `BufferedImage`(内存图) | [`Player.SPRITE`](../src/main/java/com/game/game/Player.java#L25)/[`Zombie.SPRITE`](../src/main/java/com/game/game/Zombie.java#L27) | 把 PNG 读进内存缓冲,反复贴 |
| `drawImage` 10 参数版 | [`Player.draw`](../src/main/java/com/game/game/Player.java#L71) 贴精灵图 | 源图矩形 → 目标矩形,**缩放 + 水平镜像** |
| `AlphaComposite.SRC_ATOP` | [`Player.draw`](../src/main/java/com/game/game/Player.java#L88) 受伤红闪 | 合成规则,**只染已有像素**、不染透明区 |
| `fillOval`/`fillRect`/`fillPolygon`/`drawLine` | 回退画橙猫、粒子、血条、网格 | 基础几何图元 |
| **坐标系**(原点左上、y 向下) | [`GameObject`](../src/main/java/com/game/game/GameObject.java) 类注释约定 | 图形学惯例(和数学 y 朝上相反) |
| **采样 / PCM** | [`SoundUtil`](../src/main/java/com/game/util/SoundUtil.java) 合成音效 | 数字音频的"采样"和图形的"像素"是同类思想(连续信号→离散点) |

---

## 二、「帧」是怎么用代码实现的 ⭐

**关键认知:"帧"不是 Java 的语言特性,是你要自己用一个定时器循环"攒"出来的概念。** 核心就两样:`javax.swing.Timer` + `repaint()`。

### 帧 = `Timer` 触发的一次 `tick`

[`GamePanel`](../src/main/java/com/game/game/GamePanel.java) 里的真实代码:

```java
this.timer = new Timer(16, e -> tick());   // ← 每 16 毫秒触发一次 tick()

private void tick() {                       // ← 一次 tick = 一帧
    controller.setMoveDir(moveDirX(), moveDirY());
    controller.update();   // ① 这一帧的逻辑:移动、刷怪、碰撞、计分、清理...
    repaint();             // ② 请求重绘(系统稍后在 EDT 上调 paintComponent 画出这一帧)
    if (!controller.isRunning()) timer.stop();   // 游戏结束就停
}
```

**一帧 = 一次 `tick`** = ① 改状态(`update`) + ② 画出来(`repaint` → `paintComponent`)。

### 16ms 怎么等于 60 FPS

```
1000ms ÷ 16ms ≈ 62.5 次/秒 ≈ 60 帧/秒(60 FPS)
```

- `Timer(16, ...)` = 每 16ms 触发一次 → 每秒约 60 次 → **60 帧/秒**。
- 想改帧率:把 16 改成 33 → ≈ 30 FPS;改成 8 → ≈ 120 FPS。
- 「**FPS(Frames Per Second)= 每秒 tick 多少次**」——全由这个 `Timer` 间隔决定。

### `repaint()` 不是立刻画(重要)

`repaint()` 只是**"排队请求重绘"**,它**不会**马上画。系统在事件线程(EDT)空闲时,才真正调你的 `paintComponent(Graphics g)` 把这一帧画出来。所以:

```
Timer 每 16ms 触发
    │ (回调在 EDT 上)
    ▼
tick():  controller.update()   ← 改数据(对象位置/血量/粒子...)
    │
    └─ repaint()  ← 只是"申请重绘",排队
                    │
                    ▼ (EDT 稍后)
              paintComponent()  ← 用 Graphics2D 把新数据画出来
```

> **帧 = Timer 触发 → update 改数据 → repaint 排队 → paintComponent 画**。`update` 管"世界变成什么样",`paintComponent` 管"怎么画到屏幕"。

### 帧在本项目里还当"时间单位"

[`GameController`](../src/main/java/com/game/game/GameController.java) 里:

```java
private int frameCounter;                    // 每帧 +1(在 update 里)
public int getElapsedSec() {
    return frameCounter / FRAMES_PER_SEC;    // FRAMES_PER_SEC = 60
}
```

60 帧 = 1 秒。所以「存活 10 秒」= 跑了 600 帧;刷怪间隔也按帧算(`interval` 帧 = `interval/60` 秒)。**帧既是渲染单位,也是项目的计时单位**——好处是暂停时(`update` 不跑、`frameCounter` 不增)时间自动停。

### 为什么不会卡(不阻塞 EDT)

`Timer` 的回调在 **EDT** 上跑。如果某一帧的 `update` 很慢(比如里面 `sleep` 或连数据库),EDT 就被占住 → **界面卡顿**(下一帧画不出来)。本项目:

- `update` 很轻(纯计算:移动、碰撞、改列表),不卡。
- **音效**会卡(播音效是阻塞的)→ 所以 [`SoundUtil`](../src/main/java/com/game/util/SoundUtil.java) 用**单独的守护线程**播,不占 EDT。
- **数据库存档**会卡(连 MySQL)→ 结算时同步调 DAO 是**已知隐患**(见 [已知问题 #3](../docs/已知问题与后续优化.md)),生产应挪到后台线程。

> 所以「为什么音效用单独线程、而游戏逻辑不用」——答案就在帧循环:游戏逻辑轻、跑在 EDT 上没问题;音效/DB 是慢操作、必须挪出 EDT,否则卡帧。

---

## 三、图形学和帧怎么配合(一句话)

> **帧是节奏,图形学是画法。**

每帧由 `Timer` 触发;`update` 改「模型」(对象位置、血量、粒子寿命——这些是图形学要画的数据);`paintComponent` 用图形学 API(`Graphics2D` / `drawImage` / `AlphaComposite` / `fillOval`)把模型画到屏幕。**60 次/秒重复这个过程,画面就"动"起来了。**

---

## 四、自测

1. **为什么 `Timer(16, ...)` ≈ 60 FPS?想把帧率改成 30 FPS 怎么办?**
2. **`repaint()` 和 `paintComponent()` 是什么关系?调了 `repaint()` 就立刻画了吗?**
3. **项目里哪些地方用了图形学 API?各举一例(`drawImage` / `AlphaComposite` / `BufferedImage`)。**
4. **`tick()` 里能不能写 `Thread.sleep(50)`?为什么?**
5. **`getElapsedSec()` 为什么用 `frameCounter / 60` 而不是直接读系统时间?**
6. **为什么音效用单独的守护线程播,不放 `update` 里?**

<details><summary>参考答案</summary>

1. 1000ms ÷ 16ms ≈ 60。改 30 FPS 就把 16 改成 33(1000÷30)。
2. `repaint()` 是"请求重绘"(排队),`paintComponent()` 是"真正画"。调 `repaint()` 不会立刻画,系统在 EDT 空闲时才调 `paintComponent`。
3. `drawImage` 10 参数版贴精灵图 + 镜像(Player.draw);`AlphaComposite.SRC_ATOP` 受伤红闪(Player.draw);`BufferedImage` 存精灵图(Player.SPRITE/Zombie.SPRITE)。
4. 不能。`sleep` 会**卡住 EDT** → Timer 的下一次 tick 进不来、画面冻结。慢操作必须挪到别的线程(像 SoundUtil 那样)。
5. 因为暂停时 `update` 不跑、`frameCounter` 不增 → 用帧计数算时间,**暂停自动不计入**(读系统时间做不到这点)。
6. 播音效是阻塞操作(会等播放),放 `update`/EDT 里会卡帧;丢给守护线程,主循环立刻返回,不卡。

</details>

---

*相关代码:[`GamePanel`](../src/main/java/com/game/game/GamePanel.java)(Timer + tick + paintComponent)、[`GameController`](../src/main/java/com/game/game/GameController.java)(update + frameCounter)、[`Player.draw`](../src/main/java/com/game/game/Player.java#L71)(Graphics2D/drawImage/AlphaComposite)、[`SoundUtil`](../src/main/java/com/game/util/SoundUtil.java)(后台线程、不卡帧)。*
