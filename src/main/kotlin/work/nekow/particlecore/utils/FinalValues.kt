package work.nekow.particlecore.utils

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.math.ParticleColor

@Suppress("unused")
data class FinalValues(
    var velocity: Vec3d = Vec3d.ZERO,
    var color: ParticleColor = ParticleColor.UNSET,
    var light: Int = 0,
    var prefix: MutableSet<String> = mutableSetOf(),
    var active: Boolean = true
) {
    fun active(active: Boolean): FinalValues {
        this.active = active
        return this
    }

    fun velocity(velocity: Vec3d): FinalValues {
        this.velocity = velocity
        this.prefix.addAll(arrayOf("vx", "vy", "vz"))
        return this
    }

    fun color(color: ParticleColor): FinalValues {
        this.color = color
        this.prefix.addAll(arrayOf("cr", "cg", "cb"))
        return this
    }

    fun light(light: Int): FinalValues {
        this.light = light
        this.prefix.add("light")
        return this
    }

    fun clone(): FinalValues {
        val prefix = mutableSetOf<String>(
            *prefix.toTypedArray(),
        )
        return FinalValues(
            velocity,
            color,
            light,
            prefix,
            active
        )
    }

    fun toEnvData(): ParticleEnvData {
        return ParticleEnvData(
            velocity = velocity,
            red = color.red,
            green = color.green,
            blue = color.blue,
            light = light,
            prefix = arrayListOf(*prefix.toTypedArray())
        )
    }
    companion object {
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, FinalValues> = PacketCodec.of<RegistryByteBuf, FinalValues>(
            { packet, buf ->
                buf.writeVec3d(packet.velocity)
                ParticleColor.PACKET_CODEC.encode(buf, packet.color)
                buf.writeInt(packet.light)
                buf.writeBoolean(packet.active)
                buf.writeInt(packet.prefix.size)
                packet.prefix.forEach { buf.writeString(it) }
            }, { buf ->
                val velocity = buf.readVec3d()
                val color = ParticleColor.PACKET_CODEC.decode(buf)
                val light = buf.readInt()
                val active = buf.readBoolean()
                val size = buf.readInt()
                val prefix = mutableListOf<String>()
                repeat(size) {
                    prefix.add(buf.readString())
                }
                FinalValues(
                    velocity = velocity,
                    color = color,
                    light = light,
                    active = active,
                    prefix = mutableSetOf(*prefix.toTypedArray())
                )
            }
        )
        val UNSET = FinalValues().active(false)
    }
}