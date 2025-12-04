package work.nekow.particlecore.utils

import work.nekow.particlecore.exceptions.SyntaxError

@Suppress("unused")
class Expressions(
    val expression: String = ""
) {
    data class Exp(
        val prefix: String,
        val suffix: String
    ) {
        fun build(): String = if (prefix.isNotEmpty() && suffix.isNotEmpty()) "$prefix=$suffix" else ""
    }

    companion object {
        private const val MAX_CACHE_SIZE = 50
        private val expressionCache = object : LinkedHashMap<String, Expressions?>(MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Expressions?>): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }

        fun parse(exp: String): Expressions? {
            if (exp.isBlank()) return null

            return expressionCache.getOrPut(exp) {
                parseInternal(exp)
            }
        }

        fun parseInternal(exp: String): Expressions {
            val result = Expressions()

            val length = exp.length
            var start = 0
            var brace = 0
            for (i in 0 until length) {
                val char = exp[i]
                when (char) {
                    '{' -> brace++
                    '}' -> brace--
                    ';' -> {
                        if (brace != 0) continue
                        if (start != i)
                            parseCondition(exp.substring(start, i)).let {
                                result.add(it.first, it.second)
                            }
                        start = i + 1
                    }
                }
            }
            if (start != length)
                parseCondition(exp.substring(start)).let {
                    result.add(it.first, it.second)
                }
            return result
        }

        fun parseCondition(exp: String): Pair<String?, ArrayList<Exp>> {
            if (!exp.startsWith('@'))
                return Pair(null, parseExps(exp))
            val length = exp.length
            val colon = exp.indexOf(':')
            if (colon == -1)
                throw SyntaxError("':' not found after '@'")
            if (colon == length - 1)
                throw SyntaxError("':' at expression end")
            val cond = exp.substring(1, colon)
            val exps = if (exp[colon + 1] == '{') {
                if (!exp.endsWith('}'))
                    throw SyntaxError("brace is not match")
                exp.substring(colon + 2, length - 1)
            } else exp.substring(colon + 1)
            return Pair(cond, parseExps(exps))
        }

        fun parseExps(exp: String): ArrayList<Exp> {
            val result = ArrayList<Exp>()
            val length = exp.length
            var index = 0
            while (index < length) {
                val eq = exp.indexOf('=', index)
                if (eq == -1)
                    throw SyntaxError("'=' not found in expression")
                var split = exp.indexOf(';', eq)
                if (split == -1) split = length
                result.add(Exp(
                    exp.substring(index, eq),
                    exp.substring(eq + 1, split)
                ))
                index = split + 1
            }
            return result
        }
    }

    val expressions = arrayListOf<Pair<String?, ArrayList<Exp>>>()

    init {
        if (expression.isNotBlank()) {
            parse(expression)?.expressions?.let {
                expressions.addAll(it)
            }
        }
    }

    fun add(condition: String? = null, exps: ArrayList<Exp>): Expressions {
        val list=  expressions.find { it.first == condition } ?.second
        if (list != null) {
            list.addAll(exps)
        } else {
            expressions.add(condition to exps)
        }
        return this
    }
    fun add(condition: String?, vararg exps: Exp): Expressions {
        add(condition, ArrayList(exps.asList()))
        return this
    }
    fun add(vararg exps: Exp): Expressions {
        add(null, ArrayList(exps.asList()))
        return this
    }
    fun add(prefix: String, suffix: String, condition: String? = null): Expressions {
        add(condition, Exp(prefix, suffix))
        return this
    }
    fun add(vararg exps: Pair<String, String>): Expressions {
        add(null, *exps)
        return this
    }
    fun add(condition: String? = null, vararg exps: Pair<String, String>): Expressions {
        add(condition, ArrayList(exps.map { Exp(it.first, it.second) }))
        return this
    }
    fun remove(prefix: String, condition: String? = null): Expressions {
        expressions.find { it.first == condition }?.let { pair ->
            val list = pair.second
            val needRemove = arrayListOf<Exp>()
            list.filter { it.prefix == prefix }.forEach {
                needRemove.add(it)
            }
            list.removeAll(needRemove.toSet())
        }
        return this
    }
    fun contains(prefix: String, condition: String? = null): Boolean {
        return expressions.find { pair ->
            if (pair.first != condition) return@find false
            pair.second.find { it.prefix == prefix } != null
        } != null
    }

    fun build(): String {
        val list = arrayListOf<String>()
        expressions.forEach { (condition, exps) ->
            val expStr = exps.joinToString(";") {
                it.build()
            }
            if (condition != null) {
                if (exps.size > 1) {
                    list.add("@$condition:{$expStr}")
                    return@forEach
                }
                list.add("@$condition:$expStr")
                return@forEach
            }
            list.add(expStr)
        }
        return list.joinToString(";")
    }
}