package com.trishul.mixin;
/*Extra Movement Speed in Water*/
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void trishul$tridentWaterGlide(CallbackInfo ci) {

        LivingEntity entity = (LivingEntity)(Object)this;

        if (!(entity instanceof PlayerEntity player)) return;
        if (!player.isTouchingWater()) return;
        if (!player.getMainHandStack().isOf(Items.TRIDENT) && !player.getOffHandStack().isOf(Items.TRIDENT)) return;

        Vec3d velocity = player.getVelocity();

        player.setVelocity(
                velocity.x * 1.08,
                velocity.y * 1.02,
                velocity.z * 1.08
        );
    }
}