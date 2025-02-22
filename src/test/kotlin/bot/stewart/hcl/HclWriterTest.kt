package bot.stewart.hcl

import kotlin.test.Test
import kotlin.test.assertEquals

class HclWriterTest {
    @Test
    fun `write basic values`() {
        val input =
            mapOf(
                "string" to "value",
                "number" to 42,
                "float" to 3.14,
                "boolean" to true,
                "null" to null,
            )

        val expected =
            """
            string = "value"
            number = 42
            float = 3.14
            boolean = true
            null = null
            
            """.trimIndent()

        assertEquals(expected, HclWriter.write(input))
    }

    @Test
    fun `write lists`() {
        val input =
            mapOf(
                "empty_list" to emptyList<String>(),
                "strings" to listOf("a", "b", "c"),
                "numbers" to listOf(1, 2, 3),
                "mixed" to listOf("string", 42, true),
            )

        val expected =
            """
            empty_list = []
            strings = [
              "a",
              "b",
              "c",
            ]
            numbers = [
              1,
              2,
              3,
            ]
            mixed = [
              "string",
              42,
              true,
            ]
            
            """.trimIndent()

        assertEquals(expected, HclWriter.write(input))
    }

    @Test
    fun `write maps`() {
        val input =
            mapOf(
                "tags" to
                    mapOf(
                        "Environment" to "production",
                        "Team" to "platform",
                    ),
            )

        val expected =
            """
            tags = {
              Environment = "production"
              Team = "platform"
            }
            
            """.trimIndent()

        assertEquals(expected, HclWriter.write(input))
    }

    @Test
    fun `write nested structure`() {
        val input =
            mapOf(
                "application_config" to
                    mapOf(
                        "frontend" to
                            mapOf(
                                "instances" to 2,
                                "size" to "t3.medium",
                                "zones" to listOf("us-west-2a", "us-west-2b"),
                                "scaling" to
                                    mapOf(
                                        "min" to 1,
                                        "max" to 5,
                                        "desired" to 2,
                                    ),
                            ),
                    ),
            )

        val expected =
            """
            application_config = {
              frontend = {
                instances = 2
                size = "t3.medium"
                zones = [
                  "us-west-2a",
                  "us-west-2b",
                ]
                scaling = {
                  min = 1
                  max = 5
                  desired = 2
                }
              }
            }
            
            """.trimIndent()

        assertEquals(expected, HclWriter.write(input))
    }

    @Test
    fun `write escaped strings`() {
        val input =
            mapOf(
                "escaped" to "line1\nline2\t\"quoted\"",
            )

        val expected =
            """
            escaped = "line1\nline2\t\"quoted\""
            
            """.trimIndent()

        assertEquals(expected, HclWriter.write(input))
    }
}
