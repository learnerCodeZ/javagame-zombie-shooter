package com.game.game;

import com.game.util.SoundUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 游戏大脑：持有玩家、僵尸、子弹与计分状态，驱动单帧推进。
 * <p>不含 Swing Timer，便于无界面单元测试。
 * <p>阶段⑥a 起额外维护一套视觉反馈：粒子、浮动得分文字、震屏与受伤红屏，
 * 由 {@link GamePanel} 在绘制时读取并渲染。
 */
public class GameController {

    /** 画布宽度 */
    public static final int WIDTH = 800;
    /** 画布高度 */
    public static final int HEIGHT = 600;
    /** 子弹速度（像素/帧） */
    private static final double BULLET_SPEED = 8;
    /** 僵尸生成间隔（帧，约 1.5 秒） */
    private static final int SPAWN_INTERVAL = 90;
    /** 难度递增后的最小刷怪间隔（帧，约 0.5 秒） */
    private static final int MIN_SPAWN = 30;
    /** 逻辑帧率：update() 每调用一次算一帧，每 {@value} 帧折算 1 逻辑秒（暂停时不推进）。 */
    private static final int FRAMES_PER_SEC = 60;
    /** 僵尸撞击玩家造成的伤害 */
    private static final int HIT_DAMAGE = 20;
    /** Brute 开始出现的存活秒数门槛 */
    private static final int BRUTE_MIN_SEC = 10;
    /** 每次普通刷怪替换为 Brute 的概率 */
    private static final double BRUTE_CHANCE = 0.18;
    /** 受伤红屏持续帧数（用于换算 0~1 强度） */
    private static final int DAMAGE_FLASH_MAX = 20;

    private final Player player;
    private final List<Zombie> zombies;
    private final List<Bullet> bullets;
    private final List<Particle> particles;
    private final List<FloatingText> floatingTexts;
    private final Random random;

    private int moveDx;
    private int moveDy;
    private int score;
    private int killCount;
    private boolean running;
    private int frameCounter;

    /** 震屏剩余帧 / 总帧（用于线性衰减幅度） / 幅度（像素） */
    private int shakeFrames;
    private int shakeMaxFrames;
    private int shakeMag;
    /** 本帧震屏偏移（绘制时由画布读取，保证一帧内一致） */
    private int shakeOffsetX;
    private int shakeOffsetY;
    /** 受伤红屏剩余帧 */
    private int damageFlashFrames;

    /** 游戏结束回调（在玩家死亡的当帧触发一次） */
    private Runnable onGameOver;
    /** 保证 onGameOver 只触发一次 */
    private boolean gameOverFired;

    /**
     * 构造方法：玩家置于画布中央，计分清零，开始计时。
     */
    public GameController() {
        this.player = new Player(WIDTH / 2.0, HEIGHT / 2.0);
        this.zombies = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.particles = new ArrayList<>();
        this.floatingTexts = new ArrayList<>();
        this.random = new Random();
        this.score = 0;
        this.killCount = 0;
        this.running = true;
        this.frameCounter = 0;
        this.gameOverFired = false;
    }

    /**
     * 根据鼠标位置设定玩家朝向角度。
     *
     * @param mx 鼠标横坐标
     * @param my 鼠标纵坐标
     */
    public void setAim(int mx, int my) {
        double dx = mx - player.getX();
        double dy = my - player.getY();
        player.setAngle(Math.atan2(dy, dx));
    }

    /**
     * 设置玩家移动方向（来自键盘输入），每帧由画布回填。
     * <p>dx/dy 取 -1/0/1：对键抵消即静止（如左右同按 dx=0），非零组合由 update() 归一化为斜走。
     *
     * @param dx 横向分量（右 +1 / 左 -1）
     * @param dy 纵向分量（下 +1 / 上 -1）
     */
    public void setMoveDir(int dx, int dy) {
        this.moveDx = dx;
        this.moveDy = dy;
    }

    /**
     * 在枪口位置沿当前朝向、以子弹速度发射一枚子弹，并溅出枪口火花。
     */
    public void shoot() {
        double angle = player.getAngle();
        double muzzle = player.getRadius() + 8;
        double bx = player.getX() + Math.cos(angle) * muzzle;
        double by = player.getY() + Math.sin(angle) * muzzle;
        double vx = Math.cos(angle) * BULLET_SPEED;
        double vy = Math.sin(angle) * BULLET_SPEED;
        bullets.add(new Bullet(bx, by, vx, vy));
        spawnMuzzleFlash(bx, by, angle);
        SoundUtil.shoot();
    }

    /**
     * 推进一帧：更新对象 → 更新特效 → 按节奏刷怪 → 碰撞检测 → 清理 → 判定游戏结束。
     * <p>游戏未运行时直接返回，保证可被反复安全调用。
     */
    public void update() {
        if (!running) {
            return;
        }
        // 0) 应用玩家移动输入（WASD / 方向键）：斜走归一化，并钳制在画布内
        int dx = moveDx;
        int dy = moveDy;
        if (dx != 0 || dy != 0) {
            double len = Math.hypot(dx, dy);
            double vx = dx / len * Player.SPEED;
            double vy = dy / len * Player.SPEED;
            double nx = clamp(player.getX() + vx, player.getRadius(), WIDTH - player.getRadius());
            double ny = clamp(player.getY() + vy, player.getRadius(), HEIGHT - player.getRadius());
            player.setX(nx);
            player.setY(ny);
        }
        // 1) 推进所有对象状态（玩家衰减红闪；僵尸先更新追踪目标为玩家当前位置，再移动）
        player.update();
        for (Zombie z : zombies) {
            z.setTarget(player.getX(), player.getY());
            z.update();
        }
        for (Bullet b : bullets) {
            b.update();
        }
        // 1.5) 推进视觉特效（粒子 / 浮动文字 / 震屏 / 受伤红屏），并清理已消亡的特效
        for (Particle p : particles) {
            p.update();
        }
        for (FloatingText ft : floatingTexts) {
            ft.update();
        }
        particles.removeIf(Particle::isDead);
        floatingTexts.removeIf(FloatingText::isDead);
        updateShake();
        if (damageFlashFrames > 0) {
            damageFlashFrames--;
        }
        // 2) 按帧节奏刷怪：随存活秒数，间隔从 90 帧逐步降到 30 帧
        frameCounter++;
        int interval = Math.max(MIN_SPAWN, SPAWN_INTERVAL - getElapsedSec());
        if (frameCounter % interval == 0) {
            spawnZombie();
        }
        // 3) 碰撞检测（只改状态，不增删集合元素，避免并发修改）
        handleCollisions();
        // 4) 清理：出界或已标记删除的子弹、已死亡的僵尸
        bullets.removeIf(b -> b.isDead() || b.isOffscreen(WIDTH, HEIGHT));
        zombies.removeIf(Zombie::isDead);
        // 5) 游戏结束判定
        if (player.getHp() <= 0) {
            running = false;
            fireGameOver();
        }
    }

    /**
     * 从四条边随机位置生成一只僵尸；存活满 {@value #BRUTE_MIN_SEC} 秒后，
     * 有 {@value #BRUTE_CHANCE} 概率替换为更慢、更肉、得分更高的 Brute。
     */
    private void spawnZombie() {
        boolean brute = getElapsedSec() >= BRUTE_MIN_SEC && random.nextDouble() < BRUTE_CHANCE;
        double speed = brute ? (0.6 + random.nextDouble() * 0.4) : (1.0 + random.nextDouble());
        double px = player.getX();
        double py = player.getY();
        double x;
        double y;
        int edge = random.nextInt(4);
        switch (edge) {
            case 0: // 上边
                x = random.nextDouble() * WIDTH;
                y = 0;
                break;
            case 1: // 下边
                x = random.nextDouble() * WIDTH;
                y = HEIGHT;
                break;
            case 2: // 左边
                x = 0;
                y = random.nextDouble() * HEIGHT;
                break;
            default: // 右边
                x = WIDTH;
                y = random.nextDouble() * HEIGHT;
                break;
        }
        Zombie.Type type = brute ? Zombie.Type.BRUTE : Zombie.Type.NORMAL;
        zombies.add(new Zombie(type, x, y, speed, px, py));
    }

    /**
     * 碰撞检测：①子弹击中僵尸；②僵尸撞到玩家。
     */
    private void handleCollisions() {
        // ① 子弹 × 僵尸：圆心距离 < 半径之和即命中
        for (Bullet b : bullets) {
            if (b.isDead()) {
                continue;
            }
            for (Zombie z : zombies) {
                if (z.isDead()) {
                    continue;
                }
                double dx = b.getX() - z.getX();
                double dy = b.getY() - z.getY();
                if (Math.hypot(dx, dy) < b.getRadius() + z.getRadius()) {
                    z.takeDamage(1);
                    b.setDead(true);
                    spawnHitSparks(b.getX(), b.getY());
                    SoundUtil.hit();
                    if (z.isDead()) {
                        killCount++;
                        score += z.getScoreValue();
                        spawnZombieDeath(z);
                        floatingTexts.add(new FloatingText(z.getX(), z.getY() - z.getRadius(),
                                "+" + z.getScoreValue(), new Color(255, 230, 90), 40));
                    }
                    break; // 一颗子弹只命中一只僵尸
                }
            }
        }
        // ② 僵尸 × 玩家：撞到后玩家扣血、僵尸消失（避免反复扣血）
        for (Zombie z : zombies) {
            if (z.isDead()) {
                continue;
            }
            double dx = z.getX() - player.getX();
            double dy = z.getY() - player.getY();
            if (Math.hypot(dx, dy) < z.getRadius() + player.getRadius()) {
                player.takeDamage(HIT_DAMAGE);
                SoundUtil.hurt();
                z.kill();
                spawnZombieDeath(z);
                triggerShake(14, 6);
                damageFlashFrames = DAMAGE_FLASH_MAX;
            }
        }
    }

    /**
     * 在枪口朝向溅出一束橙黄火花。
     *
     * @param x     枪口横坐标
     * @param y     枪口纵坐标
     * @param angle 朝向角度
     */
    private void spawnMuzzleFlash(double x, double y, double angle) {
        for (int i = 0; i < 6; i++) {
            double spread = (random.nextDouble() - 0.5) * 0.8;
            double a = angle + spread;
            double sp = 2.0 + random.nextDouble() * 3.0;
            int g = 160 + random.nextInt(80); // 偏橙偏黄的火花
            particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                    new Color(255, g, 40), 3, 8 + random.nextInt(6)));
        }
        // 中央白色高光闪点
        particles.add(new Particle(x, y, 0, 0, new Color(255, 245, 200), 5, 4));
    }

    /**
     * 子弹命中处溅出几颗黄白火花。
     *
     * @param x 命中横坐标
     * @param y 命中纵坐标
     */
    private void spawnHitSparks(double x, double y) {
        for (int i = 0; i < 5; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double sp = 1.0 + random.nextDouble() * 2.5;
            particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                    new Color(255, 230, 120), 2, 6 + random.nextInt(5)));
        }
    }

    /**
     * 僵尸死亡爆裂：按其血液颜色四散喷溅。
     *
     * @param z 死亡的僵尸
     */
    private void spawnZombieDeath(Zombie z) {
        Color blood = z.getBloodColor();
        int count = z.getType() == Zombie.Type.BRUTE ? 16 : 10;
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double sp = 1.0 + random.nextDouble() * 3.5;
            particles.add(new Particle(z.getX(), z.getY(), Math.cos(a) * sp, Math.sin(a) * sp,
                    blood, 3, 12 + random.nextInt(10)));
        }
    }

    /**
     * 触发一次震屏（线性衰减）。
     *
     * @param frames 持续帧数
     * @param mag    最大幅度（像素）
     */
    private void triggerShake(int frames, int mag) {
        this.shakeFrames = frames;
        this.shakeMaxFrames = frames;
        this.shakeMag = mag;
    }

    /**
     * 推进震屏：按剩余比例衰减幅度，归零后复位偏移。
     */
    private void updateShake() {
        if (shakeFrames > 0) {
            int m = Math.max(1, (int) Math.round(shakeMag * (double) shakeFrames / shakeMaxFrames));
            shakeOffsetX = random.nextInt(2 * m + 1) - m;
            shakeOffsetY = random.nextInt(2 * m + 1) - m;
            shakeFrames--;
            if (shakeFrames == 0) {
                shakeOffsetX = 0;
                shakeOffsetY = 0;
            }
        }
    }

    /**
     * 触发游戏结束回调（仅触发一次）。
     */
    private void fireGameOver() {
        if (gameOverFired) {
            return;
        }
        gameOverFired = true;
        SoundUtil.gameOver();
        if (onGameOver != null) {
            onGameOver.run();
        }
    }

    /**
     * 设置游戏结束回调。
     *
     * @param onGameOver 回调（在玩家死亡当帧于调用线程触发一次）
     */
    public void setOnGameOver(Runnable onGameOver) {
        this.onGameOver = onGameOver;
    }

    /**
     * 将 v 钳制到 [min, max] 区间。
     *
     * @param v   原值
     * @param min 下界
     * @param max 上界
     * @return 钳制后的值
     */
    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public Player getPlayer() {
        return player;
    }

    public List<Zombie> getZombies() {
        return zombies;
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public List<Particle> getParticles() {
        return particles;
    }

    public List<FloatingText> getFloatingTexts() {
        return floatingTexts;
    }

    /** 本帧震屏横向偏移（像素，绘制世界层时叠加）。 */
    public int getShakeX() {
        return shakeOffsetX;
    }

    /** 本帧震屏纵向偏移（像素，绘制世界层时叠加）。 */
    public int getShakeY() {
        return shakeOffsetY;
    }

    /** 受伤红屏强度（0~1，0 表示无）。 */
    public float getDamageFlash() {
        return DAMAGE_FLASH_MAX == 0 ? 0f : damageFlashFrames / (float) DAMAGE_FLASH_MAX;
    }

    public int getScore() {
        return score;
    }

    public int getKillCount() {
        return killCount;
    }

    public int getHp() {
        return player.getHp();
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * 已存活秒数（纯游戏内时间，不受暂停影响）。
     * <p>基于 {@code frameCounter} 折算（每 {@value #FRAMES_PER_SEC} 帧为 1 秒）。暂停时
     * {@code update()} 不被调用、frameCounter 不增长，因此暂停期间不计入存活时间——
     * 这使存档的 surviveSec、刷怪间隔与 Brute 门槛都只反映实际游玩时间。
     *
     * @return 从开始到当前的整秒数
     */
    public int getElapsedSec() {
        return frameCounter / FRAMES_PER_SEC;
    }
}
