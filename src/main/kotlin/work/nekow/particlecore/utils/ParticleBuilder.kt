package work.nekow.particlecore.utils

import net.minecraft.nbt.NbtCompound
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.particle.DustParticleEffect
import net.minecraft.particle.ParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Colors
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.getVec3d
import work.nekow.particlecore.math.ParticleColor
import kotlin.jvm.optionals.getOrDefault

class ParticleBuilder {
    /**
     * 类型
     */
    var type: ParticleEffect = DustParticleEffect(Colors.PURPLE, 1F)

    /**
     * 位置
     */
    var pos: Vec3d = Vec3d.ZERO

    /**
     * 向量
     */
    var velocity: Vec3d = Vec3d.ZERO

    /**
     * 随机偏移
     */
    var offset: Vec3d = Vec3d.ZERO

    /**
     * 存在时间
     */
    var age: Int = 10

    /**
     * 数量
     */
    var count: Int = 1

    /**
     * 粒子刻追踪函数（耗能极高）
     */
    var expression: Expressions = Expressions()

    /**
     * 颜色
     */
    var color: ParticleColor = ParticleColor.UNSET

    /**
     * 缩放
     */
    var scale: Double = 1.0
    companion object {
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, ParticleBuilder> = PacketCodec.of<RegistryByteBuf, ParticleBuilder>(
            { packet, buf ->
                ParticleTypes.PACKET_CODEC.encode(buf, packet.type)
                buf.writeVec3d(packet.pos)
                buf.writeVec3d(packet.velocity)
                buf.writeVec3d(packet.offset)
                buf.writeInt(packet.age)
                buf.writeInt(packet.count)
                buf.writeString(packet.expression.build())
                ParticleColor.PACKET_CODEC.encode(buf, packet.color)
                buf.writeDouble(packet.scale)
            }, { buf ->
                ParticleBuilder()
                    .type(ParticleTypes.PACKET_CODEC.decode(buf))
                    .pos(buf.readVec3d())
                    .velocity(buf.readVec3d())
                    .offset(buf.readVec3d())
                    .age(buf.readInt())
                    .count(buf.readInt())
                    .expression(Expressions(buf.readString()))
                    .color(ParticleColor.PACKET_CODEC.decode(buf))
                    .scale(buf.readDouble())
            }
        )

        /**
         * 将 NbtCompound 转换为 ParticleBuilder
         * 其中不包含粒子类型 (ParticleEffect)
         *
         * @param nbt NbtCompound
         * @return 读取后的粒子构造器
         */
        fun fromNbt(nbt: NbtCompound): ParticleBuilder {
            return ParticleBuilder()
                .pos(nbt.getVec3d("pos").add(0.5)) // 方块中心
                .velocity(nbt.getVec3d("v"))
                .offset(nbt.getVec3d("offset"))
                .age(nbt.getInt("age").getOrDefault(20))
                .count(nbt.getInt("count").getOrDefault(1))
                .expression(Expressions(nbt.getString("exp").getOrDefault("")))
                .scale(nbt.getDouble("scale").getOrDefault(1.0))
                // -1 为 UNSET
                .color(nbt.getVec3d("color", Vec3d(-1.0, -1.0, -1.0)).let {
                    ParticleColor(it.x.toFloat(), it.y.toFloat(), it.z.toFloat())
                })
        }
    }
    fun type(type: ParticleEffect): ParticleBuilder {
        this.type = type
        return this
    }
    fun pos(pos: Vec3d): ParticleBuilder {
        this.pos = pos
        return this
    }
    fun velocity(velocity: Vec3d): ParticleBuilder {
        this.velocity = velocity
        return this
    }
    fun offset(offset: Vec3d): ParticleBuilder {
        this.offset = offset
        return this
    }
    fun age(age: Int): ParticleBuilder {
        this.age = age
        return this
    }
    fun count(count: Int): ParticleBuilder {
        this.count = count
        return this
    }
    fun expression(expression: Expressions): ParticleBuilder {
        this.expression = expression
        return this
    }
    fun modifyExp(modifier: (Expressions) -> (Unit)): ParticleBuilder {
        modifier.invoke(this.expression)
        return this
    }
    fun buildExp(): String {
        return this.expression.build()
    }
    fun color(color: ParticleColor): ParticleBuilder {
        this.color = color
        return this
    }
    fun scale(scale: Double): ParticleBuilder {
        this.scale = scale
        return this
    }

    fun clone(): ParticleBuilder {
        return ParticleBuilder()
            .type(type)
            .pos(pos)
            .velocity(velocity)
            .offset(offset)
            .age(age)
            .count(count)
            .expression(expression)
            .color(color)
            .scale(scale)
    }

    /**
     * 调用 ParticleUtils 中的 spawnParticle 召唤粒子
     *
     * @param world 世界
     * @return ParticleUtils.spawnParticle
     */
    fun spawnAt(world: ServerWorld): Long {
        return ParticleUtils.Companion.spawnParticle(world, this)
    }
}