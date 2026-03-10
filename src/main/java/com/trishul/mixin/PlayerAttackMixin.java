package com.trishul.mixin;

import com.trishul.event.LeapAbilityHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class PlayerAttackMixin {

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At("HEAD"))
    private void trishul_onSwing(Hand hand, CallbackInfo ci) {
        if (hand != Hand.MAIN_HAND) return;
        if (!((Object)this instanceof PlayerEntity player)) return;
        LeapAbilityHandler.onLeapAttack(player);
    }
}