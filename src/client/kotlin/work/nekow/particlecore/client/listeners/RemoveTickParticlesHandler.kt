package work.nekow.particlecore.client.listeners

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import work.nekow.particlecore.client.particle.ParticleManager
import work.nekow.particlecore.network.ClearDelayParticlesS2C

class RemoveTickParticlesHandler: ClientPlayNetworking.PlayPayloadHandler<ClearDelayParticlesS2C> {
    override fun receive(
        payload: ClearDelayParticlesS2C?,
        context: ClientPlayNetworking.Context?
    ) {
        ParticleManager.clearDelayParticles()
    }
}