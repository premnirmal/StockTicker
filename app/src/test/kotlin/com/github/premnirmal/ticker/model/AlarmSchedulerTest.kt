package com.github.premnirmal.ticker.model

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.BaseUnitTest
import com.github.premnirmal.ticker.components.AppClock
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.DayOfWeek.FRIDAY
import org.threeten.bp.DayOfWeek.MONDAY
import org.threeten.bp.DayOfWeek.SATURDAY
import org.threeten.bp.DayOfWeek.SUNDAY
import org.threeten.bp.DayOfWeek.THURSDAY
import org.threeten.bp.DayOfWeek.TUESDAY
import org.threeten.bp.DayOfWeek.WEDNESDAY
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime

@HiltAndroidTest
class AlarmSchedulerTest : BaseUnitTest() {

  companion object {
    private const val TIME_1 = 1494842700000L // Monday 05/15/2017 10:05
    private const val TIME_2 = 1494669900000L // Saturday 05/13/2017 10:05
    private const val TIME_3 = 1494756300000L // Sunday 05/14/2017 10:05
    private const val TIME_4 = 1494957600000L // Tuesday 05/16/2017 18:00
    private const val TIME_5 = 1494612000000L // Friday 05/12/2017 18:00
    private const val TIME_6 = 1494951600000L // Tuesday 05/16/2017 16:20
    private const val TIME_7 = 1494950100000L // Tuesday 05/16/2017 15:55

    private const val FLIP_TIME_1 = 1494885600000L // Monday 05/15/2017 22:00
    private const val FLIP_TIME_2 = 1494712800000L // Saturday 05/13/2017 22:00
    private const val FLIP_TIME_3 = 1494799200000L // Sunday 05/14/2017 22:00
    private const val FLIP_TIME_4 = 1494914400000L // Tuesday 05/16/2017 06:00
    private const val FLIP_TIME_5 = 1494568800000L // Friday 05/12/2017 06:00

    private const val ONE_MINUTE_MS = 60 * 1000L
  }

  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var clock: AppClock
  private lateinit var appPreferences: AppPreferences
  private lateinit var alarmScheduler: AlarmScheduler

  @Before fun init() {
    clock = mock()
    sharedPreferences = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
    appPreferences = AppPreferences(ApplicationProvider.getApplicationContext(), sharedPreferences)
    alarmScheduler = AlarmScheduler(appPreferences, clock, mock())
    setSelectDays(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
  }

  private fun setSelectDays(selectedDays: Set<DayOfWeek>) {
    appPreferences.setUpdateDays(selectedDays.map {
      it.value.toString()
    }.toSet())
  }

  private fun setStartAndEndTime(startTime: String, endTime: String) {
    sharedPreferences.edit().putString(AppPreferences.START_TIME, startTime).apply()
    sharedPreferences.edit().putString(AppPreferences.END_TIME, endTime).apply()
  }

  private fun setNow(now: Long) {
    val instant = Instant.ofEpochMilli(now)
    val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
    val localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
    whenever(clock.currentTimeMillis()).thenReturn(now)
    whenever(clock.elapsedRealtime()).thenReturn(now)
    whenever(clock.todayLocal()).thenReturn(localDateTime)
    whenever(clock.todayZoned()).thenReturn(zonedDateTime)
  }

  @Test fun testAlarmTime_pastEndTime_withLastFetched_beforeEndTime() {
    setStartAndEndTime("09:30", "16:00")
    // Tuesday 16:20. Set the current time to be after the end time
    setNow(TIME_6)
    // Set last fetched to be before the end time
    val lastFetched = TIME_7 // 15:55

    // The next update should be in one minute
    val msToNextAlarm = alarmScheduler.msToNextAlarm(lastFetched)
    assertEquals(ONE_MINUTE_MS, msToNextAlarm)
  }

  @Test fun testNextAlarmTime_whenBetweenStartAndEndTime() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_1)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    assertEquals(appPreferences.updateIntervalMs, msToNextAlarm)
  }

  @Test fun testNextAlarmTime_whenPastEndTime() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_4)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val nextDay = clock.todayZoned().withHour(9).withMinute(30).plusDays(1)
    val expectedNext =
      nextDay.toInstant().toEpochMilli() - clock.currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun testNextAlarmTime_onFriday_pastEndTime() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_5)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingMonday = clock.todayZoned().withHour(9).withMinute(30).plusDays(3)
    val expectedNext =
      followingMonday.toInstant().toEpochMilli() - clock.currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun testNextAlarmTime_onSaturday() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_2)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingMonday = clock.todayZoned().withHour(9).withMinute(30).plusDays(2)
    val expectedNext =
      followingMonday.toInstant().toEpochMilli() - clock.currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun testNextAlarmTime_onSunday() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_3)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingMonday = clock.todayZoned().withHour(9).withMinute(30).plusDays(1)
    val expectedNext =
      followingMonday.toInstant().toEpochMilli() - clock.currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  //////////////////////////////////////////////////////////////
  // Tests for when start time is later than the end time.
  //////////////////////////////////////////////////////////////

  @Test fun flip_testNextAlarmTime_whenBetweenStartAndEndTime() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_1)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    assertEquals(appPreferences.updateIntervalMs, msToNextAlarm)
  }

  @Test fun flip_testNextAlarmTime_whenPastEndTime() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_4)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val nextDay = clock.todayZoned().withHour(21).withMinute(0)
    val expectedNext =
      nextDay.toInstant().toEpochMilli() - clock.currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun flip_testNextAlarmTime_onFriday_pastEndTime() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_5)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingMonday = clock.todayZoned().withHour(21).withMinute(0)
    val expectedNext =
      followingMonday.toInstant().toEpochMilli() - clock.currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun flip_testNextAlarmTime_onSaturday() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_2)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingMonday = clock.todayZoned().withHour(21).withMinute(0).plusDays(2)
    val expectedNext =
      followingMonday.toInstant().toEpochMilli() - clock.currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun flip_testNextAlarmTime_onSunday() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_3)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingMonday = clock.todayZoned().withHour(21).withMinute(0).plusDays(1)
    val expectedNext =
      followingMonday.toInstant().toEpochMilli() - clock.currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  //////////////////////////////////////////////////////////////
  // Tests for when non-default days are selected
  //////////////////////////////////////////////////////////////

  @Test fun testNextAlarmTime_saturdaySelected() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_5) // Friday 6pm

    // Select Saturday as an update day
    setSelectDays(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY))

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingSaturday = clock.todayZoned().withHour(9).withMinute(30).plusDays(1)
    val expectedNext =
      followingSaturday.toInstant().toEpochMilli() - clock.currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun testNextAlarmTime_sundaySelected() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_5) // Friday 6pm

    // Select Sunday as an update day
    setSelectDays(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SUNDAY))

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingSunday = clock.todayZoned().withHour(9).withMinute(30).plusDays(2)
    val expectedNext =
      followingSunday.toInstant().toEpochMilli() - clock.currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun flip_testNextAlarmTime_sundaySelected() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_5) // Friday 6am

    // Select Friday Sunday as an update day
    setSelectDays(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SUNDAY))

    var msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val fridayEvening = clock.todayZoned().withHour(21).withMinute(0)
    var expectedNext =
      fridayEvening.toInstant().toEpochMilli() - clock.currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)

    // Remove Friday as an update day
    setSelectDays(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, SUNDAY))
    msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val sundayEvening = clock.todayZoned().withHour(21).withMinute(0).plusDays(2)
    expectedNext =
      sundayEvening.toInstant().toEpochMilli() - clock.currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun testIsCurrentTimeWithinScheduledUpdateTime() {
    var isWithinScheduledPreferenceTime: Boolean

    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_1) // Monday 10:05am
    isWithinScheduledPreferenceTime = alarmScheduler.isCurrentTimeWithinScheduledUpdateTime()
    assertTrue(isWithinScheduledPreferenceTime)

    setStartAndEndTime("10:30", "16:30")
    setNow(TIME_1) // Monday 10:30am
    isWithinScheduledPreferenceTime = alarmScheduler.isCurrentTimeWithinScheduledUpdateTime()
    assertFalse(isWithinScheduledPreferenceTime)

    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_5) // Friday 6am
    isWithinScheduledPreferenceTime = alarmScheduler.isCurrentTimeWithinScheduledUpdateTime()
    assertFalse(isWithinScheduledPreferenceTime)

    setStartAndEndTime("21:00", "07:00")
    setNow(FLIP_TIME_5) // Friday 6am
    setSelectDays(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY))
    isWithinScheduledPreferenceTime = alarmScheduler.isCurrentTimeWithinScheduledUpdateTime()
    assertTrue(isWithinScheduledPreferenceTime)
    // Remove Friday as an update day
    setSelectDays(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY))
    isWithinScheduledPreferenceTime = alarmScheduler.isCurrentTimeWithinScheduledUpdateTime()
    assertFalse(isWithinScheduledPreferenceTime)

    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_4) // Tuesday 6pm
    setSelectDays(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
    isWithinScheduledPreferenceTime = alarmScheduler.isCurrentTimeWithinScheduledUpdateTime()
    assertFalse(isWithinScheduledPreferenceTime)

    setNow(TIME_3) // Sunday 10am
    isWithinScheduledPreferenceTime = alarmScheduler.isCurrentTimeWithinScheduledUpdateTime()
    assertFalse(isWithinScheduledPreferenceTime)
  }
}