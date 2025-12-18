package work.nekow.particlecore.utils

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.world.ServerWorld
import work.nekow.particlecore.network.ClearDelayParticlesS2C
import work.nekow.particlecore.network.FunctionParticlesS2C
import work.nekow.particlecore.network.ParticlesS2C

@Suppress("unused")
class ParticleUtils {
    companion object {
        // 最大显示距离 (32(Blocks) * 16(Chunks))
        const val MAX_SHOW_DISTANCE = 32 * 16

        /**
         * 使用发包方式召唤粒子效果
         */
        fun spawnParticles(
            world: ServerWorld,
            particles: List<ParticleBuilder>,
            delay: Int = 0,
            particleDelay: Double = 0.0
        ) {
            if (particles.isEmpty()) return
            val age = particles.maxOfOrNull { it.age } ?: 0
            val packet = ParticlesS2C(particles, delay, particleDelay)
            world.players.forEach {
                // 取粒子第一个判断距离是否需要发送
                if (it.pos.distanceTo(particles.first().pos) < MAX_SHOW_DISTANCE)
                    ServerPlayNetworking.send(it, packet)
            }
        }

        fun spawnParticle(
            world: ServerWorld,
            particle: ParticleBuilder,
            delay: Int = 0
        ) {
            spawnParticles(world, listOf(particle), delay)
        }

        fun spawnFunctionParticle(
            world: ServerWorld,
            particle: ParticleBuilder,
            function: String,
            step: Double = 1.0,
            delay: Double = 0.0,
            range: Pair<Double, Double> = Pair(0.0, 10.0),
        ) {
            val packet = FunctionParticlesS2C(
                particle = particle,
                function = function,
                step = step,
                delay = delay,
                range = range
            )
            world.players.forEach {
                if (it.pos.distanceTo(particle.pos) < MAX_SHOW_DISTANCE)
                    ServerPlayNetworking.send(it, packet)
            }
        }

        /**
         * 清除所有定时粒子
         */
        fun clearTickParticles(world: ServerWorld) {
            val packet = ClearDelayParticlesS2C()
            world.players.forEach {
                ServerPlayNetworking.send(it, packet)
            }
        }
    }
}