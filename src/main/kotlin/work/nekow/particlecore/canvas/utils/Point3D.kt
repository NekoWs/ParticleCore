package work.nekow.particlecore.canvas.utils

import net.minecraft.util.math.Vec3d
import kotlin.math.pow
import kotlin.math.sqrt

data class Point3D(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    constructor(vec3d: Vec3d) : this(vec3d.x, vec3d.y, vec3d.z)

    operator fun plus(p: Point3D) =
        Point3D(x + p.x, y + p.y, z + p.z)

    operator fun minus(p: Point3D) =
        Point3D(x - p.x, y - p.y, z - p.z)

    operator fun times(p: Point3D) =
        Point3D(x * p.x, y * p.y, z * p.z)

    operator fun div(p: Point3D) =
        Point3D(x / p.x, y / p.y, z / p.z)

    operator fun plus(other: Double) =
        Point3D(x + other, y + other, z + other)

    operator fun minus(other: Double) =
        Point3D(x - other, y - other, z - other)

    operator fun times(scalar: Double) =
        Point3D(x * scalar, y * scalar, z * scalar)

    operator fun div(scalar: Double) =
        Point3D(x / scalar, y / scalar, z / scalar)

    fun dot(other: Point3D): Double =
        x * other.x + y * other.y + z * other.z

    fun distanceTo(other: Point3D) = sqrt(
        (x - other.x).pow(2) +
            (y - other.y).pow(2) +
            (z - other.z).pow(2)
    )

    fun cross(other: Point3D): Point3D =
        Point3D(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )

    fun normalized(): Point3D {
        val len = sqrt(x * x + y * y + z * z)
        return if (len > 0) this / len else this
    }

    fun toVec3d() = Vec3d(x, y, z)
}