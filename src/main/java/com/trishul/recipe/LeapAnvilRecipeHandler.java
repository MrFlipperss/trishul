package com.trishul.recipe;

import com.trishul.Trishul;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

public class LeapAnvilRecipeHandler {

    private static int getBookEnchantLevel(ItemStack book, Identifier enchantId) {
        if (!book.isOf(Items.ENCHANTED_BOOK)) return 0;
        ItemEnchantmentsComponent enchants = book.getOrDefault(
                DataComponentTypes.STORED_ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );
        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
            if (entry.getKey().isPresent() &&
                    entry.getKey().get().getValue().equals(enchantId)) {
                return enchants.getLevel(entry);
            }
        }
        return 0;
    }

    private static int getItemEnchantLevel(ItemStack item, Identifier enchantId) {
        ItemEnchantmentsComponent enchants = item.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );
        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
            if (entry.getKey().isPresent() &&
                    entry.getKey().get().getValue().equals(enchantId)) {
                return enchants.getLevel(entry);
            }
        }
        return 0;
    }

    /**
     * Tries to find the Leap RegistryEntry from the server's registry.
     * Called lazily the first time the anvil is used.
     */
    private static void tryLoadLeapEntry(MinecraftServer server) {
        if (Trishul.LEAP_ENTRY != null) return;
        if (server == null) return;

        var registry = server.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        for (var mapEntry : registry.getEntrySet()) {
            if (mapEntry.getKey().getValue().equals(Trishul.LEAP_ID)) {
                registry.getEntry(registry.getRawId(mapEntry.getValue()))
                        .ifPresent(entry -> Trishul.LEAP_ENTRY = entry);
                Trishul.LOGGER.info("[Trishul] Leap enchantment entry loaded.");
                break;
            }
        }

        if (Trishul.LEAP_ENTRY == null) {
            Trishul.LOGGER.error("[Trishul] Leap enchantment not found!");
        }
    }

    /**
     * Case 1: Heart of the Sea + Lunge Book → Leap Book
     */
    public static ItemStack buildLeapBook(ItemStack right, MinecraftServer server) {
        if (!right.isOf(Items.ENCHANTED_BOOK)) return ItemStack.EMPTY;

        tryLoadLeapEntry(server);
        if (Trishul.LEAP_ENTRY == null) return ItemStack.EMPTY;

        int lungeLevel = getBookEnchantLevel(right, Trishul.LUNGE_ID);
        if (lungeLevel < 1 || lungeLevel > 3) return ItemStack.EMPTY;

        ItemStack leapBook = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantmentsComponent.Builder builder =
                new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        builder.set(Trishul.LEAP_ENTRY, lungeLevel);
        leapBook.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
        return leapBook;
    }

    /**
     * Case 2: Trident + Leap Book → Trident with Leap
     */
    public static ItemStack buildLeapTrident(ItemStack left, ItemStack right,
                                             MinecraftServer server) {
        if (!left.isOf(Items.TRIDENT)) return ItemStack.EMPTY;
        if (!right.isOf(Items.ENCHANTED_BOOK)) return ItemStack.EMPTY;

        tryLoadLeapEntry(server);
        if (Trishul.LEAP_ENTRY == null) return ItemStack.EMPTY;

        int leapBookLevel = getBookEnchantLevel(right, Trishul.LEAP_ID);
        if (leapBookLevel < 1) return ItemStack.EMPTY;

        int currentLeapLevel = getItemEnchantLevel(left, Trishul.LEAP_ID);
        if (currentLeapLevel >= 3) return ItemStack.EMPTY;

        int newLevel = Math.min(leapBookLevel, 3);

        ItemStack result = left.copy();
        ItemEnchantmentsComponent existing = result.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );
        ItemEnchantmentsComponent.Builder builder =
                new ItemEnchantmentsComponent.Builder(existing);
        builder.set(Trishul.LEAP_ENTRY, newLevel);
        result.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        return result;
    }
}