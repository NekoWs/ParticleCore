package work.nekow.particlecore.utils

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.math.ParticleColor

class FinalValues(
    val velocity: Vec3d = Vec3d.ZERO,
    val color: ParticleColor = ParticleColor.UNSET,
    val light: Int = 0
) {
    var active = true
    fun active(ac: Boolean): FinalValues {
        active = ac
        return this
    }

    fun toEnvData(): ParticleEnvData {
        return ParticleEnvData(
            velocity = velocity,
            red = color.red,
            green = color.green,
            blue = color.blue,
            light = light,
            prefix = arrayListOf("velocity", "")
        )
    }
    companion object {
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, FinalValues> = PacketCodec.of<RegistryByteBuf, FinalValues>(
            { packet, buf ->
                buf.writeVec3d(packet.velocity)
                ParticleColor.PACKET_CODEC.encode(buf, packet.color)
                buf.writeInt(packet.light)
                buf.writeBoolean(packet.active)
            }, { buf ->
                FinalValues(
                    buf.readVec3d(),
                    ParticleColor.PACKET_CODEC.decode(buf),
                    buf.readInt()
                ).active(buf.readBoolean())
            }
        )
        val UNSET = FinalValues().active(false)
    }
}