package work.nekow.particlecore.client.mixins;

import com.google.common.collect.EvictingQueue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.particle.ParticleGroup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.nekow.particlecore.client.ParticlecoreClient;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Shadow @Final private Queue<Particle> newParticles;
    @Shadow @Final private Map<ParticleTextureSheet, Queue<Particle>> particles;
    @Shadow protected abstract void addTo(ParticleGroup group, int count);

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Queue;poll()Ljava/lang/Object;"))
    private Object removeParticleLimit(Queue<Object> queue) {
        Particle particle;
        while ((particle = newParticles.poll()) != null) {
            Queue<Particle> queue1 = particles.computeIfAbsent(
                particle.getType(),
                sheet -> EvictingQueue.create(ParticlecoreClient.MAX_PARTICLES)
            );
            if (queue1.size() == ParticlecoreClient.MAX_PARTICLES) {
                Particle overflow = queue1.poll();
                if (overflow != null) {
                    evict(overflow);
                }
            }
            queue1.add(particle);
        }
        return null;
    }

    @Inject(method = "clearParticles", at = @At("HEAD"))
    private void clearParticles(CallbackInfo ci) {
        particles.values().forEach(queue -> queue.forEach(this::evict));
        newParticles.forEach(this::evict);
    }

    @Unique
    private void evict(Particle particle) {
        if (particle.isAlive()) {
            particle.markDead();
            particle.getGroup().ifPresent(group -> addTo(group, -1));
        }
    }
}
