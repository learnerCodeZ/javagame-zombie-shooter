package com.game.util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 合成音效工具：用代码现场生成 PCM 采样并播放，不依赖任何外部音频文件。
 * <p>提供四种局内音效：{@link #shoot()} 开火、{@link #hit()} 命中僵尸、
 * {@link #hurt()} 玩家受伤、{@link #gameOver()} 游戏结束。
 *
 * <h3>播放模型（单线路 + 队列 + 专用线程）</h3>
 * 早期版本每次播音都新开一条 {@link SourceDataLine}，连射/多音并发时会
 * 耗尽或争抢混音器线路，抛 {@link LineUnavailableException}（被静默吞掉），
 * 表现为"有时候没声音"。现改为：
 * <ol>
 *   <li>全局只开<b>一条</b> {@link SourceDataLine}（首次播放时懒加载、长期复用），
 *       不再每音效 open/close，<b>从根本上避免线路耗尽</b>。</li>
 *   <li>所有音效把"合成+写入"任务丢进 {@link #playQueue}，
 *       由<b>单一消费线程</b>串行处理，保证对共享线路的访问不会并发冲突。</li>
 *   <li>{@link #shoot()} 加防抖（两次最少间隔 {@link #SHOOT_DEBOUNCE_MS} ms），
 *       避免连射把队列堆爆、声音滞后。</li>
 * </ol>
 *
 * <h3>合成原理</h3>
 * <ol>
 *   <li>采样参数：{@code 44100Hz / 16bit / 单声道 / 有符号 / 小端序}。</li>
 *   <li>波形：方波 {@code signum(sin)}、正弦 {@code sin}，叠加指数衰减包络 {@code exp(-decay*t)}。</li>
 *   <li>频率扫描（hurt 下滑）：逐帧累加相位 {@code phase += 2π·freq·dt}，避免相位跳变咔哒声。</li>
 *   <li>振幅按 {@link GameSettings#getVolume()}/100 缩放，short 拆小端序两字节写入。</li>
 * </ol>
 *
 * <h3>不阻塞 / 不崩原则</h3>
 * <ul>
 *   <li>静音或音量为 0 直接 return，不入队。</li>
 *   <li>合成与播放都在专用守护线程完成，绝不阻塞 EDT 与游戏循环。</li>
 *   <li>整段 {@code try/catch}：无音频设备 / {@link LineUnavailableException} 等异常一律静默吞掉，
 *       音效是锦上添花，绝不让游戏崩。</li>
 *   <li>消费线程为守护线程，JVM 退出时不会被拖住。</li>
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
    /** 起音(attack)时长（秒）：开头淡入，柔化起声、消除瞬态"咔"声 */
    private static final double ATTACK_SEC = 0.005;
    /** 释音(release)时长（秒）：结尾淡出到 0，使相邻音衔接处振幅归零、连射不咔哒 */
    private static final double RELEASE_SEC = 0.005;
    /** 统一的音频格式 */
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    /**
     * 线路缓冲区大小（字节）≈ 50ms（44100Hz×16bit×单声道）。
     * <p>刻意开小：让 {@link SourceDataLine#write} 在缓冲接近满时阻塞，把消费线程节流到实时，
     * 避免连射把音频囤进线路缓冲、松手后还在响。50ms 延迟人耳察觉不到，又足够大避免欠载爆音。
     */
    private static final int LINE_BUFFER_BYTES = (int) (SAMPLE_RATE * 2 * 0.05);

    /** 开火防抖：两次开火音最少间隔（毫秒），避免连射堆爆播放队列 */
    private static final long SHOOT_DEBOUNCE_MS = 40;
    /** 待播放队列上限：shoot/hit 超过则丢弃，防止连射积压导致"停火后还在响" */
    private static final int MAX_PENDING = 2;
    /** 上次开火音的时间戳（防抖用） */
    private static volatile long lastShootMs = Long.MIN_VALUE / 2;

    /** 全局唯一复用的音频线（首次播放时懒加载）。所有音效共用，避免反复开/关线路耗尽线路。 */
    private static volatile SourceDataLine sharedLine;
    /** 线路懒加载锁 */
    private static final Object LINE_LOCK = new Object();

    /** 待播放任务队列（每项 = 合成 PCM + 写入线路）。单一消费线程串行处理。 */
    private static final BlockingQueue<Runnable> playQueue = new LinkedBlockingQueue<>();

    static {
        // 启动专用播放守护线程：阻塞取队列任务 → 确保线路 → 执行（合成 + write）
        Thread player = new Thread(SoundUtil::playLoop, "sound-player");
        player.setDaemon(true);
        player.start();
    }

    /**
     * 播放消费循环：不断从队列取任务并执行。任何异常都吞掉、继续下一个。
     */
    private static void playLoop() {
        while (true) {
            try {
                Runnable task = playQueue.take();
                ensureLine();
                task.run();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable t) {
                // 播放异常（含 LineUnavailable）不影响游戏，继续消费下一个
            }
        }
    }

    /**
     * 懒加载并复用唯一音频线；打不开（无声卡等）则 sharedLine 保持 null，播放自动静默。
     */
    private static void ensureLine() {
        SourceDataLine line = sharedLine;
        if (line != null && line.isOpen()) {
            return;
        }
        synchronized (LINE_LOCK) {
            if (sharedLine == null || !sharedLine.isOpen()) {
                try {
                    SourceDataLine l = AudioSystem.getSourceDataLine(FORMAT);
                    l.open(FORMAT, LINE_BUFFER_BYTES);
                    l.start();
                    sharedLine = l;
                } catch (LineUnavailableException | SecurityException e) {
                    sharedLine = null; // 无可用音频线：静默降级，不崩
                }
            }
        }
    }

    /**
     * 开火音：600Hz 正弦，约 40ms（短促，播得比射速快，不积压），起音淡入，听感柔和。
     * 带防抖；队列积压超 {@value #MAX_PENDING} 时丢弃，避免"停火后还在响"。
     */
    public static void shoot() {
        if (shouldSkip()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastShootMs < SHOOT_DEBOUNCE_MS) {
            return; // 防抖：距上次开火音太近，本次跳过
        }
        lastShootMs = now;
        if (playQueue.size() >= MAX_PENDING) {
            return; // 队列已积压，丢弃本次，避免"停火后还在响"
        }
        enqueue(() -> writeBuffer(buildTone(600, 40, 25, false)));
    }

    /**
     * 命中僵尸音：150Hz 正弦，约 60ms，起音淡入，听感为柔和低沉的击打。队列积压超限时丢弃。
     */
    public static void hit() {
        if (shouldSkip()) {
            return;
        }
        if (playQueue.size() >= MAX_PENDING) {
            return; // 队列已积压，丢弃本次
        }
        enqueue(() -> writeBuffer(buildTone(150, 60, 12, false)));
    }

    /**
     * 玩家受伤音：频率由 400Hz 下滑到 200Hz，约 120ms，模拟痛感的下滑鸣音。
     */
    public static void hurt() {
        if (shouldSkip()) {
            return;
        }
        enqueue(() -> writeBuffer(buildSweep(400, 200, 120, 8)));
    }

    /**
     * 游戏结束音：一串递降正弦音符（660→520→392→262Hz），听感为"完蛋了"的下沉旋律。
     */
    public static void gameOver() {
        if (shouldSkip()) {
            return;
        }
        enqueue(() -> writeBuffer(buildNotes(new double[]{660, 520, 392, 262}, 180, 4, false)));
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
     * 把一段"合成+写入"任务入队，由消费线程串行处理。
     *
     * @param task 播放任务
     */
    private static void enqueue(Runnable task) {
        playQueue.offer(task);
    }

    /**
     * 把 PCM 缓冲写入共享线路（在消费线程中调用）；线路不可用则跳过。
     * <p>{@link SourceDataLine#write} 在内部缓冲满时会阻塞，天然起到限速作用。
     *
     * @param buffer 16bit 小端序 PCM 字节缓冲区
     */
    private static void writeBuffer(byte[] buffer) {
        SourceDataLine line = sharedLine;
        if (line == null) {
            return;
        }
        try {
            line.write(buffer, 0, buffer.length);
        } catch (Exception e) {
            // 线路写入异常忽略
        }
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
        double durationSec = durationMs / 1000.0;
        for (int i = 0; i < samples; i++) {
            double t = i / (double) SAMPLE_RATE;
            double env = Math.exp(-decay * t);
            double atk = t < ATTACK_SEC ? (t / ATTACK_SEC) : 1.0;                       // 起音淡入
            double remaining = durationSec - t;
            double rel = (remaining < RELEASE_SEC) ? Math.max(0.0, remaining / RELEASE_SEC) : 1.0; // 结尾淡出
            double s = square
                    ? Math.signum(Math.sin(2 * Math.PI * freq * t))
                    : Math.sin(2 * Math.PI * freq * t);
            appendSample(buffer, i, s * amp * env * atk * rel);
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
        double durationSec = durationMs / 1000.0;
        for (int i = 0; i < samples; i++) {
            double progress = (double) i / samples;
            double freq = freqStart + (freqEnd - freqStart) * progress;
            phase += 2 * Math.PI * freq * dt;
            double t = i * dt;
            double env = Math.exp(-decay * t);
            double atk = t < ATTACK_SEC ? (t / ATTACK_SEC) : 1.0;                       // 起音淡入
            double remaining = durationSec - t;
            double rel = (remaining < RELEASE_SEC) ? Math.max(0.0, remaining / RELEASE_SEC) : 1.0; // 结尾淡出
            appendSample(buffer, i, Math.sin(phase) * amp * env * atk * rel);
        }
        return buffer;
    }

    /**
     * 合成多个递降音符的序列：把每个音符的 PCM 首尾拼接。
     *
     * @param freqs  各音符频率（按顺序播放）
     * @param eachMs 每个音符时长（毫秒）
     * @param decay  包络衰减系数
     * @param square true 为方波，false 为正弦
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
}
