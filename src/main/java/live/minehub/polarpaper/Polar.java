package live.minehub.polarpaper;

import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel;
import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import io.papermc.paper.world.PaperWorldLoader;
import live.minehub.polarpaper.source.FilePolarSource;
import live.minehub.polarpaper.source.PolarSource;
import live.minehub.polarpaper.util.CoordConversion;
import live.minehub.polarpaper.util.ExceptionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Main;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BitStorage;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.validation.ContentValidationException;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.generator.CraftWorldInfo;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class Polar {

    private static final Map<String, BukkitTask> AUTOSAVE_TASK_MAP = new HashMap<>();

    private Polar() {

    }

    public static boolean isInConfig(@NotNull String worldName) {
        return PolarPaper.getPlugin().getConfig().isSet("worlds." + worldName);
    }

    /**
     * Creates a polar world with config read from config.yml with {@link PolarWorldAccess#POLAR_PAPER_FEATURES}
     *
     * @param world The polar world
     * @param worldName The name for the polar world
     */
    @SuppressWarnings("unused")
    public static void loadWorld(@NotNull PolarWorld world, @NotNull String worldName) {
        loadWorld(world, worldName, PolarWorldAccess.POLAR_PAPER_FEATURES);
    }

    /**
     * Creates a polar world with {@link PolarWorldAccess#POLAR_PAPER_FEATURES}
     *
     * @param world The polar world
     * @param worldName The name for the polar world
     * @param config Custom config for the polar world
     */
    public static void loadWorld(@NotNull PolarWorld world, @NotNull String worldName, @NotNull Config config) {
        createWorld(world, worldName, config, PolarWorldAccess.POLAR_PAPER_FEATURES);
    }

    /**
     * Creates a polar world with config read from config.yml
     *
     * @param world The polar world
     * @param worldName The name for the polar world
     * @param worldAccess Describes how userdata should be handled (default PolarWorldAccess.POLAR_PAPER_FEATURES)
     */
    public static void loadWorld(@NotNull PolarWorld world, @NotNull String worldName, @NotNull PolarWorldAccess worldAccess) {
        FileConfiguration fileConfig = PolarPaper.getPlugin().getConfig();
        Config config = Config.readFromConfig(fileConfig, worldName); // If world not in config, use defaults
        createWorld(world, worldName, config, worldAccess);
    }

    /**
     * Load a polar world using the source defined in the config and with {@link PolarWorldAccess#POLAR_PAPER_FEATURES}
     *
     * @param source PolarSource to load from
     * @param worldName The name of the world to load
     * @return Whether loading the world was successful
     */
    public static CompletableFuture<Boolean> loadWorld(@NotNull PolarSource source, @NotNull String worldName) {
        return loadWorld(source, worldName, PolarWorldAccess.POLAR_PAPER_FEATURES);
    }

    /**
     * Load a polar world from file and with {@link PolarWorldAccess#POLAR_PAPER_FEATURES}
     *
     * @param worldName The name of the world to load
     * @return Whether loading the world was successful
     */
    public static CompletableFuture<Boolean> loadWorldFromFile(@NotNull String worldName) {
        return loadWorld(FilePolarSource.defaultFolder(worldName), worldName, PolarWorldAccess.POLAR_PAPER_FEATURES);
    }

    /**
     * Load a polar world using the source defined in the config
     *
     * @param worldName The name of the world to load
     * @param worldAccess Describes how userdata should be handled (default PolarWorldAccess.POLAR_PAPER_FEATURES)
     * @return Whether loading the world was successful
     */
    public static CompletableFuture<Boolean> loadWorld(@NotNull PolarSource source, @NotNull String worldName, @NotNull PolarWorldAccess worldAccess) {
        FileConfiguration fileConfig = PolarPaper.getPlugin().getConfig();
        Config config = Config.readFromConfig(fileConfig, worldName); // If world not in config, use defaults

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getAsyncScheduler().runNow(PolarPaper.getPlugin(), task -> {
            try {
                byte[] bytes = source.readBytes();
                PolarWorld polarWorld = PolarReader.read(bytes);
                if (polarWorld.version() == PolarWorld.VERSION_DEPRECATED_ENTITIES) {
                    PolarPaper.logger().info("Re-saving world to update legacy entities");
                    byte[] worldBytes = PolarWriter.write(polarWorld);
                    source.saveBytes(worldBytes);
                }

                Bukkit.getScheduler().runTask(PolarPaper.getPlugin(), () -> {
                    createWorld(polarWorld, worldName, config, worldAccess);
                    future.complete(true);
                });
            } catch (Exception e) {
                ExceptionUtil.log(e);
                future.complete(false);
            }
        });

        return future;
    }

    /**
     * Creates a polar world
     *
     * @param world The polar world
     * @param worldName The name for the polar world
     * @param config Custom config for the polar world
     * @param worldAccess Describes how userdata should be handled (default PolarWorldAccess.POLAR_PAPER_FEATURES)
     * @return The created bukkit world
     */
    public static @Nullable World createWorld(@NotNull PolarWorld world, @NotNull String worldName, @NotNull Config config, @NotNull PolarWorldAccess worldAccess) {
        if (Bukkit.getWorld(worldName) != null) {
            PolarPaper.logger().warning("A world with the name '" + worldName + "' already exists, skipping.");
            return null;
        }

        PolarGenerator polar = new PolarGenerator(world, worldAccess, config);
        PolarBiomeProvider polarBiomeProvider = new PolarBiomeProvider(world);

        WorldCreator worldCreator = WorldCreator.name(worldName)
                .type(config.worldType())
                .environment(config.environment())
                .generator(polar)
                .biomeProvider(polarBiomeProvider);

        World newWorld = loadWorld(worldCreator, config.difficulty(), config.gamerules(), config.allowMonsters(), config.allowAnimals(), config.time());
        if (newWorld == null) {
            PolarPaper.logger().warning("An error occurred loading polar world '" + worldName + "', skipping.");
            return null;
        }

        startAutoSaveTask(newWorld, config);

        return newWorld;
    }

    public static void startAutoSaveTask(World newWorld, Config config) {
        BukkitTask prevTask = AUTOSAVE_TASK_MAP.get(newWorld.getName());
        if (prevTask != null) prevTask.cancel();

        if (config.autoSaveIntervalTicks() == -1) return;

        BukkitTask autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(PolarPaper.getPlugin(), () -> {
            long before = System.nanoTime();
            String savingMsg = String.format("Autosaving '%s'...", newWorld.getName());
            PolarPaper.logger().info(savingMsg);
            for (Player plr : Bukkit.getOnlinePlayers()) {
                if (!plr.hasPermission("polar.notifications")) continue;
                plr.sendMessage(Component.text(savingMsg, NamedTextColor.AQUA));
            }

            saveWorldToFile(newWorld);

            int ms = (int) ((System.nanoTime() - before) / 1_000_000);
            String savedMsg = String.format("Saved '%s' in %sms", newWorld.getName(), ms);
            PolarPaper.logger().info(savedMsg);
            for (Player plr : Bukkit.getOnlinePlayers()) {
                if (!plr.hasPermission("polar.notifications")) continue;
                plr.sendMessage(Component.text(savedMsg, NamedTextColor.AQUA));
            }
        }, config.autoSaveIntervalTicks(), config.autoSaveIntervalTicks());

        AUTOSAVE_TASK_MAP.put(newWorld.getName(), autosaveTask);
    }

    @SuppressWarnings("unchecked")
    public static <T> void setGameRule(World world, GameRule<?> rule, Object value) {
        world.setGameRule((GameRule<T>) rule, (T)value);
    }

    public static Config updateConfig(World world, String worldName) {
        FileConfiguration fileConfig = PolarPaper.getPlugin().getConfig();
        Config defaultConfig = Config.getDefaultConfig(fileConfig, world);
        Config config = Config.readFromConfig(fileConfig, worldName, defaultConfig); // If world not in config, use defaults

        // Update gamerules
        Config newConfig = new Config(
                config.autoSaveIntervalTicks(),
                world.getTime(),
                config.saveOnStop(),
                config.loadOnStartup(),
                config.spawn(),
                Difficulty.valueOf(world.getDifficulty().name()),
                world.getAllowMonsters(),
                world.getAllowAnimals(),
                config.worldType(),
                config.environment(),
                defaultConfig.gamerules()
        );
        Config.writeToConfig(fileConfig, worldName, newConfig);

        return newConfig;
    }

    public static void saveWorldToFile(World world) {
        saveWorld(world, FilePolarSource.defaultFolder(world.getName()));
    }

    /**
     * Save a polar world using the source defined in the config
     *
     * @param world The bukkit world
     * @param polarWorld The polar world
     * @param source The source to save the world using
     * @param polarWorldAccess Describes how userdata should be handled (default PolarWorldAccess.POLAR_PAPER_FEATURES)
     * @param chunkSelector Used to filter which chunks should save
     */
    public static void saveWorld(World world, PolarWorld polarWorld, PolarSource source, PolarWorldAccess polarWorldAccess, ChunkSelector chunkSelector) {
        saveWorld(world, polarWorld, polarWorldAccess, source, chunkSelector);
    }

    /**
     * Updates and saves a polar world using the given source
     *
     * @param world The bukkit world (needs to be a polar world)
     * @param polarSource The source to use to save the polar world (e.g. FilePolarSource)
     */
    @SuppressWarnings("unused")
    public static void saveWorld(World world, PolarSource polarSource) {
        PolarWorld polarWorld = PolarWorld.fromWorld(world);
        if (polarWorld == null) return;
        PolarGenerator generator = PolarGenerator.fromWorld(world);
        if (generator == null) return;
        updateConfig(world, world.getName());
        saveWorld(world, polarWorld, generator.getWorldAccess(), polarSource, ChunkSelector.all());
    }

    /**
     * Updates and saves a polar world using the given source
     * Can be called asynchronously
     *
     * @param world The bukkit world to retrieve new chunks from
     * @param polarWorld The polar world
     * @param polarWorldAccess Describes how userdata should be handled (default PolarWorldAccess.POLAR_PAPER_FEATURES)
     * @param polarSource The source to use to save the polar world (e.g. FilePolarSource)
     * @param chunkSelector Used to filter which chunks should save
     */
    public static void saveWorld(World world, PolarWorld polarWorld, PolarWorldAccess polarWorldAccess, PolarSource polarSource, ChunkSelector chunkSelector) {
        updateWorld(world, polarWorld, polarWorldAccess, chunkSelector);
        byte[] worldBytes = PolarWriter.write(polarWorld);
        polarSource.saveBytes(worldBytes);
    }

    /**
     * Updates the chunks in a PolarWorld
     *
     * @param world The bukkit World
     * @see Polar#saveWorld(World, PolarSource)
     */
    public static void updateWorld(World world) {
        PolarWorld polarWorld = PolarWorld.fromWorld(world);
        if (polarWorld == null) return;
        updateWorld(world, polarWorld, PolarWorldAccess.POLAR_PAPER_FEATURES, ChunkSelector.all());
    }

    /**
     * Updates the chunks in a PolarWorld
     *
     * @param world The bukkit world to retrieve chunks from
     * @param polarWorld The polar world
     * @param polarWorldAccess Describes how userdata should be handled (default PolarWorldAccess.POLAR_PAPER_FEATURES)
     * @param chunkSelector Used to filter which chunks should update
     * @see Polar#saveWorld(World, PolarSource)
     */
    public static void updateWorld(World world, PolarWorld polarWorld, PolarWorldAccess polarWorldAccess, ChunkSelector chunkSelector) {
        // TODO: consider offsets
        // TODO: chunk holders should probably be eventually released/removed (config option?)

        ChunkSystemServerLevel chunkSystemServerLevel = ((CraftWorld) world).getHandle();
        ChunkHolderManager chunkHolderManager = chunkSystemServerLevel.moonrise$getChunkTaskScheduler().chunkHolderManager;

        for (NewChunkHolder chunkHolder : chunkHolderManager.getChunkHolders()) {
            if (chunkHolder == null) continue;
            ChunkAccess currentChunk = chunkHolder.getCurrentChunk();
            if (currentChunk == null) continue;

            int chunkX = chunkHolder.chunkX;
            int chunkZ = chunkHolder.chunkZ;

            if (!chunkSelector.test(chunkX, chunkZ)) continue;

            ChunkEntitySlices entityChunk = chunkHolder.getEntityChunk();
            boolean unsaved = currentChunk.isUnsaved();

            boolean onlyPlayers = true;
            if (entityChunk != null) {
                for (net.minecraft.world.entity.Entity nmsEntity : entityChunk.getAllEntities()) {
                    Entity entity = nmsEntity.getBukkitEntity();
                    if (entity.getType() != EntityType.PLAYER) {
                        onlyPlayers = false;
                        break;
                    }
                }
            }

            if (onlyPlayers) { // if contains no entities or the entities are all players (only difference is blocks)
                if (!unsaved) continue;

                boolean allEmpty = true;
                for (LevelChunkSection section : currentChunk.getSections()) {
                    if (!section.hasOnlyAir()) {
                        allEmpty = false;
                        break;
                    }
                }

                if (allEmpty) {
                    // check if the chunk has generated the surface yet
                    // (otherwise we don't know if it's blank because its really blank, or because it hasn't generated yet)
                    if (currentChunk.getPersistedStatus().isOrBefore(ChunkStatus.SURFACE)) continue;
                    polarWorld.removeChunkAt(chunkX, chunkZ);
                    continue;
                }
            } else {
                if (!unsaved) { // if only difference is entities
                    PolarChunk prevChunk = polarWorld.chunkAt(chunkX, chunkZ);
                    if (prevChunk == null) continue;

                    // only update entities
                    ByteArrayDataOutput userDataOutput = ByteStreams.newDataOutput();
                    List<net.minecraft.world.entity.Entity> allEntities = entityChunk.getAllEntities();
                    Entity[] entitiesArray = new Entity[allEntities.size()];
                    for (int i = 0; i < allEntities.size(); i++) {
                        entitiesArray[i] = allEntities.get(i).getBukkitEntity();
                    }
                    polarWorldAccess.saveChunkData(currentChunk, currentChunk.blockEntities.entrySet(), entitiesArray, userDataOutput);
                    byte[] userData = userDataOutput.toByteArray();

                    polarWorld.updateChunkAt(chunkX, chunkZ, prevChunk.withUserData(userData));

                    continue;
                }
            }

            int minHeight = currentChunk.getMinY();
            int maxHeight = currentChunk.getMaxY();
            PolarChunk polarChunk = createPolarChunk(polarWorldAccess, currentChunk, entityChunk, chunkX, chunkZ, minHeight, maxHeight);
            polarWorld.updateChunkAt(chunkX, chunkZ, polarChunk);

            currentChunk.tryMarkSaved();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static @Nullable World loadWorld(WorldCreator creator, Difficulty difficulty, Map<String, Object> gamerules, boolean allowMonsters, boolean allowAnimals, long time) {
        // TODO: config option for async world loading

        CraftServer craftServer = (CraftServer) Bukkit.getServer();

        // Check if already existing
        if (craftServer.getWorld(creator.name()) != null) {
            return null;
        }

        Preconditions.checkState(craftServer.getServer().getAllLevels().iterator().hasNext(), "Cannot create additional worlds on STARTUP");
        //Preconditions.checkState(!this.console.isIteratingOverLevels, "Cannot create a world while worlds are being ticked"); // Paper - Cat - Temp disable. We'll see how this goes.
        Preconditions.checkArgument(creator != null, "WorldCreator cannot be null");

        String name = creator.name();
        ChunkGenerator chunkGenerator = creator.generator();
        BiomeProvider biomeProvider = creator.biomeProvider();
        File folder = new File(craftServer.getWorldContainer(), name);
        World world = craftServer.getWorld(name);

        // Paper start
        World worldByKey = craftServer.getWorld(creator.key());
        if (world != null || worldByKey != null) {
            if (world == worldByKey) {
                return world;
            }
            throw new IllegalArgumentException("Cannot create a world with key " + creator.key() + " and name " + name + " one (or both) already match a world that exists");
        }
        // Paper end

        if (folder.exists()) {
            Preconditions.checkArgument(folder.isDirectory(), "File (%s) exists and isn't a folder", name);
        }

        if (chunkGenerator == null) {
            chunkGenerator = craftServer.getGenerator(name);
        }

        if (biomeProvider == null) {
            biomeProvider = craftServer.getBiomeProvider(name);
        }

        ResourceKey<LevelStem> actualDimension = switch (creator.environment()) {
            case NORMAL -> LevelStem.OVERWORLD;
            case NETHER -> LevelStem.NETHER;
            case THE_END -> LevelStem.END;
            default -> throw new IllegalArgumentException("Illegal dimension (" + creator.environment() + ")");
        };

        LevelStorageSource.LevelStorageAccess levelStorageAccess;
        try {
            Path pluginFolder = Path.of(PolarPaper.getPlugin().getDataFolder().getAbsolutePath());
            Path tempFolder = pluginFolder.resolve("temp");

            levelStorageAccess = LevelStorageSource.createDefault(tempFolder).validateAndCreateAccess(name, actualDimension);
        } catch (IOException | ContentValidationException ex) {
            throw new RuntimeException(ex);
        }

        boolean hardcore = creator.hardcore();

        PrimaryLevelData primaryLevelData;
        WorldLoader.DataLoadContext context = craftServer.getServer().worldLoaderContext;
        RegistryAccess.Frozen registryAccess = context.datapackDimensions();
        Registry<LevelStem> contextLevelStemRegistry = registryAccess.lookupOrThrow(Registries.LEVEL_STEM);
        Dynamic<?> dataTag = PaperWorldLoader.getLevelData(levelStorageAccess).dataTag();
        if (dataTag != null) {
            LevelDataAndDimensions levelDataAndDimensions = LevelStorageSource.getLevelDataAndDimensions(
                    dataTag, context.dataConfiguration(), contextLevelStemRegistry, context.datapackWorldgen()
            );
            primaryLevelData = (PrimaryLevelData) levelDataAndDimensions.worldData();
            registryAccess = levelDataAndDimensions.dimensions().dimensionsRegistryAccess();
        } else {
            LevelSettings levelSettings;
            WorldOptions worldOptions = new WorldOptions(creator.seed(), creator.generateStructures(), creator.bonusChest());
            WorldDimensions worldDimensions;

            DedicatedServerProperties.WorldDimensionData properties = new DedicatedServerProperties.WorldDimensionData(GsonHelper.parse((creator.generatorSettings().isEmpty()) ? "{}" : creator.generatorSettings()), creator.type().name().toLowerCase(Locale.ROOT));
            levelSettings = new LevelSettings(
                    name,
                    GameType.byId(craftServer.getDefaultGameMode().getValue()),
                    hardcore, difficulty,
                    false,
                    new GameRules(context.dataConfiguration().enabledFeatures()),
                    context.dataConfiguration()
            );
            worldDimensions = properties.create(context.datapackWorldgen());

            WorldDimensions.Complete complete = worldDimensions.bake(contextLevelStemRegistry);
            Lifecycle lifecycle = complete.lifecycle().add(context.datapackWorldgen().allRegistriesLifecycle());

            primaryLevelData = new PrimaryLevelData(levelSettings, worldOptions, complete.specialWorldProperty(), lifecycle);
            registryAccess = complete.dimensionsRegistryAccess();
        }

        contextLevelStemRegistry = registryAccess.lookupOrThrow(Registries.LEVEL_STEM);
        primaryLevelData.customDimensions = contextLevelStemRegistry;
        primaryLevelData.checkName(name);
        primaryLevelData.setModdedInfo(craftServer.getServer().getServerModName(), craftServer.getServer().getModdedStatus().shouldReportAsModified());

        if (craftServer.getServer().options.has("forceUpgrade")) {
            Main.forceUpgrade(levelStorageAccess, primaryLevelData, DataFixers.getDataFixer(), craftServer.getServer().options.has("eraseCache"), () -> true, registryAccess, craftServer.getServer().options.has("recreateRegionFiles"));
        }

        long i = BiomeManager.obfuscateSeed(primaryLevelData.worldGenOptions().seed());
        List<CustomSpawner> list = ImmutableList.of(
//                new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new VillageSiege(), new WanderingTraderSpawner(primaryLevelData)
        );
        LevelStem customStem = contextLevelStemRegistry.getValue(actualDimension);

        WorldInfo worldInfo = new CraftWorldInfo(primaryLevelData, levelStorageAccess, creator.environment(), customStem.type().value(), customStem.generator(), craftServer.getHandle().getServer().registryAccess()); // Paper - Expose vanilla BiomeProvider from WorldInfo
        if (biomeProvider == null && chunkGenerator != null) {
            biomeProvider = chunkGenerator.getDefaultBiomeProvider(worldInfo);
        }

        ResourceKey<Level> dimensionKey;
        String levelName = craftServer.getServer().getProperties().levelName;
        if (name.equals(levelName + "_nether")) {
            dimensionKey = Level.NETHER;
        } else if (name.equals(levelName + "_the_end")) {
            dimensionKey = Level.END;
        } else {
            dimensionKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(creator.key().namespace(), creator.key().value()));
        }

        ServerLevel serverLevel = new PolarServerLevel(
                craftServer.getServer(),
                craftServer.getServer().executor,
                levelStorageAccess,
                primaryLevelData,
                dimensionKey,
                customStem,
                primaryLevelData.isDebugWorld(),
                i,
                creator.environment() == World.Environment.NORMAL ? list : ImmutableList.of(),
                true,
                craftServer.getServer().overworld().getRandomSequences(),
                creator.environment(),
                chunkGenerator, biomeProvider
        );

        for (Map.Entry<String, Object> entry : gamerules.entrySet()) {
            GameRules.Key<?> key = serverLevel.getWorld().getGameRulesNMS().get(entry.getKey());
            if (key == null) continue;
            GameRules.Value<?> handle = serverLevel.getGameRules().getRule(key);
            handle.deserialize(String.valueOf(entry.getValue()));
            handle.onChanged(serverLevel);
        }

//        if (!(craftServer.getWorlds().containsKey(name.toLowerCase(Locale.ROOT)))) {
//            return null;
//        }

        serverLevel.setDayTime(time);

        craftServer.getServer().addLevel(serverLevel); // Paper - Put world into worldlist before initing the world; move up
        craftServer.getServer().initWorld(serverLevel, primaryLevelData, primaryLevelData.worldGenOptions());

        serverLevel.getChunkSource().setSpawnSettings(allowMonsters, allowAnimals);
        // Paper - Put world into worldlist before initing the world; move up

        craftServer.getServer().prepareLevel(serverLevel);

        return serverLevel.getWorld();
    }

    public static PolarChunk createPolarChunk(PolarWorldAccess worldAccess, ChunkAccess chunkAccess, ChunkEntitySlices entityChunk, int newChunkX, int newChunkZ, int minHeight, int maxHeight) {
        List<PolarChunk.BlockEntity> polarBlockEntities = new ArrayList<>();

        Registry<net.minecraft.world.level.biome.Biome> biomeRegistry = MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.BIOME);

        int worldHeight = (maxHeight + 1) - minHeight;
        int sectionCount = worldHeight / 16;

        PolarSection[] sections = new PolarSection[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            int sectionY = minHeight + i * 16;

            LevelChunkSection chunkAccessSection = chunkAccess.getSection(i);

            int[] blockData = null;
            int[] biomeData = null;

            List<String> blockPaletteStrings = new ArrayList<>();
            List<String> biomePaletteStrings = new ArrayList<>();
            if (!chunkAccessSection.hasOnlyAir()) {
                PalettedContainer.Data<BlockState> blockPaletteData = chunkAccessSection.getStates().data;
                Object[] palette = blockPaletteData.moonrise$getPalette();
                for (Object p : palette) {
                    if (p == null) continue;
                    if (!(p instanceof BlockState blockState)) continue;
                    blockPaletteStrings.add(blockState.toString());
                }

                BitStorage blockBitStorage = blockPaletteData.storage();
                int blockPaletteSize = blockBitStorage.getSize();
                blockData = new int[blockPaletteSize];

                for(int index = 0; index < blockPaletteSize; ++index) {
                    int paletteIdx = blockBitStorage.get(index);
                    blockData[index] = paletteIdx;
                }
            }
            PalettedContainer.Data<Holder<Biome>> biomePaletteData = ((PalettedContainer<Holder<Biome>>)chunkAccessSection.getBiomes()).data;
            Object[] biomePalette = biomePaletteData.moonrise$getPalette();
            for (Object p : biomePalette) {
                if (p == null) continue;
                if (!(p instanceof Holder<?> biomeHolder)) continue;
                if (!(biomeHolder.value() instanceof Biome biome)) continue;
                ResourceLocation key = biomeRegistry.getKey(biome);
                if (key == null) continue;
                String biomeString = key.getPath();
                biomePaletteStrings.add(biomeString);
            }

            BitStorage biomeBitStorage = biomePaletteData.storage();
            int biomePaletteSize = biomeBitStorage.getSize();
            biomeData = new int[biomePaletteSize];

            for(int index = 0; index < biomePaletteSize; ++index) {
                int paletteIdx = biomeBitStorage.get(index);
                biomeData[index] = paletteIdx;
            }

            sections[i] = new PolarSection(
                    blockPaletteStrings.toArray(new String[0]), blockData,
                    biomePaletteStrings.toArray(new String[0]), biomeData,
                    PolarSection.LightContent.MISSING, null, // TODO: Provide block light
                    PolarSection.LightContent.MISSING, null
            );
        }

        var registryAccess = ((CraftServer) Bukkit.getServer()).getServer().registryAccess();
        Set<Map.Entry<BlockPos, BlockEntity>> blockEntities = chunkAccess.blockEntities.entrySet();
        for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities) {
            BlockPos blockPos = entry.getKey();
            BlockEntity blockEntity = entry.getValue();

            if (blockPos == null || blockEntity == null) continue;

            CompoundTag compoundTag = blockEntity.saveWithFullMetadata(registryAccess);

            Optional<String> id = compoundTag.getString("id");
            if (id.isEmpty()) {
                PolarPaper.logger().warning("No ID in block entity data at: " + blockPos);
                PolarPaper.logger().warning("Compound tag: " + compoundTag);
                continue;
            }

            int index = CoordConversion.chunkBlockIndex(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            polarBlockEntities.add(new PolarChunk.BlockEntity(index, id.get(), compoundTag));
        }

        int[][] heightMaps = new int[PolarChunk.MAX_HEIGHTMAPS][0];
        worldAccess.saveHeightmaps(chunkAccess, heightMaps);

        ByteArrayDataOutput userDataOutput = ByteStreams.newDataOutput();
        List<net.minecraft.world.entity.Entity> allEntities = entityChunk == null ? List.of() : entityChunk.getAllEntities();
        Entity[] entitiesArray = new Entity[allEntities.size()];
        for (int i = 0; i < allEntities.size(); i++) {
            entitiesArray[i] = allEntities.get(i).getBukkitEntity();
        }
        worldAccess.saveChunkData(chunkAccess, blockEntities, entitiesArray, userDataOutput);
        byte[] userData = userDataOutput.toByteArray();

        return new PolarChunk(
                newChunkX,
                newChunkZ,
                sections,
                polarBlockEntities,
                heightMaps,
                userData
        );
    }

}
