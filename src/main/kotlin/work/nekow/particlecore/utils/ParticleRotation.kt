package work.nekow.particlecore.utils

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.util.math.Vec3d
import org.joml.Quaternionf

@Suppress("unused")
data class ParticleRotation(
    val center: Vec3d = Vec3d.ZERO,
    val quat: Quaternionf = Quaternionf(0f, 0f, 0f, 0f),
) {
    companion object {
        val UNSET = ParticleRotation()

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
}