package com.trishul.mixin;

import net.minecraft.entity.projectile.TridentEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TridentEntity.class)
public interface TridentAccessor {
    @Accessor("dealtDamage")
    void trishul$setDealtDamage(boolean value);

    @Accessor("returnTimer")
    void trishul$setReturnTimer(int value);
}