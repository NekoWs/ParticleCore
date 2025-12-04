package work.nekow.particlecore.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import work.nekow.particlecore.client.listeners.*
import work.nekow.particlecore.client.particle.ParticleManager
import work.nekow.particlecore.network.*

class ParticlecoreClient : ClientModInitializer {
    companion object {
        val client: MinecraftClient = MinecraftClient.getInstance()
        const val MAX_PARTICLES = 16384 * 4
    }

    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PacketParticlesS2C.PAYLOAD_ID,
            PacketParticlesHandler()
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

        ClientTickEvents.START_CLIENT_TICK.register { client ->
            ParticleManager.tick(client)
        }
        ClientTickEvents.START_WORLD_TICK.register { world ->
            ParticleManager.worldTick(world)
        }
    }
}
