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
    }

    @Test
    fun `wrong boolean`() {
        assertFailsWith<IllegalStateException> {
            parser.parse("key = FaLse")
        }
    }

    @Test
    fun `wrong boolean again`() {
        assertFailsWith<IllegalStateException> {
            parser.parse("key = False")
        }
    }

    @Test
    fun `not quoted fails`() {
        assertFailsWith<IllegalStateException> {
            parser.parse("key = 'single_quoted'")
        }
    }

    @Test
    fun `single quoted fails`() {
        assertFailsWith<IllegalStateException> {
            parser.parse("key = not_quoted")
        }
    }

    @Test
    fun `wrong key`() {
        assertFailsWith<IllegalStateException> {
            parser.parse("3a = \"string\"")
        }
    }
    /* other invalid keys to test
        # symbols
        my@var = "invalid"
        special#name = "invalid"
        dollar$sign = "invalid"
        "my variable" = "invalid"
        space name = "invalid"
        # reserved words
        source = "invalid"
        module = "invalid"
        count = "invalid"
        TODO check these
        _hidden = "invalid"
        __name = "invalid"
     */

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

    @Test
    fun `parse lists`() {
        val lists = this::class.java.getResourceAsStream("/lists.tfvars")?.bufferedReader()?.readText()!!
        parser.parse(lists).let { result ->
            result.let { map ->
                assertEquals(4, map.size)
                assertEquals(listOf(80, 443, 8080), map["allowed_ports"])
                assertEquals(listOf("us-west-2a", "us-west-2b", "us-west-2c"), map["availability_zones"])
                assertEquals(listOf(true, false, true), map["boolean_values"])
                assertEquals(listOf(1, "two", true), map["mixed_values"])
            }
        }
    }

    // TODO test duplicate keys
    /*
    foo = true
    foo = false
     */
    @Test
    fun `parse heredoc`() {
        val heredoc = this::class.java.getResourceAsStream("/heredoc.tfvars")?.bufferedReader()?.readText()!!
        parser.parse(heredoc).let { result ->
            result.let { map ->
                assertEquals(2, map.size)
                assertEquals("#!/bin/bash\n" +
                        "echo \"Hello, World!\"\n" +
                        "yum update -y\n" +
                        "yum install -y httpd\n" +
                        "systemctl start httpd\n" +
                        "systemctl enable httpd\n", map["user_data2"])
                // TODO indented heredoc
                /*assertEquals("#!/bin/bash\n" +
                        "echo \"Hello, World!\"\n" +
                        "yum update -y\n" +
                        "yum install -y httpd\n" +
                        "systemctl start httpd\n" +
                        "systemctl enable httpd\n", map["user_data"])
                */
            }
        }
    }

    @Test
    fun `parse list of objects`() {
        val heredoc = this::class.java.getResourceAsStream("/list_objects.tfvars")?.bufferedReader()?.readText()!!
        parser.parse(heredoc).let { result ->
            result.let { map ->
                assertEquals(1, map.size)
                map["subnet_configs"].let { list ->
                    assertEquals(true, list is List<*>)
                    list as List<Map<String, Any?>>
                    assertEquals(2, list.size)
                    val expected : List<Map<String, Any?>> = listOf(mapOf("name" to "public-1",
                                 "cidr_block" to "10.0.1.0/24",
                                 "is_public" to true,
                                 "route_table" to "public"),
                            mapOf("name" to "private-1",
                                    "cidr_block" to "10.0.2.0/24",
                                    "is_public" to false,
                                    "route_table" to "private"))
                    assertEquals(expected, list)
                }
            }
        }
    }
}