package work.nekow.particlecore.client.particle

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.particle.Particle
import net.minecraft.client.world.ClientWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.joml.Matrix4f
import work.nekow.particlecore.client.ParticlecoreClient
import work.nekow.particlecore.client.ParticlecoreClient.Companion.MAX_PARTICLES
import work.nekow.particlecore.client.ParticlecoreClient.Companion.client
import work.nekow.particlecore.math.ParticleColor
import work.nekow.particlecore.utils.ParticleEnv
import work.nekow.particlecore.utils.ParticleEnvData
import work.nekow.particlecore.utils.RotationData
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.random.Random

@Environment(EnvType.CLIENT)
@Suppress("unused")
class ParticleManager {
    companion object {
        val delayParticles = LinkedList<MutableList<ParticleSpawnData>>()
        val particles = ConcurrentHashMap<Particle, ParticleStatus>()

        private val posPool = SynchronizedPool(128) { BlockPos.Mutable() }
        private fun acquireMutablePos() = posPool.acquire() ?: BlockPos.Mutable()
        private fun releaseMutablePos(pos: BlockPos.Mutable) = posPool.release(pos)

        private val lighting = Long2IntOpenHashMap().apply { defaultReturnValue(-1) }
        private val cachedLight = Long2IntOpenHashMap().apply { defaultReturnValue(-1) }

        private val updateLightPoses = HashSet<Long>()

        private val lightUpdate = AtomicBoolean(false)

        fun flagLightUpdate() =
            lightUpdate.set(true)

        fun hasParticle(particle: Particle): Boolean =
            particles.containsKey(particle)

        /**
         * 标记一个需要更新亮度的坐标
         *
         * @param pos 坐标
         */
        fun posLightUpdate(pos: BlockPos) =
            updateLightPoses.add(pos.asLong())

        /**
         * 获取粒子数据
         *
         * @param particle 粒子
         * @return 粒子数据
         */
        operator fun get(particle: Particle): ParticleStatus? =
            particles[particle]

        /**
         * 获取粒子位置
         *
         * @param particle 粒子
         * @return 粒子位置
         */
        fun getPos(particle: Particle): Vec3d? =
            get(particle)?.pos

        /**
         * 获取粒子运行数据
         *
         * @param particle 粒子
         * @return 粒子运行数据
         */
        fun getEnv(particle: Particle): ParticleEnv? =
            get(particle)?.env

        /**
         * 获取粒子亮度
         *
         * @param particle 粒子
         * @return 粒子亮度
         */
        fun getLight(particle: Particle): Int? =
            get(particle)?.light

        /**
         * 设置粒子亮度
         *
         * @param particle 粒子
         * @param light 亮度
         * @throws NullPointerException 粒子数据不存在
         */
        fun setLight(particle: Particle, light: Int, pos: Vec3d) =
            get(particle)?.let { data ->
                if (data.light != light || data.pos != pos) {
                    data.light = light
                    flagLightUpdate()
                }
            }

        /**
         * 设置粒子位置
         *
         * @param particle 粒子
         * @param pos 粒子位置
         * @throws NullPointerException 粒子数据不存在
         */
        fun setPos(particle: Particle, pos: Vec3d) {
            get(particle)!!.pos = pos
        }

        /**
         * 设置粒子数据
         *
         * @param particle 粒子
         * @param env 粒子数据
         */
        fun setEnv(particle: Particle, env: ParticleEnv) {
            get(particle)?.env = env
        }

        /**
         * 获取粒子世界
         *
         * @param particle 粒子
         * @return 世界
         */
        fun getWorld(particle: Particle): World? =
            get(particle)?.world

        /**
         * 通过遍历 `lighting` 获取指定坐标经过光照衰减（曼哈顿距离）后的光照强度
         * TODO：光照移动不平滑
         *
         * @param blockPos 方块坐标
         * @return 光照强度
         */
        fun getLight(blockPos: BlockPos): Int {
            val posLong = blockPos.asLong()
            return if (cachedLight.containsKey(posLong)) cachedLight.get(posLong)
            else lighting.get(posLong)
        }
        /**
         * 通过遍历 `particles` 更新所有粒子的光照信息
         */
        fun updateLight() {
            lighting.clear()
            cachedLight.clear()

            val mutablePos = acquireMutablePos()
            val tmpPos = acquireMutablePos()

            for (particle in particles.keys()) {
                val pos = getPos(particle) ?: continue
                mutablePos.set(pos.x, pos.y, pos.z)
                val blockPosLong = mutablePos.asLong()

                val light = getLight(particle) ?: -1
                if (light <= 0) continue

                lighting[blockPosLong] = light

                updateLightPoses.add(blockPosLong)

                for (dx in -light..light) {
                    val ax = abs(dx)
                    val remainingY = light - ax
                    for (dy in -remainingY..remainingY) {
                        val ay = abs(dy)
                        val remainingZ = light - (ax + ay)
                        for (dz in -remainingZ..remainingZ) {
                            val az = abs(dz)
                            val distance = ax + ay + az
                            if (distance <= 0) continue
                            tmpPos.set(pos.x + dx, pos.y + dy, pos.z + dz)
                            val tmpPosLong = tmpPos.asLong()
                            val newLight = light - distance
                            val currentLight = cachedLight.get(tmpPosLong)
                            if (newLight > currentLight) {
                                cachedLight[tmpPosLong] = newLight
                            }
                        }
                    }
                }
            }

            releaseMutablePos(mutablePos)
            releaseMutablePos(tmpPos)
        }

        /**
         * 根据位置进行光照更新，范围为 pos.add(light) 到 pos.subtract(light)
         *
         * @param pos 位置
         * @param force 强制更新 15 光照
         */
        fun updateLightPos(pos: BlockPos, force: Boolean = false) {
            val l = if (force) 15 else lighting.get(pos.asLong())
            if (l <= 0) return

            try {
                client.worldRenderer.scheduleBlockRenders(
                    pos.x - l, pos.y - l, pos.z - l,
                    pos.x + l, pos.y + l, pos.z + l
                )
            } catch (_: Exception) {
                // ignore
            }
        }

        /**
         * 粒子刻计算
         *
         * @param particle 粒子
         * @param data 粒子数据
         * @return 下一个粒子数据
         */
        fun particleTick(particle: Particle, data: ParticleEnvData): ParticleEnvData {
            val env = getEnv(particle)
            if (env == null || !particle.isAlive) {
                removeParticle(particle)
                return data
            }
            setPos(particle, data.position)
            return env.next()
        }

        /**
         * 粒子管理刻
         */
        fun tick(client: MinecraftClient) {
            if (delayParticles.isNotEmpty()) {
                val dataset = delayParticles.pop()
                addParticles(ParticlecoreClient.client, dataset)
            }
            val remove = mutableListOf<Particle>()
            particles.forEach { (particle, data) ->
                if (data.age++ >= data.maxAge) {
                    remove.add(particle)
                    return@forEach
                }
                setColor(particle, data.final.color)
            }
            remove.forEach { removeParticle(it) }
        }

        fun setColor(particle: Particle, color: ParticleColor): Boolean {
            if (color == ParticleColor.UNSET) return false
            particle.setColor(
                color.red / 255,
                color.green / 255,
                color.blue / 255
            )
            return true
        }

        fun worldTick(world: ClientWorld) {
            if (lightUpdate.getAndSet(false)) {
                updateLight()
            }

            // 批量处理位置更新
            updateLightPoses.forEach { posLong ->
                updateLightPos(BlockPos.fromLong(posLong), true)
            }
            updateLightPoses.clear()
        }

        private fun addParticles(client: MinecraftClient, particles: List<ParticleSpawnData>) {
            particles.forEach { data ->
                spawnParticle(client, data)
            }
        }

        private fun halfRand(num: Number): Double = Random.nextDouble() * num.toDouble() - num.toDouble() / 2.0

        /**
         * 添加粒子效果于客户端
         *
         * @param client 客户端
         * @param data 粒子召唤数据
         */
        private fun spawnParticle(
            client: MinecraftClient,
            data: ParticleSpawnData,
        ) {
            val offset = data.offset
            val (randX, randY, randZ) = Triple(
                halfRand(offset.x),
                halfRand(offset.y),
                halfRand(offset.z),
            )
            val pos = data.pos.add(randX, randY, randZ)

            val particle = client.particleManager.addParticle(
                data.type, pos.x, pos.y, pos.z,
                0.0, 0.0, 0.0
            )

            if (particle == null) return

            val velocity = data.velocity
            particle.setVelocity(velocity.x, velocity.y, velocity.z)
            setColor(particle, data.final.color)

            particle.maxAge = data.age
            particle.scale(data.scale.toFloat())

            val currentData = ParticleStatus(
                light = -1,
                pos = pos,
                env = null,
                world = client.world,
                age = data.age,
                rotationData = RotationData(data.rotation, Matrix4f()),
                final = data.final
            )

            particles[particle] = currentData

            val expression = data.expression
            if (expression.isNotEmpty()) {
                val env = ParticleEnv(
                    expression = expression,
                    velocity = velocity,
                    offset = Vec3d(randX, randY, randZ),
                    ticks = 0,
                    center = data.pos,
                    arguments = data.args,
                    age = data.age
                )
                currentData.env = env
                val data = env.current()
                particle.setColor(
                    data.red,
                    data.green,
                    data.blue
                )
                val vec = data.velocity
                particle.setVelocity(vec.x, vec.y, vec.z)
                particle.scale(data.scale)
            }
        }

        /**
         * 召唤函数粒子
         *
         * @param data 粒子数据
         */
        fun spawnParticle(data: ParticleSpawnData) {
            spawnParticle(data)
        }

        /**
         * 召唤粒子效果，粒子效果会在 `delay` 刻后被生成
         *
         * @param data 粒子效果参数
         * @param delay 指定刻次数
         */
        fun spawnParticle(data: ParticleSpawnData, delay: Int = 0) {
            if (delayParticles.size <= delay) {
                repeat(delay - delayParticles.size + 1) {
                    delayParticles.add(mutableListOf())
                }
            }
            val particles = delayParticles[delay]
            // 数量限制
            if (particles.size < MAX_PARTICLES) {
                particles.add(data)
            }
        }

        /**
         * 移除所有延时粒子
         */
        fun clearDelayParticles() {
            delayParticles.forEach { it.clear() }
        }

        fun removeParticle(particle: Particle) {
            val data = particles[particle] ?: return

            // 更新光照标记
            if (data.light > 0) {
                posLightUpdate(BlockPos.ofFloored(data.pos))
            }

            particles.remove(particle)
            flagLightUpdate()
        }
    }

    // 高性能对象池
    class SynchronizedPool<T>(private val maxSize: Int, private val factory: () -> T) {
        private val pool = Stack<T>()

        @Synchronized
        fun acquire(): T? {
            return if (pool.isEmpty()) null else pool.pop()
        }

        @Synchronized
        fun release(obj: T) {
            if (pool.size < maxSize) {
                pool.push(obj)
            }
        }
    }
}