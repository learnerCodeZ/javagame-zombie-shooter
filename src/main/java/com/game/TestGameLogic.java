package com.game;

import com.game.game.GameController;

/**
 * 阶段④游戏核心逻辑的无界面测试（带 main，与 {@code TestDao} 同级）。
 * <p>不依赖 Swing 显示，直接驱动 {@link GameController} 跑 600 帧，验证：
 * <ul>
 *   <li>僵尸按节奏生成（zombies 列表曾大于 0）；</li>
 *   <li>{@code update()} 全程不抛异常（含碰撞遍历与 removeIf 安全清理）；</li>
 *   <li>分数/击杀随碰撞上涨，或至少逻辑跑通不崩；</li>
 *   <li>玩家血量归零时 {@code onGameOver} 回调被触发。</li>
 * </ul>
 */
public class TestGameLogic {

    public static void main(String[] args) {
        GameController controller = new GameController();

        // 用一维数组当标志位，记录游戏结束回调是否触发
        boolean[] gameOverFired = {false};
        controller.setOnGameOver(() -> gameOverFired[0] = true);

        boolean everSpawned = false; // 是否曾生成过僵尸
        int ranFrames = 0;           // 实际跑过的帧数（异常则提前结束，小于 600）

        try {
            for (int frame = 0; frame < 600; frame++) {
                // 每 30 帧沿默认角度开一枪
                if (frame % 30 == 0) {
                    controller.shoot();
                }
                // 每帧推进一次游戏逻辑
                controller.update();
                ranFrames = frame + 1;
                if (!controller.getZombies().isEmpty()) {
                    everSpawned = true;
                }
            }
        } catch (Throwable t) {
            // 全程不得抛异常；若抛出则打印并保留已跑结果
            System.out.println("【异常】update/shoot 抛出异常：" + t);
            t.printStackTrace();
        }

        System.out.println("=== 阶段④ 游戏逻辑测试 ===");
        System.out.println("跑的帧数         : " + ranFrames);
        System.out.println("最终 score       : " + controller.getScore());
        System.out.println("killCount        : " + controller.getKillCount());
        System.out.println("hp               : " + controller.getHp());
        System.out.println("剩余 zombies 数  : " + controller.getZombies().size());
        System.out.println("是否曾生成僵尸   : " + everSpawned);
        System.out.println("是否触发 gameover: " + gameOverFired[0]);
        System.out.println("controller 运行中 : " + controller.isRunning());
    }
}
