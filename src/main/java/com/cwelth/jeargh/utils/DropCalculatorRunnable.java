package com.cwelth.jeargh.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cwelth.jeargh.utils.JEARGHProfiler.dropsMap;

public class DropCalculatorRunnable implements Runnable{
    private final ServerLevel level;
    private final RandomSource randomSource = RandomSource.createNewThreadLocalInstance();
    public DropCalculatorRunnable(ServerLevel level)
    {
        this.level = level;
    }

    public List<ItemStack> getDrops(ServerLevel pLevel, BlockPos pPos, BlockState pState)
    {
        LootContext.Builder lootcontext$builder = (new LootContext.Builder(pLevel)).withRandom(randomSource).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pPos)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
        return pState.getDrops(lootcontext$builder);
    }
    @Override
    public void run() {
        for(Map.Entry<Block, List<ItemStackDrop>> dropMap : dropsMap.entrySet()) {
            Map<Item, ItemStackDrop> localDrops = new HashMap<>();
            BlockState blockState = null;
            BlockPos blockPos = null;
            ServerLevel serverLevel = null;
            for (ItemStackDrop stack : dropMap.getValue())
            {
                blockState = stack.blockState;
                blockPos = stack.blockPos;
                serverLevel = stack.serverLevel;
                if(stack.getItem() != null)
                    localDrops.put(stack.getItem(), stack);
            }

            int max_iterations = 1000;
            for(int i = 0; i < max_iterations; i++)
            {
                List<ItemStack> drops = getDrops(level, blockPos, blockState);
                for(ItemStack itemStack: drops)
                {
                    Item item = itemStack.getItem();
                    if(!localDrops.containsKey(item))
                    {
                        localDrops.put(item, new ItemStackDrop(item, blockState, blockPos, serverLevel));
                    }
                    ItemStackDrop itemStackDrop = localDrops.get(item);
                    itemStackDrop.addDrop(itemStack.getCount());
                    localDrops.put(item, itemStackDrop);
                }
            }

            List<ItemStackDrop> newDropList = new ArrayList<>();
            for(Map.Entry<Item, ItemStackDrop> entry : localDrops.entrySet())
            {
                newDropList.add(entry.getValue());
            }
            dropsMap.put(dropMap.getKey(), newDropList);
        }
    }
}
