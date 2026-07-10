# 学习笔记：Java 图片处理（抠图 / 贴图 / 翻转 / 染色）

> 结合本项目（打僵尸游戏）的真实代码讲，不空谈。
> 涉及文件：`game/Zombie.java`、`game/Player.java`（贴图/翻转/染色）、抠图临时程序（`CutZombie`/`CutPlayer`/`ResizePlayer`，用完即删）、素材 `resources/images/*.png`。
> 适用：想在 Java（Swing/Java2D）里做游戏精灵图、图片抠背景、贴图、翻转的场合。
> 日期：2026-07-10

---

## 0. 总览：游戏里"角色 = 一张图"要解决的事

把一个角色从"代码画的圆"换成"一张图片"，要踩过这些坑：

| 事 | 用到的 Java API | 本项目代码 |
|---|---|---|
| 读图（从资源加载） | `ImageIO.read(InputStream)` | `Zombie.sprite()` / `Player.sprite()` |
| 像素读写 | `BufferedImage.getRGB/setRGB` | 抠图程序 |
| 抠背景 → 透明 | flood-fill + `setRGB(x,y,0)` | `CutZombie` / `CutPlayer` |
| 缩放（省体积/提速） | `Image.getScaledInstance` | `ResizePlayer`（2048→128） |
| 贴到画布（实时缩放） | `Graphics.drawImage` | `Zombie.draw` / `Player.draw` |
| 左右翻转（朝向） | `drawImage` 10 参（反转目标 x） | `Zombie.draw` / `Player.draw` |
| 命中染色（闪白/红） | `AlphaComposite.SRC_ATOP` | `Zombie.draw` / `Player.draw` |
| 读图失败兜底 | try/catch + null 判断 | `sprite()` + draw 里 `if (sp != null)` |

下面逐个讲，**带真实代码 + 为什么这么写**。

---

## 1. 读图：从 classpath 加载（打包后也能用）

```java
// Zombie.java
private static BufferedImage SPRITE;

private static BufferedImage sprite() {
    if (SPRITE == null) {
        try {
            SPRITE = ImageIO.read(Zombie.class.getResourceAsStream("/images/zombie.png"));
        } catch (IOException | IllegalArgumentException e) {
            SPRITE = null;                       // 读不到：保持 null，draw 回退画绿圆
        }
    }
    return SPRITE;
}
```

**关键点：**
- **用 `getResourceAsStream`，不用 `new File(...)`**。`File("src/main/resources/...")` 只在你电脑上、且只在项目里跑得通；打成 jar 后路径就错了。`getResourceAsStream("/images/zombie.png")` 走 **classpath**（`resources/` 编译后在 classpath 根），jar 里也能读。这是加载"打包进程序的资源"的正道。
- **懒加载 + 静态字段**：图只读一次存 `SPRITE`，之后每帧直接用，不重复读盘。
- **try/catch 兜底**：图片缺了/路径错了 → 返回 null，`draw` 里 `if (sp != null)` 判断，读不到就回退画原来的代码形状。**游戏不会因为一张图缺了就崩。**

> `db.properties` 也是这么读的（`DBUtil` 里 `getClassLoader().getResourceAsStream("db.properties")`）——同一种套路。

---

## 2. 像素就是一个 int：ARGB 打包 + 位运算

`BufferedImage.getRGB(x, y)` 返回一个 `int`，里面**打包了 4 个 0~255 的分量**：A(alpha 透明度)、R、G、B，从高到低各占 8 位：

```
一个像素 int = 0xAARRGGBB
  alpha 在最高 8 位
  red   在次高 8 位
  green 在再次 8 位
  blue  在最低 8 位
```

**用位运算拆出来**（抠图程序里判断背景色用）：

```java
int p = im.getRGB(x, y);
int a = (p >>> 24) & 0xff;   // alpha
int r = (p >> 16) & 0xff;    // red
int g = (p >>  8) & 0xff;    // green
int b =  p        & 0xff;    // blue
```

- `>>` 右移、`& 0xff` 取最低 8 位 → 拿到各分量。
- `>>>` 是**无符号右移**（alpha 在最高位，用 `>>>` 避免符号位干扰）。

**反过来，"全透明"像素**就是把整个 int 写成 `0`：

```java
im.setRGB(x, y, 0);   // 0 = 0x00000000 → alpha=0（完全透明），画的时候看不见
```

> 这就是抠图"去背景"的本质：把背景像素的 int 设成 `0`（alpha=0），贴图时那些位置就透出来了。

---

## 3. 抠图：flood-fill 把背景"连片"变透明

这是整篇最核心的算法。需求：一张有背景的角色图（如僵尸在灰背景上），要把背景去掉、只留角色，存成透明 PNG。

### 3.1 思路
1. 判断"哪个颜色是背景" → 取**图像四角**的平均色（四角几乎总是背景）。
2. 从图像**四条边**开始，像水漫一样（flood-fill / BFS）扩散：只要当前像素"接近背景色"，就把它置透明，并继续往邻居扩散。
3. 角色（颜色离背景远）会**挡住**扩散，天然保留。

为什么从**边**开始？因为背景连着边、角色在中间；从边扩散只会吃掉背景连片，不会误伤被角色包围的内部。

### 3.2 真实代码（CutZombie 的核心）

```java
BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
im.getGraphics().drawImage(src, 0, 0, null);   // 原图复制到 ARGB 画布（能改 alpha）

// ① 背景参考色 = 四角平均
long sr = 0, sg = 0, sb = 0;
int[][] corners = {{0,0},{w-1,0},{0,h-1},{w-1,h-1}};
for (int[] c : corners) {
    int p = im.getRGB(c[0], c[1]);
    sr += (p >> 16) & 0xff;  sg += (p >> 8) & 0xff;  sb += p & 0xff;
}
int br = (int)(sr/4), bgc = (int)(sg/4), bb = (int)(sb/4);

// ② 判断一个像素算不算背景：与背景色的欧氏距离 < 容差
final double tol = 55.0;
// isBg(x,y):
int p = im.getRGB(x, y);
int r = (p >> 16) & 0xff, g = (p >> 8) & 0xff, b = p & 0xff;
double d = Math.sqrt((r-br)*(r-br) + (g-bgc)*(g-bgc) + (b-bb)*(b-bb));
return d < tol;

// ③ 从四边每个"背景像素"入队，BFS 扩散；遍历到的置透明
ArrayDeque<int[]> q = new ArrayDeque<>();
for (int x = 0; x < w; x++) { seed(x,0); seed(x,h-1); }   // 上下边
for (int y = 0; y < h; y++) { seed(0,y); seed(w-1,y); }   // 左右边
while (!q.isEmpty()) {
    int[] p = q.poll();
    im.setRGB(p[0], p[1], 0);                              // ★ 置透明
    for (邻居) { if (isBg(邻居)) { 标记已访问; q.add(邻居); } }
}

// ④ 存成透明 PNG
ImageIO.write(im, "png", out);
```

### 3.3 关键细节（都是踩过的坑）

**① 容差 `tol` 要按背景定**
- 灰背景（僵尸原图 `#9b9cab`）用 **55**：背景不是纯一色（四角略有波动 + JPG 噪点），容差给够才能把整片灰都吃掉。
- 纯黑背景（玩家图 `#000000`）用 **35**（更小）：因为角色身上也有偏暗的部分（中心 `rgb(20,26,30)`，离黑约 44），容差太大（>44）会把角色暗部也当背景啃掉。小容差只去"接近纯黑"的，保留角色。
- **规律**：背景越"纯"、角色越"暗"，容差要越小；背景越"杂"、角色越"亮"，容差可以大。

**② 用"连通"(flood-fill)而不是"全局颜色匹配"**
如果只做"凡是接近背景色的像素都透明"，会把角色身上**碰巧接近背景色的部分**也挖空（比如角色身上的灰色衣服）。flood-fill 只去**和边连通**的背景，角色内部的同色区域被角色轮廓包围、连不到边 → 保留。这是它比"按颜色一刀切"干净的根本原因。

**③ 背景"四角平均"自动探测**
不写死背景色（换个图就得改代码），而是**每张图现算四角平均**当背景——换图也能用，更通用。

**④ 必须是 `TYPE_INT_ARGB`**
抠图要改 alpha（透明度），只有 ARGB 类型有 alpha 通道。原图如果是 RGB（无 alpha），先 `new BufferedImage(w,h,TYPE_INT_ARGB)` 再 `drawImage` 复制过去。

**⑤ JPG 没有 alpha、且有压缩噪点**
玩家原图是 `.jpg`：JPG 不支持透明（必须抠成 PNG），而且 JPG 在角色边缘有压缩杂色 → 抠出来边缘可能略糙。**PNG 原图抠出来最干净**。所以"能要 PNG 就别要 JPG"。

**⑥ 水印是烧进图的文字，抠不掉**
"豆包生成"水印是文字像素（颜色和背景不同），flood-fill 不会去掉它（它不是背景色）。要么**先用修图软件擦掉**，要么抠图时**把水印所在区域一起强制清透明**。

---

## 4. 缩放：大图先缩再存（省体积 + 提速）

玩家原图 `2048×2048`，抠完 1.4MB。游戏里只画 ~52px，每帧把 2048 缩到 52 是浪费。**预处理缩到 128 再存**：

```java
BufferedImage out = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
out.getGraphics().drawImage(
    src.getScaledInstance(128, 128, Image.SCALE_SMOOTH), 0, 0, null);
ImageIO.write(out, "png", f);
```

- `getScaledInstance(w, h, Image.SCALE_SMOOTH)`：质量较好的缩放（双线性插值类）。
- 缩完 1.4MB → ~10KB，每帧缩放开销也小。
- 128 是个合适的精灵图尺寸（游戏里画 40~52px，128 缩过去足够清晰，又不太大）。

> 僵尸原图本来就是 128×135，不用缩。

---

## 5. 贴图：`drawImage` 缩放到角色大小，居中

```java
// Zombie.draw 里
int ix = (int) x, iy = (int) y;          // 角色中心(整数像素)
BufferedImage sp = sprite();
if (sp != null) {
    int sw = sp.getWidth(), sh = sp.getHeight();
    // 把整张图(0,0)~(sw,sh) 缩放到 (ix-r, iy-r)~(ix+r, iy+r) 即直径 2r，居中
    g.drawImage(sp, ix - radius, iy - radius, ix + radius, iy + radius,
                0, 0, sw, sh, null);
}
```

**10 参 `drawImage(img, dx1,dy1, dx2,dy2, sx1,sy1, sx2,sy2, observer)`** 的含义：
- 把**源图**的矩形 `(sx1,sy1)~(sx2,sy2)` → 缩放画到**目标**矩形 `(dx1,dy1)~(dx2,dy2)`。
- 这里源是整张图 `0,0,sw,sh`，目标是 `ix-r,iy-r ~ ix+r,iy+r`（一个 2r×2r 的方块，居中于角色）。
- 比 4 参 `drawImage(img,x,y,w,h)` 更灵活——**下一个翻转就靠它**。

---

## 6. 翻转/镜像：反转"目标 x"就是水平翻转 ⭐

角色要"往哪走朝哪"。最省事的做法不是旋转（侧视人形旋转会歪），而是**水平镜像**。

**10 参 drawImage 的妙用：把目标矩形 x 坐标反过来画，就是镜像。**

```java
// Zombie.draw
boolean facingLeft = ...;   // 往左走=true
if (facingLeft) {
    // 注意 dx1=ix+r(右)、dx2=ix-r(左) —— 目标 x 反向 = 水平翻转
    g.drawImage(sp, ix + radius, iy - radius, ix - radius, iy + radius,
                0, 0, sw, sh, null);
} else {
    // 正常：dx1=ix-r(左)、dx2=ix+r(右)
    g.drawImage(sp, ix - radius, iy - radius, ix + radius, iy + radius,
                0, 0, sw, sh, null);
}
```

**为什么反过来就翻转？** drawImage 是把源图的每一列映射到目标。正常 `dx1<dx2` 时源图左列画到目标左；当 `dx1>dx2`（目标 x 反向），源图左列被画到目标右 → 左右对调 = 镜像。**不用算变换矩阵，一行参数搞定。**

**朝向怎么决定：**
- 僵尸：`update()` 里看移动方向 `dx = targetX - x`，`dx<0`→往左→`facingLeft=true`；基本垂直移动（`|dx|` 极小）时**保持上次朝向**避免抖动。
- 玩家：看瞄准角 `Math.cos(angle) < 0`（鼠标在左边）→ 朝左。

---

## 7. 命中染色：`SRC_ATOP` 只染角色、不染透明区 ⭐

角色被击中要"闪白"、玩家受伤要"闪红"。贴图后，如果直接在角色范围画个半透明白矩形：

```java
// ❌ 错误做法：会画出"一个白方块"（把透明背景也染白了）
g.setColor(new Color(255,255,255,120));
g.fillRect(ix-r, iy-r, 2r, 2r);
```

因为透明背景也是这方块里的像素，会被一起染白 → 看起来是个白方块闪一下，不是"角色变白"。

**正确做法：`AlphaComposite.SRC_ATOP`** —— 它只在**已有不透明像素**（角色本体）上混合源色，透明区不受影响：

```java
// Zombie.draw（命中闪白）
if (hitFlash > 0) {
    float a = Math.min(1f, hitFlash / 6f) * 0.6f;     // 闪白强度（随帧衰减）
    Composite old = g.getComposite();                  // 保存原合成模式
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, a));
    g.setColor(Color.WHITE);
    g.fillRect(ix - radius, iy - radius, radius * 2, radius * 2);  // 只染到角色上
    g.setComposite(old);                               // ★ 用完必须恢复，否则后续绘制都受影响
}
```

**细节：**
- `SRC_ATOP`：源（你画的白）只出现在目标已有 alpha 的地方 → 角色变白，透明背景不变。
- `a` 是透明度（0~1），控制染色深浅，随 `hitFlash` 衰减 → 闪一下就恢复。
- **用完务必 `setComposite(old)` 恢复**！否则之后画的所有东西都走 SRC_ATOP，画面会乱。

> 玩家受伤红闪同理，把 `Color.WHITE` 换 `new Color(255,60,60)`。

---

## 8. 完整 draw 流程（贴图版，结合上面所有点）

```java
public void draw(Graphics2D g) {
    int ix = (int) x;
    int bob = (int) Math.round(Math.sin(animFrame * 0.25) * 2.0);  // 身体摇摆
    int iy = (int) y + bob;
    BufferedImage sp = sprite();                  // ① 懒加载精灵图(失败返回null)

    if (sp != null) {
        int sw = sp.getWidth(), sh = sp.getHeight();
        // ② 贴图 + 朝向镜像（10参 drawImage 反转目标x）
        if (facingLeft) {
            g.drawImage(sp, ix+r, iy-r, ix-r, iy+r, 0,0,sw,sh, null);
        } else {
            g.drawImage(sp, ix-r, iy-r, ix+r, iy+r, 0,0,sw,sh, null);
        }
        // ③ 命中闪白（SRC_ATOP 只染角色）
        if (hitFlash > 0) {
            float a = Math.min(1f, hitFlash/6f) * 0.6f;
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, a));
            g.setColor(Color.WHITE);
            g.fillRect(ix-r, iy-r, 2r, 2r);
            g.setComposite(old);
        }
    } else {
        // ④ 回退：读图失败时画代码形状，保证不崩
        ...画绿圆/橙猫...
    }
    // ⑤ 其它叠加（如 Brute 头顶血条）照常画
}
```

---

## 9. 细节与坑汇总

| 坑 / 细节 | 说明 |
|---|---|
| 用 `getResourceAsStream` 读图 | 别用 `new File`，打包成 jar 后 File 路径失效 |
| 像素是 ARGB 打包 int | `getRGB/setRGB`；`(p>>16)&0xff` 取红，`setRGB(x,y,0)` 置透明 |
| 抠图用 flood-fill 从边扩散 | 比"按颜色一刀切"干净（只去连通背景，不误伤角色内部同色） |
| 背景色四角自动平均 | 不写死，换图也能用 |
| 容差按背景定 | 背景纯+角色暗→小容差；背景杂+角色亮→大容差 |
| 必须 `TYPE_INT_ARGB` 才能改透明 | RGB 类型没 alpha 通道 |
| JPG 无 alpha + 有噪点 | 能用 PNG 就别用 JPG；JPG 抠出来边缘糙 |
| 水印是文字像素 | flood-fill 去不掉，得先擦掉或单独清那块区域 |
| 大图先缩再存 | 2048→128：1.4MB→10KB，每帧缩放也快 |
| 翻转用 10 参 drawImage 反转目标 x | 一行参数水平镜像，不用矩阵 |
| 染色用 SRC_ATOP | 只染不透明像素（角色），透明背景不变；用完恢复 composite |
| 单张图没法只转角色的枪 | 枪和身体一层像素；只能左右镜像，要"枪真转"得多方向素材 |
| 读图失败要回退 | try/catch + null + `if(sp!=null)`，缺图不崩 |

---

## 10. 一句话总结

> **读图用 `getResourceAsStream`（classpath）；像素是 ARGB int（位运算拆分）；抠背景靠 flood-fill 从边扩散置透明（容差按背景定）；贴图/翻转用 `drawImage`（10 参反转目标 x = 镜像）；命中染色用 `AlphaComposite.SRC_ATOP` 只染本体；读图失败回退画形状。** 全程只用 JDK，不装任何图像库。
