package work.nekow.particlecore.canvas.utils

data class Connection(
    val points: List<Point3D>,
    val type: ConnectionType,
    val controlPoints: List<Point3D>? = null,
    val isClosed: Boolean = false
)