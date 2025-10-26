package work.nekow.particlecore.client.mixins;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.nekow.particlecore.client.particle.ParticleManager;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Unique private static final int MAX_LIGHTMAP = LightmapTextureManager.pack(15, 15);

    @Inject(
            method = "getLightmapCoordinates(Lnet/minecraft/client/render/WorldRenderer$BrightnessGetter;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void getLightmapCoordinates(WorldRenderer.BrightnessGetter brightnessGetter, BlockRenderView world, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int particleLight = ParticleManager.Companion.getLight(pos);
        if (particleLight == -1) return;
        int originalLight = cir.getReturnValue();
        int i = brightnessGetter.packedBrightness(world, pos);
        int sky = LightmapTextureManager.getSkyLightCoordinates(i);
        int ret = LightmapTextureManager.pack(particleLight, sky);
        if (originalLight < ret) {
            cir.setReturnValue(Math.min(ret, MAX_LIGHTMAP));
        }
    }
}
