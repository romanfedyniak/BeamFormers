package com.beamformers;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = BeamFormers.MODID)
@Mod.EventBusSubscriber(modid = BeamFormers.MODID)
public final class BeamFormersConfig {

    @Config.Comment("How many blocks apart two beam formers can be and still reach each other.")
    @Config.RangeInt(min = 1, max = 256)
    public static int formerRange = 32;

    @Config.Comment("How many blocks apart two beam towers can be and still be linked, measured centre to centre.")
    @Config.RangeInt(min = 1, max = 256)
    public static int towerRange = 32;

    @Config.Comment("How many other towers one beam tower can be linked to.")
    @Config.RangeInt(min = 1, max = 64)
    public static int towerMaxLinks = 8;

    private BeamFormersConfig() {}

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(BeamFormers.MODID)) {
            ConfigManager.sync(BeamFormers.MODID, Config.Type.INSTANCE);
        }
    }

}
