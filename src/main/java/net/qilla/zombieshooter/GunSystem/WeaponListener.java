package net.qilla.zombieshooter.GunSystem;

import net.qilla.zombieshooter.GunSystem.GunCreation.GunData;
import net.qilla.zombieshooter.GunSystem.GunUtils.CheckValid;
import net.qilla.zombieshooter.GunSystem.GunSkeleton.GunChangeMode;
import net.qilla.zombieshooter.GunSystem.GunSkeleton.GunDisplay;
import net.qilla.zombieshooter.GunSystem.GunSkeleton.GunFire;
import net.qilla.zombieshooter.GunSystem.GunSkeleton.GunReload;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class WeaponListener implements Listener {


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (event.getHand() != EquipmentSlot.HAND || !CheckValid.isValidBoth(item)) return;

        event.setCancelled(true);

        if (event.getAction().isRightClick()) {
            new GunFire().fireMain(player, item);
        }

        if (event.getAction().isLeftClick()) {
            new GunChangeMode().modeMain(player, item);
        }
    }

    @EventHandler
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack offhandItem = event.getOffHandItem();

        if (!CheckValid.isValidBoth(offhandItem)) return;

        event.setCancelled(true);
        new GunReload().reloadMain(player);
    }

    @EventHandler
    public void changeWeapon(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack curItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack prevItem = player.getInventory().getItem(event.getNewSlot());

        GunReload.getInstance().cancelReload();
        if(CheckValid.isValidBoth(prevItem)) {
            if (prevItem.getItemMeta().getPersistentDataContainer().get(GunData.GUN_RELOAD_STATUS.getKey(), PersistentDataType.BOOLEAN)) {
                prevItem.editMeta(ItemMeta.class, meta -> {
                    meta.getPersistentDataContainer().set(GunData.GUN_RELOAD_STATUS.getKey(), PersistentDataType.BOOLEAN, false);
                    prevItem.setItemMeta(meta);
                });
            }
        }

        GunDisplay.getDisplayMap(player).updateGunFields(curItem, true);
    }

    @EventHandler
    public void clickInventory(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();

        if (CheckValid.isValidBoth(clickedItem)) {
            if (clickedItem.getItemMeta().getPersistentDataContainer().get(GunData.GUN_RELOAD_STATUS.getKey(), PersistentDataType.BOOLEAN)) {
                clickedItem.editMeta(ItemMeta.class, meta -> {
                    meta.getPersistentDataContainer().set(GunData.GUN_RELOAD_STATUS.getKey(), PersistentDataType.BOOLEAN, false);
                    clickedItem.setItemMeta(meta);
                });
            }
        }
    }

    @EventHandler
    public void playerDamageWeapon(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(GunData.GUN_TYPE.getKey())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        new GunDisplay(event.getPlayer()).display();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        GunDisplay.getDisplayMap(event.getPlayer()).remove();
    }
}
