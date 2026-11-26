package com.plusultra.client;

import com.plusultra.core.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** One particle class for every custom particle; the Def decides tint, size, motion, glow, fade and trail. */
public class StyledParticle extends TextureSheetParticle {
    private final ModParticles.Def def;
    private final float baseSize;
    private final float phase;

    protected StyledParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                             SpriteSet sprites, ModParticles.Def def) {
        super(level, x, y, z);
        this.def = def;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        int span = def.lifeMax() - def.lifeMin();
        this.lifetime = def.lifeMin() + (span > 0 ? this.random.nextInt(span + 1) : 0);
        this.baseSize = def.sizeMin() + this.random.nextFloat() * (def.sizeMax() - def.sizeMin());
        this.quadSize = this.baseSize;
        this.gravity = def.gravity();
        this.friction = def.friction();
        this.hasPhysics = false;
        this.setColor(def.r(), def.g(), def.b());
        this.roll = this.random.nextFloat() * 6.2831855f;
        this.oRoll = this.roll;
        this.phase = this.random.nextFloat() * 6.2831855f;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        if (!this.isAlive()) return;

        float f = (float) this.age / (float) this.lifetime;
        if (def.spin() != 0f) this.roll += def.spin();
        if (def.flutter() != 0f) {
            this.xd += Math.sin(this.age * 0.45 + this.phase) * def.flutter();
            this.zd += Math.cos(this.age * 0.37 + this.phase) * def.flutter();
        }
        if (def.grow() != 0f) this.quadSize = this.baseSize * (1f + def.grow() * f);
        this.alpha = f < 0.7f ? 1f : Math.max(0f, (1f - f) / 0.3f);

        if (def.trail() != null && (this.age & 1) == 0) {
            this.level.addParticle(ModParticles.type(def.trail()), this.x, this.y, this.z, 0, 0, 0);
        }
    }

    @Override
    protected int getLightColor(float partialTick) {
        return def.glow() ? 0xF000F0 : super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final ModParticles.Def def;

        public Provider(SpriteSet sprites, ModParticles.Def def) {
            this.sprites = sprites;
            this.def = def;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new StyledParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, def);
        }
    }
}
