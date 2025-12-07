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
import work.nekow.particlecore.utils.FinalValues
import work.nekow.particlecore.utils.ParticleEnv
import work.nekow.particlecore.utils.ParticleEnvData
import work.nekow.particlecore.utils.RotationData
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.random.Random

@Environment(EnvType.CLIENT)
@Suppress("unused")
class ParticleManager {
    class ParticleData(
        var light: Int,
        var pos: Vec3d,
        val id: Long,
        var age: Int,
        val color: ParticleColor,
        val rotationData: RotationData,
        var env: ParticleEnv?,
        var world: World?,
        val maxAge: Int = age,
        val final: FinalValues
    )
    companion object {
        val tickParticles = LinkedList<MutableList<ParticleSpawnData>>()
        val removeIds = HashSet<Long>()

        val particleData = ConcurrentHashMap<Particle, ParticleData>()

        val ids = ConcurrentHashMap<Long, ConcurrentLinkedQueue<Particle>>()
        val particles = mutableListOf<Particle>()

        private val posPool = SynchronizedPool<BlockPos.Mutable>(128) { BlockPos.Mutable() }
        private fun acquireMutablePos() = posPool.acquire() ?: BlockPos.Mutable()
        private fun releaseMutablePos(pos: BlockPos.Mutable) = posPool.release(pos)

        private val lighting = Long2IntOpenHashMap().apply { defaultReturnValue(-1) }
        private val cachedLight = Long2IntOpenHashMap().apply { defaultReturnValue(-1) }

        private val updateLightPoses = HashSet<Long>()

        private val lightUpdate = AtomicBoolean(false)

        fun flagLightUpdate() =
            lightUpdate.set(true)

        fun hasParticle(particle: Particle): Boolean =
            particleData.containsKey(particle)

        /**
         * 标记一个需要更新亮度的坐标
         *
         * @param pos 坐标
         */
        fun posLightUpdate(pos: BlockPos) =
            updateLightPoses.add(pos.asLong())

        /**
         * 通过 ID 获取所有匹配的粒子效果
         *
         * @param id ID
         * @return 粒子列表
         */
        fun getParticles(id: Long): ConcurrentLinkedQueue<Particle> =
            ids[id] ?: ConcurrentLinkedQueue()

        /**
         * 获取粒子数据
         *
         * @param particle 粒子
         * @return 粒子数据
         */
        fun getData(particle: Particle): ParticleData? =
            particleData[particle]

        /**
         * 获取粒子位置
         *
         * @param particle 粒子
         * @return 粒子位置
         */
        fun getPos(particle: Particle): Vec3d? =
            getData(particle)?.pos

        /**
         * 获取粒子运行数据
         *
         * @param particle 粒子
         * @return 粒子运行数据
         */
        fun getEnv(particle: Particle): ParticleEnv? =
            getData(particle)?.env

        /**
         * 获取粒子 ID
         *
         * @param particle 粒子
         * @return 粒子 ID
         */
        fun getId(particle: Particle): Long? =
            getData(particle)?.id

        /**
         * 获取粒子亮度
         *
         * @param particle 粒子
         * @return 粒子亮度
         */
        fun getLight(particle: Particle): Int? =
            getData(particle)?.light

        /**
         * 设置粒子亮度
         *
         * @param particle 粒子
         * @param light 亮度
         * @throws NullPointerException 粒子数据不存在
         */
        fun setLight(particle: Particle, light: Int, pos: Vec3d) =
            getData(particle)?.let { data ->
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
            getData(particle)!!.pos = pos
        }

        /**
         * 设置粒子数据
         *
         * @param particle 粒子
         * @param env 粒子数据
         */
        fun setEnv(particle: Particle, env: ParticleEnv) {
            getData(particle)?.env = env
        }

        /**
         * 将粒子添加到 ID 中
         *
         * @param particle 粒子
         * @param id ID
         */
        fun addToId(particle: Particle, id: Long) {
            val list = ids[id] ?: ConcurrentLinkedQueue()
            list.add(particle)
            ids[id] = list
        }

        /**
         * 将匹配 ID 的粒子效果全部移除
         * @param id ID
         */
        fun removeId(id: Long) =
            removeIds.add(id)

        /**
         * 获取粒子世界
         *
         * @param particle 粒子
         * @return 世界
         */
        fun getWorld(particle: Particle): World? =
            getData(particle)?.world

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

            // 使用预分配数组减少内存分配
            val particlesArray = particles.toTypedArray()

            for (particle in particlesArray) {
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
            if (tickParticles.isNotEmpty()) {
                val dataset = tickParticles.pop()
                addParticlesBulk(ParticlecoreClient.client, dataset)
            }
            val remove = mutableListOf<Particle>()
            particleData.forEach { (particle, data) ->
                data.age --
                if (data.age <= 0) {
                    remove.add(particle)
                    return@forEach
                }
                setColor(particle, data.color)
            }
            if (removeIds.isNotEmpty()) {
                removeIds.forEach { id ->
                    getParticles(id).forEach {
                        it.markDead()
                    }
                    ids.remove(id)
                }
                removeIds.clear()
            }
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

        private fun addParticlesBulk(client: MinecraftClient, spawnData: List<ParticleSpawnData>) {
            spawnData.forEach { data ->
                addParticleInternal(client, data)
            }
        }

        private fun halfRand(num: Number): Double = Random.nextDouble() * num.toDouble() - num.toDouble() / 2.0

        /**
         * 添加粒子效果于客户端
         *
         * @param client 客户端
         * @param data 粒子召唤数据
         */
        private fun addParticleInternal(
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
            setColor(particle, data.color)
            particle.maxAge = data.age
            particle.scale(data.scale.toFloat())

            val currentData = ParticleData(
                light = -1,
                pos = pos,
                id = data.id,
                env = null,
                world = client.world,
                age = data.age,
                color = data.color,
                rotationData = RotationData(data.rotation, Matrix4f()),
                final = data.final
            )

            particleData[particle] = currentData
            addToId(particle, data.id)

            val expression = data.expression
            if (expression.isNotEmpty()) {
                particles.add(particle)
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
        fun addParticle(data: ParticleSpawnData) {
            addTickParticle(data, 0)
        }

        /**
         * 通过标记的 ID 批量移动粒子
         *
         * @param id ID
         * @param vector 向量
         */
        fun moveParticle(id: Long, vector: Vec3d) {
            val particles = ids[id] ?: return
            for (p in particles) {
                p.move(vector.x, vector.y, vector.z)
            }
        }

        /**
         * 通过标记的 Id 批量设置粒子向量
         *
         * @param id ID
         * @param vector 向量
         */
        fun velocityParticle(id: Long, vector: Vec3d) {
            val particles = ids[id] ?: return
            for (p in particles) {
                p.setVelocity(vector.x, vector.y, vector.z)
            }
        }

        /**
         * 在 `tickAfter` 刻后召唤的粒子效果
         *
         * @param data 粒子效果参数
         * @param tickAfter 指定刻次数
         */
        fun addTickParticle(data: ParticleSpawnData, tickAfter: Int) {
            if (tickParticles.size <= tickAfter) {
                repeat(tickAfter - tickParticles.size + 1) {
                    tickParticles.add(mutableListOf())
                }
            }
            val particles = tickParticles[tickAfter]
            // 数量限制
            if (particles.size < MAX_PARTICLES) {
                particles.add(data)
            }
        }

        /**
         * 移除所有延时粒子
         */
        fun clearTickParticles() {
            tickParticles.forEach { it.clear() }
        }

        fun removeParticle(particle: Particle) {
            val data = particleData.get(particle) ?: return

            // 更新光照标记
            if (data.light > 0) {
                posLightUpdate(BlockPos.ofFloored(data.pos))
            }

            // 从所有集合中移除
            ids[data.id]?.remove(particle)
            particles.remove(particle)
            particleData.remove(particle)

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