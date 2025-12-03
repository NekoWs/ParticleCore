package work.nekow.particlecore.canvas.utils

import net.minecraft.util.math.Vec3d
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("unused")
data class Point3d(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    companion object {
        val ZERO = Point3d(0.0, 0.0, 0.0)
    }
    constructor(vec3d: Vec3d) : this(vec3d.x, vec3d.y, vec3d.z)

    operator fun plus(p: Point3d) =
        Point3d(x + p.x, y + p.y, z + p.z)

    fun plus(x: Double, y: Double, z: Double) = Point3d(x + x, y + y, z + z)

    operator fun minus(p: Point3d) =
        Point3d(x - p.x, y - p.y, z - p.z)

    operator fun times(p: Point3d) =
        Point3d(x * p.x, y * p.y, z * p.z)

    operator fun div(p: Point3d) =
        Point3d(x / p.x, y / p.y, z / p.z)

    operator fun plus(other: Double) =
        Point3d(x + other, y + other, z + other)

    operator fun minus(other: Double) =
        Point3d(x - other, y - other, z - other)

    operator fun times(scalar: Double) =
        Point3d(x * scalar, y * scalar, z * scalar)

    operator fun div(scalar: Double) =
        Point3d(x / scalar, y / scalar, z / scalar)

    fun dot(other: Point3d): Double =
        x * other.x + y * other.y + z * other.z

    fun distanceTo(other: Point3d) = sqrt(
        (x - other.x).pow(2) +
                (y - other.y).pow(2) +
                (z - other.z).pow(2)
    )

    fun cross(other: Point3d): Point3d =
        Point3d(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )

    fun lengthSquared(): Double =
        x.pow(2) + y.pow(2) + z.pow(2)

    fun magnitude(): Double = sqrt(lengthSquared())

    fun normalize(): Point3d {
        val len = magnitude()
        return if (len > 0) this / len else this
    }

    fun toVec3d() = Vec3d(x, y, z)

}