package com.infamousgc.bedrolls.util;

import com.infamousgc.bedrolls.Main;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Main.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {

    public enum NetherBedrollBehavior {
        ALLOW,
        EXPLODE,
        DISALLOW
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<NetherBedrollBehavior> NETHER_BEDROLL_BEHAVIOR = BUILDER
            .comment("Behavior when a bedroll is placed in the Nether or End (any dimension where beds don't work).",
                    "ALLOW: Bedrolls can be placed for decoration but won't set spawn. Right-clicking still explodes like a vanilla bed.",
                    "EXPLODE: Bedrolls explode immediately on placement (like beds when right-clicked)",
                    "DISALLOW: Bedrolls cannot be placed at all in these dimensions."
            )
            .defineEnum("netherBedrollBehavior", NetherBedrollBehavior.ALLOW);

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Config loaded - values are now accessible via NETHER_BEDROLL_BEHAVIOR.get()
    }
}
