package com.beamformers.proxy;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.beamformers.BeamFormersItems;
import com.beamformers.client.BeamRenderer;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(BeamRenderer.INSTANCE);
    }

    /** Every item's model is named after the item. */
    @SubscribeEvent
    public void onRegisterModels(ModelRegistryEvent event) {
        BeamFormersItems.ITEMS.forEach(item -> ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory")));
    }

}
