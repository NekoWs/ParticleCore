package work.nekow.particlecore.client.listeners

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import work.nekow.particlecore.client.particle.ParticleManager
import work.nekow.particlecore.client.particle.ParticleSpawnData
import work.nekow.particlecore.math.FunctionPoints
import work.nekow.particlecore.network.PacketFunctionParticlesS2C
import kotlin.math.floor

class FunctionParticlesHandler: ClientPlayNetworking.PlayPayloadHandler<PacketFunctionParticlesS2C> {
    override fun receive(
        payload: PacketFunctionParticlesS2C,
        context: ClientPlayNetworking.Context
    ) {
        val points = FunctionPoints(
            payload.function,
            payload.range,
            payload.step
        )
        val particle = payload.particle
        val center = particle.pos
        val delay = payload.delay

        var ticks = 0.0
        points.points.forEach { point ->
            val clone = particle.clone()
                .pos(center.add(point))
            ParticleManager.addTickParticle(
                ParticleSpawnData.fromBuilder(clone, payload.id),
                floor(ticks.also { ticks += delay }).toInt(),
            )
        }
    }
}