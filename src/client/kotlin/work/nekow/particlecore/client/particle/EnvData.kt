package work.nekow.particlecore.client.particle

import com.ezylang.evalex.data.EvaluationValue

data class EnvData(
    val expression: String,
    val arguments: HashMap<String, EvaluationValue>,
    val age: Int
) {
    // 自定义高效的hashCode实现
    override fun hashCode(): Int {
        var result = expression.hashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + age
        return result
    }

    // 确保equals逻辑高效
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnvData) return false

        return expression == other.expression &&
                arguments == other.arguments &&
                age == other.age
    }
}