package com.masoncoemaet.home.storage;

import com.masoncoemaet.home.model.HomeLocation;
import com.masoncoemaet.home.model.PlayerHomes;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

public class HomeStorage {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final boolean mysql;

    public HomeStorage(String jdbcUrl, String username, String password, boolean mysql) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.mysql = mysql;
    }

    public void init() throws SQLException, ClassNotFoundException {
        if (mysql) {
            Class.forName("com.mysql.jdbc.Driver");
        } else {
            Class.forName("org.sqlite.JDBC");
        }

        Connection connection = getConnection();
        try {
            Statement statement = connection.createStatement();
            try {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS homes (" +
                        "uuid VARCHAR(36) NOT NULL, " +
                        "slot INTEGER NOT NULL, " +
                        "world VARCHAR(128) NOT NULL, " +
                        "x DOUBLE NOT NULL, " +
                        "y DOUBLE NOT NULL, " +
                        "z DOUBLE NOT NULL, " +
                        "yaw FLOAT NOT NULL, " +
                        "pitch FLOAT NOT NULL, " +
                        "custom_name VARCHAR(32), " +
                        "PRIMARY KEY (uuid, slot))");
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
    }

    public PlayerHomes load(UUID uuid) throws SQLException {
        PlayerHomes homes = new PlayerHomes();
        Connection connection = getConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT slot, world, x, y, z, yaw, pitch, custom_name FROM homes WHERE uuid = ?");
            try {
                statement.setString(1, uuid.toString());
                ResultSet resultSet = statement.executeQuery();
                try {
                    while (resultSet.next()) {
                        int slot = resultSet.getInt("slot");
                        homes.setHome(slot, new HomeLocation(
                                resultSet.getString("world"),
                                resultSet.getDouble("x"),
                                resultSet.getDouble("y"),
                                resultSet.getDouble("z"),
                                resultSet.getFloat("yaw"),
                                resultSet.getFloat("pitch"),
                                resultSet.getString("custom_name")));
                    }
                } finally {
                    resultSet.close();
                }
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
        return homes;
    }

    public void save(UUID uuid, PlayerHomes homes) throws SQLException {
        Connection connection = getConnection();
        try {
            connection.setAutoCommit(false);
            PreparedStatement delete = connection.prepareStatement("DELETE FROM homes WHERE uuid = ?");
            PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO homes (uuid, slot, world, x, y, z, yaw, pitch, custom_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            try {
                delete.setString(1, uuid.toString());
                delete.executeUpdate();

                for (Map.Entry<Integer, HomeLocation> entry : homes.getHomes().entrySet()) {
                    HomeLocation home = entry.getValue();
                    insert.setString(1, uuid.toString());
                    insert.setInt(2, entry.getKey());
                    insert.setString(3, home.getWorld());
                    insert.setDouble(4, home.getX());
                    insert.setDouble(5, home.getY());
                    insert.setDouble(6, home.getZ());
                    insert.setFloat(7, home.getYaw());
                    insert.setFloat(8, home.getPitch());
                    insert.setString(9, home.getCustomName());
                    insert.addBatch();
                }
                insert.executeBatch();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                insert.close();
                delete.close();
            }
        } finally {
            connection.close();
        }
    }

    private Connection getConnection() throws SQLException {
        if (mysql) {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }
        return DriverManager.getConnection(jdbcUrl);
    }
}
