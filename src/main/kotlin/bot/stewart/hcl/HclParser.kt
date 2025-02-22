package bot.stewart.hcl

import java.io.File
import java.io.FileNotFoundException

class HclParser {
    private var position = 0
    private var input = ""

    // TODO should add Sequence<String> as input, or stream strings
    fun parse(file: File): Map<String, Any?> {
        if (!file.exists()) {
            throw FileNotFoundException("File does not exist: ${file.absolutePath}")
        }
        val text = file.readText()
        return parse(text)
    }

    fun parse(text: String): Map<String, Any?> {
        input = text
        position = 0
        val result = mutableMapOf<String, Any?>()

        while (position < input.length) {
            skipWhitespace()
            if (position >= input.length) break

            if (skipLineIfCommented()) continue
            val key = parseKey()
            skipWhitespace()
            expect('=') // TODO Or ':'
            skipWhitespace()
            val value = parseValue()
            result[key] = value
        }

        return result
    }

    private fun skipLineIfCommented(): Boolean {
        if (input[position] == '#') {
            while (position < input.length && input[position] != '\n') {
                position++
            }
            return true
        }
        return false
    }

    private fun parseKey(): String {
        val start = position
        assertValidKey(input[position].toString())
        while (position < input.length && (input[position].isLetterOrDigit() || input[position] == '_')) {
            position++
        }
        return input.substring(start, position)
    }

    private fun assertValidKey(key: String) {
        if (input.isNotEmpty() && key[0].isDigit()) {
            throw IllegalStateException("Key cannot start with a digit")
        }
    }

    private fun parseValue(): Any? {
        return when {
            input[position] == '"' -> parseString()
            input[position] == '{' -> parseObject()
            input[position] == '[' -> parseList()
            input[position].isDigit() || input[position] == '-' -> parseNumber()
            input.substring(position).startsWith("null") -> {
                position += 4
                null
            }
            input.substring(position).startsWith("true") -> {
                position += 4
                true
            }
            input.substring(position).startsWith("false") -> {
                position += 5
                false
            }
            input.substring(position).startsWith("<<-") -> {
                parseHeredoc(isIndented = true)
            }
            input.substring(position).startsWith("<<") -> {
                parseHeredoc(isIndented = false)
            }
            else -> throw IllegalStateException("Unexpected character at position $position: ${input[position]}")
        }
    }

    private fun parseString(): String {
        expect('"')
        val start = position
        while (position < input.length && input[position] != '"') {
            if (input[position] == '\\') position++
            position++
        }
        val value = input.substring(start, position)
        expect('"')
        return value
    }

    private fun parseNumber(): Number {
        val start = position
        var hasDecimal = false

        if (input[position] == '-') position++

        while (position < input.length &&
            (input[position].isDigit() || (!hasDecimal && input[position] == '.'))
        ) {
            if (input[position] == '.') hasDecimal = true
            position++
        }

        val numStr = input.substring(start, position)
        return if (hasDecimal) {
            numStr.toDouble()
        } else {
            // maybe to long
            numStr.toInt()
        }
    }

    private fun parseList(): List<Any?> {
        expect('[')
        val values = mutableListOf<Any?>()

        while (position < input.length && input[position] != ']') {
            skipWhitespace()
            skipLineIfCommented() // skipping to next line if commented, and skipping white space
            skipWhitespace()
            if (input[position] == ']') break
            values.add(parseValue())
            skipWhitespace()

            if (input[position] == ',') {
                position++
            }
        }

        expect(']')
        return values
    }

    private fun parseObject(): Map<String, Any?> {
        expect('{')
        val properties = mutableMapOf<String, Any?>()

        while (position < input.length && input[position] != '}') {
            skipWhitespace()
            if (input[position] == '}') break

            if (skipLineIfCommented()) continue
            val key = parseKey()
            skipWhitespace()
            expect('=')
            skipWhitespace()
            val value = parseValue()
            properties[key] = value

            skipWhitespace()
            if (input[position] == ',') {
                position++
            }
        }

        expect('}')
        return properties
    }

    private fun skipWhitespace() {
        while (position < input.length && input[position].isWhitespace()) {
            position++
        }
    }

    private fun expect(char: Char) {
        if (position >= input.length || input[position] != char) {
            throw IllegalStateException(
                "Expected '$char' at position $position, surrounding characters: ${getSurroundingCharacters(input, position)}",
            )
        }
        position++
    }

    private fun getSurroundingCharacters(
        input: String,
        position: Int,
        range: Int = 2,
    ): String {
        val before = if (position - range >= 0) input.substring(position - range, position) else ""
        val after = if (position + range < input.length) input.substring(position + 1, position + 1 + range) else ""
        return before + after
    }

    private fun parseHeredoc(isIndented: Boolean): String {
        // Skip '<<' or '<<-'
        position += if (isIndented) 3 else 2

        // Read marker (e.g., 'EOF', 'POLICY', etc.)
        val markerStart = position
        while (position < input.length && !input[position].isWhitespace()) {
            position++
        }
        val marker = input.substring(markerStart, position)

        // Skip to next line
        while (position < input.length && input[position] != '\n') {
            position++
        }
        position++ // Skip the newline

        // Read content until we find the marker
        val contentStart = position
        var contentEnd = -1
        var baseIndentation = -1

        while (position < input.length) {
            // Check if this line is the end marker
            val lineStart = position

            // For indented heredoc, calculate base indentation from marker line
            if (isIndented) {
                var spaces = 0
                while (position < input.length && input[position].isWhitespace() && input[position] != '\n') {
                    spaces++
                    position++
                }
                var isMarker = true
                for (i in marker.indices) {
                    if (position + i >= input.length || input[position + i] != marker[i]) {
                        isMarker = false
                        break
                    }
                }

                if (isMarker) {
                    baseIndentation = spaces
                    contentEnd = lineStart
                    position += marker.length
                    break
                }
            } else {
                // For standard heredoc, marker must be at start of line
                if (input.substring(position).startsWith(marker)) {
                    contentEnd = lineStart
                    position += marker.length
                    break
                }
            }

            // Move to next line
            while (position < input.length && input[position] != '\n') {
                position++
            }
            position++ // Skip the newline
        }

        if (contentEnd == -1) {
            throw IllegalStateException("Heredoc marker '$marker' not found")
        }

        // Process the content
        val content = input.substring(contentStart, contentEnd)

        return if (isIndented && baseIndentation >= 0) {
            // For indented heredoc, remove common indentation
            content.split('\n').map { line ->
                if (line.isBlank()) {
                    line
                } else {
                    line.substring(
                        minOf(
                            baseIndentation,
                            line.indexOfFirst {
                                !it.isWhitespace()
                            }.takeIf { it >= 0 } ?: baseIndentation,
                        ),
                    )
                }
            }.joinToString("\n")
        } else {
            // For standard heredoc, return content as-is
            content
        }
    }
}
