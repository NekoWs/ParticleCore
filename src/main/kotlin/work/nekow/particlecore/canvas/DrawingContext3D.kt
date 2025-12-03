package work.nekow.particlecore.canvas

import work.nekow.particlecore.canvas.utils.Point3d
import work.nekow.particlecore.canvas.utils.Quaternion
import work.nekow.particlecore.canvas.utils.Transform
import work.nekow.particlecore.math.ParticleColor
import kotlin.math.*

@Suppress("unused")
class DrawingContext3D {
    private val points = mutableListOf<Point3d>()
    private var position = Point3d.ZERO
    private var color = ParticleColor.UNSET
    private var precision = 0.1
    private var density = 1.0
    private var transform = Transform()
    private val transformStack = mutableListOf<Transform>()

    // ====== 变换操作 ======

    fun pushTransform(): DrawingContext3D {
        transformStack.add(transform.copy())
        return this
    }

    fun popTransform(): DrawingContext3D {
        if (transformStack.isNotEmpty()) {
            transform = transformStack.removeLast()
        }
        return this
    }

    fun translate(x: Double, y: Double, z: Double): DrawingContext3D {
        transform.position = transform.position.plus(x, y, z)
        return this
    }

    fun translate(vec: Point3d): DrawingContext3D {
        transform.position += vec

        return this
    }

    fun rotate(axis: Point3d, angle: Double): DrawingContext3D {
        val newRotation = Quaternion.fromAxisAngle(axis, angle)
        // 累积到当前旋转上（局部旋转）
        transform.currentRotation = newRotation.multiply(transform.currentRotation)
        return this
    }

    fun rotateX(angle: Double): DrawingContext3D = rotate(Point3d(1.0, 0.0, 0.0), angle)
    fun rotateY(angle: Double): DrawingContext3D = rotate(Point3d(0.0, 1.0, 0.0), angle)
    fun rotateZ(angle: Double): DrawingContext3D = rotate(Point3d(0.0, 0.0, 1.0), angle)

    /**
     * 绕局部X轴旋转（基于当前朝向）
     */
    fun rotateLocalX(angle: Double): DrawingContext3D {
        // 将旋转轴转换到局部空间
        val localXAxis = transform.rotation.rotateVector(Point3d(1.0, 0.0, 0.0))
        return rotate(localXAxis, angle)
    }

    /**
     * 绕局部Y轴旋转（基于当前朝向）
     */
    fun rotateLocalY(angle: Double): DrawingContext3D {
        val localYAxis = transform.rotation.rotateVector(Point3d(0.0, 1.0, 0.0))
        return rotate(localYAxis, angle)
    }

    /**
     * 绕局部Z轴旋转（基于当前朝向）
     */
    fun rotateLocalZ(angle: Double): DrawingContext3D {
        val localZAxis = transform.rotation.rotateVector(Point3d(0.0, 0.0, 1.0))
        return rotate(localZAxis, angle)
    }

    /**
     * 使用欧拉角设置旋转（覆盖之前的旋转，不是累积）
     */
    fun setRotation(pitch: Double, yaw: Double, roll: Double): DrawingContext3D {
        transform.rotation = Quaternion.fromEulerAngles(pitch, yaw, roll)
        transform.currentRotation = Quaternion.IDENTITY
        return this
    }

    /**
     * 累积欧拉角旋转
     */
    fun rotateEuler(pitch: Double, yaw: Double, roll: Double): DrawingContext3D {
        // 创建欧拉角旋转的四元数
        val eulerRotation = Quaternion.fromEulerAngles(pitch, yaw, roll)
        // 累积旋转
        transform.currentRotation = eulerRotation.multiply(transform.currentRotation)
        return this
    }

    fun lookAt(from: Point3d, to: Point3d, up: Point3d = Point3d(0.0, 1.0, 0.0)): DrawingContext3D {
        transform.commitRotation()

        val forward = (to - from).normalize()
        val right = up.cross(forward).normalize()
        val actualUp = forward.cross(right).normalize()

        // 构建旋转矩阵
        // 注意：Minecraft 使用右手坐标系
        val rotationMatrix = arrayOf(
            doubleArrayOf(right.x, actualUp.x, forward.x),
            doubleArrayOf(right.y, actualUp.y, forward.y),
            doubleArrayOf(right.z, actualUp.z, forward.z)
        )

        // 将旋转矩阵转换为四元数
        val w = sqrt(1.0 + rotationMatrix[0][0] + rotationMatrix[1][1] + rotationMatrix[2][2]) / 2.0
        val w4 = 4.0 * w
        val x = (rotationMatrix[2][1] - rotationMatrix[1][2]) / w4
        val y = (rotationMatrix[0][2] - rotationMatrix[2][0]) / w4
        val z = (rotationMatrix[1][0] - rotationMatrix[0][1]) / w4

        transform.rotation = Quaternion(x, y, z, w).normalize()
        transform.position = from

        return this
    }

    fun scale(x: Double, y: Double = x, z: Double = x): DrawingContext3D {
        transform.scale = Point3d(x, y, z)
        return this
    }

    fun resetTransform(): DrawingContext3D {
        transform = Transform()
        return this
    }

    fun setPrecision(precision: Double): DrawingContext3D {
        this.precision = max(0.01, precision)
        return this
    }

    fun density(density: Double): DrawingContext3D {
        this.density = density
        return this
    }

    fun addPoint(x: Double, y: Double, z: Double): DrawingContext3D {
        val point = transform.apply(Point3d(x, y, z))
        points.add(point)
        return this
    }

    fun addPoint(point: Point3d): DrawingContext3D = addPoint(point.x, point.y, point.z)

    // ====== 基础绘图 =====

    fun begin(): DrawingContext3D {
        points.clear()
        return this
    }

    fun moveTo(x: Double, y: Double, z: Double): DrawingContext3D {
        position = Point3d(x, y, z)
        return this
    }

    fun moveTo(point: Point3d): DrawingContext3D {
        position = point
        return this
    }

    fun lineTo(x: Double, y: Double, z: Double): DrawingContext3D {
        return lineTo(Point3d(x, y, z))
    }

    fun lineTo(point: Point3d): DrawingContext3D {
        points.addAll(interpolateLine(position, point, precision))
        position = point
        return this
    }

    fun color(color: ParticleColor): DrawingContext3D {
        this.color = color
        return this
    }

    // ===== 绘制形状 =====

    fun circle(
        radius: Double,
        normal: Point3d = Point3d(0.0, 1.0, 0.0),
        up: Point3d = Point3d(0.0, 0.0, 1.0),
        segments: Int = 32,
        extent: Double = 2 * PI,
        startAngle: Double = 0.0,
    ): DrawingContext3D {
        val n = normal.normalize()

        val (u, v) = computePlaneBasis(n, up)

        for (i in 0..segments) {
            val t = i.toDouble() / segments
            val angle = startAngle + t * extent

            // 在圆平面上的坐标
            val localX = radius * cos(angle)
            val localZ = radius * sin(angle)

            // 转换到世界空间
            val worldPoint = Point3d(
                localX * u.x + localZ * v.x,
                localX * u.y + localZ * v.y,
                localX * u.z + localZ * v.z
            )
            addPoint(worldPoint)
        }
        return this
    }

    fun sphere(
        radius: Double,
        rings: Int = 8,
        segments: Int = 16
    ): DrawingContext3D {
        for (i in 0..rings) {
            val phi = Math.PI * i / rings
            for (j in 0..segments) {
                val theta = 2 * Math.PI * j / segments

                val x = radius * sin(phi) * cos(theta)
                val y = radius * cos(phi)
                val z = radius * sin(phi) * sin(theta)

                addPoint(x, y, z)
            }
        }
        return this
    }

    fun cube(
        size: Double,
        wireframe: Boolean = true
    ): DrawingContext3D {
        val half = size / 2
        val vertices = listOf(
            Point3d(-half, -half, -half), Point3d(half, -half, -half),
            Point3d(half, half, -half), Point3d(-half, half, -half),
            Point3d(-half, -half, half), Point3d(half, -half, half),
            Point3d(half, half, half), Point3d(-half, half, half)
        )
        // 绘制边
        if (wireframe) {
            val edges = listOf(
                intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 0), // 前面
                intArrayOf(4, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 4), // 后面
                intArrayOf(0, 4), intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7)  // 连接线
            )
            edges.forEach { edge ->
                val start = vertices[edge[0]]
                val end = vertices[edge[1]]
                // 绘制线段的点
                for (k in 0..5) {
                    val t = k / 5.0
                    val x = start.x + (end.x - start.x) * t
                    val y = start.y + (end.y - start.y) * t
                    val z = start.z + (end.z - start.z) * t
                    addPoint(x, y, z)
                }
            }
        } else {
            // 填充模式：添加所有顶点
            vertices.forEach { vertex ->
                addPoint(vertex)
            }
        }
        return this
    }

    fun build(): List<Point3d> = points.toList()

    fun clear(): DrawingContext3D {
        points.clear()
        return this
    }

    companion object {
        /**
         * 直线插值
         *
         * @param start 起点
         * @param end 终点
         * @param stepSize 精度
         */
        fun interpolateLine(
            start: Point3d,
            end: Point3d,
            stepSize: Double
        ): List<Point3d> {
            val distance = start.distanceTo(end)
            val steps = max(2, ceil(distance / stepSize).toInt())

            return (0 until steps).map { i ->
                val t = i.toDouble() / (steps - 1)
                start * (1 - t) + end * t
            }
        }

        /**
         * 多段线插值
         *
         * @param points 点列表
         * @param stepSize 精度
         * @param closed 是否闭合
         */
        fun interpolatePolyline(
            points: List<Point3d>,
            stepSize: Double,
            closed: Boolean = false
        ): List<Point3d> {
            val result = mutableListOf<Point3d>()

            val segments = if (closed) {
                points.indices.map { i ->
                    points[i] to points[(i + 1) % points.size]
                }
            } else {
                points.windowed(2) { (p1, p2) -> p1 to p2 }
            }

            segments.forEach { (p1, p2) ->
                val segmentPoints = interpolateLine(p1, p2, stepSize)
                result.addAll(segmentPoints.dropLast(1)) // 避免重复点
            }

            if (points.isNotEmpty()) {
                result.add(points.last())
            }

            return result
        }

        /**
         * 贝塞尔曲线
         *
         * @param controlPoints 控制点列表
         * @param stepSize 精度
         */
        fun bezierCurve(
            controlPoints: List<Point3d>,
            stepSize: Double
        ): List<Point3d> {
            if (controlPoints.size < 2) return emptyList()

            // 估算曲线长度
            val estimatedLength = controlPoints
                .windowed(2)
                .sumOf { (p1, p2) -> p1.distanceTo(p2) }

            val steps = max(10, ceil(estimatedLength / stepSize).toInt())

            return (0..steps).map { i ->
                val t = i.toDouble() / steps
                bezierPoint(controlPoints, t)
            }
        }

        private fun bezierPoint(
            controlPoints: List<Point3d>,
            t: Double
        ): Point3d {
            var result = Point3d(0.0, 0.0, 0.0)
            val n = controlPoints.size - 1

            for (i in controlPoints.indices) {
                val binomial = combination(n, i)
                val weight = binomial * t.pow(i) * (1 - t).pow(n - i)
                result += controlPoints[i] * weight
            }

            return result
        }

        private fun combination(n: Int, k: Int): Double {
            if (k !in 0..n) return 0.0
            var result = 1.0
            for (i in 1..k) {
                result = result * (n - k + i) / i
            }
            return result
        }


        fun computePlaneBasis(normal: Point3d, upHint: Point3d): Pair<Point3d, Point3d> {
            // 如果upHint与normal平行，需要选择一个不同的up
            var up = upHint
            if (abs(up.dot(normal)) > 0.99) {
                // 选择另一个向量作为up
                up = if (abs(normal.y) < 0.9) Point3d(0.0, 1.0, 0.0) else Point3d(0.0, 0.0, 1.0)
            }

            // 计算第一个基向量（垂直于normal和up）
            val u = up.cross(normal).normalize()
            // 计算第二个基向量（垂直于normal和u）
            val v = normal.cross(u).normalize()

            return Pair(u, v)
        }
        
        /**
         * 自动计算平面基向量
         */
        private fun computePlaneBasisAuto(normal: Point3d): Pair<Point3d, Point3d> {
            // 选择一个与法向量不平行的基础向量
            val baseUp = Point3d(0.0, 1.0, 0.0)
            val baseRight = Point3d(1.0, 0.0, 0.0)

            // 检查法向量是否接近Y轴
            return if (abs(normal.y) < 0.9) {
                // 使用Y轴作为上方向
                val u = baseUp.cross(normal).normalize()
                val v = normal.cross(u).normalize()
                Pair(u, v)
            } else {
                // 使用X轴作为上方向
                val u = baseRight.cross(normal).normalize()
                val v = normal.cross(u).normalize()
                Pair(u, v)
            }
        }

        /**
         * 将旋转矩阵转换为轴-角表示
         */
        private fun matrixToAxisAngle(matrix: Array<DoubleArray>): Pair<Point3d, Double> {
            // 计算旋转角度
            val angle = acos((matrix[0][0] + matrix[1][1] + matrix[2][2] - 1.0) / 2.0)

            if (abs(angle) < 1e-10) {
                return Pair(Point3d(1.0, 0.0, 0.0), 0.0)
            }

            // 计算旋转轴
            val x = matrix[2][1] - matrix[1][2]
            val y = matrix[0][2] - matrix[2][0]
            val z = matrix[1][0] - matrix[0][1]
            val s = 2.0 * sin(angle)

            val axis = Point3d(x / s, y / s, z / s).normalize()
            return Pair(axis, angle)
        }
    }
}