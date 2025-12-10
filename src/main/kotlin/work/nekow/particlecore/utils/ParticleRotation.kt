package work.nekow.particlecore.utils

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.util.math.Vec3d
import org.joml.Quaternionf

@Suppress("unused")
data class ParticleRotation(
    var center: Vec3d = Vec3d.ZERO,
    var quat: Quaternionf = Quaternionf(),
    var local: Quaternionf = Quaternionf(),
) {
    fun center(center: Vec3d): ParticleRotation {
        this.center = center
        return this
    }
    fun quat(quat: Quaternionf): ParticleRotation {
        this.quat = quat
        return this
    }
    fun local(local: Quaternionf): ParticleRotation {
        this.local = local
        return this
    }
    fun clone(): ParticleRotation {
        return ParticleRotation(
            Vec3d.ZERO,
            Quaternionf(quat),
            Quaternionf(local)
        )
    }
    companion object {
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, ParticleRotation> = PacketCodec.of<RegistryByteBuf, ParticleRotation>(
            { packet, buf ->
                buf.writeVec3d(packet.center)
                buf.writeQuaternionf(packet.quat)
                buf.writeQuaternionf(packet.local)
            }, { buf ->
                ParticleRotation(
                    buf.readVec3d(),
                    buf.readQuaternionf(),
                    buf.readQuaternionf()
                )
            }
        )
    }
}