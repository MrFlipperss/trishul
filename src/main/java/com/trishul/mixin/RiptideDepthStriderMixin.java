package com.trishul.mixin;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class RiptideDepthStriderMixin {

    @Unique
    private static final Identifier RIPTIDE_SUPPRESS_ID =
            Identifier.of("trishul", "riptide_suppress_movement_efficiency");

    @Inject(method = "travel", at = @At("HEAD"))
    private void trishul$suppressDepthStrider(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        int riptideTicks = ((LivingEntityAccessor) this).trishul$getRiptideTicks();

        var attribute = self.getAttributeInstance(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
        if (attribute == null) return;

        attribute.removeModifier(RIPTIDE_SUPPRESS_ID);

        if (riptideTicks <= 0) return;

        // Suppress depth strider during riptide
        attribute.addTemporaryModifier(new EntityAttributeModifier(
                RIPTIDE_SUPPRESS_ID,
                -1.0,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void trishul$restoreDepthStrider(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        var attribute = self.getAttributeInstance(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
        if (attribute == null) return;
        attribute.removeModifier(RIPTIDE_SUPPRESS_ID);
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void trishul$cancelRiptideDrag(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        int riptideTicks = ((LivingEntityAccessor) this).trishul$getRiptideTicks();
        if (riptideTicks <= 0) return;
        if (!self.isTouchingWater()) return;
        if (self.isSwimming()) return;

        Vec3d velocity = self.getVelocity();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        if (horizontalSpeed < 0.3) return;

        double maxSpeed = 1.2;
        if (horizontalSpeed < maxSpeed) {
            self.setVelocity(
                    velocity.x / 0.8,
                    velocity.y,
                    velocity.z / 0.8
            );
        }
    }
}