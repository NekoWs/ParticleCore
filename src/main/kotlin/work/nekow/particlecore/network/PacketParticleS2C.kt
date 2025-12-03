package work.nekow.particlecore.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import work.nekow.particlecore.Particlecore.Companion.MOD_ID
import work.nekow.particlecore.utils.ParticleBuilder

class PacketParticleS2C(
    val particles: List<ParticleBuilder>,
    val id: Long,
    val delay: Int,
    val size: Int = particles.size,
): CustomPayload {
    companion object {
        val ID: Identifier = Identifier.of(MOD_ID, "particle")
        val PAYLOAD_ID = CustomPayload.Id<PacketParticleS2C>(ID)
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, PacketParticleS2C> = CustomPayload.codecOf(
            { packet, buf ->
                buf.writeInt(packet.size)
                packet.particles.forEach {
                    ParticleBuilder.PACKET_CODEC.encode(buf, it)
                }
                buf.writeLong(packet.id)
                buf.writeInt(packet.delay)
            }, { buf ->
                val size = buf.readInt()
                val particles = mutableListOf<ParticleBuilder>()
                repeat(size) {
                    particles.add(
                        ParticleBuilder.PACKET_CODEC.decode(buf)
                    )
                }
                val id = buf.readLong()
                val delay = buf.readInt()
                PacketParticleS2C(
                    particles = particles,
                    size = size,
                    delay = delay,
                    id = id
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