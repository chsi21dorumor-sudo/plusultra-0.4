package com.plusultra.quirk;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Signature visuals of individual Quirks: MHA-style blasts, green lightning, wings, feathers and per-Quirk auras. */
public final class QFx {
    private QFx() {}

    // ------------------------------------------------------------------ positions

    public static Vec3 right(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 f = new Vec3(look.x, 0, look.z);
        if (f.lengthSqr() < 1e-4) f = new Vec3(0, 0, 1);
        f = f.normalize();
        return new Vec3(-f.z, 0, f.x);
    }

    /** Roughly where the player's right hand is. */
    public static Vec3 hand(ServerPlayer p) {
        return p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(right(p).scale(0.35)).add(0, -0.45, 0);
    }

    public static Vec3 feet(ServerPlayer p) {
        return p.position().add(0, 0.15, 0);
    }

    public static ParticleOptions chips(net.minecraft.world.level.block.Block block) {
        return new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState());
    }

    // ------------------------------------------------------------------ One For All

    /** Green-white discharge at a hit point. */
    public static void ofaStrike(ServerLevel l, Vec3 at) {
        Anim.emit(l, "green_spark", at, 30, 0.4, 0.25);
        Anim.emit(l, "white_spark", at, 12, 0.2, 0.3);
        RandomSource r = l.getRandom();
        for (int i = 0; i < 3; i++) {
            Vec3 end = at.add((r.nextDouble() - 0.5) * 3, (r.nextDouble() - 0.2) * 2, (r.nextDouble() - 0.5) * 3);
            Anim.bolt(l, at, end, "green_bolt", "white_spark", 3, 0.5);
        }
    }

    // ------------------------------------------------------------------ Explosion (My Hero style)

    /**
     * Orange starburst flash, firework sparks, rolling black smoke, flame, shockwave and a second pop.
     */
    public static void mhaBlast(ServerLevel l, Vec3 c, double radius) {
        RandomSource r = l.getRandom();
        Anim.emit(l, "blast_flash", c, 1, 0, 0);
        for (int i = 0; i < 4; i++) {
            Anim.emit(l, "blast_flash_small",
                    c.add((r.nextDouble() - 0.5) * radius, (r.nextDouble() - 0.3) * radius * 0.6,
                            (r.nextDouble() - 0.5) * radius), 1, 0, 0);
        }
        Anim.emit(l, "blast_spark", c, (int) (30 + radius * 10), radius * 0.12, 0.45 + radius * 0.05);
        Anim.emit(l, "white_spark", c, 16, radius * 0.1, 0.35);
        Anim.emit(l, "blast_smoke", c, (int) (8 + radius * 3), radius * 0.3, 0.03);
        l.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, (int) (15 + radius * 5), radius * 0.25, radius * 0.25,
                radius * 0.25, 0.08);
        l.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        Anim.emit(l, "blast_wave", c, 1, 0, 0);
        Anim.expandingRing(l, "blast_spark", c.add(0, 0.2, 0), radius, 4, 3);
        Anim.after(3, () -> {
            Anim.emit(l, "blast_flash_small", c.add(0, radius * 0.2, 0), 2, radius * 0.3, 0);
            Anim.emit(l, "blast_spark", c, 20, radius * 0.1, 0.3);
            Anim.emit(l, "blast_smoke", c.add(0, 0.5, 0), 6, radius * 0.25, 0.02);
        });
    }

    /** Small pop used for trailing explosions while flying. */
    public static void miniBlast(ServerLevel l, Vec3 c) {
        Anim.emit(l, "blast_flash_small", c, 1, 0.1, 0);
        Anim.emit(l, "blast_spark", c, 8, 0.15, 0.25);
        Anim.emit(l, "blast_smoke", c, 2, 0.15, 0.02);
        l.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 4, 0.1, 0.1, 0.1, 0.04);
    }

    // ------------------------------------------------------------------ Fierce Wings

    /** Wings drawn from feathers that flap for the given number of ticks, following the player. */
    public static void wings(ServerPlayer p, int ticks, double span) {
        Anim.follow(p, ticks, q -> {
            ServerLevel l = q.serverLevel();
            Vec3 r = right(q);
            Vec3 look = q.getLookAngle();
            Vec3 behind = new Vec3(-look.x, 0, -look.z);
            if (behind.lengthSqr() > 1e-4) behind = behind.normalize().scale(0.3);
            Vec3 base = q.position().add(0, 1.35, 0).add(behind);
            double flap = Math.sin(q.tickCount * 0.9);
            for (int s = -1; s <= 1; s += 2) {
                for (int j = 0; j < 3; j++) {
                    double ang = 0.35 + j * 0.38 + flap * 0.35;
                    double len = span * (1.0 - j * 0.12);
                    for (int k = 1; k <= 4; k++) {
                        double dist = len * k / 4.0;
                        Vec3 pos = base.add(r.scale(s * dist * Math.cos(ang))).add(0, dist * Math.sin(ang) - j * 0.25, 0);
                        if (k == 4) Anim.emit(l, "feather", pos, 1, 0.03, 0.0);
                        else Anim.emit(l, "red_spark", pos, 1, 0.03, 0.0);
                    }
                }
            }
        });
    }

    /** Feathers launched from the wings toward a point, staggered. Returns the flight time. */
    public static int featherVolley(ServerPlayer p, Vec3 target, int count) {
        ServerLevel l = p.serverLevel();
        RandomSource r = p.getRandom();
        for (int i = 0; i < count; i++) {
            final int side = i % 2 == 0 ? 1 : -1;
            Anim.after(i / 2, () -> {
                if (p.isRemoved()) return;
                Vec3 from = p.position().add(0, 1.3 + r.nextDouble() * 0.6, 0)
                        .add(right(p).scale(side * (0.5 + r.nextDouble() * 0.5)));
                Vec3 to = target.add((r.nextDouble() - 0.5) * 0.6, (r.nextDouble() - 0.5) * 0.8,
                        (r.nextDouble() - 0.5) * 0.6);
                Anim.fly(l, "feather_fly", from, to);
            });
        }
        return 10 + count / 2;
    }

    public static void featherImpact(ServerLevel l, Vec3 at) {
        Anim.emit(l, "feather", at, 8, 0.3, 0.12);
        Anim.emit(l, "red_spark", at, 10, 0.25, 0.2);
        Anim.emit(l, "white_spark", at, 4, 0.15, 0.2);
    }

    // ------------------------------------------------------------------ per-Quirk aura

    /** One pulse of the glow around a buffed player; called every few ticks while the aura lasts. */
    public static void aura(ServerPlayer p, String quirkId, int rgb) {
        ServerLevel l = p.serverLevel();
        Vec3 body = p.position().add(0, 0.9, 0);
        Vec3 r = right(p);
        RandomSource rnd = p.getRandom();
        switch (quirkId) {
            case "one_for_all" -> {
                Anim.emit(l, "green_spark", body, 4, 0.4, 0.05);
                if (rnd.nextInt(2) == 0) Anim.arcs(l, body, 0.6, 1, "green_bolt", "white_spark");
            }
            case "half_cold_half_hot" -> {
                Anim.emit(l, ParticleTypes.FLAME, body.add(r.scale(-0.4)), 2, 0.2, 0.01);
                Anim.emit(l, "blast_spark", body.add(r.scale(-0.4)), 1, 0.25, 0.03);
                Anim.emit(l, "ice_shard", body.add(r.scale(0.4)), 1, 0.25, 0.02);
                Anim.emit(l, ParticleTypes.SNOWFLAKE, body.add(r.scale(0.4)), 2, 0.25, 0.01);
            }
            case "explosion" -> {
                Anim.emit(l, "blast_spark", p.position().add(0, 1.0, 0).add(r.scale(0.4)), 2, 0.2, 0.06);
                Anim.emit(l, "white_spark", body, 1, 0.4, 0.04);
                if (rnd.nextInt(4) == 0) Anim.emit(l, "blast_smoke", body.add(0, 0.4, 0), 1, 0.3, 0.01);
            }
            case "hardening" -> {
                Anim.emit(l, "red_spark", body, 2, 0.4, 0.03);
                if (rnd.nextInt(3) == 0) Anim.emit(l, chips(Blocks.STONE), body, 2, 0.4, 0.03);
            }
            case "zero_gravity" -> Anim.emit(l, "zero_orb", p.position().add(0, 0.3, 0), 3, 0.5, 0.02);
            case "engine" -> {
                Anim.emit(l, "blue_spark", p.position().add(0, 0.3, 0), 3, 0.3, 0.1);
                Anim.emit(l, ParticleTypes.CLOUD, p.position().add(0, 0.2, 0), 1, 0.2, 0.02);
                if (rnd.nextInt(3) == 0) Anim.arcs(l, body, 0.5, 1, "blue_bolt", "blue_spark");
            }
            case "creation" -> Anim.emit(l, "gold_spark", body, 3, 0.45, 0.04);
            case "invisibility" -> Anim.emit(l, "shimmer", body, 4, 0.45, 0.02);
            case "fierce_wings" -> {
                if (rnd.nextInt(2) == 0) Anim.emit(l, "feather", body.add(0, 0.5, 0), 1, 0.4, 0.02);
                Anim.emit(l, "red_spark", body, 1, 0.4, 0.04);
            }
            case "dark_shadow" -> Anim.emit(l, "shadow_wisp", body, 2, 0.35, 0.02);
            case "black_whip" -> Anim.emit(l, "whip_wisp", body, 2, 0.35, 0.02);
            case "acid" -> {
                Anim.emit(l, "acid_bubble", body, 2, 0.4, 0.02);
                Anim.emit(l, "acid_drop", body.add(0, 0.5, 0), 1, 0.35, 0.0);
            }
            case "earphone_jack" -> {
                Anim.emit(l, ParticleTypes.NOTE, body.add(0, 0.7, 0), 1, 0.4, 1);
                if (p.tickCount % 9 == 0) Anim.emit(l, "sound_wave", body, 1, 0, 0);
            }
            case "tape" -> Anim.emit(l, "tape", body, 2, 0.45, 0.02);
            default -> Anim.emit(l, "white_spark", body, 3, 0.4, 0.04);
        }
    }
}
