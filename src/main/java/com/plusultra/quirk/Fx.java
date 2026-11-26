package com.plusultra.quirk;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server-side visual effects built from vanilla particles: rings, beams, spirals, spheres, auras. */
public final class Fx {
    private Fx() {}

    public static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(
                new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), scale);
    }

    /** Flat horizontal ring. */
    public static void ring(ServerLevel l, ParticleOptions o, Vec3 c, double r, int n) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            l.sendParticles(o, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    /** Straight line of particles. */
    public static void beam(ServerLevel l, ParticleOptions o, Vec3 from, Vec3 to, double step) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1e-3) return;
        Vec3 dir = d.scale(1.0 / len);
        for (double t = 0; t <= len; t += step) {
            Vec3 p = from.add(dir.scale(t));
            l.sendParticles(o, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0);
        }
    }

    /** Rising spiral around a vertical axis. */
    public static void spiral(ServerLevel l, ParticleOptions o, Vec3 base, double height, double radius,
                              double turns, int n) {
        for (int i = 0; i < n; i++) {
            double f = i / (double) n;
            double a = f * turns * Math.PI * 2;
            l.sendParticles(o, base.x + Math.cos(a) * radius, base.y + f * height,
                    base.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** Hollow sphere of particles. */
    public static void sphere(ServerLevel l, ParticleOptions o, Vec3 c, double r, int n) {
        for (int i = 0; i < n; i++) {
            double y = 1 - 2 * (i + 0.5) / n;
            double rad = Math.sqrt(1 - y * y);
            double phi = i * 2.399963;
            l.sendParticles(o, c.x + Math.cos(phi) * rad * r, c.y + y * r, c.z + Math.sin(phi) * rad * r,
                    1, 0, 0, 0, 0);
        }
    }

    public static void burst(ServerLevel l, ParticleOptions o, Vec3 c, int count, double spread, double speed) {
        l.sendParticles(o, c.x, c.y, c.z, count, spread, spread, spread, speed);
    }

    /** Falling streak (count 0 makes the offsets a velocity). */
    public static void streak(ServerLevel l, ParticleOptions o, Vec3 from, double speed) {
        l.sendParticles(o, from.x, from.y, from.z, 0, 0, -1, 0, speed);
    }

    /** Harmless lightning for show. */
    public static void lightning(ServerLevel l, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
        if (bolt == null) return;
        bolt.moveTo(at.x, at.y, at.z);
        bolt.setVisualOnly(true);
        l.addFreshEntity(bolt);
    }

    /** One pulse of the glow around a buffed player (per-Quirk look, see QFx). */
    public static void aura(ServerPlayer p, int rgb) {
        QFx.aura(p, PlayerData.get(p).auraQuirk, rgb);
    }

    /** Keeps an aura running for the given number of ticks, in the active Quirk's color. */
    public static void startAura(ServerPlayer p, int ticks) {
        PlayerData d = PlayerData.get(p);
        Quirk q = d.activeQuirk();
        d.auraUntil = p.level().getGameTime() + ticks;
        d.auraColor = q == null ? 0xFFFFFF : q.color();
        d.auraQuirk = q == null ? "" : q.id();
    }

    public static void levelUp(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Vec3 c = p.position();
        ring(l, dust(0xFFD700, 1.3f), c.add(0, 0.1, 0), 1.2, 20);
        ring(l, dust(0xFFD700, 1.3f), c.add(0, 1.0, 0), 1.6, 24);
        spiral(l, ParticleTypes.END_ROD, c, 2.2, 0.8, 2, 30);
        Anim.emit(l, "gold_spark", c.add(0, 1, 0), 30, 0.6, 0.2);
        Anim.emit(l, "white_spark", c.add(0, 1, 0), 10, 0.4, 0.15);
        l.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.2f);
    }
}
