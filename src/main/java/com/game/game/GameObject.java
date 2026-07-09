package com.game.game;

import java.awt.Graphics2D;

/**
 * 游戏对象统一接口：所有可更新、可绘制的实体都实现它。
 * <p>约定坐标以画布像素为单位，原点在左上角，x 向右、y 向下。
 */
public interface GameObject {

    /**
     * 每帧推进自身状态（位置、血量等）。
     */
    void update();

    /**
     * 在画布上绘制自身。
     *
     * @param g 画布绘图上下文
     */
    void draw(Graphics2D g);
}
