package net.qilla.shootergame.statsystem.damageindicator;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.qilla.shootergame.util.Randomizer;
import net.qilla.shootergame.ShooterGame;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftTextDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.text.NumberFormat;
import java.util.List;

public class Indicator {
    final Randomizer random = new Randomizer();

    private final Location location;
    private final IndicatorType type;
    private final long amount;

    public Indicator(final Location location, final IndicatorType type, final long amount) {
        this.location = location;
        this.type = type;
        this.amount = amount;
    }

    public void mainIndicator() {
        location.setX(randomizeCoord(location.getX()));
        location.setY(randomizeCoord(location.getY()));
        location.setZ(randomizeCoord(location.getZ()));

        final String formattedDamage = NumberFormat.getInstance().format(amount);

        Bukkit.getOnlinePlayers().forEach(player -> {
            if (location.getWorld() != player.getWorld()) return;
            if (location.distance(player.getLocation()) > 32) return;

            ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
            ServerLevel nmsWorld = ((CraftWorld) location.getWorld()).getHandle();
            CraftTextDisplay craftTextDisplay = new CraftTextDisplay(nmsWorld.getCraftServer(), EntityType.TEXT_DISPLAY.create(nmsWorld));

            if (type == IndicatorType.DAMAGE) craftTextDisplay.text(MiniMessage.miniMessage().deserialize("<red>" + formattedDamage + "</red>"));
            else craftTextDisplay.text(MiniMessage.miniMessage().deserialize("<green>" + formattedDamage + "</green>"));

            craftTextDisplay.setBackgroundColor(Color.fromARGB(0));
            craftTextDisplay.setBillboard(Display.Billboard.CENTER);
            craftTextDisplay.teleport(location);
            craftTextDisplay.setSeeThrough(true);
            Transformation transformation = new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(0, 0, 0, 1),
                    new Vector3f(0.35f, 0.35f, 0.35f),
                    new Quaternionf(0, 0, 0, 1));
            craftTextDisplay.setTransformation(transformation);
            craftTextDisplay.setViewRange(16);

            nmsPlayer.connection.sendPacket(new ClientboundAddEntityPacket(craftTextDisplay.getHandle(), craftTextDisplay.getHandle().getId()));
            List<SynchedEntityData.DataValue<?>> dataList = craftTextDisplay.getHandle().getEntityData().packAll();
            nmsPlayer.connection.sendPacket(new ClientboundSetEntityDataPacket(craftTextDisplay.getHandle().getId(), dataList));

            enlarge(craftTextDisplay, nmsPlayer, 1);
        });
    }

    private void enlarge(final CraftTextDisplay craftTextDisplay, final ServerPlayer nmsPlayer, final float indicatorSize) {
        new BukkitRunnable() {
            final float scale = 1.35f;
            @Override
            public void run() {
                final Vector3f newScale = craftTextDisplay.getTransformation().getScale().mul(scale);

                Transformation transformation = new Transformation(
                        craftTextDisplay.getTransformation().getTranslation(),
                        craftTextDisplay.getTransformation().getLeftRotation(),
                        newScale,
                        craftTextDisplay.getTransformation().getRightRotation());

                craftTextDisplay.setTransformation(transformation);

                List<SynchedEntityData.DataValue<?>> updatedSize = craftTextDisplay.getHandle().getEntityData().packDirty();
                nmsPlayer.connection.sendPacket(new ClientboundSetEntityDataPacket(craftTextDisplay.getHandle().getId(), updatedSize));
                if(craftTextDisplay.getTransformation().getScale().y >= indicatorSize) {
                    shrink(craftTextDisplay, nmsPlayer);
                    cancel();
                }
            }
        }.runTaskTimer(ShooterGame.getInstance(), 0, 1);
    }

    private void shrink(final CraftTextDisplay craftTextDisplay, final ServerPlayer nmsPlayer) {
        new BukkitRunnable() {
            final float scale = 0.65f;
            @Override
            public void run() {
                final Vector3f newScale = craftTextDisplay.getTransformation().getScale().mul(scale);

                Transformation transformation = new Transformation(
                        craftTextDisplay.getTransformation().getTranslation(),
                        craftTextDisplay.getTransformation().getLeftRotation(),
                        newScale,
                        craftTextDisplay.getTransformation().getRightRotation());
                craftTextDisplay.setTransformation(transformation);
                List<SynchedEntityData.DataValue<?>> updatedSize = craftTextDisplay.getHandle().getEntityData().packDirty();
                nmsPlayer.connection.sendPacket(new ClientboundSetEntityDataPacket(craftTextDisplay.getHandle().getId(), updatedSize));
                if(craftTextDisplay.getTransformation().getScale().y <= 0.25f) {
                    cancel();
                    nmsPlayer.connection.send(new ClientboundRemoveEntitiesPacket(craftTextDisplay.getHandle().getId()));
                }
            }
        }.runTaskTimer(ShooterGame.getInstance(), 30, 1);
    }

    private double randomizeCoord(final double coord) {
        return random.between(coord - 0.50f, coord + 0.50f);
    }

    public enum IndicatorType {
        HEAL, DAMAGE
    }
}