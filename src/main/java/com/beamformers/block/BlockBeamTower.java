package com.beamformers.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import appeng.api.implementations.items.IMemoryCard;

import com.beamformers.tile.TileBeamTower;

/**
 * Towers are linked with AE2's memory card: sneaking with it on a tower stores that tower, using it on another
 * links the two or unlinks them. The card's own settings copying is not used here.
 */
public class BlockBeamTower extends BlockMounted {

    public BlockBeamTower() {
        super(TileBeamTower.class);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        final ItemStack held = player.getHeldItem(hand);
        if (!(held.getItem() instanceof IMemoryCard)) {
            return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
        }
        final TileBeamTower tile = this.getTileEntity(world, pos);
        if (tile == null) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }
        final IMemoryCard card = (IMemoryCard) held.getItem();
        if (player.isSneaking()) {
            card.setMemoryCardContents(held, this.getTranslationKey(), tile.cardData());
            player.sendStatusMessage(new TextComponentTranslation("chat.beamformers.tower_stored"), true);
        } else if (this.getTranslationKey().equals(card.getSettingsName(held))) {
            player.sendStatusMessage(tile.toggleLink(card.getData(held)), true);
        } else {
            player.sendStatusMessage(new TextComponentTranslation("chat.beamformers.tower_no_card"), true);
        }
        return true;
    }

}
