package bot.stewart.hcl

import java.io.File
import kotlin.reflect.KProperty1

object HclWriter {
    private val indentUnit: String
        get() = "  "

    fun write(input: Map<String, Any?>): String {
        return StringBuilder().apply {
            writeMap(input, 0)
        }.toString()
    }

    fun write(input: Map<String, Any?>, path: String): File {
        val string = StringBuilder().apply {
            writeMap(input, 0)
        }.toString()
        return File(path).apply {
            writeText(string)
        }
    }

    fun write(obj: Any): String {
        return StringBuilder().apply {
            writeObject(obj, 0)
        }.toString()
    }

    fun write(obj: Any, path: String): File {
        val string = StringBuilder().apply {
            writeObject(obj, 0)
        }.toString()
        return File(path).apply {
            writeText(string)
        }
    }

    private fun StringBuilder.writeObject(obj: Any, indent: Int) {
        val properties = obj::class.members
            .filterIsInstance<KProperty1<Any, *>>()
            .sortedBy { it.name }

        properties.forEach { prop ->
            val value = prop.get(obj)
            if (value != null || !prop.returnType.isMarkedNullable) {
                writeIndent(indent)
                append(prop.name)
                append(" = ")
                writeValue(value, indent)
                append("\n")
            }
        }
    }

    private fun StringBuilder.writeMap(map: Map<String, Any?>, indent: Int) {
        map.forEach { (key, value) ->
            writeIndent(indent)
            append(key)
            append(" = ")
            writeValue(value, indent)
            append("\n")
        }
    }

    private fun StringBuilder.writeValue(value: Any?, indent: Int) {
        when (value) {
            null -> append("null")
            is String -> append("\"${value.escape()}\"")
            is Number -> append(value)
            is Boolean -> append(value)
            is List<*> -> writeList(value, indent)
            is Map<*, *> -> {
                append("{\n")
                @Suppress("UNCHECKED_CAST")
                writeMap(value as Map<String, Any?>, indent + 1)
                writeIndent(indent)
                append("}")
            }
            else -> when {
                value::class.isData -> {
                    append("{\n")
                    writeObject(value, indent + 1)
                    writeIndent(indent)
                    append("}")
                }
                else -> throw IllegalArgumentException("Unsupported type: ${value::class.java}")
            }
        }
    }

    private fun StringBuilder.writeList(list: List<*>, indent: Int) {
        if (list.isEmpty()) {
            append("[]")
            return
        }

        append("[\n")
        list.forEach { item ->
            writeIndent(indent + 1)
            writeValue(item, indent + 1)
            append(",\n")
        }
        writeIndent(indent)
        append("]")
    }

    private fun StringBuilder.writeIndent(indent: Int) {
        repeat(indent) {
            append(indentUnit)
        }
    }

    private fun String.escape(): String {
        return replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}