package com.hrznstudio.albedo.event;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraftforge.eventbus.api.Event;

public class RenderChunkUniformsEvent extends Event {
    private final ChunkRenderDispatcher.ChunkRender renderChunk;

    public RenderChunkUniformsEvent(ChunkRenderDispatcher.ChunkRender r) {
        super();
        this.renderChunk = r;
    }

    public ChunkRenderDispatcher.ChunkRender getChunk() {
        return renderChunk;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }
}
