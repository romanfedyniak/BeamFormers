package com.beamformers.former;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.visualiser.INetworkVisualiserProvider;
import appeng.api.networking.visualiser.IVisualiserSink;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.DimensionalCoord;

/**
 * Draws a beam in AE2's network visualiser. A part's node has no place in the world of its own, so the
 * visualiser leaves out whatever joins a part; such a beam is hung off the cable its former sits on instead.
 */
public final class BeamVisualiser implements INetworkVisualiserProvider {

    @Override
    public void contribute(IGridNode node, IVisualiserSink sink) {
        final DimensionalCoord location = node.getGridBlock().getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }
        final TileEntity te = location.getWorld().getTileEntity(location.getPos());
        if (te instanceof IBeamFormer) {
            link(((IBeamFormer) te).getBeamEnd(), sink);
        } else if (te instanceof IPartHost) {
            final IPartHost host = (IPartHost) te;
            for (EnumFacing side : EnumFacing.values()) {
                final IPart part = host.getPart(side);
                if (part instanceof IBeamFormer) {
                    link(((IBeamFormer) part).getBeamEnd(), sink);
                }
            }
        }
    }

    /** Two blocks are joined node to node, which the visualiser draws already. */
    private static void link(BeamEnd end, IVisualiserSink sink) {
        final BeamEnd partner = end.getPartner();
        final IGridConnection connection = end.getConnection();
        if (!end.isOwner() || partner == null || connection == null
                || end.getFormer() instanceof TileEntity && partner.getFormer() instanceof TileEntity) {
            return;
        }
        sink.link(partner.getFormer().getFormerPos(), connection.getUsedChannels(), end.getCapacity(), null);
    }

}
