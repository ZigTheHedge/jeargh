package com.cwelth.jeargh.utils;

import com.cwelth.jeargh.JEARGH;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class JEARGHProfiler implements Runnable{
    public static final int CHUNK_SIZE = 16;
    public static int chunk_radius_to_profile = 100;
    public static int max_threads;
    private ExecutorService executorService;
    public static int chunks_to_profile;
    public static int chunks_to_profile_total;
    public static int profiled_chunks;
    public static int profiled_chunks_total;
    public static boolean shouldAbort = false;
    public boolean shouldMerge = false;
    public Player player;
    public static ConcurrentMap<Block, HashMap<Integer, Integer>> collectedData = new ConcurrentHashMap<>();
    public static ConcurrentMap<Block, List<ItemStackDrop>> dropsMap = new ConcurrentHashMap<>();
    private List<WorldGenJson> worldGen = new ArrayList<>();
    private List<WorldGenJson> worldGenExisting = new ArrayList<>();

    public JEARGHProfiler()
    {
        max_threads = Runtime.getRuntime().availableProcessors() * 2;
    }

    public static String getPath() {
        File file = Minecraft.getInstance().gameDirectory;
        try {
            return file.getCanonicalFile().getPath();
        } catch (final IOException e) {

        }
        return file.getPath();
    }

    public void cyclicRename()
    {
        int counter = 0;
        while(true)
        {
            counter++;
            File file_old = new File(getPath() + "/config/world-gen-" + counter + ".json");
            if(!file_old.exists()) break;
        }
        for(int ic = counter; ic > 1; ic--)
        {
            File file_old = new File(getPath() + "/config/world-gen-" + (ic - 1) + ".json");
            File file_new = new File(getPath() + "/config/world-gen-" + (ic) + ".json");
            if(!file_old.renameTo(file_new))
            {
                player.sendSystemMessage(Component.translatable("profiling.generate.backup.failed", file_old.getName()));
            }
        }
        File file_old = new File(getPath() + "/config/world-gen.json");
        File file_new = new File(getPath() + "/config/world-gen-1.json");
        if(!file_old.renameTo(file_new))
        {
            player.sendSystemMessage(Component.translatable("profiling.generate.backup.failed", file_old.getName()));
        }
    }
    public void generateWorldGenFile()
    {
        player.sendSystemMessage(Component.translatable("profiling.generate.file"));
        File file = new File(getPath() + "/config/world-gen.json");
        if(file.exists())
        {
            player.sendSystemMessage(Component.translatable("profiling.generate.backup"));
            cyclicRename();
        }

        Gson builder = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        try {
            Writer writer = new FileWriter(file);
            builder.toJson(worldGen, writer);
            writer.flush();
            writer.close();
        } catch (JsonIOException e) {
            JEARGH.LOGGER.error("Cannot save world-gen.json. " + e.getLocalizedMessage());
        } catch (IOException e) {
            JEARGH.LOGGER.error("Cannot save world-gen.json. " + e.getLocalizedMessage());
        }
        player.sendSystemMessage(Component.translatable("profiling.generate.done"));
    }
    public String buildItemDrops(Block block)
    {
        if(dropsMap.containsKey(block)) {
            String toRet = "";
            for(ItemStackDrop drop : dropsMap.get(block))
            {
                String toAdd = ForgeRegistries.ITEMS.getKey(drop.getItem()) + "," + drop.getChance();
                toRet += (toRet.isEmpty())? toAdd : ";" + toAdd;
            }
            return toRet;
        }
        return ForgeRegistries.BLOCKS.getKey(block).toString() + ",1.0";
    }
    public void dumpForDimension(String dimension)
    {
        for(Map.Entry<Block, HashMap<Integer, Integer>> ore: collectedData.entrySet())
        {
            String blockName = ForgeRegistries.BLOCKS.getKey(ore.getKey()).toString();
            String blockDrops = buildItemDrops(ore.getKey());
            worldGen.add(new WorldGenJson(blockName, blockDrops, WorldGenJson.BuildDistrib(ore.getValue(), chunks_to_profile * CHUNK_SIZE), WorldGenJson.BuildRawDistrib(ore.getValue()), dimension));
        }
        collectedData.clear();
        profiled_chunks = 0;
    }

    public boolean loadWorldGen()
    {
        File file = new File(getPath() + "/config/world-gen.json");
        if(!file.exists())
        {
            player.sendSystemMessage(Component.translatable("profiling.merge.nofile"));
            return false;
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            worldGenExisting = new Gson().fromJson(br, new TypeToken<List<WorldGenJson>>() {}.getType());
            if(worldGenExisting == null) return false;
            br.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public void loadMapFromWorldGen(String dimension)
    {
        collectedData.clear();
        for(WorldGenJson entry: worldGenExisting)
        {
            if(entry.dim.equals(dimension))
            {
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.block));
                String[] distribs = entry.distrib_raw.split(";");
                for(String distrib : distribs)
                {
                    String[] gen = distrib.split(",");
                    JEARGHProfilerRunnable.addToMap(block, Integer.parseInt(gen[0]), Integer.parseInt(gen[1]));
                }
            }
        }
    }
    public void loadToWorldGen(String except_dimension)
    {
        for(WorldGenJson entry: worldGenExisting)
        {
            if(!entry.dim.equals(except_dimension))
            {
                worldGen.add(entry);
            }
        }
    }

    public void fixIncorrectHeights(Player player, int chunk_radius_to_profile)
    {
        this.player = player;
        JEARGHProfiler.chunk_radius_to_profile = chunk_radius_to_profile;
        chunks_to_profile = chunk_radius_to_profile * chunk_radius_to_profile * 4;
        player.sendSystemMessage(Component.translatable("profiling.fix"));
        worldGen.clear();
        dropsMap.clear();
        loadWorldGen();
        for(WorldGenJson entry: worldGenExisting)
        {
            if(!entry.dim.equals("minecraft:overworld"))
            {
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.block));
                String[] distribs = entry.distrib_raw.split(";");
                for(String distrib : distribs)
                {
                    String[] gen = distrib.split(",");
                    JEARGHProfilerRunnable.addToMap(block, Integer.parseInt(gen[0]) + 64, Integer.parseInt(gen[1]));
                }
                dumpForDimension(entry.dim);
            } else
                worldGen.add(entry);
        }
        player.sendSystemMessage(Component.translatable("profiling.complete"));
        generateWorldGenFile();
    }
    public void start(Player player, int chunk_radius_to_profile, boolean shouldMerge)
    {
        shouldAbort = false;
        this.shouldMerge = shouldMerge;
        profiled_chunks = 0;
        profiled_chunks_total = 0;
        worldGen.clear();
        dropsMap.clear();
        JEARGHProfiler.chunk_radius_to_profile = chunk_radius_to_profile;
        this.player = player;
        new Thread(this).start();
    }

    public void abort()
    {
        shouldAbort = true;
        player.sendSystemMessage(Component.translatable("profiling.abort.start"));
        executorService.shutdownNow();
        awaitTermination();
        player.sendSystemMessage(Component.translatable("profiling.abort"));
        collectedData.clear();
        profiled_chunks = 0;
        profiled_chunks_total = 0;
    }

    private void awaitTermination() {
        while (true) {
            try {
                if (executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    break;
                } else {
                    if(shouldAbort)
                    {
                        JEARGH.LOGGER.warn("Forced shutdown!");
                        executorService.shutdownNow();
                        break;
                    }
                }
            } catch (InterruptedException ex) {
                // Nope
                JEARGH.LOGGER.warn("Interrupted!");
            }
        }
    }

    @Override
    public void run() {
        if(player.level.isClientSide()) return;
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        int xPosition = player.getOnPos().getX();
        int zPosition = player.getOnPos().getZ();

        if(shouldMerge)
        {
            ServerLevel serverLevel = (ServerLevel)player.level;
            if(!loadWorldGen()) return;
            loadMapFromWorldGen(serverLevel.dimension().location().toString());

            player.sendSystemMessage(Component.translatable("profiling.start.merge", max_threads));
            chunks_to_profile = chunk_radius_to_profile * chunk_radius_to_profile * 4;
            chunks_to_profile_total = chunks_to_profile;

            executorService = Executors.newFixedThreadPool(max_threads);
            player.sendSystemMessage(Component.translatable("profiling.start.dimension", serverLevel.dimension().location(), 1, 1, chunk_radius_to_profile, chunks_to_profile));
            for (int x = -chunk_radius_to_profile * CHUNK_SIZE + xPosition; x < chunk_radius_to_profile * CHUNK_SIZE + xPosition; x += CHUNK_SIZE) {
                for (int z = -chunk_radius_to_profile * CHUNK_SIZE + zPosition; z < chunk_radius_to_profile * CHUNK_SIZE + zPosition; z += CHUNK_SIZE) {
                    executorService.execute(new JEARGHProfilerRunnable(x, z, serverLevel, player));
                }
            }
            executorService.shutdown();
            awaitTermination();
            if(shouldAbort) return;
            player.sendSystemMessage(Component.translatable("profiling.complete.dimension", serverLevel.dimension().location()));
            executorService = Executors.newFixedThreadPool(1);
            executorService.execute(new DropCalculatorRunnable(serverLevel));
            executorService.shutdown();
            awaitTermination();
            dumpForDimension(serverLevel.dimension().location().toString());
            loadToWorldGen(serverLevel.dimension().location().toString());
        } else
        {
            player.sendSystemMessage(Component.translatable("profiling.start", max_threads));
            int dimcount = 0;
            int dimtotal = server.levelKeys().size();
            chunks_to_profile = chunk_radius_to_profile * chunk_radius_to_profile * 4;
            chunks_to_profile_total = chunks_to_profile * dimtotal;
            for (ResourceKey<Level> levelResourceKey : server.levelKeys()) {
                dimcount++;
                ServerLevel serverLevel = server.getLevel(levelResourceKey);
                executorService = Executors.newFixedThreadPool(max_threads);
                player.sendSystemMessage(Component.translatable("profiling.start.dimension", serverLevel.dimension().location(), dimcount, dimtotal, chunk_radius_to_profile, chunks_to_profile));
                for (int x = -chunk_radius_to_profile * CHUNK_SIZE + xPosition; x < chunk_radius_to_profile * CHUNK_SIZE + xPosition; x += CHUNK_SIZE) {
                    for (int z = -chunk_radius_to_profile * CHUNK_SIZE + zPosition; z < chunk_radius_to_profile * CHUNK_SIZE + zPosition; z += CHUNK_SIZE) {
                        executorService.execute(new JEARGHProfilerRunnable(x, z, serverLevel, player));
                    }
                }
                executorService.shutdown();
                awaitTermination();
                if(shouldAbort) return;
                player.sendSystemMessage(Component.translatable("profiling.complete.dimension", serverLevel.dimension().location()));
                executorService = Executors.newFixedThreadPool(1);
                executorService.execute(new DropCalculatorRunnable(serverLevel));
                executorService.shutdown();
                awaitTermination();
                dumpForDimension(serverLevel.dimension().location().toString());
            }
        }

        player.sendSystemMessage(Component.translatable("profiling.complete"));
        generateWorldGenFile();
    }
}
