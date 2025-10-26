package work.nekow.particlecore.client.listeners

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import work.nekow.particlecore.client.particle.ParticleManager
import work.nekow.particlecore.network.PacketMoveParticleS2C

class MoveParticleHandler: ClientPlayNetworking.PlayPayloadHandler<PacketMoveParticleS2C> {
    override fun receive(
        payload: PacketMoveParticleS2C,
        context: ClientPlayNetworking.Context
    ) {
        ParticleManager.moveParticle(payload.id, payload.vec)
    }
}