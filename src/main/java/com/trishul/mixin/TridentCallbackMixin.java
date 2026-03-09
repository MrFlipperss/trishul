package com.trishul.mixin;

import com.trishul.api.TridentCallbackAccess;
import net.minecraft.entity.projectile.TridentEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentEntity.class)
public abstract class TridentCallbackMixin implements TridentCallbackAccess {

    @Unique
    private boolean trishul$frozen = false;

    @Unique
    public void trishul$freeze() {
        this.trishul$frozen = true;
        TridentEntity self = (TridentEntity)(Object)this;
        self.setVelocity(0, 0, 0);
        self.setNoGravity(true);
    }

    @Unique
    public void trishul$unfreeze() {
        this.trishul$frozen = false;
        TridentEntity self = (TridentEntity)(Object)this;
        self.setNoGravity(false);
    }

    @Unique
    public boolean trishul$isFrozen() {
        return this.trishul$frozen;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void trishul$tickFrozen(CallbackInfo ci) {
        if (!this.trishul$frozen) return;
        TridentEntity self = (TridentEntity)(Object)this;
        self.setVelocity(0, 0, 0);
    }
}