package com.infamousgc.bedrolls.mixin;

import com.infamousgc.bedrolls.client.BedrollClientData;
import com.infamousgc.bedrolls.network.RespawnAtWorldSpawnPacket;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {

    protected DeathScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void replaceButtons(CallbackInfo ci) {
        // Find and remove vanilla's "Respawn" button (first button)
        List<AbstractWidget> toRemove = new ArrayList<>();
        for (var child : this.children()) {
            if (child instanceof Button button) {
                String text = button.getMessage().getString();
                if (text.equals(Component.translatable("deathScreen.respawn").getString())) {
                    toRemove.add(button);
                    break;
                }
            }
        }
        for (var widget : toRemove) {
            this.removeWidget(widget);
        }

        boolean hasBedroll = BedrollClientData.hasBedroll;

        int centerX = this.width / 2;
        int buttonY = this.height / 4 + 72;

        // Respawn at Bedroll
        Button bedrollButton = Button.builder(
                hasBedroll
                ? Component.translatable("deathScreen.respawnAtBedroll")
                : Component.translatable("deathScreen.noBedroll"),
                btn -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.respawn();
                        this.minecraft.setScreen(null);
                    }
                })
                .bounds(centerX - 100, buttonY, 200, 20)
                .build();
        bedrollButton.active = hasBedroll;
        this.addRenderableWidget(bedrollButton);

        // Respawn at World Spawn
        this.addRenderableWidget(Button.builder(
                Component.translatable("deathScreen.respawnAtWorldSpawn"),
                btn -> {
                    PacketDistributor.sendToServer(new RespawnAtWorldSpawnPacket());
                    if (this.minecraft != null) {
                        this.minecraft.player.respawn();
                        this.minecraft.setScreen(null);
                    }
                })
                .bounds(centerX - 100, buttonY + 24, 200, 20)
                .build());

        // Re-layout: shift the existing Title Screen button down so it doesn't overlap
        for (var child : this.children()) {
            if (child instanceof Button button &&
            button.getMessage().getString().equals(Component.translatable("deathScreen.titleScreen").getString())) {
                button.setY(button.getY() + 48);
            }

        }
    }
}
