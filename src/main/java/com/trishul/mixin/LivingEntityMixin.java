package com.trishul.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private static final Identifier TRIDENT_GLIDE_ID =
            Identifier.of("trishul", "trident_water_glide");

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void trishul$tridentWaterGlide(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;

        if (!(entity instanceof PlayerEntity player)) return;

        var attribute = player.getAttributeInstance(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
        if (attribute == null) return;

        // Always remove first to reset cleanly each tick
        attribute.removeModifier(TRIDENT_GLIDE_ID);

        if (!player.isTouchingWater()) return;

        // Skip during riptide
        int riptideTicks = ((LivingEntityAccessor) this).trishul$getRiptideTicks();
        if (riptideTicks > 0) return;

        // Check either hand for a trident
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        ItemStack tridentStack = null;

        if (mainHand.isOf(Items.TRIDENT)) tridentStack = mainHand;
        else if (offHand.isOf(Items.TRIDENT)) tridentStack = offHand;

        if (tridentStack == null) return;

        // Read riptide level
        ItemEnchantmentsComponent enchantments = tridentStack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );

        int riptideLevel = 0;
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(Enchantments.RIPTIDE)) {
                riptideLevel = enchantments.getLevel(entry);
                break;
            }
        }

        // Sprinting already multiplies speed, so needs a smaller boost
        // Walking needs a larger boost to feel meaningful
        // Riptide level scales both up
        double bonus;
        if (player.isSprinting()) {
            // Sprint: +0.6 base, +0.15 per riptide level, cap 1.0
            bonus = Math.min(0.6 + (riptideLevel * 0.15), 1.0);
        } else {
            // Walk: +1.5 base, +0.3 per riptide level, cap 2.4
            bonus = Math.min(1.5 + (riptideLevel * 0.3), 2.4);
        }

        attribute.addTemporaryModifier(new EntityAttributeModifier(
                TRIDENT_GLIDE_ID,
                bonus,
                EntityAttributeModifier.Operation.ADD_VALUE
        ));
    }
}