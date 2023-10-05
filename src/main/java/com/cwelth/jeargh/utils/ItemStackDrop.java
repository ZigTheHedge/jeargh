package com.cwelth.jeargh.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

public class ItemStackDrop {
    private final Item item;
    private float chance = 0;
    private int count = 0;
    public BlockState blockState;
    public BlockPos blockPos;
    public ServerLevel serverLevel;
    public ItemStackDrop(Item item, BlockState blockState, BlockPos blockPos, ServerLevel serverLevel)
    {
        this.item = item;
        this.blockState = blockState;
        this.blockPos = blockPos;
        this.serverLevel = serverLevel;
    }
    public ItemStackDrop(Item item, float chance, BlockState blockState, BlockPos blockPos, ServerLevel serverLevel)
    {
        this(item, blockState, blockPos, serverLevel);
        this.chance = chance;
        this.count = 1;
    }

    public void addDrop(int qty)
    {
        count++;
        chance += (float)qty;
    }
    public Item getItem()
    {
        return item;
    }

    public float getChance()
    {
        if(count == 0) return 0;
        return chance / count;
    }
}
