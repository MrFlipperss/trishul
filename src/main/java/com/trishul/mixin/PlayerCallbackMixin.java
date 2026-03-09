package com.trishul.mixin;

import net.minecraft.entity.LivingEntity;
import com.trishul.api.TridentCallbackAccess;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerCallbackMixin {

    @Unique
    private static final int CALLBACK_COOLDOWN = 60;

    @Unique
    private int trishul$callbackCooldown = 0;

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void trishul$tickCooldown(CallbackInfo ci) {
        if (!((LivingEntity)(Object)this instanceof PlayerEntity)) return;
        if (this.trishul$callbackCooldown > 0) {
            this.trishul$callbackCooldown--;
        }
    }

    @Inject(method = "swingHand*", at = @At("HEAD"))
    private void trishul$onSwing(net.minecraft.util.Hand hand, CallbackInfo ci) {
        if (!((LivingEntity)(Object)this instanceof PlayerEntity self)) return;
        if (hand != net.minecraft.util.Hand.MAIN_HAND) return;
        if (!(self.getEntityWorld() instanceof ServerWorld serverWorld)) return;

        int riptideTicks = ((LivingEntityAccessor) this).trishul$getRiptideTicks();
        if (riptideTicks > 0) return;

        if (!self.getMainHandStack().isEmpty() &&
                !self.getMainHandStack().isOf(Items.TRIDENT)) return;

        if (this.trishul$callbackCooldown > 0) return;

        Box searchBox = self.getBoundingBox().expand(128);
        java.util.List<TridentEntity> tridents = serverWorld.getEntitiesByClass(
                TridentEntity.class,
                searchBox,
                t -> t.getOwner() == self
        );

        if (tridents.isEmpty()) return;

        TridentEntity trident = tridents.getFirst();
        TridentCallbackAccess callbackMixin = (TridentCallbackAccess) trident;

        if (trident.isNoClip()) return;

        callbackMixin.trishul$freeze();
        ((TridentAccessor) trident).trishul$setDealtDamage(true);
        callbackMixin.trishul$unfreeze();
        this.trishul$callbackCooldown = CALLBACK_COOLDOWN;
    }
}