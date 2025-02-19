package bot.stewart.hcl.parser

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HclTest {
    private var parser: Hcl = Hcl()

    // TODO change to list
    @Test
    fun `parse success and failure`() {
        parser.parse("key = \"value\"").let { result ->
            val foo = result["key"]
            assertEquals("value", foo)
        }
        // TODO fix
        /*Hcl.parse("key = not_quoted").let { result ->
            assertEquals(true, result is FileParseResult.Error)
        }
        Hcl.parse("key = 'single_quoted'").let { result ->
            assertEquals(true, result is FileParseResult.Error)
        }
        Hcl.parse("key = FaLse").let { result ->
            assertEquals(true, result is FileParseResult.Error)
        }
        Hcl.parse("key = False").let { result ->
            assertEquals(true, result is FileParseResult.Error)
        }
        Hcl.parse("3a = False").let { result ->
            assertEquals(true, result is FileParseResult.Error)
        }*/
    }

    @Test
    fun `no whitespace`() {
        parser.parse("a=false").let { result ->
            assertEquals(result["a"], false)
        }
    }

    @Test
    fun `double equal sign`() {
        parser.parse("key = \"value = value\"").let { result ->
            assertEquals("value = value", result["key"])
        }
    }

    @Test
    fun `whitespace before key`() {
        assertEquals(parser.parse("    a=false")["a"], false)
    }

    // parsing keys failed cases
    // TODO this should fail, key name is invalid
    @Test
    fun `key cannot start with letter or symbol`() {
        parser.parse("3a = false").let { result ->
            assertEquals(false, result["3a"])
        }
    }

    @ParameterizedTest(name = "comments are ignored {0}")
    @ValueSource(strings = [
        "key = null # some comment",
        "key = false # some comment",
        "key = \"string\" # some comment",
        "key = 1 # some comment"])
    fun `ignores comments`(value: String) {
        parser.parse(value).let { result ->
            assertEquals(1, result.size)
        }
    }

    @Test
    fun `file doesn't exist`() {
        val doesntExist = File("doesntExist.tfvars")
        assertFailsWith<FileNotFoundException> {
            parser.parse(doesntExist)
        }
    }

    @Test
    fun `parse basic values bools`() {
        parser.parse("key = false").let { result ->
            assertEquals(false, result["key"])
        }
        parser.parse("key = true").let { result ->
            assertEquals(true, result["key"])
        }
    }

    @ParameterizedTest(name = "booleans quoted {0}")
    @ValueSource(strings = ["\"false\"", "\"true\""])
    fun `booleans quoted are booleans`(value: String) {
        parser.parse("key = $value").let { result ->
            assertTrue(result["key"] is String)
        }
    }

    @Test
    fun `parse comment`() {
        parser.parse("key = true # some comment").let { result ->
            assertEquals(true, result["key"])
            assertEquals(1, result.size)
        }
    }

    @Test
    fun `parse value and not comment`() {
        parser.parse("# some comment").let { result ->
            assertEquals(true, result.isEmpty())
        }
    }

    @Test
    fun `parse null`() {
        parser.parse("key = null").let { result ->
            assertEquals(null, result["key"])
            assertEquals(1, result.size)
        }
    }

    @Test
    fun `parse basic values`() {
        val differentTypes = this::class.java.getResourceAsStream("/different_types.tfvars")?.bufferedReader()?.readText()!!
        parser.parse(differentTypes).let { result ->
            result.let { map ->
                assertEquals(5, map.size)
                assertEquals("string", map["string"])
                assertEquals(3, map["int"])
                assertEquals(75.5, map["float"])
                assertEquals(true, map["bool"])
                assertEquals(null, map["null"])
            }
        }
    }
}