package work.nekow.particlecore.client.mixins;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.nekow.particlecore.client.particle.ParticleEnvData;
import work.nekow.particlecore.client.particle.ParticleManager;
import work.nekow.particlecore.math.ParticleColor;
import work.nekow.particlecore.utils.ParticlePath;
import work.nekow.particlecore.utils.ParticlePathData;

import java.util.ArrayList;

@Mixin(Particle.class)
public abstract class ParticleMixin {
    @Unique private int light = -1;

    @Shadow protected double velocityX;
    @Shadow protected double velocityY;
    @Shadow protected double velocityZ;
    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;
    @Shadow protected float red;
    @Shadow protected float green;
    @Shadow protected float blue;
    @Shadow protected float alpha;
    @Shadow protected int age;
    @Shadow protected int maxAge;
    @Shadow protected float angle;
    @Shadow protected float gravityStrength;
    @Shadow public abstract void markDead();

    @Shadow
    public abstract Particle scale(float scale);

    @Shadow
    public abstract void setVelocity(double velocityX, double velocityY, double velocityZ);

    @Unique
    private ParticlePathData particlePathData = null;

    @Unique
    private Double time = 0.0;

    @Inject(method = "tick", at = @At("HEAD"))
    public void pathTick(CallbackInfo ci) {
        var self = (Particle)(Object) this;
        if (particlePathData == null) {
            var data = ParticleManager.Companion.getData(self);
            if (data != null) {
                particlePathData = data.getPath();
            }
        }
        if (particlePathData == null) return;
        var path = particlePathData.getPath();
        time += particlePathData.getSpeed();
        if (path instanceof ParticlePath.EmptyPath) return;
        var pos = path.apply(time);
        setVelocity(pos.getX(), pos.getY(), pos.getZ());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        Particle self = (Particle)(Object) this;
        if (!ParticleManager.Companion.hasParticle(self)) {
            return;
        }
        if (age++ >= maxAge) {
            markDead();
            return;
        }

        ParticleEnvData data = ParticleManager.Companion.particleTick(
            self,
            new ParticleEnvData(
                new Vec3d(velocityX, velocityY, velocityZ),
                new Vec3d(x, y, z),
                red, green, blue, alpha, angle, new ArrayList<>(), light, gravityStrength, 1F
            )
        );
        ArrayList<String> prefixList = data.getPrefix();
        for (String prefix : prefixList) {
            setData(data, prefix);
        }
    }

    @Inject(method = "markDead", at = @At("TAIL"))
    public void markDead(CallbackInfo ci) {
        Particle self = (Particle)(Object) this;
        ParticleManager.Companion.removeParticle(self);
    }

    @Unique
    public void setData(ParticleEnvData data, String prefix) {
        Particle self = (Particle)(Object) this;
        Vec3d velocity = data.getVelocity();
        Vec3d position = data.getPosition();
        switch (prefix) {
            case "vx" -> velocityX = velocity.x;
            case "vy" -> velocityY = velocity.y;
            case "vz" -> velocityZ = velocity.z;
            case "cr" -> red = data.getRed();
            case "cg" -> green = data.getGreen();
            case "cb" -> blue = data.getBlue();
            case "ca" -> alpha = data.getAlpha();
            case "angle" -> angle = data.getAngle();
            case "light" -> {
                light = data.getLight();
                ParticleManager.Companion.setLight(self, light, position);
            }
            case "gravity" -> gravityStrength = data.getGravity();
            case "scale" -> scale(data.getScale());
        }
    }
}
