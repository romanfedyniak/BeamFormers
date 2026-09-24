package com.beamformers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.registry.GameRegistry;

import appeng.block.AEBaseTileBlock;
import appeng.core.features.ActivityState;
import appeng.core.features.BlockStackSrc;
import appeng.tile.AEBaseTile;

import com.beamformers.block.BlockDenseBeamFormer;
import com.beamformers.tile.TileDenseBeamFormer;

public final class BeamFormersBlocks {

    public static final List<Block> BLOCKS = new ArrayList<>();

    public static final BlockDenseBeamFormer DENSE_BEAM_FORMER = add(new BlockDenseBeamFormer(), "dense_beam_former");

    private BeamFormersBlocks() {}

    private static <T extends Block> T add(T block, String name) {
        block.setRegistryName(BeamFormers.id(name));
        block.setTranslationKey(BeamFormers.MODID + "." + name);
        block.setCreativeTab(BeamFormersItems.TAB);
        BLOCKS.add(block);
        return block;
    }

    /** Called while blocks are registered. */
    public static void registerTiles() {
        registerTile(TileDenseBeamFormer.class, DENSE_BEAM_FORMER, "dense_beam_former");
    }

    private static void registerTile(Class<? extends TileEntity> tile, AEBaseTileBlock block, String name) {
        GameRegistry.registerTileEntity(tile, BeamFormers.id(name));
        AEBaseTile.registerTileItem(tile, new BlockStackSrc(block, 0, ActivityState.Enabled));
    }

}
