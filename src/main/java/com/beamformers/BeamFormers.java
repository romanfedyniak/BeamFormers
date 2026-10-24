package com.beamformers;

import org.apache.logging.log4j.Logger;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.beamformers.proxy.CommonProxy;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, useMetadata = true,
        dependencies = BeamFormers.DEPENDENCIES)
public class BeamFormers {

    static final String DEPENDENCIES = "required-after:appliedenergistics2@[1.6.0,);after:theoneprobe";

    public static final String MODID = Tags.MOD_ID;

    @SidedProxy(clientSide = "com.beamformers.proxy.ClientProxy", serverSide = "com.beamformers.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static Logger log;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        proxy.preInit(event);
        if (Loader.isModLoaded("theoneprobe")) {
            FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe",
                    "com.beamformers.integration.TheOneProbeModule");
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

}
