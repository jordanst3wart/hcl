package bot.stewart.hcl

import kotlin.test.Test
import kotlin.test.assertEquals

class HclWriterPojoTest {
    data class SimpleConfig(
        val name: String,
        val count: Int,
        val enabled: Boolean,
    )

    data class ServerConfig(
        val size: String,
        val tags: Map<String, String>,
        val ports: List<Int>,
    )

    data class ScalingConfig(
        val min: Int,
        val max: Int,
        val desired: Int,
    )

    data class ApplicationConfig(
        val name: String,
        val server: ServerConfig,
        val scaling: ScalingConfig,
        val environment: String?,
    )

    @Test
    fun `write simple data class`() {
        val config =
            SimpleConfig(
                name = "test-app",
                count = 3,
                enabled = true,
            )

        val expected =
            """
            count = 3
            enabled = true
            name = "test-app"
            
            """.trimIndent()

        assertEquals(expected, HclWriter.write(config))
    }

    @Test
    fun `write nested data classes`() {
        val config =
            ApplicationConfig(
                name = "production-api",
                server =
                    ServerConfig(
                        size = "t3.large",
                        tags =
                            mapOf(
                                "Environment" to "production",
                                "Team" to "platform",
                            ),
                        ports = listOf(80, 443, 8080),
                    ),
                scaling =
                    ScalingConfig(
                        min = 2,
                        max = 6,
                        desired = 4,
                    ),
                environment = null,
            )

        val expected =
            """
            name = "production-api"
            scaling = {
              desired = 4
              max = 6
              min = 2
            }
            server = {
              ports = [
                80,
                443,
                8080,
              ]
              size = "t3.large"
              tags = {
                Environment = "production"
                Team = "platform"
              }
            }
            
            """.trimIndent()

        assertEquals(expected, HclWriter.write(config))
    }

    data class ConfigWithNullable(
        val required: String,
        val optional: String?,
    )

    @Test
    fun `write data class with nullable fields`() {
        val config =
            ConfigWithNullable(
                required = "value",
                optional = null,
            )

        val expected =
            """
            required = "value"
            
            """.trimIndent()

        assertEquals(expected, HclWriter.write(config))
    }

    data class ConfigWithList(
        val items: List<SimpleConfig>,
    )

    @Test
    fun `write data class with list of data classes`() {
        val config =
            ConfigWithList(
                items =
                    listOf(
                        SimpleConfig("item1", 1, true),
                        SimpleConfig("item2", 2, false),
                    ),
            )

        val expected =
            """
            items = [
              {
                count = 1
                enabled = true
                name = "item1"
              },
              {
                count = 2
                enabled = false
                name = "item2"
              },
            ]
            
            """.trimIndent()

        assertEquals(expected, HclWriter.write(config))
    }
}
