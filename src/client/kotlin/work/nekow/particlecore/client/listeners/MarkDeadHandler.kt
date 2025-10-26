package work.nekow.particlecore.client.listeners

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import work.nekow.particlecore.client.particle.ParticleManager
import work.nekow.particlecore.network.PacketMarkDeadS2C

class MarkDeadHandler: ClientPlayNetworking.PlayPayloadHandler<PacketMarkDeadS2C> {
    override fun receive(
        payload: PacketMarkDeadS2C,
        context: ClientPlayNetworking.Context
    ) {
        ParticleManager.removeId(payload.id)
    }
}