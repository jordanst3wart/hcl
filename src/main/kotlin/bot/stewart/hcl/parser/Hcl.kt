package bot.stewart.hcl.parser

import java.io.File
import java.io.FileNotFoundException

class Hcl {
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
        while (position < input.length && (input[position].isLetterOrDigit() || input[position] == '_')) {
            position++
        }
        return input.substring(start, position)
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
            (input[position].isDigit() || (!hasDecimal && input[position] == '.'))) {
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
            throw IllegalStateException("Expected '$char' at position $position, surrounding characters: ${getSurroundingCharacters(input, position)}")
        }
        position++
    }

    private fun getSurroundingCharacters(input: String, position: Int, range: Int = 2): String {
        val before = if (position - range >= 0) input.substring(position - range, position) else ""
        val after = if (position + range < input.length) input.substring(position + 1, position + 1 + range) else ""
        return before + after
    }
}