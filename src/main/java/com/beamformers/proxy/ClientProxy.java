package com.beamformers.proxy;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import appeng.client.render.model.AutoRotatingModel;

import com.beamformers.BeamFormersBlocks;
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

    /** A block model is drawn turned the way its tile faces. */
    @SubscribeEvent
    public void onBakeModels(ModelBakeEvent event) {
        for (Block block : BeamFormersBlocks.BLOCKS) {
            final ModelResourceLocation location = new ModelResourceLocation(block.getRegistryName(), "normal");
            final IBakedModel model = event.getModelRegistry().getObject(location);
            if (model != null) {
                event.getModelRegistry().putObject(location, new AutoRotatingModel(model));
            }
        }
    }

}
