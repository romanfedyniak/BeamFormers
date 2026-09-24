package com.beamformers.integration;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Function;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.pathing.ChannelTiers;
import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.core.AEConfig;
import appeng.core.features.AEFeature;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;

import com.beamformers.BeamFormers;
import com.beamformers.BeamFormersConfig;
import com.beamformers.former.BeamEnd;
import com.beamformers.former.IBeamFormer;
import com.beamformers.tile.TileBeamTower;

/**
 * Handed to The One Probe by name through IMC. The text goes out as translation keys, so each player reads it in
 * their own language rather than the server's.
 */
public final class TheOneProbeModule implements Function<ITheOneProbe, Void>, IProbeInfoProvider {

    @Nullable
    @Override
    public Void apply(@Nullable ITheOneProbe probe) {
        if (probe != null) {
            probe.registerProvider(this);
        }
        return null;
    }

    @Override
    public String getID() {
        return BeamFormers.MODID + ":beams";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo info, EntityPlayer player, World world, IBlockState state,
            IProbeHitData data) {
        final TileEntity te = world.getTileEntity(data.getPos());
        if (te instanceof TileBeamTower) {
            addTower(mode, info, world, (TileBeamTower) te);
        } else if (te instanceof IBeamFormer) {
            addFormer(info, world, ((IBeamFormer) te).getBeamEnd());
        } else if (te instanceof IPartHost && data.getHitVec() != null) {
            final SelectedPart selected = ((IPartHost) te).selectPartGlobal(data.getHitVec());
            if (selected.part instanceof IBeamFormer) {
                addFormer(info, world, ((IBeamFormer) selected.part).getBeamEnd());
            }
        }
    }

    private static void addFormer(IProbeInfo info, World world, BeamEnd end) {
        final BeamEnd partner = end.getPartner();
        final IGridConnection connection = end.getConnection();
        if (partner != null) {
            final BlockPos from = end.getFormer().getFormerPos();
            final BlockPos to = partner.getFormer().getFormerPos();
            info.text(TextStyleClass.INFO + key("top.beamformers.partner") + " " + TextStyleClass.OK + xyz(to)
                    + TextStyleClass.INFO + " (" + (int) Math.round(Math.sqrt(from.distanceSq(to))) + " "
                    + key("top.beamformers.blocks") + ")");
            if (connection != null) {
                channels(info, connection.getUsedChannels(), end.getCapacity());
            }
        } else if (end.getBlocker() != null) {
            info.text(TextStyleClass.WARNING + key("top.beamformers.blocked") + " " + blocker(world, end.getBlocker()));
        } else {
            info.text(TextStyleClass.WARNING + key("top.beamformers.no_partner"));
        }
        if (end.isHidden()) {
            info.text(TextStyleClass.INFO + key("top.beamformers.hidden"));
        }
    }

    private static void addTower(ProbeMode mode, IProbeInfo info, World world, TileBeamTower tower) {
        final List<TileBeamTower.LinkState> links = tower.getLinkStates();
        final int max = BeamFormersConfig.towerMaxLinks;
        final long active = links.stream().filter(link -> link.active).count();
        info.text(TextStyleClass.INFO + key("top.beamformers.links") + " " + TextStyleClass.OK + links.size()
                + TextStyleClass.INFO + " / " + max + ", " + key("top.beamformers.active") + " " + TextStyleClass.OK
                + active);
        if (mode == ProbeMode.EXTENDED) {
            for (TileBeamTower.LinkState link : links) {
                if (link.active) {
                    info.text(TextStyleClass.OK + xyz(link.target) + TextStyleClass.INFO + ": "
                            + (channelsShown() ? link.usedChannels + " / " + ChannelTiers.capacityOf(ChannelTiers.DENSE) + " "
                                    + key("top.beamformers.channels_word") : key("top.beamformers.active")));
                } else if (link.blocker != null) {
                    info.text(TextStyleClass.WARNING + xyz(link.target) + ": " + key("top.beamformers.blocked") + " "
                            + blocker(world, link.blocker));
                } else {
                    info.text(TextStyleClass.WARNING + xyz(link.target) + ": " + key("top.beamformers.waiting"));
                }
            }
        }
        if (tower.isHidden()) {
            info.text(TextStyleClass.INFO + key("top.beamformers.hidden"));
        }
    }

    private static boolean channelsShown() {
        return AEConfig.instance().isFeatureEnabled(AEFeature.CHANNELS);
    }

    private static void channels(IProbeInfo info, int used, int capacity) {
        if (channelsShown()) {
            info.text(TextStyleClass.INFO + key("top.beamformers.channels") + " " + TextStyleClass.OK + used
                    + (capacity < 0 ? "" : TextStyleClass.INFO + " / " + TextStyleClass.OK + capacity));
        }
    }

    // Names the block without loading its chunk.
    private static String blocker(World world, BlockPos pos) {
        final String name = world.isBlockLoaded(pos)
                ? key(world.getBlockState(pos).getBlock().getTranslationKey() + ".name")
                : key("top.beamformers.unloaded");
        return name + " " + xyz(pos);
    }

    private static String xyz(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static String key(String key) {
        return IProbeInfo.STARTLOC + key + IProbeInfo.ENDLOC;
    }

}
