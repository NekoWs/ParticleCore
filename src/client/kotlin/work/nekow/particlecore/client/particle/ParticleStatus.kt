package work.nekow.particlecore.client.particle

import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import work.nekow.particlecore.utils.FinalValues
import work.nekow.particlecore.utils.ParticleEnv
import work.nekow.particlecore.utils.RotationData

class ParticleStatus(
    var light: Int,
    var pos: Vec3d,
    var age: Int,
    val rotationData: RotationData,
    var env: ParticleEnv?,
    var world: World?,
    val maxAge: Int = age,
    val final: FinalValues
)