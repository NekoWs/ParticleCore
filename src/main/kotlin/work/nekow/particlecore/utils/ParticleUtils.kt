package work.nekow.particlecore.utils

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.network.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.round

@Suppress("unused")
class ParticleUtils {
    companion object {
        private val indexes = ConcurrentHashMap<Long, Int>()
        private val idCounter = AtomicLong(0)

        // 最大显示距离 (32(Blocks) * 16(Chunks))
        const val MAX_SHOW_DISTANCE = 32 * 16

        fun nextId(age: Int, delay: Double = 0.0): Long {
            val id = idCounter.get()
            if (id == Long.MAX_VALUE) {
                // TODO 清理会导致复用旧粒子 ID，当重复时可能会出现问题
                indexes.clear()
                idCounter.set(0)
            }
            idCounter.set(id + 1)
            indexes[id] = round(age * delay).toInt()
            return id
        }

        fun removeId(id: Long) {
            indexes.remove(id)
        }

        fun init() {
            ServerTickEvents.END_SERVER_TICK.register { server ->
                val remove = mutableListOf<Long>()
                indexes.forEach { (id, age) ->
                    if (age < 1) {
                        remove.add(id)
                    } else {
                        indexes[id] = age - 1
                    }
                }
                remove.forEach {
                    removeId(it)
                }
            }
        }

        /**
         * 使用发包方式召唤粒子效果
         */
        fun spawnParticles(
            world: ServerWorld,
            particles: List<ParticleBuilder>,
            delay: Int = 0,
            particleDelay: Double = 0.0
        ): Long? {
            if (particles.isEmpty()) return null
            val age = particles.maxOfOrNull { it.age } ?: 0
            val id = nextId(age)
            val packet = PacketParticlesS2C(particles, id, delay, particleDelay)
            world.players.forEach {
                // 取粒子第一个判断距离是否需要发送
                if (it.pos.distanceTo(particles.first().pos) < MAX_SHOW_DISTANCE)
                    ServerPlayNetworking.send(it, packet)
            }
            return id
        }

        fun spawnParticle(
            world: ServerWorld,
            particle: ParticleBuilder,
            delay: Int = 0
        ): Long {
            return spawnParticles(world, listOf(particle), delay)!!
        }

        fun spawnFunctionParticle(
            world: ServerWorld,
            particle: ParticleBuilder,
            function: String,
            step: Double = 1.0,
            delay: Double = 0.0,
            range: Pair<Double, Double> = Pair(0.0, 10.0),
        ): Long {
            val id = nextId(particle.age, delay)
            val packet = PacketFunctionParticlesS2C(
                particle = particle,
                function = function,
                step = step,
                delay = delay,
                range = range,
                id = id,
            )
            world.players.forEach {
                if (it.pos.distanceTo(particle.pos) < MAX_SHOW_DISTANCE)
                    ServerPlayNetworking.send(it, packet)
            }
            return id
        }

        /**
         * 发送粒子清除包
         *
         * @param world 世界
         * @param id ID
         */
        fun markDead(world: ServerWorld, id: Long) {
            val packet = PacketMarkDeadS2C(id)
            world.players.forEach {
                ServerPlayNetworking.send(it, packet)
            }
        }

        /**
         * 移动粒子以指定向量
         *
         * @param id ID
         * @param vector 向量
         */
        fun moveParticle(world: ServerWorld, id: Long, vector: Vec3d) {
            val packet = PacketMoveParticleS2C(id, vector)
            world.players.forEach {
                ServerPlayNetworking.send(it, packet)
            }
        }

        /**
         * 设置粒子以指定向量
         *
         * @param id ID
         * @param vector 向量
         */
        fun velocityParticle(world: ServerWorld, id: Long, vector: Vec3d) {
            val packet = PacketVelocityParticleS2C(id, vector)
            world.players.forEach {
                ServerPlayNetworking.send(it, packet)
            }
        }

        /**
         * 清除所有定时粒子
         */
        fun clearTickParticles(world: ServerWorld) {
            val packet = PacketRemoveTickParticlesS2C()
            world.players.forEach {
                ServerPlayNetworking.send(it, packet)
            }
        }
    }
}