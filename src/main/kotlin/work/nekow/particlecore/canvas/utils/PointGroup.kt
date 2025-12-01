package work.nekow.particlecore.canvas.utils

import kotlin.math.abs

@Suppress("unused")
class PointGroup(
    val points: List<Point3D>,
    val connections: List<Connection> = emptyList(),
    val isClosed: Boolean = false
) {
    // 计算包围盒
    fun boundingBox(): Pair<Point3D, Point3D> {
        val xs = points.map { it.x }
        val ys = points.map { it.y }
        val zs = points.map { it.z }

        val minPoint = Point3D(xs.minOrNull()!!, ys.minOrNull()!!, zs.minOrNull()!!)
        val maxPoint = Point3D(xs.maxOrNull()!!, ys.maxOrNull()!!, zs.maxOrNull()!!)

        return minPoint to maxPoint
    }

    // 计算中心点
    fun center(): Point3D {
        val sum = points.reduce { acc, point -> acc + point }
        return sum / points.size.toDouble()
    }

    // 检查是否是平面多边形
    fun isPlanar(threshold: Double = 0.001): Boolean {
        if (points.size < 3) return true

        val v1 = points[1] - points[0]
        val v2 = points[2] - points[0]
        val normal = v1.cross(v2)

        for (i in 3 until points.size) {
            val vi = points[i] - points[0]
            val dot = normal.dot(vi)
            if (abs(dot) > threshold) return false
        }
        return true
    }
}