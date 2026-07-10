-- ============================================================
-- 迁移脚本: 为"排行榜按难度拆分(简单/困难)"升级数据库 (无损, 可重复执行)
-- 作用: 给 game_record 表加 difficulty 列(默认 'EASY')
-- 说明: 已有的老战绩没有难度概念，其数值等同现在的简单档，
--       故统一落 'EASY'(靠 DEFAULT 自动填充，无需单独 UPDATE)。
-- 日期: 2026-07-10
-- ============================================================
USE game_db;

-- 1) game_record 表加 difficulty 列(若已存在则跳过, 保证可重复执行)
SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = 'game_db' AND table_name = 'game_record' AND column_name = 'difficulty');
SET @ddl := IF(@col = 0,
               'ALTER TABLE game_record ADD COLUMN difficulty VARCHAR(8) NOT NULL DEFAULT ''EASY'' COMMENT ''难度 EASY/HARD''',
               'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) game_record 加复合索引 idx_diff_score(difficulty, score) (若已存在则跳过, 与 schema.sql 全新装对齐)
SET @idx := (SELECT COUNT(*) FROM information_schema.statistics
             WHERE table_schema = 'game_db' AND table_name = 'game_record' AND index_name = 'idx_diff_score');
SET @ddl := IF(@idx = 0,
               'ALTER TABLE game_record ADD INDEX idx_diff_score (difficulty, score)',
               'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 验证：确认列已加、老数据落 'EASY'
SELECT id, score, difficulty FROM game_record LIMIT 5;
SHOW COLUMNS FROM game_record LIKE 'difficulty';
