package com.plusultra.quirk;

import com.plusultra.config.PUConfig;
import com.plusultra.item.HeroCostume;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Gameplay + animation of the first six Quirks. Everything here runs on the logical server;
 * the visuals are built from Anim/QFx (custom particles) and are purely cosmetic.
 */
public final class Abilities {
    private Abilities() {}

    // ------------------------------------------------------------------ helpers

    public static ServerLevel level(ServerPlayer p) {
        return p.serverLevel();
    }

    public static boolean canHit(ServerPlayer p, Entity e) {
        if (!(e instanceof LivingEntity le) || e == p || !le.isAlive()) return false;
        if (le instanceof Player && !PUConfig.PVP.get()) return false;
        if (le instanceof TamableAnimal pet && pet.isOwnedBy(p)) return false;
        return true;
    }

    /** Living entities in front of the player within range; cosMin is the cosine of the half-angle. */
    public static List<LivingEntity> cone(ServerPlayer p, double range, double cosMin) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().inflate(range), x -> canHit(p, x))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > range) continue;
            if (d < 1.2 || to.normalize().dot(look) >= cosMin) out.add(e);
        }
        return out;
    }

    public static List<LivingEntity> around(ServerPlayer p, Vec3 center, double r) {
        return p.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(r),
                x -> canHit(p, x) && x.position().distanceTo(center) <= r);
    }

    public static Vec3 center(LivingEntity e) {
        return e.getBoundingBox().getCenter();
    }

    public static void hurt(ServerPlayer p, LivingEntity target, double amount) {
        double mult = PlayerData.get(p).damageMultiplier() * PUConfig.DAMAGE_MULT.get()
                * (HeroCostume.pieces(p) == 4 ? 1.1 : 1.0);
        target.hurt(p.damageSources().playerAttack(p), (float) (amount * mult));
    }

    public static void shove(LivingEntity target, Vec3 dir, double horizontal, double vertical) {
        target.push(dir.x * horizontal, vertical, dir.z * horizontal);
        target.hurtMarked = true;
    }

    public static Vec3 flat(Vec3 v) {
        return new Vec3(v.x, 0, v.z).normalize();
    }

    public static void dash(ServerPlayer p, double speed, double lift) {
        Vec3 l = p.getLookAngle();
        p.setDeltaMovement(l.x * speed, l.y * speed * 0.6 + lift, l.z * speed);
        p.hurtMarked = true;
        p.fallDistance = 0;
    }

    public static void particles(ServerPlayer p, ParticleOptions type, Vec3 at, int count, double spread, double speed) {
        level(p).sendParticles(type, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    public static void sound(ServerPlayer p, SoundEvent s, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, 1.0f, pitch);
    }

    public static void effect(LivingEntity e, Holder<MobEffect> eff, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(eff, ticks, amp));
    }

    /** Damage, knockback, sound and optional block damage of an explosion at a point. */
    public static void blastDamage(ServerPlayer p, Vec3 at, double radius, double damage) {
        ServerLevel l = level(p);
        l.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.5f, 0.6f);
        l.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6f, 1.6f);
        for (LivingEntity t : around(p, at, radius)) {
            hurt(p, t, damage);
            Vec3 away = t.position().subtract(at);
            shove(t, away.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : away.normalize(), 1.0, 0.5);
        }
        if (PUConfig.BLOCK_DAMAGE.get()) {
            l.explode(p, at.x, at.y, at.z, (float) (radius * 0.6), Level.ExplosionInteraction.MOB);
        }
    }

    /** Launches the player toward the block they look at (no visuals). Returns the anchor point, or null. */
    public static Vec3 grappleTo(ServerPlayer p, double range, double speed) {
        HitResult hit = p.pick(range, 1.0f, false);
        if (hit.getType() == HitResult.Type.MISS) return null;
        Vec3 to = hit.getLocation();
        Vec3 from = p.position().add(0, 1.0, 0);
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1.5) return null;
        Vec3 v = d.normalize().scale(Math.min(speed, 0.6 + len * 0.08));
        p.setDeltaMovement(v.x, v.y + 0.25, v.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        return to;
    }

    /** Pulls the player toward the block they are looking at, with a plain dust rope (Grappling Hook item). */
    public static boolean grapple(ServerPlayer p, double range, double speed) {
        Vec3 to = grappleTo(p, range, speed);
        if (to == null) return false;
        Fx.beam(level(p), Fx.dust(0xDDDDDD, 0.9f), p.getEyePosition().add(0, -0.4, 0), to, 0.5);
        sound(p, SoundEvents.CROSSBOW_SHOOT, 0.9f);
        return true;
    }

    /** Blue exhaust jetting out of both ankles, opposite to the look direction. */
    private static void exhaust(ServerPlayer q) {
        ServerLevel l = q.serverLevel();
        Vec3 look = q.getLookAngle();
        Vec3 back = new Vec3(-look.x, -0.15, -look.z);
        Vec3 r = QFx.right(q);
        for (int s = -1; s <= 1; s += 2) {
            Vec3 ankle = q.position().add(r.scale(0.22 * s)).add(0, 0.2, 0);
            Anim.coneBurst(l, "blue_spark", ankle, back, 0.7, 0.25, 5);
            Anim.emit(l, ParticleTypes.CLOUD, ankle, 1, 0.08, 0.03);
        }
    }

    // ------------------------------------------------------------------ One For All

    public static void smash(ServerPlayer p, double power) {
        ServerLevel l = level(p);
        Vec3 look = p.getLookAngle();
        Vec3 fist = QFx.hand(p);
        Vec3 pt = p.getEyePosition().add(look.scale(2.5));
        for (LivingEntity t : cone(p, 4.5, 0.6)) {
            hurt(p, t, 9 * power);
            shove(t, flat(look), 1.8, 0.45);
            QFx.ofaStrike(l, center(t));
        }
        Anim.bolt(l, fist, pt, "green_bolt", "white_spark", 4, 0.5);
        Anim.bolt(l, fist, pt.add(0.4, 0.3, -0.2), "green_bolt", "green_spark", 3, 0.7);
        QFx.ofaStrike(l, pt);
        Anim.emit(l, "green_wave", pt, 1, 0, 0);
        Anim.expandingRing(l, "green_spark", p.position().add(0, 0.15, 0), 4.0, 5, 3);
        particles(p, ParticleTypes.EXPLOSION, pt, 1, 0, 0);
        sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 0.7f);
        sound(p, SoundEvents.ANVIL_LAND, 1.4f);
    }

    public static void delawareLeap(ServerPlayer p, double power) {
        ServerLevel l = level(p);
        Vec3 feet = QFx.feet(p);
        Anim.emit(l, "green_wave", feet, 1, 0, 0);
        Anim.expandingRing(l, "green_spark", feet, 2.8, 4, 3);
        Anim.emit(l, "green_spark", feet, 30, 0.4, 0.3);
        Anim.arcs(l, feet.add(0, 1, 0), 0.6, 4, "green_bolt", "white_spark");
        p.setDeltaMovement(p.getLookAngle().x * 1.0, 1.4 + 0.05 * power, p.getLookAngle().z * 1.0);
        p.hurtMarked = true;
        p.fallDistance = 0;
        effect(p, MobEffects.SLOW_FALLING, 80, 0);
        Anim.follow(p, 14, q -> {
            Vec3 at = q.position().add(0, 0.6, 0);
            Anim.emit(l, "green_spark", at, 6, 0.25, 0.05);
            Anim.emit(l, "white_spark", at, 2, 0.2, 0.05);
            if (q.tickCount % 3 == 0) Anim.arcs(l, at.add(0, 0.4, 0), 0.5, 1, "green_bolt", "white_spark");
        });
        sound(p, SoundEvents.ENDER_DRAGON_FLAP, 1.2f);
    }

    public static void fullCowl(ServerPlayer p, double power) {
        int t = 300;
        effect(p, MobEffects.DAMAGE_BOOST, t, 2);
        effect(p, MobEffects.MOVEMENT_SPEED, t, 1);
        effect(p, MobEffects.DAMAGE_RESISTANCE, t, 1);
        effect(p, MobEffects.JUMP, t, 1);
        ServerLevel l = level(p);
        Vec3 feet = QFx.feet(p);
        Fx.lightning(l, p.position());
        Anim.wrap(l, "green_spark", feet, 2.4, 0.9, 0.5, 3, 10, 5);
        Anim.wrap(l, "white_spark", feet, 2.4, 0.9, 0.5, -3, 10, 2);
        Anim.emit(l, "green_wave", p.position().add(0, 1, 0), 1, 0, 0);
        for (int i = 0; i < 6; i++) {
            Anim.after(i, () -> Anim.arcs(l, p.position().add(0, 1, 0), 0.8, 2, "green_bolt", "white_spark"));
        }
        Fx.startAura(p, t);
        sound(p, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.8f);
    }

    // ------------------------------------------------------------------ Half-Cold Half-Hot

    public static void flameBurst(ServerPlayer p, double power) {
        ServerLevel l = level(p);
        Vec3 look = p.getLookAngle();
        Vec3 from = QFx.hand(p);
        for (LivingEntity t : cone(p, 7, 0.75)) {
            hurt(p, t, 6 * power);
            t.setRemainingFireTicks(100);
        }
        Anim.coneBurst(l, ParticleTypes.FLAME, from, look, 0.9, 0.18, 28);
        Anim.coneBurst(l, "blast_spark", from, look, 0.8, 0.22, 16);
        Anim.coneBurst(l, "blast_smoke", from.add(look), look, 0.15, 0.3, 4);
        Anim.emit(l, "blast_flash_small", from.add(look.scale(0.8)), 1, 0, 0);
        sound(p, SoundEvents.FIRECHARGE_USE, 1.0f);
    }

    public static void iceBurst(ServerPlayer p, double power) {
        ServerLevel l = level(p);
        Vec3 look = p.getLookAngle();
        Vec3 from = QFx.hand(p);
        for (LivingEntity t : cone(p, 7, 0.75)) {
            hurt(p, t, 5 * power);
            t.setTicksFrozen(200);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 100, 2);
        }
        Anim.coneBurst(l, "ice_shard", from, look, 0.9, 0.2, 20);
        Anim.coneBurst(l, ParticleTypes.SNOWFLAKE, from, look, 0.7, 0.25, 20);
        Anim.coneBurst(l, "white_spark", from, look, 0.6, 0.2, 8);
        Anim.emit(l, "ice_wave", from.add(look.scale(0.8)), 1, 0, 0);
        sound(p, SoundEvents.GLASS_BREAK, 0.6f);
    }

    public static void flashfreezeHeatwave(ServerPlayer p, double power) {
        Vec3 c = p.position();
        ServerLevel l = level(p);
        RandomSource r = p.getRandom();
        for (LivingEntity t : around(p, c, 9)) {
            hurt(p, t, 10 * power);
            t.setRemainingFireTicks(120);
            t.setTicksFrozen(300);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 120, 3);
            Vec3 away = t.position().subtract(c);
            shove(t, away.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : away.normalize(), 1.2, 0.4);
        }
        Anim.emit(l, "blast_wave", c.add(0, 1, 0), 1, 0, 0);
        Anim.after(2, () -> Anim.emit(l, "ice_wave", c.add(0, 1, 0), 1, 0, 0));
        Anim.expandingRing(l, "blast_spark", c.add(0, 0.3, 0), 8, 8, 3);
        Anim.after(2, () -> Anim.expandingRing(l, "ice_shard", c.add(0, 0.3, 0), 8, 8, 2));
        for (int i = 0; i < 14; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double dist = 2 + r.nextDouble() * 6;
            final Vec3 base = c.add(Math.cos(a) * dist, 0, Math.sin(a) * dist);
            final boolean ice = i % 2 == 0;
            Anim.after(1 + r.nextInt(8), () -> {
                if (ice) {
                    Anim.pillar(l, "ice_shard", base, 2.5, 0.3, 4, 3);
                    Anim.emit(l, "white_spark", base.add(0, 2, 0), 6, 0.3, 0.1);
                } else {
                    Anim.pillar(l, "blast_spark", base, 2.5, 0.3, 4, 3);
                    Anim.emit(l, ParticleTypes.FLAME, base.add(0, 1, 0), 8, 0.3, 0.05);
                }
            });
        }
        sound(p, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f);
    }

    // ------------------------------------------------------------------ Explosion

    public static void blast(ServerPlayer p, double power) {
        ServerLevel l = level(p);
        Vec3 at = p.getEyePosition().add(p.getLookAngle().scale(3.0));
        Vec3 palm = QFx.hand(p);
        Anim.emit(l, "blast_spark", palm, 18, 0.15, 0.2);
        Anim.emit(l, "white_spark", palm, 6, 0.1, 0.2);
        sound(p, SoundEvents.FIRECHARGE_USE, 1.6f);
        Anim.after(2, () -> {
            QFx.mhaBlast(l, at, 2.5);
            blastDamage(p, at, 2.5, 7 * power);
        });
    }

    public static void blastPropulsion(ServerPlayer p, double power) {
        ServerLevel l = level(p);
        Vec3 look = p.getLookAngle();
        Vec3 r = QFx.right(p);
        Vec3 eye = p.getEyePosition();
        QFx.miniBlast(l, QFx.hand(p));
        QFx.miniBlast(l, eye.add(look.scale(0.6)).add(r.scale(-0.35)).add(0, -0.45, 0));
        dash(p, 1.8, 0.2);
        Anim.follow(p, 10, q -> {
            Vec3 back = q.position().add(0, 0.9, 0).subtract(q.getLookAngle().scale(0.8));
            QFx.miniBlast(l, back);
        });
        sound(p, SoundEvents.FIRECHARGE_USE, 1.2f);
    }

    public static void howitzerImpact(ServerPlayer p, double power) {
        ServerLevel l = level(p);
        Vec3 at = p.pick(20, 1.0f, false).getLocation();
        Anim.gather(l, "blast_spark", QFx.hand(p), 1.8, 8, 4);
        sound(p, SoundEvents.FIRECHARGE_USE, 0.6f);
        Anim.after(8, () -> {
            if (p.isRemoved()) return;
            Anim.strand(l, "blast_spark", QFx.hand(p), at, 3, 0.4);
            QFx.mhaBlast(l, at, 5.5);
            blastDamage(p, at, 5.5, 14 * power);
            Anim.after(2, () -> QFx.mhaBlast(l, at.add(1.5, 0, 1), 3));
            Anim.after(4, () -> QFx.mhaBlast(l, at.add(-1.5, 0, -1), 3));
        });
    }

    // ------------------------------------------------------------------ Hardening

    public static void harden(ServerPlayer p, double power) {
        effect(p, MobEffects.DAMAGE_RESISTANCE, 400, 1);
        effect(p, MobEffects.ABSORPTION, 400, 1);
        effect(p, MobEffects.MOVEMENT_SLOWDOWN, 400, 0);
        ServerLevel l = level(p);
        Vec3 c = p.position().add(0, 1, 0);
        Anim.shell(l, "red_spark", c, 1.2, 6, 8);
        Anim.emit(l, QFx.chips(Blocks.STONE), c, 30, 0.5, 0.15);
        Anim.emit(l, "white_spark", c, 10, 0.5, 0.1);
        Fx.startAura(p, 400);
        sound(p, SoundEvents.ANVIL_LAND, 0.8f);
    }

    public static void redRiotPunch(ServerPlayer p, double power) {
        ServerLevel l = level(p);
        Vec3 look = p.getLookAngle();
        Vec3 from = QFx.hand(p);
        RandomSource r = p.getRandom();
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Anim.after(i * 2, () -> Anim.coneBurst(l, "red_spark", from.add(0, (k - 1) * 0.15, 0), look, 1.4, 0.05, 6));
        }
        List<LivingEntity> hit = cone(p, 3.5, 0.8);
        if (!hit.isEmpty()) {
            LivingEntity t = hit.get(0);
            hurt(p, t, 12 * power);
            shove(t, flat(look), 1.2, 0.3);
            final Vec3 c = center(t);
            for (int i = 0; i < 3; i++) {
                Anim.after(i * 2, () -> {
                    Vec3 at = c.add((r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.4);
                    Anim.emit(l, "white_spark", at, 8, 0.3, 0.25);
                    Anim.emit(l, "red_spark", at, 10, 0.3, 0.3);
                    Anim.emit(l, QFx.chips(Blocks.STONE), at, 8, 0.3, 0.2);
                });
            }
        }
        sound(p, SoundEvents.PLAYER_ATTACK_CRIT, 0.8f);
    }

    public static void unbreakable(ServerPlayer p, double power) {
        effect(p, MobEffects.DAMAGE_RESISTANCE, 200, 3);
        effect(p, MobEffects.DAMAGE_BOOST, 200, 1);
        effect(p, MobEffects.ABSORPTION, 200, 3);
        ServerLevel l = level(p);
        Vec3 c = p.position().add(0, 1, 0);
        Anim.shell(l, "red_spark", c, 1.5, 6, 10);
        Anim.expandingRing(l, "red_spark", QFx.feet(p), 2.5, 4, 3);
        Anim.emit(l, QFx.chips(Blocks.STONE), QFx.feet(p), 40, 1.2, 0.25);
        Anim.emit(l, "white_spark", c, 16, 0.6, 0.2);
        Fx.lightning(l, p.position());
        Fx.startAura(p, 200);
        sound(p, SoundEvents.ANVIL_LAND, 0.5f);
    }

    // ------------------------------------------------------------------ Zero Gravity

    public static void floatTargets(ServerPlayer p, double power) {
        Vec3 c = p.getEyePosition().add(p.getLookAngle().scale(4));
        ServerLevel l = level(p);
        for (LivingEntity t : around(p, c, 5)) {
            effect(t, MobEffects.LEVITATION, 80, 0);
            Anim.wrap(l, "zero_orb", t.position(), 2.5, 0.6, 0.3, 3, 12, 3);
        }
        Anim.expandingRing(l, "zero_orb", c.add(0, 0.2, 0), 5, 6, 3);
        Anim.shell(l, "zero_orb", c, 5, 6, 10);
        Anim.emit(l, "zero_orb", c, 40, 2.5, 0.04);
        sound(p, SoundEvents.ENDER_EYE_LAUNCH, 1.2f);
    }

    public static void weightless(ServerPlayer p, double power) {
        effect(p, MobEffects.SLOW_FALLING, 200, 0);
        effect(p, MobEffects.JUMP, 200, 3);
        effect(p, MobEffects.MOVEMENT_SPEED, 200, 0);
        ServerLevel l = level(p);
        Anim.wrap(l, "zero_orb", QFx.feet(p), 3.0, 0.9, 0.4, 3, 10, 4);
        Anim.expandingRing(l, "zero_orb", QFx.feet(p), 2.0, 4, 3);
        Anim.follow(p, 12, q -> Anim.emit(l, "zero_orb", q.position().add(0, 0.3, 0), 3, 0.4, 0.03));
        Fx.startAura(p, 200);
        sound(p, SoundEvents.ENDER_EYE_LAUNCH, 1.6f);
    }

    public static void meteorShower(ServerPlayer p, double power) {
        Vec3 c = p.position();
        ServerLevel l = level(p);
        RandomSource r = p.getRandom();
        for (LivingEntity t : around(p, c, 9)) {
            hurt(p, t, 4 * power);
            effect(t, MobEffects.LEVITATION, 100, 1);
            effect(t, MobEffects.GLOWING, 200, 0);
        }
        Anim.shell(l, "zero_orb", c.add(0, 1, 0), 9, 6, 8);
        for (int i = 0; i < 14; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double dist = 2 + r.nextDouble() * 7;
            final Vec3 land = c.add(Math.cos(a) * dist, 0.1, Math.sin(a) * dist);
            Anim.after(r.nextInt(28), () -> {
                int flight = Anim.fly(l, "meteor", land.add(5, 12, 3), land);
                Anim.after(flight, () -> {
                    Anim.emit(l, "blast_flash_small", land.add(0, 0.5, 0), 1, 0, 0);
                    Anim.emit(l, "zero_orb", land.add(0, 0.4, 0), 18, 0.3, 0.15);
                    Anim.emit(l, "blast_spark", land.add(0, 0.4, 0), 10, 0.2, 0.2);
                    Anim.emit(l, ParticleTypes.FLAME, land.add(0, 0.3, 0), 6, 0.2, 0.05);
                    Anim.expandingRing(l, "zero_orb", land.add(0, 0.2, 0), 1.6, 3, 3);
                });
            });
        }
        sound(p, SoundEvents.ENDER_DRAGON_GROWL, 1.8f);
    }

    // ------------------------------------------------------------------ Engine

    public static void reciproBurst(ServerPlayer p, double power) {
        effect(p, MobEffects.MOVEMENT_SPEED, 120, 3);
        ServerLevel l = level(p);
        Anim.emit(l, "blue_wave", QFx.feet(p), 1, 0, 0);
        Anim.arcs(l, p.position().add(0, 0.5, 0), 0.6, 3, "blue_bolt", "blue_spark");
        exhaust(p);
        Anim.follow(p, 8, Abilities::exhaust);
        Fx.startAura(p, 120);
        sound(p, SoundEvents.FIRECHARGE_USE, 1.6f);
    }

    public static void engineKick(ServerPlayer p, double power) {
        ServerLevel l = level(p);
        Vec3 look = p.getLookAngle();
        for (LivingEntity t : cone(p, 5, 0.7)) {
            hurt(p, t, 8 * power);
            shove(t, flat(look), 1.4, 0.35);
            Vec3 c = center(t);
            Anim.emit(l, "white_spark", c, 12, 0.3, 0.3);
            Anim.emit(l, "blue_spark", c, 18, 0.3, 0.3);
            Anim.emit(l, "blue_wave", c, 1, 0, 0);
            Anim.bolt(l, p.position().add(0, 0.9, 0), c, "blue_bolt", "blue_spark", 3, 0.4);
        }
        Anim.coneBurst(l, "blue_spark", QFx.hand(p), look, 1.2, 0.1, 16);
        dash(p, 1.6, 0.1);
        Anim.follow(p, 8, Abilities::exhaust);
        sound(p, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f);
    }

    public static void reciproExtend(ServerPlayer p, double power) {
        effect(p, MobEffects.MOVEMENT_SPEED, 240, 4);
        effect(p, MobEffects.JUMP, 240, 1);
        effect(p, MobEffects.DIG_SPEED, 240, 1);
        ServerLevel l = level(p);
        Fx.lightning(l, p.position());
        Anim.emit(l, "blue_wave", p.position().add(0, 1, 0), 1, 0, 0);
        Anim.after(3, () -> Anim.emit(l, "blue_wave", p.position().add(0, 1, 0), 1, 0, 0));
        Anim.shell(l, "blue_spark", p.position().add(0, 1, 0), 1.4, 6, 8);
        for (int i = 0; i < 8; i++) {
            Anim.after(i, () -> Anim.arcs(l, p.position().add(0, 1, 0), 0.8, 2, "blue_bolt", "blue_spark"));
        }
        Anim.follow(p, 14, Abilities::exhaust);
        Fx.startAura(p, 240);
        sound(p, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0f);
    }
}
