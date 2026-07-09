package com.game.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 游戏大脑：持有玩家、僵尸、子弹与计分状态，驱动单帧推进。
 * <p>不含 Swing Timer，便于无界面单元测试。
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
    /** 击杀一只僵尸得分 */
    private static final int KILL_SCORE = 10;
    /** 僵尸撞击玩家造成的伤害 */
    private static final int HIT_DAMAGE = 20;

    private final Player player;
    private final List<Zombie> zombies;
    private final List<Bullet> bullets;
    private final Random random;

    private int score;
    private int killCount;
    private final long startMs;
    private boolean running;
    private int frameCounter;

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
        this.random = new Random();
        this.score = 0;
        this.killCount = 0;
        this.startMs = System.currentTimeMillis();
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
     * 在枪口位置沿当前朝向、以子弹速度发射一枚子弹。
     */
    public void shoot() {
        double angle = player.getAngle();
        double muzzle = player.getRadius() + 8;
        double bx = player.getX() + Math.cos(angle) * muzzle;
        double by = player.getY() + Math.sin(angle) * muzzle;
        double vx = Math.cos(angle) * BULLET_SPEED;
        double vy = Math.sin(angle) * BULLET_SPEED;
        bullets.add(new Bullet(bx, by, vx, vy));
    }

    /**
     * 推进一帧：更新对象 → 按节奏刷怪 → 碰撞检测 → 清理 → 判定游戏结束。
     * <p>游戏未运行时直接返回，保证可被反复安全调用。
     */
    public void update() {
        if (!running) {
            return;
        }
        // 1) 推进所有对象状态
        for (Zombie z : zombies) {
            z.update();
        }
        for (Bullet b : bullets) {
            b.update();
        }
        // 2) 按帧节奏刷怪
        frameCounter++;
        if (frameCounter % SPAWN_INTERVAL == 0) {
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
     * 从四条边随机位置生成一只僵尸，速度 1~2 像素/帧，目标为玩家。
     */
    private void spawnZombie() {
        double speed = 1.0 + random.nextDouble(); // 1.0 ~ 2.0
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
        zombies.add(new Zombie(x, y, speed, px, py));
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
                    if (z.isDead()) {
                        killCount++;
                        score += KILL_SCORE;
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
                z.kill();
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

    public Player getPlayer() {
        return player;
    }

    public List<Zombie> getZombies() {
        return zombies;
    }

    public List<Bullet> getBullets() {
        return bullets;
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
     * 已存活秒数。
     *
     * @return 从开始到当前的整秒数
     */
    public int getElapsedSec() {
        return (int) ((System.currentTimeMillis() - startMs) / 1000);
    }
}
