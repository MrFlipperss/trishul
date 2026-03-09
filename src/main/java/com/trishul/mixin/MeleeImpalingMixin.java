package com.trishul.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EntityTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntity.class)
public abstract class MeleeImpalingMixin {

    @ModifyVariable(
            method = "attack",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;getDamageAgainst(Lnet/minecraft/entity/Entity;FLnet/minecraft/entity/damage/DamageSource;)F"
            ),
            ordinal = 0
    )
    private float trishul$meleeBedRockImpaling(float damage, Entity target) {
        PlayerEntity self = (PlayerEntity)(Object)this;

        // Only when holding a trident
        if (!self.getMainHandStack().isOf(Items.TRIDENT)) return damage;

        if (!(target instanceof LivingEntity livingTarget)) return damage;

        boolean isAquatic = livingTarget.getType().isIn(EntityTypeTags.AQUATIC);
        boolean isWet = livingTarget.isTouchingWater() || livingTarget.isSubmergedInWater();

        if (isAquatic || !isWet) return damage;

        ItemStack stack = self.getMainHandStack();
        ItemEnchantmentsComponent enchantments = stack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );

        int impalingLevel = 0;
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(Enchantments.IMPALING)) {
                impalingLevel = enchantments.getLevel(entry);
                break;
            }
        }

        if (impalingLevel <= 0) return damage;

        damage += impalingLevel * 1.5f;
        return damage;
    }
}