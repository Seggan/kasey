package io.github.seggan.kasey

import com.tfowl.ktor.client.plugins.JsoupPlugin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.time.*
import java.time.format.DateTimeFormatter

internal inline fun constructClient(
    cookiesStorage: CookiesStorage,
    crossinline customConfig: HttpClientConfig<*>.() -> Unit = {}
) = HttpClient(CIO) {
    followRedirects = true
    install(HttpCookies) {
        storage = cookiesStorage
    }
    install(UserAgent) {
        agent = USER_AGENT
    }
    install(JsoupPlugin)
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
    customConfig()
}

private const val USER_AGENT = "Mozilla/5.0 (compatible; automated) Kasey/1.0"

internal val JsonElement.ulong: ULong get() = jsonPrimitive.content.toULong()

private val SIMPLE_TIME = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneOffset.UTC)
private val SIMPLE_TIME_REGEX = "\\d{1,2}:\\d{2} [AP]M".toRegex()

private val DAY_OF_WEEK_TIME = DateTimeFormatter.ofPattern("E h:mm a").withZone(ZoneOffset.UTC)
private val DAY_OF_WEEK_TIME_REGEX = "\\w{3} \\d{1,2}:\\d{2} [AP]M".toRegex()

private val MONTH_DAY_TIME = DateTimeFormatter.ofPattern("MMM d h:mm a").withZone(ZoneOffset.UTC)
private val MONTH_DAY_TIME_REGEX = "\\w{3} \\d{1,2} \\d{1,2}:\\d{2} [AP]M".toRegex()

private val MONTH_DAY_YEAR_TIME = DateTimeFormatter.ofPattern("MMM d, u h:mm a").withZone(ZoneOffset.UTC)
private val MONTH_DAY_YEAR_TIME_REGEX = "\\w{3} \\d{1,2}, \\d{4} \\d{1,2}:\\d{2} [AP]M".toRegex()

internal fun parseSeTime(time: String): Instant {
    return when {
        SIMPLE_TIME_REGEX.matches(time) -> {
            val localTime = LocalTime.parse(time, SIMPLE_TIME)
            localTime.atDate(LocalDate.now()).atZone(ZoneOffset.UTC).toInstant()
        }
        time.startsWith("yst ") -> {
            val localTime = LocalTime.parse(time.removePrefix("yst "), SIMPLE_TIME)
            localTime.atDate(LocalDate.now().minusDays(1)).atZone(ZoneOffset.UTC).toInstant()
        }
        DAY_OF_WEEK_TIME_REGEX.matches(time) -> {
            val temporalAccessor = DAY_OF_WEEK_TIME.parse(time)
            val localTime = LocalTime.from(temporalAccessor)
            val dayOfWeek = DayOfWeek.from(temporalAccessor)
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            var daysAgo = now.dayOfWeek.value - dayOfWeek.value
            if (daysAgo < 0) daysAgo += 7
            val date = now.toLocalDate().minusDays(daysAgo.toLong())
            localTime.atDate(date).atZone(ZoneOffset.UTC).toInstant()
        }
        MONTH_DAY_TIME_REGEX.matches(time) -> {
            val temporalAccessor = MONTH_DAY_TIME.parse(time)
            val localTime = LocalTime.from(temporalAccessor)
            val monthDay = MonthDay.from(temporalAccessor)
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val date = monthDay.atYear(now.year)
            localTime.atDate(date).atZone(ZoneOffset.UTC).toInstant()
        }
        MONTH_DAY_YEAR_TIME_REGEX.matches(time) -> {
            val dateTime = LocalDateTime.parse(time, MONTH_DAY_YEAR_TIME)
            dateTime.atZone(ZoneOffset.UTC).toInstant()
        }
        else -> throw IllegalArgumentException("Invalid time format: $time")
    }
}