package com.trishul;

import com.trishul.event.LeapAbilityHandler;
import net.fabricmc.api.ModInitializer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Trishul implements ModInitializer {

    public static final String MOD_ID = "trishul";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Identifier LEAP_ID  = Identifier.of(MOD_ID, "leap");
    public static final Identifier LUNGE_ID = Identifier.of("minecraft", "lunge");

    // Will be populated lazily the first time an anvil is used
    public static RegistryEntry<Enchantment> LEAP_ENTRY = null;

    @Override
    public void onInitialize() {
        LeapAbilityHandler.register();
        LOGGER.info("Trishul initialized!");
    }
}