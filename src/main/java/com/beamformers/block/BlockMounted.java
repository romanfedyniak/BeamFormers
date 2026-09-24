package com.beamformers.block;

import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.AEBaseTileBlock;
import appeng.util.Platform;

import com.beamformers.client.GlowLayer;
import com.beamformers.tile.TileMounted;

/** A block for a {@link TileMounted}; sneaking with an empty hand hides or shows its beams. */
public class BlockMounted extends AEBaseTileBlock {

    public BlockMounted(Class<? extends TileMounted> tile) {
        super(Material.IRON);
        this.setTileEntity(tile);
        this.setLightOpacity(0);
        this.setFullSize(false);
        this.setOpaque(false);
        this.fullBlock = false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT || layer == GlowLayer.get();
    }

    @Override
    public boolean onActivated(World world, BlockPos pos, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem,
            EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking() || heldItem != null && !heldItem.isEmpty()) {
            return false;
        }
        final TileMounted tile = this.getTileEntity(world, pos);
        if (tile == null) {
            return false;
        }
        if (Platform.isServer()) {
            player.sendStatusMessage(new TextComponentTranslation(
                    tile.toggleHidden() ? "chat.beamformers.beam_hidden" : "chat.beamformers.beam_shown"), true);
        }
        return true;
    }

}
