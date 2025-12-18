package work.nekow.particlecore.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import work.nekow.particlecore.ParticleCore.Companion.MOD_ID
import work.nekow.particlecore.utils.ParticleBuilder

class ParticlesS2C(
    val particles: List<ParticleBuilder>,
    val delay: Int,
    val particleDelay: Double,
    val size: Int = particles.size,
): CustomPayload {
    companion object {
        val ID: Identifier = Identifier.of(MOD_ID, "particle")
        val PAYLOAD_ID = CustomPayload.Id<ParticlesS2C>(ID)
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, ParticlesS2C> = CustomPayload.codecOf(
            { packet, buf ->
                buf.writeInt(packet.size)
                packet.particles.forEach {
                    ParticleBuilder.PACKET_CODEC.encode(buf, it)
                }
                buf.writeInt(packet.delay)
                buf.writeDouble(packet.particleDelay)
            }, { buf ->
                val size = buf.readInt()
                val particles = mutableListOf<ParticleBuilder>()
                repeat(size) {
                    particles.add(
                        ParticleBuilder.PACKET_CODEC.decode(buf)
                    )
                }
                val delay = buf.readInt()
                val particleDelay = buf.readDouble()
                ParticlesS2C(
                    particles = particles,
                    size = size,
                    delay = delay,
                    particleDelay = particleDelay
                )
            }
        )
        fun init() {
            PayloadTypeRegistry.playS2C().register(PAYLOAD_ID, PACKET_CODEC)
        }
    }
    override fun getId(): CustomPayload.Id<out CustomPayload?> {
        return PAYLOAD_ID
    }
}