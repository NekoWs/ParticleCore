package work.nekow.particlecore.client.mixins;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.nekow.particlecore.client.particle.ParticleManager;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity> {
    @Inject(method = "getBlockLight", at = @At("RETURN"), cancellable = true)
    private void getBlockLight(T entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int particleLight = ParticleManager.Companion.getLight(pos);
        if (particleLight == -1) return;
        int original = cir.getReturnValue();
        if (original < particleLight) {
            cir.setReturnValue(particleLight);
        }
    }
}
