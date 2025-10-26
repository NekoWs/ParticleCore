package work.nekow.particlecore.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import work.nekow.particlecore.network.PacketFourierParticleS2C
import work.nekow.particlecore.network.PacketFunctionParticlesS2C
import work.nekow.particlecore.network.PacketLineParticlesS2C
import work.nekow.particlecore.network.PacketMarkDeadS2C
import work.nekow.particlecore.network.PacketMoveParticleS2C
import work.nekow.particlecore.network.PacketParticleS2C
import work.nekow.particlecore.network.PacketRemoveTickParticlesS2C
import work.nekow.particlecore.network.PacketVelocityParticleS2C
import work.nekow.particlecore.client.listeners.FourierParticleHandler
import work.nekow.particlecore.client.listeners.FunctionParticlesHandler
import work.nekow.particlecore.client.listeners.LineParticleHandler
import work.nekow.particlecore.client.listeners.MarkDeadHandler
import work.nekow.particlecore.client.listeners.MoveParticleHandler
import work.nekow.particlecore.client.listeners.PacketParticleHandler
import work.nekow.particlecore.client.listeners.RemoveTickParticlesHandler
import work.nekow.particlecore.client.listeners.VelocityParticleHandler
import work.nekow.particlecore.client.particle.ParticleManager

class ParticlecoreClient : ClientModInitializer {
    companion object {
        val client: MinecraftClient = MinecraftClient.getInstance()
        const val MAX_PARTICLES = 16384 * 4
    }

    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PacketParticleS2C.PAYLOAD_ID,
            PacketParticleHandler()
        )
        ClientPlayNetworking.registerGlobalReceiver(PacketFourierParticleS2C.PAYLOAD_ID,
            FourierParticleHandler()
        )
        ClientPlayNetworking.registerGlobalReceiver(PacketMarkDeadS2C.PAYLOAD_ID,
            MarkDeadHandler()
        )
        ClientPlayNetworking.registerGlobalReceiver(PacketMoveParticleS2C.PAYLOAD_ID,
            MoveParticleHandler()
        )
        ClientPlayNetworking.registerGlobalReceiver(PacketVelocityParticleS2C.PAYLOAD_ID,
            VelocityParticleHandler()
        )
        ClientPlayNetworking.registerGlobalReceiver(PacketRemoveTickParticlesS2C.PAYLOAD_ID,
            RemoveTickParticlesHandler()
        )
        ClientPlayNetworking.registerGlobalReceiver(PacketLineParticlesS2C.PAYLOAD_ID,
            LineParticleHandler()
        )
        ClientPlayNetworking.registerGlobalReceiver(PacketFunctionParticlesS2C.PAYLOAD_ID,
            FunctionParticlesHandler()
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            ParticleManager.tick(client)
        }
        ClientTickEvents.END_WORLD_TICK.register { world ->
            ParticleManager.worldTick(world)
        }
    }
}
