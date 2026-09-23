package com.beamformers.item;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.api.AEApi;
import appeng.api.parts.IPartItem;

import com.beamformers.part.PartSmallBeamFormer;

public class ItemSmallBeamFormer extends Item implements IPartItem<PartSmallBeamFormer> {

    @Nullable
    @Override
    public PartSmallBeamFormer createPartFromItemStack(ItemStack is) {
        return new PartSmallBeamFormer(is);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing side, float hitX, float hitY, float hitZ) {
        return AEApi.instance().partHelper().placeBus(player.getHeldItem(hand), pos, side, player, hand, world);
    }

}
