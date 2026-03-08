package com.trishul.mixin;
/*Remove Depth Strider effect when rip tiding*/
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class RiptideDepthStriderMixin {

    private static final Identifier RIPTIDE_SUPPRESS_ID =
            Identifier.of("trishul", "riptide_suppress_movement_efficiency");

    @Inject(method = "travel", at = @At("HEAD"))
    private void trishul$suppressDepthStrider(Vec3d movementInput, CallbackInfo ci) {
        int riptideTicks = ((LivingEntityAccessor)(Object)this).trishul$getRiptideTicks();
        if (riptideTicks <= 0) return;

        PlayerEntity self = (PlayerEntity)(Object)this;
        var attribute = self.getAttributeInstance(EntityAttributes.MOVEMENT_EFFICIENCY);
        if (attribute == null) return;

        if (attribute.getModifier(RIPTIDE_SUPPRESS_ID) == null) {
            attribute.addTemporaryModifier(new EntityAttributeModifier(
                    RIPTIDE_SUPPRESS_ID,
                    -1.0,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void trishul$restoreDepthStrider(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        var attribute = self.getAttributeInstance(EntityAttributes.MOVEMENT_EFFICIENCY);
        if (attribute == null) return;

        attribute.removeModifier(RIPTIDE_SUPPRESS_ID);
    }
}