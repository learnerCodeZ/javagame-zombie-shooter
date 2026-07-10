-- ============================================================
-- 游戏数据库 game_db 建表脚本
-- 数据库 : MySQL 8.0
-- 字符集 : utf8mb4（支持中文）
-- 特点   : 可重复执行（带 DROP IF EXISTS，反复跑不会报错）
-- 日期   : 2026-07-09
-- ============================================================

-- 1. 建库（如果不存在才建）
CREATE DATABASE IF NOT EXISTS game_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 2. 切换到这个库
USE game_db;

-- 3. 先删旧表（注意顺序：先删有外键的 game_record、password_reset_request，再删 user）
DROP TABLE IF EXISTS game_record;
DROP TABLE IF EXISTS password_reset_request;
DROP TABLE IF EXISTS user;

-- 4. 用户表
CREATE TABLE user (
    id          INT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL                 COMMENT '登录用户名',
    password    VARCHAR(64)  NOT NULL                 COMMENT '密码（MD5 加密后存）',
    nickname    VARCHAR(50)           DEFAULT NULL    COMMENT '昵称',
    role        VARCHAR(10)  NOT NULL DEFAULT 'user'  COMMENT '角色 admin/user',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)                 -- 用户名唯一，不能重复
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 5.1 游戏记录表（外键关联 user）
CREATE TABLE game_record (
    id           INT      NOT NULL AUTO_INCREMENT  COMMENT '记录ID',
    user_id      INT      NOT NULL                 COMMENT '所属用户ID（外键→user.id）',
    score        INT      NOT NULL DEFAULT 0       COMMENT '得分',
    kill_count   INT      NOT NULL DEFAULT 0       COMMENT '击杀数',
    survive_sec  INT      NOT NULL DEFAULT 0       COMMENT '存活秒数',
    difficulty   VARCHAR(8) NOT NULL DEFAULT 'EASY' COMMENT '难度 EASY/HARD',
    record_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (id),
    KEY idx_user       (user_id),                    -- 按用户查记录的索引
    KEY idx_score      (score),                      -- 排行榜按分数排序的索引
    KEY idx_diff_score (difficulty, score),          -- 按难度过滤+分数排序的复合索引
    CONSTRAINT fk_record_user FOREIGN KEY (user_id) REFERENCES user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏记录表';

-- 5.2 密码重置申请表（外键关联 user）
CREATE TABLE password_reset_request (
    id            INT          NOT NULL AUTO_INCREMENT  COMMENT '申请ID',
    user_id       INT          NOT NULL                 COMMENT '申请用户ID（外键→user.id）',
    status        VARCHAR(10)  NOT NULL DEFAULT 'pending' COMMENT '状态 pending/approved/rejected',
    request_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    handle_time   DATETIME              DEFAULT NULL    COMMENT '处理时间（通过/拒绝时写入）',
    PRIMARY KEY (id),
    KEY idx_reset_user   (user_id),                      -- 按用户查申请的索引
    KEY idx_reset_status (status),                       -- 按 pending 查待审核的索引
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置申请表';

-- 6. 测试数据
--    密码统一是 123456，MD5('123456') = e10adc3949ba59abbe56e057f20f883e
INSERT INTO user (username, password, nickname, role) VALUES
    ('admin',   'e10adc3949ba59abbe56e057f20f883e', '管理员', 'admin'),
    ('player1', 'e10adc3949ba59abbe56e057f20f883e', '张三',   'user'),
    ('player2', 'e10adc3949ba59abbe56e057f20f883e', '李四',   'user');

INSERT INTO game_record (user_id, score, kill_count, survive_sec) VALUES
    (1, 320, 32, 210),
    (2, 150, 15, 120),
    (3, 480, 48, 305),
    (2, 200, 20, 150),
    (3,  90,  9,  60);

-- 7. 验证：跑完看一眼数据对不对
SELECT 'user'                  AS 表名, COUNT(*) AS 行数 FROM user
UNION ALL
SELECT 'game_record',                 COUNT(*)         FROM game_record
UNION ALL
SELECT 'password_reset_request',      COUNT(*)         FROM password_reset_request;

SELECT u.nickname 昵称, r.score 分数, r.kill_count 击杀, r.survive_sec 存活秒, r.record_time 时间
FROM game_record r JOIN user u ON r.user_id = u.id
ORDER BY r.score DESC LIMIT 10;
