package pluginsfix.frameend.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import pluginsfix.frameend.domain.PlacedEgg;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SqliteStorage implements Storage {
    private final File dataFolder;
    private final Logger logger;
    private HikariDataSource dataSource;

    public SqliteStorage(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    @Override
    public void init() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "data.db");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("FrameEndSQLitePool");

        this.dataSource = new HikariDataSource(config);

        createTables();
    }

    private void createTables() {
        String eggsTable = """
                CREATE TABLE IF NOT EXISTS frameend_placed_eggs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_uuid TEXT NOT NULL,
                    world_name TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    current_durability INTEGER NOT NULL,
                    max_durability INTEGER NOT NULL,
                    repair_count INTEGER NOT NULL,
                    placed_time INTEGER NOT NULL,
                    aura_type TEXT DEFAULT 'DEFAULT'
                );
                """;

        String cooldownsTable = """
                CREATE TABLE IF NOT EXISTS frameend_compass_cooldowns (
                    player_uuid TEXT PRIMARY KEY,
                    cooldown_until INTEGER NOT NULL
                );
                """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(eggsTable);
            stmt.execute(cooldownsTable);
            try {
                stmt.execute("ALTER TABLE frameend_placed_eggs ADD COLUMN aura_type TEXT DEFAULT 'DEFAULT'");
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize SQLite tables", e);
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public int insertPlacedEgg(UUID ownerUuid, String worldName, int x, int y, int z,
                               int currentDurability, int maxDurability, int repairCount, long placedTime, String auraType) {
        String sql = "INSERT INTO frameend_placed_eggs (owner_uuid, world_name, x, y, z, current_durability, max_durability, repair_count, placed_time, aura_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ownerUuid.toString());
            ps.setString(2, worldName);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, z);
            ps.setInt(6, currentDurability);
            ps.setInt(7, maxDurability);
            ps.setInt(8, repairCount);
            ps.setLong(9, placedTime);
            ps.setString(10, auraType != null ? auraType : "DEFAULT");
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to insert placed egg", e);
        }
        return -1;
    }

    @Override
    public void updatePlacedEgg(int id, int currentDurability, int repairCount) {
        String sql = "UPDATE frameend_placed_eggs SET current_durability = ?, repair_count = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentDurability);
            ps.setInt(2, repairCount);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update placed egg id=" + id, e);
        }
    }

    @Override
    public void updatePlacedEggAura(int id, String auraType) {
        String sql = "UPDATE frameend_placed_eggs SET aura_type = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auraType != null ? auraType : "DEFAULT");
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update placed egg aura id=" + id, e);
        }
    }

    @Override
    public void deletePlacedEgg(int id) {
        String sql = "DELETE FROM frameend_placed_eggs WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete placed egg id=" + id, e);
        }
    }

    @Override
    public List<PlacedEgg> loadAllPlacedEggs() {
        List<PlacedEgg> list = new ArrayList<>();
        String sql = "SELECT id, owner_uuid, world_name, x, y, z, current_durability, max_durability, repair_count, placed_time, aura_type FROM frameend_placed_eggs";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                String world = rs.getString("world_name");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                int curDur = rs.getInt("current_durability");
                int maxDur = rs.getInt("max_durability");
                int repCount = rs.getInt("repair_count");
                long placedTime = rs.getLong("placed_time");
                String aura = rs.getString("aura_type");

                list.add(new PlacedEgg(id, ownerUuid, world, x, y, z, curDur, maxDur, repCount, placedTime, aura));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load placed eggs", e);
        }
        return list;
    }

    @Override
    public long getCompassCooldown(UUID playerUuid) {
        String sql = "SELECT cooldown_until FROM frameend_compass_cooldowns WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("cooldown_until");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get compass cooldown for " + playerUuid, e);
        }
        return 0L;
    }

    @Override
    public void setCompassCooldown(UUID playerUuid, long timestampMillis) {
        String sql = "INSERT INTO frameend_compass_cooldowns (player_uuid, cooldown_until) VALUES (?, ?) ON CONFLICT(player_uuid) DO UPDATE SET cooldown_until = excluded.cooldown_until";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setLong(2, timestampMillis);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to set compass cooldown for " + playerUuid, e);
        }
    }
}
