package work.nekow.particlecore.utils

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.util.math.Vec3d
import org.joml.Quaternionf

@Suppress("unused")
data class ParticleRotation(
    val center: Vec3d = Vec3d.ZERO,
    val quat: Quaternionf = Quaternionf(),
) {
    companion object {
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, ParticleRotation> = PacketCodec.of<RegistryByteBuf, ParticleRotation>(
            { packet, buf ->
                buf.writeVec3d(packet.center)
                buf.writeQuaternionf(packet.quat)
            }, { buf ->
                ParticleRotation(
                    buf.readVec3d(),
                    buf.readQuaternionf()
                )
            }
        )
    }

    override fun hashCode(): Int {
        var result = center.hashCode()
        result = 31 * result + quat.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParticleRotation

        if (center != other.center) return false
        if (quat != other.quat) return false

        return true
    }
}