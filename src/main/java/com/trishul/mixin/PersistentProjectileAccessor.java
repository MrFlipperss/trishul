// src/main/java/com/trishul/mixin/PersistentProjectileAccessor.java
package com.trishul.mixin;
/*Used my ImpalingEnchantmentMixin to read enchantments*/
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PersistentProjectileEntity.class)
public interface PersistentProjectileAccessor {
    @Accessor("stack")
    ItemStack trishul$getStack();
}