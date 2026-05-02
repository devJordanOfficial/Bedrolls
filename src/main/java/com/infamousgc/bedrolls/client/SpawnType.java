package com.infamousgc.bedrolls.client;

import net.minecraft.network.chat.Component;

public enum SpawnType {
    NONE("deathScreen.noBedroll", false),
    BEDROLL("deathScreen.respawnAtBedroll", true),
    ANCHOR_CHARGED("deathScreen.respawnAtAnchor", true),
    ANCHOR_EMPTY("deathScreen.noAnchorCharged", false);

    private final String translationKey;
    public final boolean buttonActive;

    SpawnType(String translationKey, boolean buttonActive) {
        this.translationKey = translationKey;
        this.buttonActive = buttonActive;
    }

    public Component getButtonLabel() {
        return Component.translatable(translationKey);
    }
}
