package com.plusultra.core;

import com.plusultra.PlusUltra;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All custom particles, defined in one table. Each entry becomes a registered particle type and (client side)
 * a StyledParticle with the given look. Sprites live in textures/particle/, frames listed in particles/&lt;id&gt;.json.
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, PlusUltra.MODID);

    /**
     * @param grow    final size = size * (1 + grow) at end of life
     * @param spin    roll change per tick (radians)
     * @param flutter sideways drift strength (feathers, orbs)
     * @param trail   id of a particle left behind while flying, or null
     */
    public record Def(String id, float r, float g, float b, float sizeMin, float sizeMax, int lifeMin, int lifeMax,
                      float gravity, float friction, boolean glow, float grow, float spin, float flutter,
                      String trail) {}

    private static Def d(String id, int rgb, float s0, float s1, int l0, int l1, float gravity, float friction,
                         boolean glow, float grow, float spin, float flutter, String trail) {
        return new Def(id, ((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f,
                s0, s1, l0, l1, gravity, friction, glow, grow, spin, flutter, trail);
    }

    public static final List<Def> DEFS = List.of(
            // id, tint, size, life, gravity, friction, glow, grow, spin, flutter, trail
            d("green_bolt", 0x8CFF5A, 0.9f, 1.4f, 3, 5, 0f, 1f, true, 0f, 0f, 0f, null),
            d("green_spark", 0x8CFF5A, 0.15f, 0.3f, 8, 16, 0.02f, 0.9f, true, 0f, 0.1f, 0f, null),
            d("green_wave", 0x8CFF5A, 0.4f, 0.4f, 8, 10, 0f, 1f, true, 5.5f, 0f, 0f, null),
            d("white_spark", 0xFFFFFF, 0.15f, 0.3f, 6, 12, 0f, 0.88f, true, 0f, 0.15f, 0f, null),
            d("blast_flash", 0xFFB830, 1.6f, 2.4f, 6, 8, 0f, 1f, true, 0.6f, 0.05f, 0f, null),
            d("blast_flash_small", 0xFFB830, 0.6f, 0.9f, 5, 7, 0f, 1f, true, 0.6f, 0.05f, 0f, null),
            d("blast_spark", 0xFF8A1C, 0.2f, 0.4f, 14, 24, 0.4f, 0.93f, true, 0f, 0.2f, 0f, null),
            d("blast_smoke", 0x1E1A1A, 1.0f, 1.7f, 22, 36, -0.02f, 0.94f, false, 0.8f, 0.02f, 0f, null),
            d("blast_wave", 0xFFA030, 0.4f, 0.4f, 8, 10, 0f, 1f, true, 6f, 0f, 0f, null),
            d("ice_shard", 0xA8E6FF, 0.25f, 0.45f, 16, 26, 0.12f, 0.95f, false, 0f, 0.2f, 0f, null),
            d("ice_wave", 0xA8E6FF, 0.4f, 0.4f, 8, 10, 0f, 1f, true, 6f, 0f, 0f, null),
            d("red_spark", 0xE63B2E, 0.15f, 0.3f, 8, 14, 0.05f, 0.9f, true, 0f, 0.12f, 0f, null),
            d("zero_orb", 0xFFB3D1, 0.15f, 0.3f, 30, 50, -0.012f, 0.96f, true, 0f, 0f, 0.003f, null),
            d("meteor", 0xFFA6D9, 0.75f, 0.95f, 12, 12, 0f, 1f, true, 0f, 0f, 0f, "zero_orb"),
            d("blue_spark", 0x4DA6FF, 0.15f, 0.3f, 8, 16, 0.02f, 0.9f, true, 0f, 0.1f, 0f, null),
            d("blue_bolt", 0x6AB8FF, 0.9f, 1.4f, 3, 5, 0f, 1f, true, 0f, 0f, 0f, null),
            d("blue_wave", 0x6AB8FF, 0.4f, 0.4f, 8, 10, 0f, 1f, true, 6f, 0f, 0f, null),
            d("gold_spark", 0xFFC03A, 0.15f, 0.3f, 8, 16, 0.03f, 0.9f, true, 0f, 0.1f, 0f, null),
            d("cannon_shell", 0xFFC03A, 0.5f, 0.6f, 10, 10, 0f, 1f, true, 0f, 0.1f, 0f, "gold_spark"),
            d("shimmer", 0xBFD9FF, 0.12f, 0.22f, 14, 24, 0f, 0.93f, true, 0f, 0f, 0.004f, null),
            d("feather", 0xE0382A, 0.28f, 0.42f, 24, 40, 0.015f, 0.96f, false, 0f, 0.08f, 0.006f, null),
            d("feather_fly", 0xE8452E, 0.32f, 0.42f, 10, 10, 0f, 1f, false, 0f, 0.25f, 0f, "red_spark"),
            d("shadow_wisp", 0x1C1C3A, 0.5f, 0.9f, 20, 34, -0.01f, 0.93f, false, 0.9f, 0.03f, 0f, null),
            d("whip_wisp", 0x5A2272, 0.5f, 0.9f, 20, 34, -0.01f, 0.93f, false, 0.9f, 0.03f, 0f, null),
            d("acid_drop", 0xA6E22E, 0.14f, 0.24f, 16, 28, 0.5f, 0.95f, false, 0f, 0f, 0f, null),
            d("acid_bubble", 0xB8F040, 0.12f, 0.25f, 12, 22, -0.03f, 0.95f, true, 0f, 0f, 0f, null),
            d("acid_glob", 0x9BE020, 0.45f, 0.55f, 8, 8, 0f, 1f, true, 0f, 0f, 0f, "acid_drop"),
            d("sound_wave", 0xFF6FB5, 0.35f, 0.4f, 9, 11, 0f, 1f, true, 5.5f, 0f, 0f, null),
            d("sound_wave_big", 0xFF6FB5, 0.4f, 0.4f, 12, 14, 0f, 1f, true, 12f, 0f, 0f, null),
            d("tape", 0xEDEDE8, 0.26f, 0.36f, 40, 60, 0f, 0.8f, false, 0f, 0f, 0f, null),
            d("pink_wire", 0xC0408F, 0.22f, 0.3f, 14, 20, 0f, 0.8f, false, 0f, 0f, 0f, null),
            d("whip_cord", 0x5A2272, 0.18f, 0.24f, 5, 7, 0f, 0.8f, false, 0f, 0f, 0f, null),
            d("tape_cord", 0xEDEDE8, 0.18f, 0.24f, 5, 7, 0f, 0.8f, false, 0f, 0f, 0f, null)
    );

    private static final Map<String, DeferredHolder<ParticleType<?>, SimpleParticleType>> HOLDERS =
            new LinkedHashMap<>();
    private static final Map<String, Def> BY_ID = new LinkedHashMap<>();

    static {
        for (Def def : DEFS) {
            HOLDERS.put(def.id(), PARTICLES.register(def.id(), () -> new SimpleParticleType(false)));
            BY_ID.put(def.id(), def);
        }
    }

    private ModParticles() {}

    public static SimpleParticleType type(String id) {
        return HOLDERS.get(id).get();
    }

    public static Def def(String id) {
        return BY_ID.get(id);
    }
}
