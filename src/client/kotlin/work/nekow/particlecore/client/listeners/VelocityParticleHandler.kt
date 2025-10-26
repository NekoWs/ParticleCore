package work.nekow.particlecore.client.listeners

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import work.nekow.particlecore.client.particle.ParticleManager
import work.nekow.particlecore.network.PacketVelocityParticleS2C

class VelocityParticleHandler: ClientPlayNetworking.PlayPayloadHandler<PacketVelocityParticleS2C> {
    override fun receive(
        payload: PacketVelocityParticleS2C,
        context: ClientPlayNetworking.Context
    ) {
        ParticleManager.velocityParticle(payload.id, payload.vec)
    }
}