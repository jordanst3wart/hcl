package bot.stewart.hcl.parser

import java.io.File

// Version with error handling
sealed class FileParseResult<out T> {
    data class Success<T>(val value: T) : FileParseResult<T>()
    data class Error(val message: String) : FileParseResult<Nothing>()
}

// TODO rename to what is in the readme
object TerraformFileParser {
    fun parseTfvarsFile(filePath: String): FileParseResult<Map<String, Any>> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return FileParseResult.Error("File does not exist: $filePath")
            }

            val content = file.readLines()
            val result = parseLines(content)
            FileParseResult.Success(result)
        } catch (e: Exception) {
            FileParseResult.Error("Failed to parse file: ${e.message}")
        }
    }

    private fun parseLines(lines: List<String>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        lines.forEachIndexed { index, line ->
            try {
                // Skip comments and empty lines
                if (line.isBlank() || line.trimStart().startsWith("#")) {
                    return@forEachIndexed
                }

                // Split on the first equals sign
                // will need to deal with ":" as well
                val parts = line.split("=", limit = 2)
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

    private fun parseValue(valueStr: String): Any {
        // Remove quotes if present
        val cleanValue = valueStr.trim().let {
            if (it.startsWith("\"") && it.endsWith("\"")) {
                it.substring(1, it.length - 1)
            } else {
                it
            }
        }

        // Try parsing as different types
        return when {
            cleanValue.equals("true", ignoreCase = true) -> true
            cleanValue.equals("false", ignoreCase = true) -> false
            cleanValue.toIntOrNull() != null -> cleanValue.toInt()
            cleanValue.toDoubleOrNull() != null -> cleanValue.toDouble()
            else -> cleanValue
        }
    }
}