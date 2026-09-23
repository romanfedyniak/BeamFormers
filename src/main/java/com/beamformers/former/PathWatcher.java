package com.beamformers.former;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Wakes a beam when a block on its path changes, when the block its colour comes from is updated at all, or when
 * the unloaded chunk its path ran into loads. Nothing scans on a timer.
 */
public final class PathWatcher implements IWorldEventListener {

    public static final PathWatcher INSTANCE = new PathWatcher();

    private final Map<World, Long2ObjectMap<Set<BeamEnd>>> byBlock = new WeakHashMap<>();
    private final Map<World, Long2ObjectMap<Set<BeamEnd>>> byColorSource = new WeakHashMap<>();
    private final Map<World, Long2ObjectMap<Set<BeamEnd>>> byChunk = new WeakHashMap<>();
    private final Map<BeamEnd, Watch> watches = new HashMap<>();

    private PathWatcher() {}

    void watch(BeamEnd end, World world, List<BlockPos> blocks, BlockPos colorSource, @Nullable ChunkPos chunk) {
        this.unwatch(end);
        final long[] keys = new long[blocks.size()];
        final Long2ObjectMap<Set<BeamEnd>> worldBlocks = this.byBlock.computeIfAbsent(world, w -> new Long2ObjectOpenHashMap<>());
        for (int i = 0; i < keys.length; i++) {
            keys[i] = blocks.get(i).toLong();
            worldBlocks.computeIfAbsent(keys[i], k -> new ReferenceOpenHashSet<>()).add(end);
        }
        final long colorKey = colorSource.toLong();
        this.byColorSource.computeIfAbsent(world, w -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(colorKey, k -> new ReferenceOpenHashSet<>()).add(end);
        final long chunkKey = chunk == null ? 0 : ChunkPos.asLong(chunk.x, chunk.z);
        if (chunk != null) {
            this.byChunk.computeIfAbsent(world, w -> new Long2ObjectOpenHashMap<>())
                    .computeIfAbsent(chunkKey, k -> new ReferenceOpenHashSet<>()).add(end);
        }
        this.watches.put(end, new Watch(world, keys, colorKey, chunk != null, chunkKey));
    }

    void unwatch(BeamEnd end) {
        final Watch watch = this.watches.remove(end);
        if (watch == null) {
            return;
        }
        final Long2ObjectMap<Set<BeamEnd>> worldBlocks = this.byBlock.get(watch.world);
        if (worldBlocks != null) {
            for (long key : watch.blocks) {
                removeFrom(worldBlocks, key, end);
            }
        }
        final Long2ObjectMap<Set<BeamEnd>> worldColors = this.byColorSource.get(watch.world);
        if (worldColors != null) {
            removeFrom(worldColors, watch.colorSource, end);
        }
        final Long2ObjectMap<Set<BeamEnd>> worldChunks = this.byChunk.get(watch.world);
        if (watch.hasChunk && worldChunks != null) {
            removeFrom(worldChunks, watch.chunk, end);
        }
    }

    private static void removeFrom(Long2ObjectMap<Set<BeamEnd>> map, long key, BeamEnd end) {
        final Set<BeamEnd> set = map.get(key);
        if (set != null && set.remove(end) && set.isEmpty()) {
            map.remove(key);
        }
    }

    private static void wakeAll(@Nullable Set<BeamEnd> ends) {
        if (ends != null) {
            for (BeamEnd end : new ArrayList<>(ends)) {
                end.getFormer().wake();
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote) {
            event.getWorld().addEventListener(this);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        this.byBlock.remove(event.getWorld());
        this.byColorSource.remove(event.getWorld());
        this.byChunk.remove(event.getWorld());
        this.watches.values().removeIf(watch -> watch.world == event.getWorld());
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        final Long2ObjectMap<Set<BeamEnd>> worldChunks = this.byChunk.get(event.getWorld());
        if (worldChunks != null) {
            wakeAll(worldChunks.get(ChunkPos.asLong(event.getChunk().x, event.getChunk().z)));
        }
    }

    @Override
    public void notifyBlockUpdate(World world, BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
        final Long2ObjectMap<Set<BeamEnd>> worldColors = this.byColorSource.get(world);
        if (worldColors != null) {
            wakeAll(worldColors.get(pos.toLong()));
        }
        if (oldState == newState) {
            return;
        }
        final Long2ObjectMap<Set<BeamEnd>> worldBlocks = this.byBlock.get(world);
        if (worldBlocks != null) {
            wakeAll(worldBlocks.get(pos.toLong()));
        }
    }

    @Override
    public void notifyLightSet(BlockPos pos) {}

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}

    @Override
    public void playSoundToAllNearExcept(@Nullable EntityPlayer player, SoundEvent sound, SoundCategory category,
            double x, double y, double z, float volume, float pitch) {}

    @Override
    public void playRecord(SoundEvent sound, BlockPos pos) {}

    @Override
    public void spawnParticle(int particleID, boolean ignoreRange, double x, double y, double z, double xSpeed,
            double ySpeed, double zSpeed, int... parameters) {}

    @Override
    public void spawnParticle(int id, boolean ignoreRange, boolean minimiseParticleLevel, double x, double y,
            double z, double xSpeed, double ySpeed, double zSpeed, int... parameters) {}

    @Override
    public void onEntityAdded(Entity entity) {}

    @Override
    public void onEntityRemoved(Entity entity) {}

    @Override
    public void broadcastSound(int soundID, BlockPos pos, int data) {}

    @Override
    public void playEvent(EntityPlayer player, int type, BlockPos pos, int data) {}

    @Override
    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {}

    private static final class Watch {

        final World world;
        final long[] blocks;
        final long colorSource;
        final boolean hasChunk;
        final long chunk;

        Watch(World world, long[] blocks, long colorSource, boolean hasChunk, long chunk) {
            this.world = world;
            this.blocks = blocks;
            this.colorSource = colorSource;
            this.hasChunk = hasChunk;
            this.chunk = chunk;
        }
    }

}
