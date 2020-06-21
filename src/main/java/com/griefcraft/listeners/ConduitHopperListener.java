package com.griefcraft.listeners;

import com.griefcraft.cache.BlockCache;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Flag;
import com.griefcraft.model.Protection;
import net.socialhangover.conduit.event.HopperDrainEvent;
import net.socialhangover.conduit.event.HopperFillEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Hopper;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ConduitHopperListener implements Listener {

    private final LWCPlugin plugin;

    public ConduitHopperListener(LWCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrain(HopperDrainEvent event) {
        if (plugin.getLWC().useAlternativeHopperProtection() &&
                !(event.getHopperInventory().getHolder() instanceof HopperMinecart)) {
            return;
        }
        if (handleMoveItemEvent(event.getHopperInventory(), event.getSourceInventory(), Flag.Type.HOPPEROUT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFill(HopperFillEvent event) {
        if (plugin.getLWC().useAlternativeHopperProtection() &&
                !(event.getHopperInventory().getHolder() instanceof HopperMinecart)) {
            return;
        }
        if (handleMoveItemEvent(event.getHopperInventory(), event.getDestinationInventory(), Flag.Type.HOPPERIN)) {
            event.setCancelled(true);
        }
    }

    private boolean handleMoveItemEvent(Inventory initiator, Inventory inventory, Flag.Type flag) {
        LWC lwc = LWC.getInstance();

        if (inventory == null) {
            return false;
        }

        Location location;
        InventoryHolder holder;
        Location hopperLocation = null;
        InventoryHolder hopperHolder;

        try {
            holder = inventory.getHolder();
            hopperHolder = initiator.getHolder();
        } catch (AbstractMethodError e) {
            return false;
        }

        try {
            if (holder instanceof BlockState) {
                location = ((BlockState) holder).getLocation();
            } else if (holder instanceof DoubleChest) {
                location = ((DoubleChest) holder).getLocation();
            } else {
                return false;
            }

            if (hopperHolder instanceof Hopper) {
                hopperLocation = ((Hopper) hopperHolder).getLocation();
            } else if (hopperHolder instanceof HopperMinecart) {
                hopperLocation = ((HopperMinecart) hopperHolder).getLocation();
            }
        } catch (Exception e) {
            return false;
        }

        // High-intensity zone: increase protection cache if it's full,
        // otherwise
        // the database will be getting rammed
        lwc.getProtectionCache().increaseIfNecessary();

        // Attempt to load the protection at that location
        Protection protection = lwc.findProtection(location);

        // If no protection was found we can safely ignore it
        if (protection == null) {
            return false;
        }

        if (hopperLocation != null
                && Boolean.parseBoolean(lwc.resolveProtectionConfiguration(Material.HOPPER, "enabled"))) {
            Protection hopperProtection = lwc.findProtection(hopperLocation);

            if (hopperProtection != null) {
                // if they're owned by the same person then we can allow the
                // move
                if (protection.getOwner().equals(hopperProtection.getOwner())) {
                    return false;
                }
            }
        }

        BlockCache blockCache = BlockCache.getInstance();
        boolean denyHoppers = Boolean.parseBoolean(
                lwc.resolveProtectionConfiguration(blockCache.getBlockType(protection.getBlockId()), "denyHoppers"));

        return denyHoppers ^ (protection.hasFlag(Flag.Type.HOPPER) || protection.hasFlag(flag));
    }

}
