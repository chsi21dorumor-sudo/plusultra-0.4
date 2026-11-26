package com.plusultra.quirk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Creation, Invisibility, Fierce Wings, Dark Shadow, Black Whip, Acid, Earphone Jack and Tape.
 * Gameplay logic with animated visuals (strands that extend and wrap, feather volleys, projectiles, pulses).
 */
public final class MoreAbilities {
    private MoreAbilities() {}

    // ------------------------------------------------------------------ helpers

    private static LivingEntity nearest(ServerPlayer p, double range, double cos) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : Abilities.cone(p, range, cos)) {
            double d = e.distanceToSqr(p);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    private static List<LivingEntity> nearestN(ServerPlayer p, double range, double cos, int n) {
        List<LivingEntity> all = new ArrayList<>(Abilities.cone(p, range, cos));
        all.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        return all.size() > n ? all.subList(0, n) : all;
    }

    private static void pull(LivingEntity t, ServerPlayer p, double strength) {
        Vec3 d = p.position().subtract(t.position());
        if (d.lengthSqr() < 1e-4) return;
        Vec3 n = d.normalize();
        t.push(n.x * strength, 0.25, n.z * strength);
        t.hurtMarked = true;
    }

    private static Vec3 center(LivingEntity e) {
        return Abilities.center(e);
    }

    // ------------------------------------------------------------------ Creation

    public static void forgeShield(ServerPlayer p, double power) {
        Abilities.effect(p, MobEffects.ABSORPTION, 300, 2);
        Abilities.effect(p, MobEffects.DAMAGE_RESISTANCE, 300, 1);
        ServerLevel l = Abilities.level(p);
        Vec3 c = p.position().add(0, 1, 0);
        Anim.gather(l, "gold_spark", QFx.hand(p), 1.4, 4, 4);
        Anim.after(4, () -> {
            Anim.shell(l, "gold_spark", p.position().add(0, 1, 0), 1.5, 6, 10);
            Anim.emit(l, QFx.chips(Blocks.IRON_BLOCK), p.position().add(0, 1, 0), 30, 0.6, 0.15);
            Anim.emit(l, "white_spark", p.position().add(0, 1, 0), 10, 0.5, 0.2);
        });
        Anim.expandingRing(l, "gold_spark", QFx.feet(p), 1.8, 4, 3);
        Fx.startAura(p, 300);
        Abilities.sound(p, SoundEvents.ANVIL_USE, 1.2f);
    }

    public static void creationCannon(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        final LivingEntity t = nearest(p, 26, 0.96);
        Vec3 from = QFx.hand(p);
        final Vec3 to = t != null ? center(t) : p.pick(26, 1.0f, false).getLocation();
        Anim.gather(l, "gold_spark", from, 0.9, 2, 4);
        int flight = Anim.fly(l, "cannon_shell", from, to);
        Anim.after(flight, () -> {
            Anim.emit(l, "blast_flash_small", to, 1, 0, 0);
            Anim.emit(l, "gold_spark", to, 24, 0.4, 0.3);
            Anim.emit(l, QFx.chips(Blocks.IRON_BLOCK), to, 16, 0.4, 0.2);
            Anim.emit(l, "white_spark", to, 8, 0.2, 0.3);
            Anim.expandingRing(l, "gold_spark", to, 1.5, 3, 3);
            if (t != null && t.isAlive() && !p.isRemoved()) {
                Abilities.hurt(p, t, 10 * power);
                Abilities.shove(t, Abilities.flat(p.getLookAngle()), 1.5, 0.3);
            }
        });
        Abilities.sound(p, SoundEvents.ANVIL_LAND, 1.8f);
    }

    public static void ironGuardians(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        Vec3 look = Abilities.flat(p.getLookAngle());
        Vec3 right = QFx.right(p);
        for (int s = -1; s <= 1; s += 2) {
            final Vec3 pos = p.position().add(look.scale(2)).add(right.scale(2.0 * s));
            final double y = p.getY();
            final float yaw = p.getYRot();
            Anim.expandingRing(l, "gold_spark", new Vec3(pos.x, y + 0.1, pos.z), 1.6, 5, 3);
            Anim.pillar(l, "gold_spark", new Vec3(pos.x, y, pos.z), 2.8, 0.6, 8, 5);
            Anim.after(10, () -> {
                IronGolem g = EntityType.IRON_GOLEM.create(l);
                if (g == null) return;
                g.moveTo(pos.x, y, pos.z, yaw, 0f);
                g.setPlayerCreated(true);
                l.addFreshEntity(g);
                Anim.emit(l, QFx.chips(Blocks.IRON_BLOCK), pos.add(0, 1.3, 0), 30, 0.8, 0.15);
                Anim.emit(l, "blast_flash_small", pos.add(0, 1.2, 0), 1, 0, 0);
                Scheduler.schedule(700, g::discard);
            });
        }
        Fx.lightning(l, p.position());
        Abilities.sound(p, SoundEvents.ANVIL_USE, 0.6f);
    }

    // ------------------------------------------------------------------ Invisibility

    public static void vanish(ServerPlayer p, double power) {
        Abilities.effect(p, MobEffects.INVISIBILITY, 200, 0);
        Abilities.effect(p, MobEffects.MOVEMENT_SPEED, 200, 1);
        ServerLevel l = Abilities.level(p);
        Vec3 c = p.position().add(0, 1, 0);
        Anim.shell(l, "shimmer", c, 1.3, 5, 10);
        Anim.emit(l, "shimmer", c, 40, 0.6, 0.12);
        Anim.emit(l, "white_spark", c, 10, 0.5, 0.15);
        Anim.expandingRing(l, "shimmer", QFx.feet(p), 2.0, 4, 3);
        Abilities.sound(p, SoundEvents.ENDER_EYE_LAUNCH, 1.8f);
    }

    public static void lightBendStrike(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        LivingEntity t = nearest(p, 9, 0.7);
        Vec3 start = p.position().add(0, 1, 0);
        Anim.emit(l, "shimmer", start, 20, 0.4, 0.1);
        if (t == null) {
            Anim.expandingRing(l, "shimmer", QFx.feet(p), 1.5, 3, 3);
            return;
        }
        Vec3 face = Abilities.flat(t.getLookAngle());
        Vec3 behind = t.position().subtract(face.scale(1.6));
        Anim.line(l, "shimmer", start, new Vec3(behind.x, t.getY() + 1, behind.z), 0.25);   // afterimage trail
        Anim.emit(l, "white_spark", start, 8, 0.3, 0.2);
        p.connection.teleport(behind.x, t.getY(), behind.z, t.getYRot(), 0f);
        Abilities.hurt(p, t, 11 * power);
        Vec3 c = center(t);
        Vec3 dir = c.subtract(p.getEyePosition());
        Anim.emit(l, "white_spark", p.position().add(0, 1, 0), 10, 0.3, 0.2);
        Anim.coneBurst(l, "white_spark", c, dir, 1.0, 0.7, 16);            // slash flare
        Anim.coneBurst(l, "shimmer", c, dir.scale(-1), 0.5, 0.8, 10);
        Anim.emit(l, ParticleTypes.SWEEP_ATTACK, c, 3, 0.3, 0);
        Abilities.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f);
    }

    public static void mirage(ServerPlayer p, double power) {
        Abilities.effect(p, MobEffects.INVISIBILITY, 300, 0);
        Abilities.effect(p, MobEffects.MOVEMENT_SPEED, 300, 2);
        ServerLevel l = Abilities.level(p);
        Vec3 c = p.position().add(0, 1, 0);
        for (LivingEntity t : Abilities.around(p, p.position(), 9)) {
            Abilities.effect(t, MobEffects.BLINDNESS, 160, 0);
            Abilities.effect(t, MobEffects.MOVEMENT_SLOWDOWN, 100, 1);
            Anim.wrap(l, "shimmer", t.position(), 2.0, 0.6, 0.6, 2, 8, 3);
        }
        for (int i = 0; i < 3; i++) {
            Anim.after(i * 3, () -> Anim.emit(l, "ice_wave", p.position().add(0, 1, 0), 1, 0, 0));
        }
        Anim.expandingRing(l, "shimmer", QFx.feet(p), 9, 8, 3);
        Anim.shell(l, "shimmer", c, 8, 6, 14);
        Fx.startAura(p, 300);
        Abilities.sound(p, SoundEvents.ENDER_DRAGON_GROWL, 1.9f);
    }

    // ------------------------------------------------------------------ Fierce Wings

    public static void windGale(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        Vec3 look = p.getLookAngle();
        for (LivingEntity t : Abilities.cone(p, 6, 0.5)) {
            Abilities.hurt(p, t, 3 * power);
            Abilities.shove(t, Abilities.flat(look), 1.6, 0.5);
        }
        p.setDeltaMovement(look.x * 1.4, 0.9, look.z * 1.4);
        p.hurtMarked = true;
        p.fallDistance = 0;
        Abilities.effect(p, MobEffects.SLOW_FALLING, 100, 0);
        QFx.wings(p, 10, 2.6);
        Vec3 chest = p.position().add(0, 1.2, 0);
        Anim.coneBurst(l, "feather", chest, look, 0.6, 0.5, 14);
        Anim.coneBurst(l, ParticleTypes.CLOUD, chest, look, 0.7, 0.3, 12);
        Anim.follow(p, 10, q -> {
            Vec3 b = q.position().add(0, 1.0, 0);
            Anim.emit(l, "feather", b, 2, 0.3, 0.03);
            Anim.emit(l, ParticleTypes.CLOUD, b, 1, 0.3, 0.02);
        });
        Anim.expandingRing(l, ParticleTypes.CLOUD, QFx.feet(p), 2.0, 4, 3);
        Abilities.sound(p, SoundEvents.ENDER_DRAGON_FLAP, 1.0f);
    }

    public static void featherBarrage(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        List<LivingEntity> targets = nearestN(p, 15, 0.85, 5);
        QFx.wings(p, 8, 2.4);
        if (targets.isEmpty()) {
            Vec3 ahead = p.getEyePosition().add(p.getLookAngle().scale(10));
            int ft = QFx.featherVolley(p, ahead, 8);
            Anim.after(ft, () -> QFx.featherImpact(l, ahead));
        }
        for (LivingEntity t : targets) {
            final Vec3 at = center(t);
            int ft = QFx.featherVolley(p, at, 8);
            Anim.after(ft, () -> {
                QFx.featherImpact(l, at);
                if (t.isAlive() && !p.isRemoved()) Abilities.hurt(p, t, 6 * power);
            });
        }
        Abilities.sound(p, SoundEvents.ARROW_SHOOT, 1.6f);
    }

    public static void soar(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        for (LivingEntity t : Abilities.around(p, p.position(), 5)) {
            Abilities.hurt(p, t, 8 * power);
            Vec3 away = t.position().subtract(p.position());
            Abilities.shove(t, away.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : away.normalize(), 1.4, 0.6);
        }
        Vec3 look = p.getLookAngle();
        p.setDeltaMovement(look.x * 0.8, 2.2, look.z * 0.8);
        p.hurtMarked = true;
        p.fallDistance = 0;
        Abilities.effect(p, MobEffects.SLOW_FALLING, 400, 0);
        Abilities.effect(p, MobEffects.MOVEMENT_SPEED, 400, 1);
        Abilities.effect(p, MobEffects.JUMP, 400, 2);
        QFx.wings(p, 24, 3.4);
        Anim.emit(l, "feather", p.position().add(0, 1, 0), 30, 0.8, 0.2);
        Anim.expandingRing(l, "feather", QFx.feet(p), 4, 5, 2);
        Anim.expandingRing(l, "red_spark", QFx.feet(p), 4, 5, 3);
        Anim.follow(p, 16, q -> Anim.emit(l, "feather", q.position().add(0, 0.5, 0), 2, 0.4, 0.03));
        Fx.startAura(p, 300);
        Abilities.sound(p, SoundEvents.ENDER_DRAGON_FLAP, 0.7f);
    }

    // ------------------------------------------------------------------ Dark Shadow

    public static void shadowJab(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        Vec3 look = p.getLookAngle();
        Vec3 from = QFx.hand(p);
        Vec3 tip = p.getEyePosition().add(look.scale(5));
        Anim.strand(l, "shadow_wisp", from, tip, 4, 0.5);                       // tendril lashing out
        for (LivingEntity t : Abilities.cone(p, 5.5, 0.7)) {
            Abilities.hurt(p, t, 9 * power);
            Abilities.effect(t, MobEffects.WITHER, 60, 0);
            Abilities.effect(t, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
            Abilities.shove(t, Abilities.flat(look), 1.2, 0.3);
            final Vec3 c = center(t);
            Anim.after(3, () -> {
                Anim.emit(l, "shadow_wisp", c, 10, 0.4, 0.08);
                Anim.emit(l, ParticleTypes.SQUID_INK, c, 14, 0.4, 0.1);
                Anim.coneBurst(l, "white_spark", c, look, 0.9, 0.9, 10);       // claw slashes
                Anim.emit(l, ParticleTypes.SWEEP_ATTACK, c, 2, 0.3, 0);
            });
        }
        Abilities.sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 0.6f);
    }

    public static void shadowVeil(ServerPlayer p, double power) {
        boolean dark = p.level().getMaxLocalRawBrightness(p.blockPosition()) < 8;
        Abilities.effect(p, MobEffects.DAMAGE_RESISTANCE, 300, dark ? 2 : 1);
        Abilities.effect(p, MobEffects.DAMAGE_BOOST, 300, dark ? 1 : 0);
        Abilities.effect(p, MobEffects.ABSORPTION, 300, 1);
        ServerLevel l = Abilities.level(p);
        Vec3 c = p.position().add(0, 1, 0);
        Anim.gather(l, "shadow_wisp", c, 2.5, 8, 3);
        Anim.after(8, () -> {
            Vec3 now = p.position().add(0, 1, 0);
            Anim.shell(l, "shadow_wisp", now, 1.3, 4, 8);
            Vec3 eyes = p.getEyePosition().add(p.getLookAngle().scale(0.3));
            Vec3 r = QFx.right(p);
            Anim.emit(l, "red_spark", eyes.add(r.scale(0.12)), 3, 0.02, 0);   // glowing eyes
            Anim.emit(l, "red_spark", eyes.add(r.scale(-0.12)), 3, 0.02, 0);
        });
        Fx.startAura(p, 300);
        Abilities.sound(p, SoundEvents.WITHER_SPAWN, 1.8f);
    }

    public static void rampage(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        Vec3 c = p.position();
        RandomSource r = p.getRandom();
        for (LivingEntity t : Abilities.around(p, c, 7)) {
            Abilities.hurt(p, t, 12 * power);
            Abilities.effect(t, MobEffects.WITHER, 100, 1);
            Vec3 away = t.position().subtract(c);
            Abilities.shove(t, away.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : away.normalize(), 1.6, 0.5);
        }
        Abilities.effect(p, MobEffects.DAMAGE_BOOST, 300, 2);
        Abilities.effect(p, MobEffects.MOVEMENT_SPEED, 300, 1);
        Anim.expandingRing(l, "shadow_wisp", c.add(0, 0.3, 0), 7, 6, 2);
        for (int i = 0; i < 8; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double dist = 1.5 + r.nextDouble() * 5;
            final Vec3 base = c.add(Math.cos(a) * dist, 0, Math.sin(a) * dist);
            Anim.after(r.nextInt(6), () -> Anim.pillar(l, "shadow_wisp", base, 4, 0.6, 5, 4));
        }
        Vec3 eyes = p.getEyePosition();
        Anim.emit(l, "red_spark", eyes, 12, 0.15, 0.05);
        Anim.emit(l, ParticleTypes.LARGE_SMOKE, c.add(0, 1, 0), 30, 1.5, 0.05);
        Fx.startAura(p, 300);
        Abilities.sound(p, SoundEvents.ENDER_DRAGON_GROWL, 0.6f);
    }

    // ------------------------------------------------------------------ Black Whip

    public static void lash(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        final LivingEntity t = nearest(p, 15, 0.92);
        Vec3 from = QFx.hand(p);
        final Vec3 end = t != null ? center(t) : from.add(p.getLookAngle().scale(10));
        Anim.strand(l, "whip_wisp", from, end, 4, 0.45);
        Anim.after(4, () -> {
            Anim.emit(l, "white_spark", end, 12, 0.2, 0.3);                    // whip crack
            Anim.emit(l, "whip_wisp", end, 8, 0.3, 0.08);
            if (t != null && t.isAlive() && !p.isRemoved()) {
                Abilities.hurt(p, t, 6 * power);
                pull(t, p, 1.5);
            }
        });
        Abilities.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f);
    }

    public static void tetherSwing(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        Vec3 from = QFx.hand(p);
        Vec3 anchor = Abilities.grappleTo(p, 30, 2.2);
        if (anchor == null) {
            Anim.strand(l, "whip_wisp", from, from.add(p.getLookAngle().scale(8)), 3, 0.45);
            return;
        }
        Anim.strand(l, "whip_wisp", from, anchor, 3, 0.45);
        Anim.emit(l, "whip_wisp", anchor, 6, 0.2, 0.05);
        Anim.after(3, () -> Anim.rope(p, "whip_cord", anchor, 10));
        Abilities.sound(p, SoundEvents.CROSSBOW_SHOOT, 0.9f);
    }

    public static void constrict(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        Vec3 from = QFx.hand(p);
        for (LivingEntity t : Abilities.around(p, p.position(), 8)) {
            Abilities.hurt(p, t, 8 * power);
            Abilities.effect(t, MobEffects.MOVEMENT_SLOWDOWN, 100, 3);
            Abilities.effect(t, MobEffects.WEAKNESS, 100, 0);
            final Vec3 tp = t.position();
            Anim.strand(l, "whip_wisp", from, center(t), 5, 0.45);
            Anim.after(5, () -> Anim.wrap(l, "whip_wisp", tp, 2.0, 0.8, 0.4, 3, 10, 3));
            pull(t, p, 1.1);
        }
        Anim.expandingRing(l, "whip_wisp", QFx.feet(p), 8, 6, 2);
        Anim.shell(l, "whip_wisp", p.position().add(0, 1, 0), 1.6, 5, 8);
        Abilities.sound(p, SoundEvents.WITHER_SHOOT, 0.7f);
    }

    // ------------------------------------------------------------------ Acid

    private static void pool(ServerPlayer p, Vec3 c, double radius, double damage, int pulses) {
        if (pulses <= 0) return;
        ServerLevel l = Abilities.level(p);
        RandomSource r = p.getRandom();
        Anim.emit(l, "acid_bubble", c.add(0, 0.2, 0), (int) (6 + radius * 2), radius * 0.5, 0.02);
        Anim.emit(l, "acid_drop", c.add(0, 1.8, 0), (int) (4 + radius), radius * 0.5, 0.0);
        Anim.emit(l, ParticleTypes.SMOKE, c.add(0, 0.3, 0), 2, radius * 0.4, 0.01);
        for (int i = 0; i < 6; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            Anim.emit(l, "acid_bubble", c.add(Math.cos(a) * radius, 0.15, Math.sin(a) * radius), 1, 0.05, 0.01);
        }
        for (LivingEntity t : Abilities.around(p, c, radius)) {
            Abilities.hurt(p, t, damage);
            Abilities.effect(t, MobEffects.POISON, 40, 0);
            Abilities.effect(t, MobEffects.MOVEMENT_SLOWDOWN, 30, 0);
        }
        Scheduler.schedule(10, () -> pool(p, c, radius, damage, pulses - 1));
    }

    public static void acidSpit(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        final LivingEntity t = nearest(p, 17, 0.93);
        Vec3 from = QFx.hand(p);
        final Vec3 to = t != null ? center(t) : p.pick(17, 1.0f, false).getLocation();
        int flight = Anim.fly(l, "acid_glob", from, to);
        Anim.after(flight, () -> {
            Anim.emit(l, "acid_drop", to, 20, 0.35, 0.12);
            Anim.emit(l, "acid_bubble", to, 12, 0.3, 0.06);
            Anim.emit(l, ParticleTypes.SMOKE, to, 6, 0.25, 0.02);
            Anim.expandingRing(l, "acid_bubble", to.add(0, -0.3, 0), 1.2, 3, 3);
            if (t != null && t.isAlive() && !p.isRemoved()) {
                Abilities.hurt(p, t, 6 * power);
                Abilities.effect(t, MobEffects.POISON, 100, 0);
                Abilities.effect(t, MobEffects.WEAKNESS, 100, 0);
            }
        });
        Abilities.sound(p, SoundEvents.SLIME_SQUISH, 0.8f);
    }

    public static void acidPool(ServerPlayer p, double power) {
        Vec3 c = p.position().add(Abilities.flat(p.getLookAngle()).scale(4));
        ServerLevel l = Abilities.level(p);
        Anim.expandingRing(l, "acid_bubble", c.add(0, 0.2, 0), 3.0, 4, 3);
        Anim.emit(l, "acid_drop", c.add(0, 1.0, 0), 16, 0.8, 0.1);
        Abilities.sound(p, SoundEvents.SLIME_SQUISH, 0.5f);
        pool(p, c, 3.0, 2.0 * power, 10);
    }

    public static void acidFlood(ServerPlayer p, double power) {
        Abilities.effect(p, MobEffects.MOVEMENT_SPEED, 300, 1);
        Abilities.effect(p, MobEffects.DAMAGE_RESISTANCE, 160, 0);
        ServerLevel l = Abilities.level(p);
        Vec3 c = p.position();
        Anim.expandingRing(l, "acid_bubble", c.add(0, 0.25, 0), 6.5, 8, 3);
        Anim.expandingRing(l, "acid_drop", c.add(0, 0.6, 0), 6.5, 8, 2);
        Anim.shell(l, "acid_bubble", c.add(0, 1, 0), 1.5, 5, 8);
        Abilities.sound(p, SoundEvents.SLIME_SQUISH, 0.4f);
        pool(p, c, 6.5, 3.0 * power, 16);
    }

    // ------------------------------------------------------------------ Earphone Jack

    public static void heartbeatPulse(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        Vec3 look = p.getLookAngle();
        for (LivingEntity t : Abilities.cone(p, 9, 0.8)) {
            Abilities.hurt(p, t, 6 * power);
            Abilities.effect(t, MobEffects.CONFUSION, 100, 0);
            Abilities.shove(t, Abilities.flat(look), 1.3, 0.3);
        }
        final Vec3 target = p.pick(9, 1.0f, false).getLocation();
        Vec3 ears = p.getEyePosition();
        Vec3 r = QFx.right(p);
        Anim.strand(l, "pink_wire", ears.add(r.scale(0.25)), target.add(0.3, 0, 0), 4, 0.3);   // earphone jacks
        Anim.strand(l, "pink_wire", ears.add(r.scale(-0.25)), target.add(-0.3, 0, 0), 4, 0.3);
        Anim.after(4, () -> {
            Anim.emit(l, "sound_wave", target.add(0, 0.5, 0), 1, 0, 0);
            Anim.emit(l, ParticleTypes.NOTE, target.add(0, 0.8, 0), 6, 0.6, 1);
            Anim.emit(l, ParticleTypes.SONIC_BOOM, target.add(0, 0.5, 0), 1, 0, 0);
        });
        Anim.after(7, () -> Anim.emit(l, "sound_wave", target.add(0, 0.5, 0), 1, 0, 0));
        Abilities.sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f);
    }

    public static void soundSense(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        Abilities.effect(p, MobEffects.NIGHT_VISION, 400, 0);
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24),
                x -> x != p && x.isAlive())) {
            Abilities.effect(e, MobEffects.GLOWING, 200, 0);
            final Vec3 at = center(e);
            double dist = at.distanceTo(p.position());
            Anim.after((int) Math.min(18, dist * 0.7), () -> {                    // ping arrives after the scan wave
                Anim.emit(l, "sound_wave", at, 1, 0, 0);
                Anim.emit(l, ParticleTypes.NOTE, at.add(0, 1, 0), 1, 0.3, 1);
            });
        }
        for (int i = 0; i < 3; i++) {
            Anim.after(i * 4, () -> Anim.emit(l, "sound_wave_big", p.position().add(0, 1, 0), 1, 0, 0));
        }
        Anim.expandingRing(l, "sound_wave", QFx.feet(p), 24, 18, 1);
        Abilities.sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f);
    }

    public static void heartbeatFuzz(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        Vec3 c = p.position();
        for (LivingEntity t : Abilities.around(p, c, 10)) {
            Abilities.hurt(p, t, 10 * power);
            Abilities.effect(t, MobEffects.CONFUSION, 160, 0);
            Abilities.effect(t, MobEffects.WEAKNESS, 160, 0);
            Vec3 away = t.position().subtract(c);
            Abilities.shove(t, away.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : away.normalize(), 1.6, 0.5);
        }
        Vec3 r = QFx.right(p);
        Vec3 ears = p.getEyePosition();
        Anim.strand(l, "pink_wire", ears.add(r.scale(0.25)), c.add(r.scale(2.2)), 3, 0.3);   // jacks plug into the ground
        Anim.strand(l, "pink_wire", ears.add(r.scale(-0.25)), c.add(r.scale(-2.2)), 3, 0.3);
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Anim.after(3 + i * 5, () -> {
                Anim.emit(l, "sound_wave_big", c.add(0, 1, 0), 1, 0, 0);
                Anim.expandingRing(l, "sound_wave", c.add(0, 0.3, 0), 10, 4, 2);
                Anim.emit(l, ParticleTypes.NOTE, c.add(0, 1.5, 0), 8 + k * 2, 3.0, 1);
                Anim.emit(l, ParticleTypes.SONIC_BOOM, c.add(r.scale(2.2)).add(0, 0.5, 0), 1, 0, 0);
                Anim.emit(l, ParticleTypes.SONIC_BOOM, c.add(r.scale(-2.2)).add(0, 0.5, 0), 1, 0, 0);
            });
        }
        Abilities.sound(p, SoundEvents.WARDEN_SONIC_BOOM, 1.2f);
    }

    // ------------------------------------------------------------------ Tape

    public static void tapeShot(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        final LivingEntity t = nearest(p, 15, 0.92);
        Vec3 from = QFx.hand(p);
        final Vec3 to = t != null ? center(t) : from.add(p.getLookAngle().scale(10));
        Anim.strand(l, "tape", from, to, 4, 0.3);                               // tape flies out from the arm
        if (t != null) {
            final Vec3 base = t.position();
            Anim.after(4, () -> {
                Anim.wrap(l, "tape", base, 2.0, 0.55, 0.45, 3, 6, 4);          // and wraps around the target
                if (t.isAlive() && !p.isRemoved()) {
                    Abilities.hurt(p, t, 4 * power);
                    Abilities.effect(t, MobEffects.MOVEMENT_SLOWDOWN, 80, 4);
                    pull(t, p, 0.7);
                }
            });
        }
        Abilities.sound(p, SoundEvents.SLIME_SQUISH, 1.6f);
    }

    public static void tapeSwing(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        Vec3 from = QFx.hand(p);
        Vec3 anchor = Abilities.grappleTo(p, 28, 2.0);
        if (anchor == null) {
            Anim.strand(l, "tape", from, from.add(p.getLookAngle().scale(8)), 3, 0.3);
            return;
        }
        Anim.strand(l, "tape", from, anchor, 3, 0.3);
        Anim.emit(l, "tape", anchor, 6, 0.2, 0.05);
        Anim.after(3, () -> Anim.rope(p, "tape_cord", anchor, 10));
        Abilities.sound(p, SoundEvents.CROSSBOW_SHOOT, 0.9f);
    }

    public static void tapeCocoon(ServerPlayer p, double power) {
        ServerLevel l = Abilities.level(p);
        Vec3 from = QFx.hand(p);
        for (LivingEntity t : Abilities.around(p, p.position(), 7.5)) {
            Abilities.hurt(p, t, 5 * power);
            Abilities.effect(t, MobEffects.MOVEMENT_SLOWDOWN, 100, 7);
            Abilities.effect(t, MobEffects.WEAKNESS, 100, 1);
            final Vec3 base = t.position();
            final Vec3 mid = center(t);
            Anim.strand(l, "tape", from, mid, 5, 0.3);
            Anim.after(5, () -> Anim.wrap(l, "tape", base, 2.2, 1.0, 0.3, 4, 12, 4));   // spiral tightens
            Anim.after(17, () -> Anim.shell(l, "tape", mid, 0.9, 4, 12));               // final cocoon
        }
        Abilities.effect(p, MobEffects.DAMAGE_RESISTANCE, 100, 0);
        Anim.expandingRing(l, "tape", QFx.feet(p), 7.5, 6, 2);
        Abilities.sound(p, SoundEvents.SLIME_SQUISH, 0.5f);
    }
}
