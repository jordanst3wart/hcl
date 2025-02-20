package bot.stewart.hcl

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HclParserTest {
    private var parser: HclParser = HclParser()

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

    @Test
    fun `parse map objects`() {
        val mapObjects = this::class.java.getResourceAsStream("/map_objects.tfvars")?.bufferedReader()?.readText()!!
        parser.parse(mapObjects).let { result ->
            result.let { map ->
                assertEquals(1, map.size)
                map["tags"].let { tags ->
                    assertEquals(true, tags is Map<*, *>)
                    tags as Map<String, String>
                    assertEquals(4, tags.size)
                    assertEquals("production", tags["Environment"])
                    assertEquals("platform", tags["Team"])
                    assertEquals("operations", tags["Owner"])
                    assertEquals("12345", tags["CostCenter"])
                }
            }
        }
    }

    @Test
    fun `parse map of lists`() {
        val mapOfLists = this::class.java.getResourceAsStream("/map_of_lists.tfvars")?.bufferedReader()?.readText()!!
        parser.parse(mapOfLists).let { result ->
            result.let { map ->
                assertEquals(1, map.size)
                map["security_groups"].let { securityGroups ->
                    assertEquals(true, securityGroups is Map<*, *>)
                    securityGroups as Map<String, List<String>>
                    assertEquals(3, securityGroups.size)

                    assertEquals(listOf("80", "443"), securityGroups["web"])
                    assertEquals(listOf("8080", "8443"), securityGroups["app"])
                    assertEquals(listOf("5432", "27017"), securityGroups["db"])
                }
            }
        }
    }

    @Test
    fun `parse nested maps`() {
        val nestedMaps = this::class.java.getResourceAsStream("/nested_maps.tfvars")?.bufferedReader()?.readText()!!
        parser.parse(nestedMaps).let { result ->
            result.let { map ->
                assertEquals(1, map.size)
                map["instance_settings"].let { instanceSettings ->
                    assertEquals(true, instanceSettings is Map<*, *>)
                    instanceSettings as Map<String, Map<String, Int>>
                    assertEquals(3, instanceSettings.size)

                    instanceSettings["small"].let { small ->
                        assertNotNull(small)
                        assertEquals(3, small.size)
                        assertEquals(2, small["cpu"])
                        assertEquals(4, small["memory"])
                        assertEquals(50, small["disk"])
                    }

                    instanceSettings["medium"].let { medium ->
                        assertNotNull(medium)
                        assertEquals(3, medium.size)
                        assertEquals(4, medium["cpu"])
                        assertEquals(8, medium["memory"])
                        assertEquals(100, medium["disk"])
                    }

                    instanceSettings["large"].let { large ->
                        assertNotNull(large)
                        assertEquals(3, large.size)
                        assertEquals(8, large["cpu"])
                        assertEquals(16, large["memory"])
                        assertEquals(200, large["disk"])
                    }
                }
            }
        }
    }

    @Test
    fun `parse complex nested structure`() {
        val nestedStructure = this::class.java.getResourceAsStream("/nested_structure.tfvars")?.bufferedReader()?.readText()!!
        parser.parse(nestedStructure).let { result ->
            result.let { map ->
                assertEquals(1, map.size)
                map["application_config"].let { appConfig ->
                    assertEquals(true, appConfig is Map<*, *>)
                    appConfig as Map<String, Map<String, Any>>
                    assertEquals(2, appConfig.size)

                    appConfig["frontend"].let { frontend ->
                        assertNotNull(frontend)
                        assertEquals(4, frontend.size)
                        assertEquals(2, frontend["instances"])
                        assertEquals("t3.medium", frontend["size"])
                        assertEquals(listOf("us-west-2a", "us-west-2b"), frontend["zones"])

                        (frontend["scaling"] as Map<String, Int>).let { scaling ->
                            assertEquals(3, scaling.size)
                            assertEquals(1, scaling["min"])
                            assertEquals(5, scaling["max"])
                            assertEquals(2, scaling["desired"])
                        }
                    }

                    appConfig["backend"].let { backend ->
                        assertNotNull(backend)
                        assertEquals(4, backend.size)
                        assertEquals(3, backend["instances"])
                        assertEquals("t3.large", backend["size"])
                        assertEquals(listOf("us-west-2a", "us-west-2b", "us-west-2c"), backend["zones"])

                        (backend["scaling"] as Map<String, Int>).let { scaling ->
                            assertEquals(3, scaling.size)
                            assertEquals(2, scaling["min"])
                            assertEquals(6, scaling["max"])
                            assertEquals(3, scaling["desired"])
                        }
                    }
                }
            }
        }
    }
}