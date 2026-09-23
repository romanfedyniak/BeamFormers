package com.beamformers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.beamformers.item.ItemSmallBeamFormer;

public final class BeamFormersItems {

    public static final List<Item> ITEMS = new ArrayList<>();

    public static final CreativeTabs TAB = new CreativeTabs(BeamFormers.MODID) {

        @Override
        public ItemStack createIcon() {
            return new ItemStack(SMALL_BEAM_FORMER);
        }
    };

    public static final Item SMALL_BEAM_FORMER = register(new ItemSmallBeamFormer(), "small_beam_former");

    private BeamFormersItems() {}

    private static Item register(Item item, String name) {
        item.setRegistryName(BeamFormers.id(name));
        item.setTranslationKey(BeamFormers.MODID + "." + name);
        item.setCreativeTab(TAB);
        ITEMS.add(item);
        return item;
    }

}
