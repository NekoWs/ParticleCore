package work.nekow.particlecore.canvas.utils

data class Transform(
    var position: Point3d = Point3d.ZERO,
    var rotation: Quaternion = Quaternion.IDENTITY,
    var scale: Point3d = Point3d(1.0, 1.0, 1.0)
) {
    var currentRotation: Quaternion = Quaternion.IDENTITY

    fun apply(point: Point3d): Point3d {
        var transformed = point

        // 1. 缩放
        transformed = Point3d(
            transformed.x * scale.x,
            transformed.y * scale.y,
            transformed.z * scale.z
        )

        // 2. 旋转
        transformed = rotation.rotateVector(transformed)

        // 3. 平移
        transformed += position

        return transformed
    }

    fun copy(): Transform {
        val copy = Transform(
            position, rotation, scale
        )
        copy.currentRotation = currentRotation
        return copy
    }

    fun resetRotation() {
        currentRotation = Quaternion.IDENTITY
    }

    // 将累积的旋转应用到主旋转上
    fun commitRotation() {
        rotation = rotation.multiply(currentRotation)
        currentRotation = Quaternion.IDENTITY
    }
}