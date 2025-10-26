package work.nekow.particlecore

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.NbtCompoundArgumentType
import net.minecraft.command.argument.ParticleEffectArgumentType
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import work.nekow.particlecore.math.FourierTerm
import work.nekow.particlecore.network.PacketFourierParticleS2C
import work.nekow.particlecore.utils.ParticleBuilder
import work.nekow.particlecore.utils.ParticleUtils
import kotlin.jvm.optionals.getOrDefault
import kotlin.math.PI

class ParticleCommands {
    companion object {
        fun init() {
            CommandRegistrationCallback.EVENT.register { dispatcher, access, env ->
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
                dispatcher.register(literal("fourierp").then(
                    argument("particle", ParticleEffectArgumentType.particleEffect(access)).then(
                    argument("particle_data", NbtCompoundArgumentType.nbtCompound()).then(
                    argument("pos", BlockPosArgumentType.blockPos()).then(
                    argument("terms", StringArgumentType.string()).then(
                    argument("time_step", DoubleArgumentType.doubleArg()).then(
                    argument("duration", DoubleArgumentType.doubleArg()).then(
                    argument("delay", DoubleArgumentType.doubleArg(0.0)).then(
                    argument("rotate", NbtCompoundArgumentType.nbtCompound()).then(
                    argument("fscale", NbtCompoundArgumentType.nbtCompound()).then(
                    argument("particle_delay", IntegerArgumentType.integer(0)
                    ).executes { fourierp(it) }
                    ).executes { fourierp(it) }
                    ).executes { fourierp(it) }
                    ).executes { fourierp(it) }
                    ).executes { fourierp(it) }
                    ).executes { fourierp(it) }
                    ).executes { fourierp(it) }
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
        fun fourierp(context: CommandContext<ServerCommandSource>): Int {
            executor(context, "fourierp")
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
                "fourierp" -> {
                    val termsStr = StringArgumentType.getString(context, "terms").split(";")
                    // 时长
                    val duration = runCatching {
                        DoubleArgumentType.getDouble(context, "duration")
                    }.getOrDefault(PI)
                    // 步长
                    val timeStep = runCatching {
                        DoubleArgumentType.getDouble(context, "time_step")
                    }.getOrDefault(0.01)
                    val delay = runCatching {
                        DoubleArgumentType.getDouble(context, "delay")
                    }.getOrDefault(0.0)

                    val rotateData = runCatching {
                        NbtCompoundArgumentType.getNbtCompound(context, "rotate")
                    }.getOrDefault(NbtCompound())
                    val rotate = rotateData.getVec3d("rotate")
                    val rotateTo = rotateData.getVec3d("to")
                    val rotateDelay = rotateData.getDouble("delay").getOrDefault(0.0)
                    val rotateVelocity = rotateData.getBoolean("velocity").getOrDefault(true)
                    val rotateDirection = rotateData.getInt("direction").getOrDefault(1)

                    val fscaleData = runCatching {
                        NbtCompoundArgumentType.getNbtCompound(context, "fscale")
                    }.getOrDefault(NbtCompound())
                    val fscale = fscaleData.getDouble("fscale").getOrDefault(1.0)
                    val fscaleTo = fscaleData.getDouble("to").getOrDefault(fscale)
                    val fscaleSteps = fscaleData.getInt("steps").getOrDefault(1)

                    val particleDelay = runCatching {
                        IntegerArgumentType.getInteger(context, "particle_delay")
                    }.getOrDefault(0)

                    val terms = mutableListOf<FourierTerm>()
                    try {
                        for (t in termsStr) {
                            val split = t.split(",")
                            if (split.size != 3) continue
                            val nums = split.map { it.toDouble() }
                            terms.add(FourierTerm(nums[0], nums[1], Math.toRadians(nums[2])))
                        }
                    } catch (e: Exception) {
                        context.source.sendFeedback({ Text.of(e.message) }, true)
                    }

                    ParticleUtils.spawnFourierParticle(
                        world = world,
                        particle = particle,
                        duration = duration,
                        timeStep = timeStep,
                        length = terms.size,
                        terms = terms,
                        delay = delay,
                        fscale = PacketFourierParticleS2C.FPScale(
                            fscale = fscale,
                            fscaleTo = fscaleTo,
                            fscaleSteps = fscaleSteps
                        ),
                        rotate = PacketFourierParticleS2C.FPRotate(
                            rotate = rotate,
                            rotateTo = rotateTo,
                            rotateDelay = rotateDelay,
                            rotateVelocity = rotateVelocity,
                            rotateDirection = rotateDirection
                        ),
                        particleDelay = particleDelay,
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