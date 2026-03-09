package com.trishul.mixin;
/*Adds Bedrock-Style Impaling & Knockback to thrown Tridents*/
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentEntity.class)
public abstract class ImpalingEnchantmentMixin {

    @Definition(id = "getDamage", method = "Lnet/minecraft/enchantment/EnchantmentHelper;getDamage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;F)F")
    @Expression("? = getDamage(?, ?, ?, ?, ?)")
    @ModifyVariable(
            method = "onEntityHit",
            at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER),
            ordinal = 0
    )
    private float trishul$bedrockImpaling(float damage, EntityHitResult hit) {
        if (!(hit.getEntity() instanceof LivingEntity target))
            return damage;

        ItemStack stack = ((PersistentProjectileAccessor) this).trishul$getStack();
        if (stack == null) return damage;

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

        boolean isAquatic = target.getType().isIn(EntityTypeTags.AQUATIC);
        boolean isWet = target.isTouchingWater() || target.isSubmergedInWater();

        if (!isAquatic && isWet) {
            damage += impalingLevel * 1.5f;
        }

        return damage;
    }

    @Inject(
            method = "onEntityHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;sidedDamage(Lnet/minecraft/entity/damage/DamageSource;F)Z"
            )
    )
    private void trishul$applyThrownKnockback(EntityHitResult entityHitResult, CallbackInfo ci) {
        if (!(entityHitResult.getEntity() instanceof LivingEntity target)) return;

        ItemStack stack = ((PersistentProjectileAccessor) this).trishul$getStack();
        if (stack == null) return;

        ItemEnchantmentsComponent enchantments = stack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );

        int knockbackLevel = 0;
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(Enchantments.KNOCKBACK)) {
                knockbackLevel = enchantments.getLevel(entry);
                break;
            }
        }

        if (knockbackLevel > 0) {
            Vec3d velocity = ((TridentEntity)(Object)this).getVelocity();
            target.takeKnockback(
                    knockbackLevel * 0.5,
                    -velocity.x,
                    -velocity.z
            );
        }
    }
}