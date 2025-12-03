package work.nekow.particlecore.canvas.utils

import kotlin.math.cos
import kotlin.math.sin

data class Transform(
    var position: Point3D = Point3D.ZERO,
    var rotation: Rotation = Rotation(Point3D(0.0, 1.0, 0.0), 0.0),
    var scale: Point3D = Point3D(1.0, 1.0, 1.0)
) {
    fun apply(point: Point3D): Point3D {
        var transformed = point

        // 1. 缩放
        transformed = Point3D(
            transformed.x * scale.x,
            transformed.y * scale.y,
            transformed.z * scale.z
        )

        // 2. 旋转
        if (rotation.angle != 0.0 && rotation.axis.lengthSquared() > 0) {
            transformed = rotateAroundAxis(transformed, rotation.axis, rotation.angle)
        }

        // 3. 平移
        transformed += position

        return transformed
    }

    fun copy(): Transform {
        return Transform(
            position.copy(),
            rotation.copy(),
            scale.copy()
        )
    }

    private fun rotateAroundAxis(point: Point3D, axis: Point3D, angle: Double): Point3D {
        val normalizedAxis = axis.normalize()
        val cos = cos(angle)
        val sin = sin(angle)
        val oneMinusCos = 1.0 - cos

        val x = normalizedAxis.x
        val y = normalizedAxis.y
        val z = normalizedAxis.z

        val rotationMatrix = arrayOf(
            doubleArrayOf(
                cos + x * x * oneMinusCos,
                x * y * oneMinusCos - z * sin,
                x * z * oneMinusCos + y * sin
            ),
            doubleArrayOf(
                y * x * oneMinusCos + z * sin,
                cos + y * y * oneMinusCos,
                y * z * oneMinusCos - x * sin
            ),
            doubleArrayOf(
                z * x * oneMinusCos - y * sin,
                z * y * oneMinusCos + x * sin,
                cos + z * z * oneMinusCos
            )
        )

        return Point3D(
            point.x * rotationMatrix[0][0] + point.y * rotationMatrix[0][1] + point.z * rotationMatrix[0][2],
            point.x * rotationMatrix[1][0] + point.y * rotationMatrix[1][1] + point.z * rotationMatrix[1][2],
            point.x * rotationMatrix[2][0] + point.y * rotationMatrix[2][1] + point.z * rotationMatrix[2][2]
        )
    }
}