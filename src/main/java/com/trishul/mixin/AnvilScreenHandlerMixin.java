package com.trishul.mixin;

import com.trishul.recipe.LeapAnvilRecipeHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.ForgingSlotsManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {

    protected AnvilScreenHandlerMixin(ScreenHandlerType<?> type, int syncId,
                                      PlayerInventory playerInventory,
                                      ScreenHandlerContext context,
                                      ForgingSlotsManager forgingSlotsManager) {
        super(type, syncId, playerInventory, context, forgingSlotsManager);
    }

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void trishul_injectAnvilResult(CallbackInfo ci) {
        ItemStack left  = this.slots.get(0).getStack();
        ItemStack right = this.slots.get(1).getStack();

        if (left.isEmpty() || right.isEmpty()) return;

        // Get the server instance for registry access
        MinecraftServer server = null;
        if (this.player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
            server = (net.minecraft.server.MinecraftServer) sp.getEntityWorld().getServer();
        }
        if (server == null) return;

        // Case 1: Heart of the Sea + Lunge Book → Leap Book
        if (left.isOf(Items.HEART_OF_THE_SEA)) {
            ItemStack leapBook = LeapAnvilRecipeHandler.buildLeapBook(right, server);
            if (!leapBook.isEmpty()) {
                this.slots.get(2).setStack(leapBook);
                this.sendContentUpdates();
                return;
            }
        }

        // Case 2: Trident + Leap Book → Trident with Leap
        if (left.isOf(Items.TRIDENT)) {
            ItemStack leapTrident = LeapAnvilRecipeHandler.buildLeapTrident(left, right, server);
            if (!leapTrident.isEmpty()) {
                this.slots.get(2).setStack(leapTrident);
                this.sendContentUpdates();
            }
        }
    }
}