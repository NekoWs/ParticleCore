package work.nekow.particlecore.canvas.utils

import kotlin.math.*

@Suppress("unused")
data class Quaternion(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val w: Double = 1.0
) {
    companion object {
        val IDENTITY = Quaternion()

        fun fromAxisAngle(axis: Point3d, angle: Double): Quaternion {
            val normalizedAxis = axis.normalize()
            val halfAngle = angle * 0.5
            val sinHalf = sin(halfAngle)

            return Quaternion(
                x = normalizedAxis.x * sinHalf,
                y = normalizedAxis.y * sinHalf,
                z = normalizedAxis.z * sinHalf,
                w = cos(halfAngle)
            )
        }

        fun fromEulerAngles(pitch: Double, yaw: Double, roll: Double): Quaternion {
            val cy = cos(yaw * 0.5)
            val sy = sin(yaw * 0.5)
            val cp = cos(pitch * 0.5)
            val sp = sin(pitch * 0.5)
            val cr = cos(roll * 0.5)
            val sr = sin(roll * 0.5)

            return Quaternion(
                x = sr * cp * cy - cr * sp * sy,
                y = cr * sp * cy + sr * cp * sy,
                z = cr * cp * sy - sr * sp * cy,
                w = cr * cp * cy + sr * sp * sy
            )
        }
    }

    fun normalize(): Quaternion {
        val length = sqrt(x * x + y * y + z * z + w * w)
        return if (length > 0.0) {
            Quaternion(x / length, y / length, z / length, w / length)
        } else {
            IDENTITY
        }
    }

    fun conjugate(): Quaternion = Quaternion(-x, -y, -z, w)

    fun multiply(other: Quaternion): Quaternion {
        return Quaternion(
            x = w * other.x + x * other.w + y * other.z - z * other.y,
            y = w * other.y - x * other.z + y * other.w + z * other.x,
            z = w * other.z + x * other.y - y * other.x + z * other.w,
            w = w * other.w - x * other.x - y * other.y - z * other.z
        ).normalize()
    }

    fun rotateVector(vec: Point3d): Point3d {
        val p = Quaternion(vec.x, vec.y, vec.z, 0.0)
        val result = this.multiply(p).multiply(conjugate())
        return Point3d(result.x, result.y, result.z)
    }

    fun toEulerAngles(): Triple<Double, Double, Double> {
        // 转换为欧拉角（偏航Yaw，俯仰Pitch，滚转Roll）
        val sinP = 2.0 * (w * x + y * z)
        val cosP = 1.0 - 2.0 * (x * x + y * y)
        val pitch = atan2(sinP, cosP)

        val sinY = 2.0 * (w * y - z * x)
        val yaw = if (abs(sinY) >= 1) {
            (Math.PI / 2).withSign(sinY)
        } else {
            asin(sinY)
        }

        val sinR = 2.0 * (w * z + x * y)
        val cosR = 1.0 - 2.0 * (y * y + z * z)
        val roll = atan2(sinR, cosR)

        return Triple(yaw, pitch, roll)
    }
}