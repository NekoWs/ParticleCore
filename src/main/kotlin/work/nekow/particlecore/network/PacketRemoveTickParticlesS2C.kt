package work.nekow.particlecore.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import work.nekow.particlecore.Particlecore.Companion.MOD_ID

class PacketRemoveTickParticlesS2C: CustomPayload {
    companion object {
        val ID: Identifier = Identifier.of(MOD_ID, "remove_tick_particles")
        val PAYLOAD_ID = CustomPayload.Id<PacketRemoveTickParticlesS2C>(ID)
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, PacketRemoveTickParticlesS2C> = CustomPayload.codecOf(
            { packet, buf ->
                buf
            }, { buf ->
                PacketRemoveTickParticlesS2C()
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