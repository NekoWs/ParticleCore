package work.nekow.particlecore.canvas.utils

enum class FillMethod {
    TRIANGULATION,    // 三角剖分填充
    VOXEL,            // 体素化填充
    SURFACE_SAMPLING, // 表面采样填充
    CONTOUR           // 轮廓填充
}