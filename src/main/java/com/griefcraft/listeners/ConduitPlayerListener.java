package com.griefcraft.listeners;

import com.griefcraft.cache.BlockCache;
import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Flag;
import com.griefcraft.model.Protection;
import net.socialhangover.conduit.events.HopperDrainEvent;
import net.socialhangover.conduit.events.HopperFillEvent;
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

public class ConduitPlayerListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onDrain(HopperDrainEvent event) {
        if (handleMoveItemEvent(event.getSource(), event.getHopperLocation(), event.getHopperLocation(), event.getSource())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFill(HopperFillEvent event) {
        if (handleMoveItemEvent(event.getHopperLocation(), event.getDestination(), event.getHopperLocation(), event.getDestination())) {
            event.setCancelled(true);
        }
    }

    private boolean handleMoveItemEvent(Inventory source, Inventory destination, Inventory initiator, Inventory inventory) {
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
        boolean protectHopper = protection.hasFlag(Flag.Type.HOPPER);
        boolean protectHopperIn = inventory == destination && protection.hasFlag(Flag.Type.HOPPERIN);
        boolean protectHopperOut = inventory == source && protection.hasFlag(Flag.Type.HOPPEROUT);

        // xor = (a && !b) || (!a && b)
        if (denyHoppers ^ (protectHopper || protectHopperIn || protectHopperOut)) {
            return true;
        }

        return false;
    }

}
