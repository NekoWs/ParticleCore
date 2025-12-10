package work.nekow.particlecore.canvas.utils

import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.utils.ParticleBuilder
import work.nekow.particlecore.utils.ParticleUtils

@Suppress("unused")
data class ParticleBuilders(
    val particles: List<ParticleBuilder>
) {
    /**
     * 批量移动并召唤粒子效果
     */
    fun spawnAt(
        world: ServerWorld,
        pos: Vec3d,
        delay: Int = 0,
        particleDelay: Double = 0.0
    ) {
        val move = particles.map { particle ->
            particle.clone()
                .pos(particle.pos.add(pos))
                .rotation {
                    center(center.add(pos))
                }
        }
        ParticleUtils.spawnParticles(
            world, move, delay, particleDelay
        )
    }
}