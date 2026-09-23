package com.beamformers.proxy;

import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import appeng.api.AEApi;
import appeng.api.networking.visualiser.NetworkVisualisers;

import com.beamformers.BeamFormersItems;
import com.beamformers.former.BeamVisualiser;
import com.beamformers.former.PathWatcher;
import com.beamformers.part.PartSmallBeamFormer;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PathWatcher.INSTANCE);
        AEApi.instance().registries().partModels().registerModels(PartSmallBeamFormer.MODEL.getModels());
        NetworkVisualisers.register(new BeamVisualiser());
    }

    @SubscribeEvent
    public void onRegisterItems(RegistryEvent.Register<Item> event) {
        BeamFormersItems.ITEMS.forEach(event.getRegistry()::register);
    }

}
