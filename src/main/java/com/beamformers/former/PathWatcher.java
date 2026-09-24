package com.beamformers.former;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
 * an unloaded chunk its path ran into loads. Nothing scans on a timer.
 */
public final class PathWatcher implements IWorldEventListener {

    public interface Watcher {

        /** Asks for a look along the paths on the next grid tick. */
        void wake();

    }

    public static final PathWatcher INSTANCE = new PathWatcher();

    private final Map<World, Long2ObjectMap<Set<Watcher>>> byBlock = new WeakHashMap<>();
    private final Map<World, Long2ObjectMap<Set<Watcher>>> byColorSource = new WeakHashMap<>();
    private final Map<World, Long2ObjectMap<Set<Watcher>>> byChunk = new WeakHashMap<>();
    private final Map<Watcher, Watch> watches = new HashMap<>();

    private PathWatcher() {}

    public void watch(Watcher watcher, World world, Collection<BlockPos> blocks, BlockPos colorSource,
            Collection<ChunkPos> chunks) {
        this.unwatch(watcher);
        final long[] keys = blocks.stream().mapToLong(BlockPos::toLong).toArray();
        final long colorKey = colorSource.toLong();
        final long[] chunkKeys = chunks.stream().mapToLong(c -> ChunkPos.asLong(c.x, c.z)).toArray();
        add(this.byBlock, world, keys, watcher);
        add(this.byColorSource, world, new long[] { colorKey }, watcher);
        add(this.byChunk, world, chunkKeys, watcher);
        this.watches.put(watcher, new Watch(world, keys, colorKey, chunkKeys));
    }

    public void unwatch(Watcher watcher) {
        final Watch watch = this.watches.remove(watcher);
        if (watch == null) {
            return;
        }
        remove(this.byBlock, watch.world, watch.blocks, watcher);
        remove(this.byColorSource, watch.world, new long[] { watch.colorSource }, watcher);
        remove(this.byChunk, watch.world, watch.chunks, watcher);
    }

    private static void add(Map<World, Long2ObjectMap<Set<Watcher>>> maps, World world, long[] keys, Watcher watcher) {
        final Long2ObjectMap<Set<Watcher>> map = maps.computeIfAbsent(world, w -> new Long2ObjectOpenHashMap<>());
        for (long key : keys) {
            map.computeIfAbsent(key, k -> new ReferenceOpenHashSet<>()).add(watcher);
        }
    }

    private static void remove(Map<World, Long2ObjectMap<Set<Watcher>>> maps, World world, long[] keys, Watcher watcher) {
        final Long2ObjectMap<Set<Watcher>> map = maps.get(world);
        if (map == null) {
            return;
        }
        for (long key : keys) {
            final Set<Watcher> set = map.get(key);
            if (set != null && set.remove(watcher) && set.isEmpty()) {
                map.remove(key);
            }
        }
    }

    private static void wakeAll(@Nullable Set<Watcher> watchers) {
        if (watchers != null) {
            new ArrayList<>(watchers).forEach(Watcher::wake);
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
        final Long2ObjectMap<Set<Watcher>> worldChunks = this.byChunk.get(event.getWorld());
        if (worldChunks != null) {
            wakeAll(worldChunks.get(ChunkPos.asLong(event.getChunk().x, event.getChunk().z)));
        }
    }

    @Override
    public void notifyBlockUpdate(World world, BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
        final Long2ObjectMap<Set<Watcher>> worldColors = this.byColorSource.get(world);
        if (worldColors != null) {
            wakeAll(worldColors.get(pos.toLong()));
        }
        if (oldState == newState) {
            return;
        }
        final Long2ObjectMap<Set<Watcher>> worldBlocks = this.byBlock.get(world);
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
        final long[] chunks;

        Watch(World world, long[] blocks, long colorSource, long[] chunks) {
            this.world = world;
            this.blocks = blocks;
            this.colorSource = colorSource;
            this.chunks = chunks;
        }
    }

}
