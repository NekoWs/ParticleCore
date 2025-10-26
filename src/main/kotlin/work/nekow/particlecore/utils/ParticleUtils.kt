package work.nekow.particlecore.utils

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.math.FourierTerm
import work.nekow.particlecore.network.PacketFourierParticleS2C
import work.nekow.particlecore.network.PacketFourierParticleS2C.FPRotate
import work.nekow.particlecore.network.PacketFourierParticleS2C.FPScale
import work.nekow.particlecore.network.PacketFunctionParticlesS2C
import work.nekow.particlecore.network.PacketLineParticlesS2C
import work.nekow.particlecore.network.PacketMarkDeadS2C
import work.nekow.particlecore.network.PacketMoveParticleS2C
import work.nekow.particlecore.network.PacketParticleS2C
import work.nekow.particlecore.network.PacketRemoveTickParticlesS2C
import work.nekow.particlecore.network.PacketVelocityParticleS2C
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.round

class ParticleUtils {
    companion object {
        private val indexes = ConcurrentHashMap<Long, Int>()
        private var idCounter = AtomicLong(0)

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
        fun spawnParticle(
            world: ServerWorld,
            particle: ParticleBuilder
        ): Long {
            val id = nextId(particle.age)
            val packet = PacketParticleS2C(particle, id)
            world.players.forEach {
                ServerPlayNetworking.send(it, packet)
            }
            return id
        }

        fun spawnLineParticle(
            world: ServerWorld,
            particle: ParticleBuilder,
            from: Vec3d,
            to: Vec3d,
            step: Double = 1.0,
            delay: Double = 0.0
        ): Long {
            val id = nextId(particle.age, delay)
            val packet = PacketLineParticlesS2C(
                particle = particle,
                from = from,
                to = to,
                step = step,
                delay = delay,
                id = id,
            )
            world.players.forEach {
                ServerPlayNetworking.send(it, packet)
            }
            return id
        }

        /**
         * 使用发包方式召唤傅里叶级数粒子效果
         */
        fun spawnFourierParticle(
            world: ServerWorld,
            particle: ParticleBuilder,
            duration: Double,
            timeStep: Double,
            length: Int,
            terms: List<FourierTerm>,
            delay: Double,
            fscale: FPScale,
            rotate: FPRotate,
            particleDelay: Int = 0
        ): Long {
            val id = nextId(particle.age, delay)
            val packet = PacketFourierParticleS2C(
                particle = particle,
                duration = duration,
                timeStep = timeStep,
                length = length,
                terms = terms,
                id = id,
                delay = delay,
                fscale = fscale,
                rotate = rotate,
                particleDelay = particleDelay
            )
            world.players.forEach {
                ServerPlayNetworking.send(it, packet)
            }
            return id
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