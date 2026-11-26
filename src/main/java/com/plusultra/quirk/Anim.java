package com.plusultra.quirk;

import com.plusultra.core.ModParticles;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Multi-tick visual building blocks. Everything here only sends particles (and schedules more),
 * so it never changes gameplay. Particle ids are those in {@link ModParticles}.
 */
public final class Anim {
    private Anim() {}

    // ------------------------------------------------------------------ basic emitters

    /** Burst: count particles with gaussian position spread and gaussian velocity of the given speed. */
    public static void emit(ServerLevel l, String id, Vec3 at, int count, double spread, double speed) {
        l.sendParticles(ModParticles.type(id), at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    public static void emit(ServerLevel l, ParticleOptions o, Vec3 at, int count, double spread, double speed) {
        l.sendParticles(o, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    /** A single particle with an exact velocity in blocks per tick. */
    public static void vel(ServerLevel l, String id, Vec3 at, Vec3 v) {
        l.sendParticles(ModParticles.type(id), at.x, at.y, at.z, 0, v.x, v.y, v.z, 1.0);
    }

    public static void vel(ServerLevel l, ParticleOptions o, Vec3 at, Vec3 v) {
        l.sendParticles(o, at.x, at.y, at.z, 0, v.x, v.y, v.z, 1.0);
    }

    /** Runs a task a few ticks from now; delay 0 runs immediately. */
    public static void after(int ticks, Runnable r) {
        if (ticks <= 0) r.run();
        else Scheduler.schedule(ticks, r);
    }

    // ------------------------------------------------------------------ shaped bursts

    /** Particles shot out in a cone: dir is the cone axis, spread the sideways randomness (0.2 is about 12 degrees). */
    public static void coneBurst(ServerLevel l, String id, Vec3 origin, Vec3 dir, double speed, double spread, int count) {
        RandomSource r = l.getRandom();
        Vec3 d = dir.normalize();
        for (int i = 0; i < count; i++) {
            Vec3 v = d.add((r.nextDouble() - 0.5) * 2 * spread, (r.nextDouble() - 0.5) * 2 * spread,
                    (r.nextDouble() - 0.5) * 2 * spread).normalize().scale(speed * (0.55 + r.nextDouble() * 0.7));
            vel(l, id, origin, v);
        }
    }

    public static void coneBurst(ServerLevel l, ParticleOptions o, Vec3 origin, Vec3 dir, double speed, double spread,
                                 int count) {
        RandomSource r = l.getRandom();
        Vec3 d = dir.normalize();
        for (int i = 0; i < count; i++) {
            Vec3 v = d.add((r.nextDouble() - 0.5) * 2 * spread, (r.nextDouble() - 0.5) * 2 * spread,
                    (r.nextDouble() - 0.5) * 2 * spread).normalize().scale(speed * (0.55 + r.nextDouble() * 0.7));
            vel(l, o, origin, v);
        }
    }

    /** Particles on a sphere shell that rise/expand over a few ticks. */
    public static void shell(ServerLevel l, String id, Vec3 c, double radius, int ticks, int perTick) {
        RandomSource r = l.getRandom();
        for (int t = 0; t < ticks; t++) {
            final double rad = radius * (0.5 + 0.5 * (t + 1) / ticks);
            after(t, () -> {
                for (int i = 0; i < perTick; i++) {
                    double y = r.nextDouble() * 2 - 1;
                    double a = r.nextDouble() * Math.PI * 2;
                    double q = Math.sqrt(1 - y * y);
                    emit(l, id, c.add(Math.cos(a) * q * rad, y * rad, Math.sin(a) * q * rad), 1, 0.02, 0);
                }
            });
        }
    }

    /** Flat ring that grows outward over several ticks (shockwave). */
    public static void expandingRing(ServerLevel l, String id, Vec3 c, double maxRadius, int ticks, int perBlock) {
        for (int t = 1; t <= ticks; t++) {
            final double r = maxRadius * t / ticks;
            final int n = Math.max(8, (int) (2 * Math.PI * r * perBlock));
            after(t - 1, () -> {
                for (int i = 0; i < n; i++) {
                    double a = Math.PI * 2 * i / n;
                    emit(l, id, c.add(Math.cos(a) * r, 0, Math.sin(a) * r), 1, 0.02, 0);
                }
            });
        }
    }

    /** Vanilla-particle version of {@link #expandingRing}. */
    public static void expandingRing(ServerLevel l, ParticleOptions o, Vec3 c, double maxRadius, int ticks, int perBlock) {
        for (int t = 1; t <= ticks; t++) {
            final double r = maxRadius * t / ticks;
            final int n = Math.max(8, (int) (2 * Math.PI * r * perBlock));
            after(t - 1, () -> {
                for (int i = 0; i < n; i++) {
                    double a = Math.PI * 2 * i / n;
                    emit(l, o, c.add(Math.cos(a) * r, 0, Math.sin(a) * r), 1, 0.02, 0);
                }
            });
        }
    }

    /** Vertical column of particles that grows upward over a few ticks. */
    public static void pillar(ServerLevel l, String id, Vec3 base, double height, double radius, int ticks, int perTick) {
        RandomSource r = l.getRandom();
        for (int t = 0; t < ticks; t++) {
            final double top = height * (t + 1) / ticks;
            after(t, () -> {
                for (int i = 0; i < perTick; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    emit(l, id, base.add(Math.cos(a) * radius, r.nextDouble() * top, Math.sin(a) * radius), 1, 0.02, 0);
                }
            });
        }
    }

    // ------------------------------------------------------------------ lines and wraps

    /** Sends a flying particle (life = its flight time) from a to b. Returns the flight time in ticks. */
    public static int fly(ServerLevel l, String id, Vec3 from, Vec3 to) {
        int ticks = Math.max(1, ModParticles.def(id).lifeMin());
        vel(l, id, from, to.subtract(from).scale(1.0 / ticks));
        return ticks;
    }

    /** A line of particles that extends from a toward b over the given ticks, then fades on its own. */
    public static void strand(ServerLevel l, String id, Vec3 from, Vec3 to, int ticks, double step) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.2) return;
        final double df = step / len;
        for (int i = 0; i < ticks; i++) {
            final double f0 = i / (double) ticks;
            final double f1 = (i + 1) / (double) ticks;
            after(i, () -> {
                for (double f = f0; f < f1; f += df) emit(l, id, from.add(d.scale(f)), 1, 0.02, 0);
            });
        }
    }

    /** A line that stays put, re-drawn at once (taut rope while swinging). */
    public static void line(ServerLevel l, String id, Vec3 from, Vec3 to, double step) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.2) return;
        for (double t = 0; t < len; t += step) emit(l, id, from.add(d.scale(t / len)), 1, 0.01, 0);
    }

    /** Re-draws a line from the player's hand to a fixed anchor each tick (rope while the player moves). */
    public static void rope(ServerPlayer p, String id, Vec3 anchor, int ticks) {
        for (int t = 0; t < ticks; t++) {
            after(t, () -> {
                if (p.isRemoved()) return;
                line(p.serverLevel(), id, QFx.hand(p), anchor, 0.6);
            });
        }
    }

    /** A spiral that winds up around a point, optionally tightening (r1 < r0), drawn over several ticks. */
    public static void wrap(ServerLevel l, String id, Vec3 base, double height, double r0, double r1, double turns,
                            int ticks, int perTick) {
        final int total = ticks * perTick;
        for (int t = 0; t < ticks; t++) {
            final int tt = t;
            after(t, () -> {
                for (int k = 0; k < perTick; k++) {
                    double f = (tt * perTick + k) / (double) total;
                    double a = f * turns * Math.PI * 2;
                    double r = r0 + (r1 - r0) * f;
                    emit(l, id, base.add(Math.cos(a) * r, f * height, Math.sin(a) * r), 1, 0.01, 0);
                }
            });
        }
    }

    /** Particles that gather inward toward a point (charge-up). */
    public static void gather(ServerLevel l, String id, Vec3 target, double radius, int ticks, int perTick) {
        RandomSource r = l.getRandom();
        for (int t = 0; t < ticks; t++) {
            after(t, () -> {
                for (int i = 0; i < perTick; i++) {
                    double y = r.nextDouble() * 2 - 1;
                    double a = r.nextDouble() * Math.PI * 2;
                    double q = Math.sqrt(1 - y * y);
                    Vec3 off = new Vec3(Math.cos(a) * q, y, Math.sin(a) * q).scale(radius);
                    vel(l, id, target.add(off), off.scale(-1.0 / 6.0));
                }
            });
        }
    }

    // ------------------------------------------------------------------ lightning

    /** Jagged bolt between two points: a few big bolt sprites on the zigzag plus a thin spark core along it. */
    public static void bolt(ServerLevel l, Vec3 from, Vec3 to, String boltId, String coreId, int segments, double jitter) {
        RandomSource r = l.getRandom();
        Vec3 prev = from;
        for (int i = 1; i <= segments; i++) {
            Vec3 p = from.add(to.subtract(from).scale(i / (double) segments));
            if (i < segments) {
                p = p.add((r.nextDouble() - 0.5) * jitter, (r.nextDouble() - 0.5) * jitter,
                        (r.nextDouble() - 0.5) * jitter);
            }
            emit(l, boltId, prev.add(p).scale(0.5), 1, 0, 0);
            Vec3 seg = p.subtract(prev);
            double len = seg.length();
            for (double t = 0; t < len; t += 0.3) emit(l, coreId, prev.add(seg.scale(t / len)), 1, 0.01, 0);
            prev = p;
        }
    }

    /** Several short bolts crawling over a body. */
    public static void arcs(ServerLevel l, Vec3 center, double radius, int count, String boltId, String coreId) {
        RandomSource r = l.getRandom();
        for (int i = 0; i < count; i++) {
            Vec3 a = center.add((r.nextDouble() - 0.5) * radius, (r.nextDouble() - 0.5) * radius * 1.6,
                    (r.nextDouble() - 0.5) * radius);
            Vec3 b = center.add((r.nextDouble() - 0.5) * radius * 2.2, (r.nextDouble() - 0.5) * radius * 2.6,
                    (r.nextDouble() - 0.5) * radius * 2.2);
            bolt(l, a, b, boltId, coreId, 3, radius * 0.35);
        }
    }

    // ------------------------------------------------------------------ following the player

    /** Runs an emitter at the player's current position for the next few ticks (dash trails, exhaust). */
    public static void follow(ServerPlayer p, int ticks, Consumer<ServerPlayer> emitter) {
        for (int t = 1; t <= ticks; t++) {
            after(t, () -> {
                if (!p.isRemoved() && p.isAlive()) emitter.accept(p);
            });
        }
    }
}
