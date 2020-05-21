package com.hrznstudio.albedo.lighting;

import com.hrznstudio.albedo.event.GatherLightsEvent;
import com.hrznstudio.albedo.util.TriConsumer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class BlockLighting extends ForgeRegistryEntry<BlockLighting> {
    private final Block block;
    private final TriConsumer<BlockPos, BlockState, GatherLightsEvent> consumer;

    public BlockLighting(Block block, TriConsumer<BlockPos, BlockState, GatherLightsEvent> consumer) {
        this.block = block;
        this.consumer = consumer;
    }

    public Block getBlock() {
        return block;
    }

    public TriConsumer<BlockPos, BlockState, GatherLightsEvent> getConsumer() {
        return consumer;
    }
}
