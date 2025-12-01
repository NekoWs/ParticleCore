package work.nekow.particlecore.canvas.utils

import kotlin.math.pow
import kotlin.math.sqrt

// 三角形数据结构
@Suppress("unused")
data class Triangle(val p1: Point3D, val p2: Point3D, val p3: Point3D) {
    fun normal(): Point3D = (p2 - p1).cross(p3 - p1).normalized()

    fun area(): Double {
        val v1 = p2 - p1
        val v2 = p3 - p1
        return v1.cross(v2).let {
            sqrt(it.x.pow(2) + it.y.pow(2) + it.z.pow(2))
        } / 2
    }
}