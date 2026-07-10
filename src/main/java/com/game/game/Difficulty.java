package com.game.game;

/**
 * 游戏难度档位：把"随难度而变"的刷怪与伤害参数集中在一处。
 * <p>{@link GameController} 构造时按所选档位读取这些字段并注入单局逻辑——
 * 同一套代码、不同的数值，实现"简单 / 困难"两种体验。
 *
 * <p>调难度只动这里的数值即可，{@code GameController} 里不再有任何写死的魔法数。
 * <ul>
 *   <li>{@link #EASY} 简单：沿用项目原始数值（开局 1.5s/只、封顶 0.5s/只；
 *       满 10s 后 18% 概率刷壮汉；撞到玩家扣 20 血）。</li>
 *   <li>{@link #HARD} 困难：刷怪更密（开局 1.0s/只、封顶 0.33s/只）、
 *       5s 起即有 35% 概率刷壮汉、撞到玩家扣 25 血。</li>
 * </ul>
 *
 * <p>两档的<b>玩家能力完全一致</b>（移速/血量/子弹不变），难度只来自威胁变强，
 * 而非削弱玩家。
 */
public enum Difficulty {

    /** 简单：原始数值，节奏从容。 */
    EASY(90, 30, 10, 0.18, 20, "简单"),

    /** 困难：刷怪更密更快、壮汉更早更多、单次撞击伤害更高。 */
    HARD(60, 20, 5, 0.35, 25, "困难");

    /** 初始刷怪间隔（帧，60 帧 ≈ 1 秒）。 */
    public final int spawnInterval;
    /** 随存活时间递增后的最小刷怪间隔（帧，封顶值）。 */
    public final int minSpawn;
    /** Brute（壮汉）开始出现的存活秒数门槛。 */
    public final int bruteMinSec;
    /** 每次刷怪替换为 Brute 的概率（0~1）。 */
    public final double bruteChance;
    /** 僵尸撞击玩家造成的伤害。 */
    public final int hitDamage;
    /** 中文显示名（主菜单弹窗 / 游戏窗口标题 / HUD 标签用）。 */
    public final String label;

    Difficulty(int spawnInterval, int minSpawn, int bruteMinSec,
               double bruteChance, int hitDamage, String label) {
        this.spawnInterval = spawnInterval;
        this.minSpawn = minSpawn;
        this.bruteMinSec = bruteMinSec;
        this.bruteChance = bruteChance;
        this.hitDamage = hitDamage;
        this.label = label;
    }
}
