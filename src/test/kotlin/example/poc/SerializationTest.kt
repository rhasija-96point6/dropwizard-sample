package example.poc

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.lifecycle.Managed
import io.dropwizard.setup.Environment
import org.apache.commons.lang3.RandomStringUtils
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.junit.jupiter.api.*
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SerializationTest {

    private val wiremock = WireMockServer(WireMockConfiguration.options().port(8280).notifier(ConsoleNotifier(false)))
    private lateinit var requestId: String
    private lateinit var rteClinicServiceApplication: RTEClinicServiceApplication

    @BeforeAll
    fun setupAll() {
        rteClinicServiceApplication = RTEClinicServiceApplication()
        rteClinicServiceApplication.run("server")
    }

    @BeforeEach
    fun setUp() {
        wiremock.start()
        requestId = RandomStringUtils.randomAlphanumeric(10)
    }

    @AfterEach
    fun afterEach() {
        wiremock.resetAll()
        wiremock.stop()
    }

    @Test
    fun test() {
        assertEquals(1, 1)
    }

    companion object {

        fun getRandomDataGenerator(): EasyRandom {
            val parameters = EasyRandomParameters()
            parameters.stringLengthRange(3, 10)
            parameters.collectionSizeRange(1, 1)
            return EasyRandom(parameters)
        }

        // This will be populated using `ObjectMapper` when `RTEClinicServiceApplication` starts.
        // Reason it is lateinit is to avoid creating multiple object mappers all throughout the test.
        lateinit var SUCCESS_SEARCH_RESULTS: String
    }

    inner class RTEClinicServiceApplication() : Application<Configuration>() {

        lateinit var objectMapper: ObjectMapper

        override fun run(config: Configuration, environment: Environment) {
            objectMapper = environment.objectMapper
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                .registerModule(
                    KotlinModule.Builder()
                        .withReflectionCacheSize(512)
                        .configure(KotlinFeature.NullToEmptyCollection, false)
                        .configure(KotlinFeature.NullToEmptyMap, false)
                        .configure(KotlinFeature.NullIsSameAsDefault, false)
                        .configure(KotlinFeature.SingletonSupport, false)
                        .configure(KotlinFeature.StrictNullChecks, false)
                        .build()
                )
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // added for 1.3 -> 2.0 migration


            val randomDataGenerator = getRandomDataGenerator()
            val rteSearchResultsResponse = randomDataGenerator.nextObject(Customer::class.java)

            objectMapper.writeValueAsString(rteSearchResultsResponse)

            // Set up successful search results with latest object from static code above
            SUCCESS_SEARCH_RESULTS =
                """
                    {
                    	"firstName": "Super",
                        "lastName": "Man"
                """.trimIndent()
            val dependencies = RTEKBServiceDependencies()
            dependencies.start()
            environment.lifecycle().manage(dependencies)
        }
    }

    inner class RTEKBServiceDependencies(
    ) : Managed {

        override fun start() {}
        override fun stop() {}
    }
}