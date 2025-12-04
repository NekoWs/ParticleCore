package work.nekow.particlecore.utils

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.util.math.Vec3d

data class ParticlePathData(
    val path: ParticlePath = ParticlePath.EmptyPath(),
    var center: Vec3d = Vec3d.ZERO,
    val speed: Double = 0.1
) {
    companion object {
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, ParticlePathData> = PacketCodec.of<RegistryByteBuf, ParticlePathData>(
            { packet, buf ->
                ParticlePath.PACKET_CODEC.encode(buf, packet.path)
                buf.writeVec3d(Vec3d.ZERO)
                buf.writeDouble(packet.speed)
            }, { buf ->
                ParticlePathData(
                    ParticlePath.PACKET_CODEC.decode(buf),
                    buf.readVec3d(),
                    buf.readDouble(),
                )
            }
        )
    }
}