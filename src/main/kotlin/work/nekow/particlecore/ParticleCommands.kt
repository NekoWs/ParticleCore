package work.nekow.particlecore

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.NbtCompoundArgumentType
import net.minecraft.command.argument.ParticleEffectArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import work.nekow.particlecore.utils.ParticleBuilder
import work.nekow.particlecore.utils.ParticleUtils

class ParticleCommands {
    companion object {
        fun init() {
            CommandRegistrationCallback.EVENT.register { dispatcher, access, _ ->
                dispatcher.register(literal("particlex").then(
                    argument("particle", ParticleEffectArgumentType.particleEffect(access)).then(
                    argument("particle_data", NbtCompoundArgumentType.nbtCompound()).then(
                    argument("pos", BlockPosArgumentType.blockPos()
                    ).executes { particlex(it) }
                    )
                    )
                ))
                dispatcher.register(literal("functionp").then(
                    argument("particle", ParticleEffectArgumentType.particleEffect(access)).then(
                    argument("particle_data", NbtCompoundArgumentType.nbtCompound()).then(
                    argument("pos", BlockPosArgumentType.blockPos()).then(
                    argument("function", StringArgumentType.string()).then(
                    argument("step", DoubleArgumentType.doubleArg(0.0)).then(
                    argument("delay", DoubleArgumentType.doubleArg(0.0)).then(
                    argument("start", DoubleArgumentType.doubleArg()).then(
                    argument("end", DoubleArgumentType.doubleArg()
                    ).executes { functionp(it) }
                    ).executes { functionp(it) }
                    ).executes { functionp(it) }
                    ).executes { functionp(it) }
                    ).executes { functionp(it) }
                    )
                    )
                    )
                ))
                dispatcher.register(literal("clear_tick_particles").executes {
                    ParticleUtils.clearTickParticles(it.source.world)
                    System.gc()
                    return@executes 1
                })
            }
        }

        fun particlex(context: CommandContext<ServerCommandSource>): Int {
            executor(context, "particlex")
            return 1
        }
        fun functionp(context: CommandContext<ServerCommandSource>): Int {
            executor(context, "functionp")
            return 1
        }
        
        fun executor(context: CommandContext<ServerCommandSource>, type: String) {
            val particleEffect = ParticleEffectArgumentType.getParticle(context, "particle")
            val particleData = NbtCompoundArgumentType.getNbtCompound(context, "particle_data")
            val pos = BlockPosArgumentType.getBlockPos(context, "pos")
            val particle = ParticleBuilder.fromNbt(particleData)
                .type(particleEffect)
                .pos(pos.toCenterPos())
            val world = context.source.world

            when (type) {
                "particlex" -> {
                    ParticleUtils.spawnParticle(
                        world = world,
                        particle = particle
                    )
                }
                "functionp" -> {
                    val function = StringArgumentType.getString(context, "function")
                    val step = runCatching { DoubleArgumentType.getDouble(context, "step") }.getOrDefault(0.1)
                    val delay = runCatching { DoubleArgumentType.getDouble(context, "delay") }.getOrDefault(0.0)
                    val start = runCatching { DoubleArgumentType.getDouble(context, "start") }.getOrDefault(-10.0)
                    val end = runCatching { DoubleArgumentType.getDouble(context, "end") }.getOrDefault(10.0)

                    ParticleUtils.spawnFunctionParticle(
                        world = world,
                        particle = particle,
                        function = function,
                        step = step,
                        delay = delay,
                        range = Pair(start, end)
                    )
                }
            }
        }
    }
}