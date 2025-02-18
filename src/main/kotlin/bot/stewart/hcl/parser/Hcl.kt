package bot.stewart.hcl.parser

import java.io.File

// Version with error handling
sealed class FileParseResult<out T> {
    data class Success<T>(val value: T) : FileParseResult<T>()
    data class Error(val message: String) : FileParseResult<Nothing>()
}

/*
    * HCL Parser
    * can read a file, string, or stream of strings, and converts it to a map of key-value pairs
 */
// TODO string contents...
// TODO stream input
object Hcl {
    fun parse(file: File): FileParseResult<Map<String, Any?>> {
        return try {
            if (!file.exists()) {
                return FileParseResult.Error("File does not exist: ${file.absolutePath}")
            }

            // TODO check bufferedReader is needed
            file.bufferedReader().useLines { lines ->
                val result = parseLines(lines)
                FileParseResult.Success(result)
            }
        } catch (e: Exception) {
            FileParseResult.Error("Failed to parse file: ${e.message}")
        }
    }

    fun parse(content: String): FileParseResult<Map<String, Any?>> {
        return try {
            val result = parseLines(content.lineSequence())
            FileParseResult.Success(result)
        } catch (e: Exception) {
            FileParseResult.Error("Failed to parse file: ${e.message}")
        }
    }

    fun parse(content: List<String>): FileParseResult<Map<String, Any?>> {
        return try {
            val result = parseLines(content.asSequence())
            FileParseResult.Success(result)
        } catch (e: Exception) {
            FileParseResult.Error("Failed to parse file: ${e.message}")
        }
    }

    fun parse(content: Sequence<String>): FileParseResult<Map<String, Any?>> {
        return try {
            val result = parseLines(content)
            FileParseResult.Success(result)
        } catch (e: Exception) {
            FileParseResult.Error("Failed to parse file: ${e.message}")
        }
    }

    private fun parseLines(lines: Sequence<String>): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        lines.forEachIndexed { index, line ->
            try {
                // Skip comments and empty lines
                if (line.isBlank() || line.trimStart().startsWith("#")) {
                    return@forEachIndexed
                }

                // Split on the first equals sign
                // TODO will need to deal with ":" as well
                val parts = line.split("=", limit = 2)
                // TODO will error if multiple equals signs i.e "key = \"value = value\""
                if (parts.size != 2) {
                    return@forEachIndexed
                }

                val key = parts[0].trim()
                val valueStr = parts[1].trim()

                // Parse the value based on its format
                val value = parseValue(valueStr)

                if (key.isNotEmpty()) {
                    result[key] = value
                }
            } catch (e: Exception) {
                throw IllegalStateException("Error parsing line ${index + 1}: ${e.message}")
            }
        }

        return result
    }

    // TODO could make this a sealed class
    private fun parseValue(valueStr: String): Any? {
        return when {
            valueStr.equals("null", ignoreCase = false) -> null
            valueStr.equals("true", ignoreCase = false) -> true
            valueStr.equals("false", ignoreCase = false) -> false
            valueStr.toIntOrNull() != null -> valueStr.toInt()
            valueStr.toDoubleOrNull() != null -> valueStr.toDouble() // float???
            valueStr.startsWith("\"") && valueStr.endsWith("\"") -> cleanString(valueStr)
            else -> throw IllegalArgumentException("Unknown value type: $valueStr")
        }
    }

    private fun cleanString(valueStr: String): String {
        return valueStr.trim().let {
            if (it.startsWith("\"") && it.endsWith("\"")) {
                it.substring(1, it.length - 1)
            } else {
                it
            }
        }
    }
}