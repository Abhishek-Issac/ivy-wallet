package com.ivy.ui.time.impl

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone

/**
 * Regression coverage for issue #3495 - "the selected date is always one day
 * earlier than intended".
 *
 * Material3 hands back midnight UTC for the tapped day. The old code converted
 * that with a device-zone converter, which loses a day in every zone behind UTC
 * and looks correct in India or Japan - the reason it survived so long.
 */
@RunWith(TestParameterInjector::class)
class DatePickerUtcConversionTest {

    private lateinit var originalZone: TimeZone

    @Before
    fun setup() {
        originalZone = TimeZone.getDefault()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    @Test
    fun `keeps the chosen day in a zone behind UTC`(
        @TestParameter zone: String,
    ) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone))

        val chosen = LocalDate.of(2026, 11, 23)
        val millis = chosen.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        datePickerMillisToLocalDate(millis) shouldBe chosen
    }

    @Test
    fun `keeps every day of the year in a zone behind UTC`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))

        var day = LocalDate.of(2026, 1, 1)
        while (day.year == 2026) {
            val millis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            datePickerMillisToLocalDate(millis) shouldBe day
            day = day.plusDays(1)
        }
    }

    @Test
    fun `the old device-zone conversion is what produced the bug`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))

        val chosen = LocalDate.of(2026, 11, 23)
        val millis = chosen.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        // documents the regression: device-zone maths drops a day here
        val buggy = java.time.Instant.ofEpochMilli(millis)
            .atZone(TimeZone.getDefault().toZoneId())
            .toLocalDate()
        buggy shouldBe chosen.minusDays(1)

        datePickerMillisToLocalDate(millis) shouldBe chosen
    }
}
