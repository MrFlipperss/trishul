package com.trishul.event;

import com.trishul.Trishul;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;

import java.util.*;

public class LeapAbilityHandler {

    private enum LeapState { RISING, DASHING }

    private static class LeapData {
        LeapState state = LeapState.RISING;
        UUID targetId;
        int leapLevel;
        int safetyTicks = 0;
        int risingTicks = 0;
        double lastY = -1;
        boolean dashApplied = false; // NEW
    }

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Map<UUID, LeapData> activeLeaps = new HashMap<>();
    private static final long COOLDOWN_MS = 5000;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(LeapAbilityHandler::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (activeLeaps.isEmpty()) return;

        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, LeapData> entry : new HashMap<>(activeLeaps).entrySet()) {
            UUID playerId = entry.getKey();
            LeapData data = entry.getValue();

            // Get player directly from player manager — works in singleplayer
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                toRemove.add(playerId);
                continue;
            }

            ServerWorld world = (ServerWorld) player.getEntityWorld();

            data.safetyTicks++;
            if (data.safetyTicks > 80) {
                stopPlayer(player);
                toRemove.add(playerId);
                continue;
            }

            // Find target in player's current world
            Entity targetEntity = world.getEntity(data.targetId);
            if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
                stopPlayer(player);
                toRemove.add(playerId);
                continue;
            }

            if (data.state == LeapState.RISING) {
                data.risingTicks++;

                double currentY = player.getY();

                if (data.lastY >= 0 && currentY <= data.lastY) {
                    // Player stopped rising — switch to dash
                    data.state = LeapState.DASHING;
                    player.sendMessage(Text.literal("§aSWITCHING TO DASH"), true);
                }

                data.lastY = currentY;

                // Lock horizontal while rising
                Vec3d vel = player.getVelocity();
                player.setVelocity(0, vel.y, 0);
                player.velocityDirty = true;
                player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

                // Safety fallback — switch after 20 ticks regardless
                if (data.risingTicks >= 20) {
                    data.state = LeapState.DASHING;
                }
            } else if (data.state == LeapState.DASHING) {
                // Only apply velocity ONCE when entering dash
                if (!data.dashApplied) {
                    double dx = target.getX() - player.getX();
                    double dy = (target.getY() + target.getHeight() * 0.33) - player.getY();
                    double dz = target.getZ() - player.getZ();
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    double speed = (0.8 + (data.leapLevel * 0.1)) * 1.2;

                    player.setVelocity(
                            (dx / len) * speed,
                            (dy / len) * speed,
                            (dz / len) * speed
                    );
                    player.velocityDirty = true;
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                    data.dashApplied = true;
                }

                // Check each tick if we've reached the target
                double dist = player.distanceTo(target);
                if (dist <= 1.5) {
                    stopPlayer(player);
                    applyHit(player, target, world, player.getMainHandStack());
                    toRemove.add(playerId);
                }
            }
        }

        toRemove.forEach(activeLeaps::remove);
    }

    // Stops player like Riptide landing
    private static void stopPlayer(ServerPlayerEntity player) {
        player.setVelocity(0, 0, 0);
        player.velocityDirty = true;
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
    }

    // Called from mixin — single click, immediate trigger
    public static boolean onLeapAttack(PlayerEntity player) {
        World world = player.getEntityWorld();
        if (world.isClient()) return false;

        ItemStack heldItem = player.getMainHandStack();
        if (!heldItem.isOf(Items.TRIDENT)) return false;

        int leapLevel = getLeapLevel(heldItem);
        if (leapLevel <= 0) return false;

        // Always cancel default melee if holding Leap trident
        // (return true = cancel normal attack in mixin)
        UUID playerId = player.getUuid();

        // Don't re-trigger if already leaping
        if (activeLeaps.containsKey(playerId)) return true;

        // Cooldown check
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(playerId)) {
            long elapsed = now - cooldowns.get(playerId);
            if (elapsed < COOLDOWN_MS) {
                long remaining = (COOLDOWN_MS - elapsed) / 1000 + 1;
                if (player instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(
                            Text.literal("§cLeap on cooldown! " + remaining + "s remaining."),
                            true
                    );
                }
                return true; // still cancel default attack
            }
        }

        // Find target in crosshair within range
        double range = 6.0 + leapLevel; // 7/8/9 blocks
        LivingEntity target = findCrosshairTarget(player, range);
        if (target == null) return true; // cancel melee but no leap

        // === Phase 1: Pop upward immediately ===
        player.setVelocity(0, 0.566, 0); // straight up, ~2 blocks
        player.velocityDirty = true;

        if (player instanceof ServerPlayerEntity sp) {
            sp.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        }

        // Sound
        world.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDER_DRAGON_FLAP,
                SoundCategory.PLAYERS, 1.0f, 1.5f);

        // Particles
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY(), player.getZ(),
                    20, 0.3, 0.3, 0.3, 0.1);
        }

        // Register leap
        LeapData data = new LeapData();
        data.targetId = target.getUuid();
        data.leapLevel = leapLevel;
        activeLeaps.put(playerId, data);

        cooldowns.put(playerId, now);

        if (player instanceof ServerPlayerEntity sp) {
            sp.sendMessage(Text.literal("§bLeap!"), true);
        }

        return true; // cancel default melee
    }

    private static void applyHit(ServerPlayerEntity player,
                                 LivingEntity target, ServerWorld world,
                                 ItemStack trident) {
        boolean blocking = false;
        if (target instanceof PlayerEntity tp && tp.isBlocking()) {
            // Check if attacker is in front of the defender
            // Get defender's look direction
            Vec3d defenderLook = tp.getRotationVec(1.0f);
            // Get direction from defender to attacker
            Vec3d defenderToAttacker = new Vec3d(
                    player.getX() - tp.getX(),
                    player.getY() - tp.getY(),
                    player.getZ() - tp.getZ()
            ).normalize();

            // Dot product > 0 means attacker is in front of defender
            // Shield covers ~180 degree arc in front
            double dot = defenderLook.dotProduct(defenderToAttacker);
            blocking = dot > 0.0;
        }

        // Calculate sharpness bonus (each level adds 0.5 damage, same as vanilla)
        float sharpnessBonus = 0.0f;
        ItemEnchantmentsComponent enchants = trident.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );
        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
            if (entry.getKey().isPresent() &&
                    entry.getKey().get().getValue()
                            .equals(Identifier.of("minecraft", "sharpness"))) {
                int sharpLevel = enchants.getLevel(entry);
                // Vanilla formula: 0.5 * level + 0.5 (for level >= 1)
                sharpnessBonus = 0.5f * sharpLevel + 0.5f;
                break;
            }
        }

        float damage;

        if (blocking) {
            // Weak hit through shield — base 3.0 + sharpness
            damage = 3.0f + sharpnessBonus;
            target.damage(world, world.getDamageSources().magic(), damage);
        } else {
            // Critical hit — base 13.5 + sharpness
            damage = 13.5f + sharpnessBonus;
            target.damage(world, world.getDamageSources().playerAttack(player), damage);
            world.spawnParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    15, 0.3, 0.3, 0.3, 0.2);
            world.playSound(null,
                    target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                    SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        player.sendMessage(Text.literal(
                blocking
                        ? "§eWeak Hit! (shield) §f" + damage + " dmg"
                        : "§cCritical Hit! §f" + damage + " dmg"
        ), true);
    }

    private static LivingEntity findCrosshairTarget(PlayerEntity player, double range) {
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1.0f);
        Vec3d end = eyePos.add(lookVec.multiply(range));

        Box searchBox = new Box(
                Math.min(eyePos.x, end.x) - 1, Math.min(eyePos.y, end.y) - 1,
                Math.min(eyePos.z, end.z) - 1,
                Math.max(eyePos.x, end.x) + 1, Math.max(eyePos.y, end.y) + 1,
                Math.max(eyePos.z, end.z) + 1
        );

        List<Entity> candidates = player.getEntityWorld().getOtherEntities(
                player, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator()
        );

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : candidates) {
            double dist = player.distanceTo(e);
            if (dist > range) continue;

            Vec3d toTarget = new Vec3d(
                    e.getX() - eyePos.x,
                    (e.getY() + e.getHeight() / 2) - eyePos.y,
                    e.getZ() - eyePos.z
            ).normalize();

            double dot = toTarget.dotProduct(lookVec);
            if (dot > 0.90) { // ~18 degrees — feels natural
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = (LivingEntity) e;
                }
            }
        }

        return closest;
    }

    private static int getLeapLevel(ItemStack stack) {
        ItemEnchantmentsComponent enchants = stack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );
        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
            if (entry.getKey().isPresent() &&
                    entry.getKey().get().getValue().equals(Trishul.LEAP_ID)) {
                return enchants.getLevel(entry);
            }
        }
        return 0;
    }
}