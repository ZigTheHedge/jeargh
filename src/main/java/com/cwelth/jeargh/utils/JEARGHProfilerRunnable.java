package com.cwelth.jeargh.utils;

import com.cwelth.jeargh.JEARGH;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.cwelth.jeargh.utils.JEARGHProfiler.collectedData;
import static com.cwelth.jeargh.utils.JEARGHProfiler.dropsMap;

public class JEARGHProfilerRunnable implements Runnable{
    private final int minX;
    private final int minZ;
    private final ServerLevel level;
    private final Player player;

    public JEARGHProfilerRunnable(int minX, int minZ, ServerLevel level, Player player)
    {
        this.minX = minX;
        this.minZ = minZ;
        this.level = level;
        this.player = player;
    }

    @Override
    public void run() {
        profile();
        JEARGHProfiler.profiled_chunks++;
        JEARGHProfiler.profiled_chunks_total++;
        if(!JEARGHProfiler.shouldAbort) {
            int percent = JEARGHProfiler.profiled_chunks_total * 100 / JEARGHProfiler.chunks_to_profile_total;
            player.displayClientMessage(Component.translatable("profiling.progress", JEARGHProfiler.profiled_chunks, JEARGHProfiler.chunks_to_profile, JEARGHProfiler.profiled_chunks_total, JEARGHProfiler.chunks_to_profile_total, percent), true);
        }
    }

    public static void addToMap(Block block, int height, int qty)
    {
        HashMap<Integer, Integer> data;
        if(collectedData.containsKey(block))
        {
            data = collectedData.get(block);
            if(data.containsKey(height))
            {
                data.put(height, data.get(height) + qty);
            } else
                data.put(height, qty);
        } else
        {
            data = new HashMap<>();
            data.put(height, qty);
        }
        collectedData.put(block, data);
    }

    public void addToDrops(BlockState blockState, BlockPos pos)
    {
        if(!dropsMap.containsKey(blockState.getBlock()))
        {
            List<ItemStackDrop> toReturn = new ArrayList<>();
            List<ItemStack> drops = Block.getDrops(blockState, level, pos, null);
            for(ItemStack stack: drops)
            {
                toReturn.add(new ItemStackDrop(stack.getItem(), stack.getCount(), blockState, pos, level));
            }
            dropsMap.put(blockState.getBlock(), toReturn);
        }
    }
    public void profile()
    {
        int lowestPoint = level.getMinBuildHeight();
        int highestPoint = level.getLogicalHeight();
        for (int y = lowestPoint; y <= highestPoint; y++) {
            for (int x = minX; x <= minX + JEARGHProfiler.CHUNK_SIZE; x++) {
                for (int z = minZ; z <= minZ + JEARGHProfiler.CHUNK_SIZE; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState foundBlock = level.getBlockState(pos);
                    if (foundBlock.is(Tags.Blocks.ORES)) {
                        addToMap(foundBlock.getBlock(), y + 64, 1);
                        addToDrops(foundBlock, pos);
                    }
                }
            }
        }
    }
}
