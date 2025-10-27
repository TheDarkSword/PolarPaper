package live.minehub.polarpaper;

import net.minecraft.world.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public record Config(
        @NotNull String source,
        int autoSaveIntervalTicks,
        long time,
        boolean saveOnStop,
        boolean loadOnStartup,
        @NotNull Location spawn,
        @NotNull Difficulty difficulty,
        boolean allowMonsters,
        boolean allowAnimals,
        boolean allowWorldExpansion,
        @NotNull WorldType worldType,
        @NotNull World.Environment environment,
        @NotNull Map<String, Object> gamerules
) {

    public static final Map<String, Object> DEFAULT_GAMERULES = new HashMap<>() {{
        put("doMobSpawning", false);
        put("doFireTick", false);
        put("randomTickSpeed", 0);
        put("mobGriefing", false);
        put("doVinesSpread", false);
        put("pvp", true);
        put("projectilesCanBreakBlocks", false);
        put("tntExplodes", false);
        put("coralDeath", false); // custom gamerule
        put("blockPhysics", false); // custom gamerule
        put("liquidPhysics", false); // custom gamerule
    }};

    public static final Config DEFAULT = new Config(
            "file",
            -1,
            1000L,
            false,
            true,
            new Location(null, 0, 64, 0),
            Difficulty.NORMAL,
            true,
            true,
            true,
            WorldType.NORMAL,
            World.Environment.NORMAL,
            DEFAULT_GAMERULES
    );

    public static @NotNull Config getDefaultConfig(World world) {
        // Add gamerules from world into config
        Map<String, Object> gameruleMap = new HashMap<>(Config.DEFAULT_GAMERULES);

        for (String name : world.getGameRules()) {
            org.bukkit.GameRule<?> gamerule = org.bukkit.GameRule.getByName(name);
            if (gamerule == null) continue;

            Object gameRuleValue = world.getGameRuleValue(gamerule);
            if (gameRuleValue == null) continue;
            Object gameRuleDefault = world.getGameRuleDefault(gamerule);
            if (gameRuleValue != gameRuleDefault) {
                gameruleMap.put(name, gameRuleValue);
            }
        }

        return new Config(
                "file",
                -1,
                world.getFullTime(),
                Config.DEFAULT.saveOnStop,
                Config.DEFAULT.loadOnStartup,
                world.getSpawnLocation(),
                Difficulty.valueOf(world.getDifficulty().name()),
                world.getAllowMonsters(),
                world.getAllowAnimals(),
                Config.DEFAULT.allowWorldExpansion,
                WorldType.NORMAL,
                world.getEnvironment(),
                gameruleMap
        );
    }

    public @NotNull String spawnString() {
        return locationToString(spawn());
    }

    public @NotNull Config withSpawnPos(Location location) {
        return new Config(this.source, this.autoSaveIntervalTicks, this.time, this.saveOnStop, this.loadOnStartup, location, this.difficulty, this.allowMonsters, this.allowAnimals, this.allowWorldExpansion, this.worldType, this.environment, this.gamerules);
    }


    public static @NotNull Config readFromConfig(FileConfiguration config, String worldName) {
        return readFromConfig(config, worldName, DEFAULT);
    }

    public static @NotNull Config readFromConfig(FileConfiguration config, String worldName, Config defaultConfig) {
        String prefix = String.format("worlds.%s.", worldName);

        try {
            String source = config.getString(prefix + "source", defaultConfig.source);
            int autoSaveIntervalTicks = config.getInt(prefix + "autosaveIntervalTicks", defaultConfig.autoSaveIntervalTicks);
            long time = config.getLong(prefix + "time", defaultConfig.time);
            boolean saveOnStop = config.getBoolean(prefix + "saveOnStop", defaultConfig.saveOnStop);
            boolean loadOnStartup = config.getBoolean(prefix + "loadOnStartup", defaultConfig.loadOnStartup);
            String spawn = config.getString(prefix + "spawn", locationToString(defaultConfig.spawn));
            Difficulty difficulty = Difficulty.valueOf(config.getString(prefix + "difficulty", defaultConfig.difficulty.name()));
            boolean allowMonsters = config.getBoolean(prefix + "allowMonsters", defaultConfig.allowMonsters);
            boolean allowAnimals = config.getBoolean(prefix + "allowAnimals", defaultConfig.allowAnimals);
            boolean allowWorldExpansion = config.getBoolean(prefix + "allowWorldExpansion", defaultConfig.allowWorldExpansion);
            WorldType worldType = WorldType.valueOf(config.getString(prefix + "worldType", defaultConfig.worldType.name()));
            World.Environment environment = World.Environment.valueOf(config.getString(prefix + "environment", defaultConfig.environment.name()));


            List<Map<?, ?>> gamerules = config.getMapList(prefix + "gamerules");

            Map<String, Object> gamerulesMap = Config.convertYmlGamerules(gamerules);

            if (gamerules.isEmpty()) gamerulesMap.putAll(defaultConfig.gamerules);


            return new Config(
                    source,
                    autoSaveIntervalTicks,
                    time,
                    saveOnStop,
                    loadOnStartup,
                    stringToLocation(spawn),
                    difficulty,
                    allowMonsters,
                    allowAnimals,
                    allowWorldExpansion,
                    worldType,
                    environment,
                    gamerulesMap
            );
        } catch (IllegalArgumentException e) {
            PolarPaper.logger().warning("Failed to read config, using defaults");
            PolarPaper.logger().log(Level.INFO, e.getMessage(), e);
            return defaultConfig;
        }
    }

    public static void writeToConfig(FileConfiguration fileConfig, String worldName, Config config) {
        String prefix = String.format("worlds.%s.", worldName);

        fileConfig.set(prefix + "source", config.source);
        fileConfig.set(prefix + "autosaveIntervalTicks", config.autoSaveIntervalTicks);
        fileConfig.set(prefix + "time", config.time);
        fileConfig.setInlineComments(prefix + "autosaveIntervalTicks", List.of("-1 to disable"));
        fileConfig.set(prefix + "saveOnStop", config.saveOnStop);
        fileConfig.set(prefix + "loadOnStartup", config.loadOnStartup);
        fileConfig.set(prefix + "spawn", locationToString(config.spawn));
        fileConfig.set(prefix + "difficulty", config.difficulty.name());
        fileConfig.set(prefix + "allowMonsters", config.allowMonsters);
        fileConfig.set(prefix + "allowAnimals", config.allowAnimals);
        fileConfig.set(prefix + "allowWorldExpansion", config.allowWorldExpansion);
        fileConfig.setInlineComments(prefix + "allowWorldExpansion", List.of("Whether the world can grow and load more chunks"));
        fileConfig.set(prefix + "worldType", config.worldType.name());
        fileConfig.setInlineComments(prefix + "worldType", List.of("One of: NORMAL, FLAT, AMPLIFIED, LARGE_BIOMES"));
        fileConfig.set(prefix + "environment", config.environment.name());
        fileConfig.setInlineComments(prefix + "environment", List.of("One of: NORMAL, NETHER, THE_END, CUSTOM"));
        fileConfig.set(prefix + "gamerules", config.gamerulesList());
        fileConfig.setInlineComments(prefix + "gamerules", List.of("Custom rules: liquidPhysics, blockPhysics, coralDeath"));

        Path pluginFolder = Path.of(PolarPaper.getPlugin().getDataFolder().getAbsolutePath());
        Path configFile = pluginFolder.resolve("config.yml");
        try {
            fileConfig.save(configFile.toFile());
        } catch (IOException e) {
            PolarPaper.logger().warning("Failed to save world to config file");
            PolarPaper.logger().warning(e.toString());
        }
    }

    private static String locationToString(Location spawn) {
        return String.format("%s, %s, %s, %s, %s",
                spawn.x(),
                spawn.y(),
                spawn.z(),
                spawn.getYaw(),
                spawn.getPitch());
    }

    private static Location stringToLocation(String string) {
        String[] split = string.split(",");
        try {
            if (split.length == 3) { // x y z
                String x = split[0];
                String y = split[1];
                String z = split[2];
                return new Location(null, Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z));
            } else if (split.length == 5) { // x y z yaw pitch
                String x = split[0];
                String y = split[1];
                String z = split[2];
                String yaw = split[3];
                String pitch = split[4];
                return new Location(null, Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z), Float.parseFloat(yaw), Float.parseFloat(pitch));
            } else {
                PolarPaper.logger().warning("Failed to parse spawn pos: " + string);
                return DEFAULT.spawn;
            }
        } catch (Exception e) {
            PolarPaper.logger().warning("Failed to parse spawn pos: " + string);
            return DEFAULT.spawn;
        }
    }

    public @NotNull List<Map<String, ?>> gamerulesList() {
        List<Map<String, ?>> gamerules = new ArrayList<>();
        for (Map.Entry<String, Object> entry : gamerules().entrySet()) {
            gamerules.add(Map.of(entry.getKey(), entry.getValue()));
        }
        return gamerules;
    }

    public static @NotNull Map<String, Object> convertYmlGamerules(List<Map<?, ?>> ymlGamerules) {
        Map<String, Object> gamerules = new HashMap<>();
        for (Map<?, ?> ymlGamerule : ymlGamerules) {
            for (Map.Entry<?, ?> entry : ymlGamerule.entrySet()) {
                if (!(entry.getKey() instanceof String key)) continue;

                gamerules.put(key, entry.getValue());
            }
        }
        return gamerules;
    }

}
