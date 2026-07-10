# 扩展功能复盘：暗色 UI 主题 + 角色精灵图（抠图/贴图/朝向）

| 项 | 内容 |
|---|---|
| 范围 | ① 全界面统一暗色游戏风主题；② 僵尸/玩家从代码绘制换成抠图精灵图 + 按方向翻转（阶段⑥之后的美术增强） |
| 状态 | ✅ 完成、编译通过、已推送 |
| 日期 | 2026-07-10 |

---

## 一、暗色 UI 主题

### 1.1 做了什么
建了一个 `UIStyle` 工具类，把"现代游戏风暗色主题"（深底 + 橙/绿主色，贴橙猫打绿僵尸）统一套到**所有 Swing 界面**：登录/注册/主菜单/排行榜/用户管理/改密/游戏窗口+设置菜单+HUD。

### 1.2 核心知识点 ⭐

**① 用 `UIManager` 做全局主题（一次设置，全局生效）**
`UIStyle.initGlobalTheme()` 在 `GameApp.main` 启动时调一次，用 `UIManager.put("xxx.background/foreground", ...)` 给 Panel / OptionPane / Button / TextField / Table / TableHeader / ScrollPane / ScrollBar / CheckBox / ComboBox 等设暗色默认值。好处：`JOptionPane` 弹窗、`JTable` 这些**自动暗色**，不用每个手动配色。

**② 自绘组件（圆角按钮/输入框）**
`GameButton extends JButton`：`setContentAreaFilled(false)` 关掉默认填充，自己在 `paintComponent` 里 `fillRoundRect` 画底色 + 鼠标 hover 换色；白字 + 手型光标。`GameField/GamePasswordField` 同理自绘圆角暗色输入框。这是 Swing 做"扁平现代风"的标准套路。

**③ 配色/字体锁定在一个类**
所有颜色（BG/PANEL/FIELD/PRIMARY 橙/SECONDARY 绿/DANGER 红/TEXT/MUTED…）和字体（微软雅黑 TITLE/H2/BODY/BUTTON/SMALL）都是 `UIStyle` 的 `public static final` 常量，全站引用，**改一处全站变**。

**④ Look&Feel 的取舍**
故意用**跨平台 Metal L&F**（而非 Windows 原生），因为它最稳定地遵循上面的 `UIManager` 暗色覆盖；代价是不像原生 Windows 窗口（换来的是统一暗色现代风）。

**⑤ 为什么不用 FlatLaf 之类的主题库**
自己写 `UIStyle` 显得"我懂 Swing 主题机制"、零外部依赖；用库虽然省事但少了一份"自建主题系统"的体现——课设/答辩更看重自己实现。

### 1.3 踩坑 / 收获
- Swing 组件默认不透明，要逐个 `setOpaque(false)`/`setBackground`，否则透不出暗底。
- `JTable` 暗色最繁琐：行底/字色/表头/选中/网格/`JScrollPane` viewport/滚动条 都得单独设（封进 `UIStyle.table()` / `scrollPane()` 一次搞定）。
- 选中色用**实色**（非真 alpha 半透明），否则 JTable 重绘有残影。

---

## 二、角色精灵图（抠图 + 贴图 + 朝向翻转）

### 2.1 做了什么
把僵尸、玩家从"代码画圆/猫"换成**抠出来的透明背景精灵图**（`images/zombie.png`、`images/player.png`），按移动/瞄准方向**左右翻转**，玩家变大（半径 20→26，壮汉级）。

### 2.2 核心知识点 ⭐

**① 抠图：用 Java `ImageIO` + flood-fill 去背景（不装任何库）**
思路：读图 → 转成 `TYPE_INT_ARGB` → 算**四角平均色**当作背景 → 从图像**四边**做 flood-fill，凡与背景色欧氏距离 < 容差的**连通**像素都置 `alpha=0`（透明）→ 存 PNG。主体（僵尸绿/角色彩）离背景色远，天然挡住 flood-fill，不会被波及。
- 容差按背景定：**灰背景**（僵尸原图）用 55；**纯黑背景**（玩家图）用小容差 35，避免啃到角色偏暗的部分。
- 临时 Java 小程序（`CutZombie`/`CutPlayer` 等）编译跑完即删，**零下载零安装**，只用 JDK。

**② 透明 PNG**
PNG 支持 alpha 通道；抠图后保存的 `TYPE_INT_ARGB` 图，背景 `alpha=0`、角色 `alpha=255`，贴到游戏里就是"透明背景的角色"。

**③ `drawImage` 缩放贴图**
`g.drawImage(sprite, x, y, 直径, 直径, null)` 把精灵图缩放到角色大小（按 `radius*2`）。读图在静态字段懒加载一次（`ImageIO.read(getClass().getResourceAsStream("/images/xxx.png"))`），避免每帧重读。

**④ 朝向翻转：10 参 `drawImage` 反转目标 x = 水平镜像**
`drawImage(img, dx1,dy1, dx2,dy2, sx1,sy1, sx2,sy2, null)`：把**目标 x 坐标反向**（`ix+radius → ix-radius`）就实现水平翻转，不用算变换矩阵。
- 僵尸：`update()` 里看水平移动方向 `dx`，往左 `facingLeft=true`、往右 `false`；基本垂直移动时保持上次朝向避免抖动。
- 玩家：看瞄准角 `cos(angle)<0`（鼠标在左）就翻转。

**⑤ 读图失败回退（绝不崩）**
精灵图懒加载用 try/catch，读不到（路径错/文件缺）返回 null，`draw` 里 `if (sp != null) 贴图 else 画原来的代码形状`。音效也是这思路——**锦上添花的功能失败不能拖垮主流程**。

**⑥ `SRC_ATOP` 命中闪白/红只染色本体**
贴图后想"命中闪白"，若直接画白矩形会把透明区也染白。用 `AlphaComposite.SRC_ATOP` 画白/红：只在已有不透明像素（角色本体）上混合，透明背景不受影响——干净的"染色"效果。

**⑦ 大图先缩再存**
玩家原图 2048×2048（抠完 1.4MB），先缩到 128×128（~10KB）再存：省仓库体积、每帧缩放开销小。僵尸原图本就 128，无需缩。

### 2.3 踩坑 / 收获
- **单张图没法只转角色的枪**：枪和身体是一层像素，旋转整图侧视人形会"歪倒"；只翻转左右（镜像）最自然。要"枪真转"得提供多方向素材（真·魂斗罗式），单图做不到——本项目用"翻转 + 画面准星"表达瞄准。
- **flood-fill 容差要按背景调**：黑背景配小容差、灰背景配大容差；容差太大啃主体、太小留背景边。
- **JPG 没有透明通道**，必须抠图转 PNG 才能透明；且 JPG 有压缩噪点，边缘不如 PNG 干净。
- **水印是烧进图的文字**（非背景色），抠图去不掉，得**用户先在图里擦掉**（或抠图时连那块区域一起清透明）再抠。

---

## 三、整体收获
- **UI**：掌握了 Swing 的两种主题手段——`UIManager` 全局默认（对付 JOptionPane/表格等）+ 自绘组件（圆角按钮/输入框）；体会到"配色/字体集中一个工具类"的维护价值。
- **美术**：学会了**纯代码（JDK ImageIO）抠图**（flood-fill 去背景）、透明 PNG、`drawImage` 缩放与镜像、`SRC_ATOP` 局部染色、懒加载+回退兜底。
- **工程**：临时处理程序（抠图/缩放/探测）编译跑完即删、零依赖；图片大就先缩；可选功能失败要回退不崩——这些"小习惯"让项目更稳。

---

*相关文件：`util/UIStyle.java`（主题）、`game/Zombie.java` / `game/Player.java`（贴图+翻转）、`resources/images/zombie.png` / `player.png`（精灵图）。*
