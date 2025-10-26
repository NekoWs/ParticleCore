package work.nekow.particlecore.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import work.nekow.particlecore.utils.ParticleBuilder
import work.nekow.particlecore.Particlecore.Companion.MOD_ID

class PacketParticleS2C(
    val particle: ParticleBuilder,
    val id: Long,
): CustomPayload {
    companion object {
        val ID: Identifier = Identifier.of(MOD_ID, "particle")
        val PAYLOAD_ID = CustomPayload.Id<PacketParticleS2C>(ID)
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, PacketParticleS2C> = CustomPayload.codecOf(
            { packet, buf ->
                ParticleBuilder.PACKET_CODEC.encode(buf, packet.particle)
                buf.writeLong(packet.id)
            }, { buf ->
                PacketParticleS2C(
                    ParticleBuilder.PACKET_CODEC.decode(buf),
                    buf.readLong()
                )
            }
        )
        fun init() {
            PayloadTypeRegistry.playS2C().register(PAYLOAD_ID, PACKET_CODEC)
        }
    }
    override fun getId(): CustomPayload.Id<out CustomPayload?>? {
        return PAYLOAD_ID
    }
}