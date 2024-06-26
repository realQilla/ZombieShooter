package net.qilla.shootergame.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.qilla.shootergame.ShooterGame;
import net.qilla.shootergame.gunsystem.guncreation.guntype.GunBase;
import net.qilla.shootergame.gunsystem.guncreation.GunPDC;
import net.qilla.shootergame.permission.PermissionCommand;
import net.qilla.shootergame.util.ItemManagement;
import net.qilla.shootergame.gunsystem.guncreation.gunmod.GunID;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GiveGunCom implements CommandExecutor, TabExecutor {

    private final ShooterGame plugin;

    public GiveGunCom(ShooterGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>This command can only be executed by a player.</red>"));
            return true;
        }

        if(!player.hasPermission(PermissionCommand.getBlock)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You do not have permission to execute this command.</red>"));
            return true;
        }

        if(args.length != 2) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid arguments</red>"));
            return true;
        }

        String gunName = args[0];
        String gunTier = args[1];

        if(!plugin.getGunRegistry().getRegistry().containsKey(new GunID(gunName, gunTier))) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Selected gun does not exist.</red>"));
            return true;
        }

        GunBase gunType = plugin.getGunRegistry().getRegistry().get(new GunID(gunName, gunTier));

        ItemStack item = getItemStack(gunType);
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.25f, 2.0f);
        ItemManagement.giveItem(player, item);
        return true;
    }

    private static @NotNull ItemStack getItemStack(GunBase gunType) {
        ItemStack item = new ItemStack(gunType.getGunMaterial());
        item.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage().deserialize(gunType.getGunName()));
            List<String> lore = gunType.getGunLore();
            List<Component> loreComponents = new ArrayList<>();
            for(String loreLine: lore) {
                loreComponents.add(MiniMessage.miniMessage().deserialize(loreLine));
            }

            meta.lore(loreComponents);
            meta.getPersistentDataContainer().set(GunPDC.GUN_TYPE.getKey(), PersistentDataType.STRING, gunType.getTypeID().gunType());
            meta.getPersistentDataContainer().set(GunPDC.GUN_TIER.getKey(), PersistentDataType.STRING, String.valueOf(gunType.getTypeID().tier()));
            meta.getPersistentDataContainer().set(GunPDC.GUN_UUID.getKey(), PersistentDataType.STRING, String.valueOf(UUID.randomUUID()));
            meta.getPersistentDataContainer().set(GunPDC.GUN_CAPACITY.getKey(), PersistentDataType.INTEGER, gunType.getAmmunitionMod().gunCapacity());
            meta.getPersistentDataContainer().set(GunPDC.GUN_MAGAZINE.getKey(), PersistentDataType.INTEGER, gunType.getAmmunitionMod().gunMagazine());
            meta.getPersistentDataContainer().set(GunPDC.GUN_FIRE_MODE.getKey(), PersistentDataType.INTEGER, 0);
            meta.getPersistentDataContainer().set(GunPDC.GUN_RELOAD_STATUS.getKey(), PersistentDataType.BOOLEAN, false);
            meta.setAttributeModifiers(null);
        });
        return item;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!sender.hasPermission(PermissionCommand.getBlock)) return List.of();

        Map<String, List<String>> gunTypesAndTiers = new HashMap<>();
        for (Map.Entry<GunID, GunBase> entry : plugin.getGunRegistry().getRegistry().entrySet()) {
            String gunType = entry.getKey().gunType();
            String gunTier = entry.getKey().tier();

            gunTypesAndTiers.computeIfAbsent(gunType, k -> new ArrayList<>()).add(gunTier);
        }

        if (args.length == 1) {
            return new ArrayList<>(gunTypesAndTiers.keySet());
        } else if (args.length == 2 && gunTypesAndTiers.containsKey(args[0])) {
            return gunTypesAndTiers.get(args[0]);
        }
        return List.of();
    }
}
