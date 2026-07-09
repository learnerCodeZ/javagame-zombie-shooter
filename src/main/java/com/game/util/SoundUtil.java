package com.game.util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * 合成音效工具：用代码现场生成 PCM 采样并播放，不依赖任何外部音频文件。
 * <p>提供四种局内音效：{@link #shoot()} 开火、{@link #hit()} 命中僵尸、
 * {@link #hurt()} 玩家受伤、{@link #gameOver()} 游戏结束。
 *
 * <h3>合成原理</h3>
 * <ol>
 *   <li>采样参数：{@code 44100Hz / 16bit / 单声道 / 有符号 / 小端序} 的
 *       {@link AudioFormat}，由 {@link AudioFormat} 描述给 {@link SourceDataLine}。</li>
 *   <li>生成波形：逐帧计算归一化样本（方波用 {@code signum(sin)}，正弦用 {@code sin}），
 *       再叠加一条指数衰减包络 {@code exp(-decay*t)} 模拟"打一下就消散"。</li>
 *   <li>频率扫描（如 hurt 的下滑）：逐帧累加相位 {@code phase += 2π·freq·dt}，
 *       让频率随时间从高滑到低，得到下滑音。</li>
 *   <li>振幅缩放：按 {@link GameSettings#getVolume()}/100 缩放最大幅值后，
 *       把每个 short 拆成小端序两字节写入缓冲区。</li>
 *   <li>播放：交给 {@link SourceDataLine} 的 {@code write/drain} 推送到声卡。</li>
 * </ol>
 *
 * <h3>不阻塞 / 不崩原则</h3>
 * <ul>
 *   <li>每个音效先判 {@link GameSettings#isMuted()} 与音量；为 0 直接 return。</li>
 *   <li>否则<b>起一个守护线程</b>异步合成并播放（fire-and-forget），
 *       绝不阻塞 EDT 与游戏循环（Timer 也跑在 EDT 上）。</li>
 *   <li>线程内整段 {@code try/catch(Exception)}：无音频设备、
 *       {@link javax.sound.sampled.LineUnavailableException} 等任何异常一律<b>静默吞掉</b>，
 *       音效是锦上添花，绝不让游戏崩。</li>
 *   <li>线程 {@code setDaemon(true)}，JVM 退出时不被这些短音拖住。</li>
 * </ul>
 */
public final class SoundUtil {

    private SoundUtil() {
        // 工具类，不实例化
    }

    /** 采样率（Hz） */
    private static final float SAMPLE_RATE = 44100f;
    /** 基准最大幅值（约满幅 1/3，避免削波失真，再按音量缩放） */
    private static final double MAX_AMP = 10000.0;

    /**
     * 开火音：880Hz 方波，约 50ms，快衰减，听感清脆。
     */
    public static void shoot() {
        if (shouldSkip()) {
            return;
        }
        playAsync(() -> buildTone(880, 50, 30, true));
    }

    /**
     * 命中僵尸音：220Hz 方波，约 70ms，听感为低沉"啪"的击打。
     */
    public static void hit() {
        if (shouldSkip()) {
            return;
        }
        playAsync(() -> buildTone(220, 70, 18, true));
    }

    /**
     * 玩家受伤音：频率由 400Hz 下滑到 200Hz，约 120ms，模拟痛感的下滑鸣音。
     */
    public static void hurt() {
        if (shouldSkip()) {
            return;
        }
        playAsync(() -> buildSweep(400, 200, 120, 8));
    }

    /**
     * 游戏结束音：一串递降正弦音符（660→520→392→262Hz），
     * 听感为"完蛋了"的下沉旋律。
     */
    public static void gameOver() {
        if (shouldSkip()) {
            return;
        }
        playAsync(() -> buildNotes(new double[]{660, 520, 392, 262}, 180, 4, false));
    }

    /**
     * 是否应当跳过本次播放（静音 或 音量为 0）。
     *
     * @return true 表示不发声
     */
    private static boolean shouldSkip() {
        return GameSettings.isMuted() || GameSettings.getVolume() <= 0;
    }

    /**
     * 在守护线程中合成并播放一段波形（fire-and-forget）。
     * <p>所有音频异常被静默吞掉，绝不外泄。
     *
     * @param builder 构造 PCM 字节缓冲区的回调
     */
    private static void playAsync(WaveBuilder builder) {
        Thread t = new Thread(() -> {
            try {
                byte[] buffer = builder.build();
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format, buffer.length);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
                line.stop();
                line.close();
            } catch (Exception e) {
                // 静默吞掉：无设备 / LineUnavailable / 任何音频异常都不影响游戏
            }
        }, "sound-fx");
        t.setDaemon(true);
        t.start();
    }

    /**
     * 合成一段固定频率的音。
     *
     * @param freq       频率（Hz）
     * @param durationMs 时长（毫秒）
     * @param decay      包络衰减系数（越大消散越快）
     * @param square     true 为方波，false 为正弦
     * @return 16bit 小端序 PCM 字节缓冲区
     */
    private static byte[] buildTone(double freq, int durationMs, double decay, boolean square) {
        int samples = (int) (SAMPLE_RATE * durationMs / 1000.0);
        byte[] buffer = new byte[samples * 2];
        double amp = MAX_AMP * (GameSettings.getVolume() / 100.0);
        for (int i = 0; i < samples; i++) {
            double t = i / (double) SAMPLE_RATE;
            double env = Math.exp(-decay * t);
            double s = square
                    ? Math.signum(Math.sin(2 * Math.PI * freq * t))
                    : Math.sin(2 * Math.PI * freq * t);
            appendSample(buffer, i, s * amp * env);
        }
        return buffer;
    }

    /**
     * 合成一段频率扫描音（freqStart 线性滑向 freqEnd）。
     * <p>用相位累加（而非按当前频率直接算 sin(2πft)）避免扫描过程中的相位跳变/咔哒声。
     *
     * @param freqStart  起始频率（Hz）
     * @param freqEnd    结束频率（Hz）
     * @param durationMs 时长（毫秒）
     * @param decay      包络衰减系数
     * @return 16bit 小端序 PCM 字节缓冲区
     */
    private static byte[] buildSweep(double freqStart, double freqEnd, int durationMs, double decay) {
        int samples = (int) (SAMPLE_RATE * durationMs / 1000.0);
        byte[] buffer = new byte[samples * 2];
        double amp = MAX_AMP * (GameSettings.getVolume() / 100.0);
        double dt = 1.0 / SAMPLE_RATE;
        double phase = 0;
        for (int i = 0; i < samples; i++) {
            double progress = (double) i / samples;
            double freq = freqStart + (freqEnd - freqStart) * progress;
            phase += 2 * Math.PI * freq * dt;
            double env = Math.exp(-decay * (i * dt));
            appendSample(buffer, i, Math.sin(phase) * amp * env);
        }
        return buffer;
    }

    /**
     * 合成多个递降音符的序列：把每个音符的 PCM 首尾拼接。
     *
     * @param freqs     各音符频率（按顺序播放）
     * @param eachMs    每个音符时长（毫秒）
     * @param decay     包络衰减系数
     * @param square    true 为方波，false 为正弦
     * @return 拼接后的 16bit 小端序 PCM 字节缓冲区
     */
    private static byte[] buildNotes(double[] freqs, int eachMs, double decay, boolean square) {
        byte[][] parts = new byte[freqs.length][];
        int total = 0;
        for (int i = 0; i < freqs.length; i++) {
            parts[i] = buildTone(freqs[i], eachMs, decay, square);
            total += parts[i].length;
        }
        byte[] buffer = new byte[total];
        int pos = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, buffer, pos, part.length);
            pos += part.length;
        }
        return buffer;
    }

    /**
     * 把一个 double 样本写入缓冲区第 index 个采样位置（16bit 小端序）。
     *
     * @param buffer PCM 字节缓冲区
     * @param index  采样索引（每采样占 2 字节）
     * @param value  归一化样本值（已乘振幅，可能超出 short 范围，会被钳制）
     */
    private static void appendSample(byte[] buffer, int index, double value) {
        int v = (int) Math.round(value);
        if (v > Short.MAX_VALUE) {
            v = Short.MAX_VALUE;
        } else if (v < Short.MIN_VALUE) {
            v = Short.MIN_VALUE;
        }
        short val = (short) v;
        buffer[2 * index] = (byte) (val & 0xff);          // 低字节
        buffer[2 * index + 1] = (byte) ((val >> 8) & 0xff); // 高字节
    }

    /** 构造一段 PCM 字节缓冲区的回调（函数式接口）。 */
    @FunctionalInterface
    private interface WaveBuilder {
        /**
         * 构造缓冲区。
         *
         * @return 16bit 小端序 PCM 字节缓冲区
         */
        byte[] build();
    }
}
