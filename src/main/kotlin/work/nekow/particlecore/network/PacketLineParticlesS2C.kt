package work.nekow.particlecore.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.utils.ParticleBuilder
import work.nekow.particlecore.ParticleCore.Companion.MOD_ID

class PacketLineParticlesS2C(
    val particle: ParticleBuilder,
    val from: Vec3d,
    val to: Vec3d,
    val step: Double,
    val delay: Double,
    val id: Long
): CustomPayload {
    companion object {
        val ID: Identifier = Identifier.of(MOD_ID, "line_particle")
        val PAYLOAD_ID = CustomPayload.Id<PacketLineParticlesS2C>(ID)
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, PacketLineParticlesS2C> = PacketCodec.of<RegistryByteBuf, PacketLineParticlesS2C>(
            { packet, buf ->
                ParticleBuilder.PACKET_CODEC.encode(buf, packet.particle)
                buf.writeVec3d(packet.from)
                buf.writeVec3d(packet.to)
                buf.writeDouble(packet.step)
                buf.writeDouble(packet.delay)
                buf.writeLong(packet.id)
            }, { buf ->
                PacketLineParticlesS2C(
                    ParticleBuilder.PACKET_CODEC.decode(buf),
                    buf.readVec3d(),
                    buf.readVec3d(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readLong(),
                )
            }
        )

        fun init() {
            PayloadTypeRegistry.playS2C().register(PAYLOAD_ID, PACKET_CODEC)
        }
    }
    override fun getId(): CustomPayload.Id<out CustomPayload> {
        return PAYLOAD_ID
    }
}