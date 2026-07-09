package com.game.util;

/**
 * 全局游戏设置（音量 / 静音），仅作用于本次运行。
 * <p>使用静态字段保存，不持久化到数据库或文件；程序重启后回到默认值。
 * <p>本阶段（⑥b）供 {@link SoundUtil} 在合成音效前读取，决定是否播放与音量大小；
 * 局内"设置 / 暂停"菜单（{@code GameWindow.openSettings}）会实时写入这些值。
 */
public final class GameSettings {

    private GameSettings() {
        // 工具类，不实例化
    }

    /** 音量（0~100），默认 70 */
    private static int volume = 70;
    /** 是否静音，默认 false */
    private static boolean muted = false;

    /**
     * 获取当前音量。
     *
     * @return 音量（0~100）
     */
    public static int getVolume() {
        return volume;
    }

    /**
     * 设置音量；超出 [0,100] 会被钳制到区间内。
     *
     * @param volume 音量（0~100）
     */
    public static void setVolume(int volume) {
        GameSettings.volume = Math.max(0, Math.min(100, volume));
    }

    /**
     * 是否静音。
     *
     * @return true 表示当前静音
     */
    public static boolean isMuted() {
        return muted;
    }

    /**
     * 设置静音状态。
     *
     * @param muted true 表示静音
     */
    public static void setMuted(boolean muted) {
        GameSettings.muted = muted;
    }
}
