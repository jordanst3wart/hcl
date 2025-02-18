package bot.stewart.hcl.parser

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HclTest {

    // TODO change to list
    @Test
    fun `parse success and failure`() {
        Hcl.parse("key = \"value\"").let { result ->
            assertEquals(true, result is FileParseResult.Success)
        }
        Hcl.parse("key = not_quoted").let { result ->
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
    }

    @ParameterizedTest(name = "comments are ignored {0}")
    @ValueSource(strings = [
        "# some comment",
        "key = false # some comment",
        "key = \"string\" # some comment",
        "key = 1 # some comment"])
    fun `ignores comments`(value: String) {
        Hcl.parse(value).let { result ->
            assertEquals(true, result is FileParseResult.Success)
        }
    }

    @Test
    fun `file doesn't exist`() {
        val doesntExist = File("doesntExist.tfvars")
        Hcl.parse(doesntExist).let { result ->
            assertEquals(true, result is FileParseResult.Error)
            result as FileParseResult.Error
            assertEquals("File does not exist: ${doesntExist.absolutePath}", result.message)
        }
    }

    @Test
    fun `parse basic values bools`() {
        Hcl.parse("key = false").let { result ->
            assertEquals(true, result is FileParseResult.Success)
            result as FileParseResult.Success
            assertFalse(result.value["key"] as Boolean, "Failed to parse false boolean")
        }
        Hcl.parse("key = true").let { result ->
            assertEquals(true, result is FileParseResult.Success)
            result as FileParseResult.Success
            assertTrue(result.value["key"] as Boolean, "Failed to parse true boolean")
        }
    }

    @ParameterizedTest(name = "booleans quoted {0}")
    @ValueSource(strings = ["\"false\"", "\"true\""])
    fun `booleans quoted are booleans`(value: String) {
        Hcl.parse("key = $value").let { result ->
            assertTrue(result is FileParseResult.Success)
            assertTrue(result.value["key"] is String)
        }
    }

    @Test
    fun `parse basic values`() {
        val differentTypes = this::class.java.getResourceAsStream("/different_types.tfvars")?.bufferedReader()?.readLines()!!
        Hcl.parse(differentTypes).let { result ->
            assertEquals(true, result is FileParseResult.Success)
            result as FileParseResult.Success
            result.value.let { map ->
                assertEquals(4, map.size)
                assertEquals("string", map["string"])
                assertEquals(3, map["int"])
                assertEquals(75.5, map["float"])
                assertEquals(true, map["bool"])
            }
        }
    }
}