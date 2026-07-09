package com.game.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * 数据库工具类，统一管理：
 *   1) 从 classpath 下的 db.properties 读取连接配置；
 *   2) 获取数据库连接；
 *   3) 关闭资源。
 * 所有 DAO 都通过本类获取连接。
 */
public class DBUtil {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    // 类加载时读取一次配置
    static {
        Properties props = new Properties();
        // db.properties 放在 classpath（src/main/resources）下
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new RuntimeException("找不到 db.properties，请确认它在 src/main/resources 下");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("读取 db.properties 失败：" + e.getMessage(), e);
        }
        URL = props.getProperty("url");
        USER = props.getProperty("user");
        PASSWORD = props.getProperty("password");
    }

    /**
     * 获取数据库连接。
     *
     * @return JDBC 连接
     */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            // 友好提示，不直接抛裸异常
            throw new RuntimeException(
                    "数据库连接失败，请检查：1) MySQL 是否已启动；"
                    + "2) db.properties 里 user/password 是否正确；"
                    + "3) 端口 3306 是否被占用。\n原始错误：" + e.getMessage(),
                    e);
        }
    }

    /**
     * 关闭资源（顺序：结果集 -> 语句 -> 连接），忽略关闭时的异常。
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ignored) {
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
