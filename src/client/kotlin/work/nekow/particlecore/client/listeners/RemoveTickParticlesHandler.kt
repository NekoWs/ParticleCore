package work.nekow.particlecore.client.listeners

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import work.nekow.particlecore.client.particle.ParticleManager
import work.nekow.particlecore.network.PacketRemoveTickParticlesS2C

class RemoveTickParticlesHandler: ClientPlayNetworking.PlayPayloadHandler<PacketRemoveTickParticlesS2C> {
    override fun receive(
        payload: PacketRemoveTickParticlesS2C?,
        context: ClientPlayNetworking.Context?
    ) {
        ParticleManager.clearTickParticles()
    }
}