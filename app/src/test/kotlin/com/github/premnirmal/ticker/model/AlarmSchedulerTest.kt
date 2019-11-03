package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.BaseUnitTest
import com.nhaarman.mockitokotlin2.whenever
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

    private val alarmScheduler: AlarmScheduler = AlarmScheduler()
    private val appPreferences: AppPreferences = alarmScheduler.appPreferences
  }

  @Before fun init() {
    setSelectDays(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
  }

  private fun setSelectDays(selectedDays: Set<DayOfWeek>) {
    appPreferences.setUpdateDays(selectedDays.map {
      it.value.toString()
    }.toSet())
  }

  private fun setStartAndEndTime(startTime: String, endTime: String) {
    val preferences = appPreferences.sharedPreferences
    preferences.edit().putString(AppPreferences.START_TIME, startTime).apply()
    preferences.edit().putString(AppPreferences.END_TIME, endTime).apply()
  }

  private fun setNow(now: Long) {
    val instant = Instant.ofEpochMilli(now)
    val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
    val localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
    val clock = appPreferences.clock()
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
    val nextDay = appPreferences.clock().todayZoned().withHour(9).withMinute(30).plusDays(1)
    val expectedNext =
      nextDay.toInstant().toEpochMilli() - appPreferences.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun testNextAlarmTime_onFriday_pastEndTime() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_5)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingMonday = appPreferences.clock().todayZoned().withHour(9).withMinute(30).plusDays(3)
    val expectedNext =
      followingMonday.toInstant().toEpochMilli() - appPreferences.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun testNextAlarmTime_onSaturday() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_2)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingMonday = appPreferences.clock().todayZoned().withHour(9).withMinute(30).plusDays(2)
    val expectedNext =
      followingMonday.toInstant().toEpochMilli() - appPreferences.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun testNextAlarmTime_onSunday() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_3)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingMonday = appPreferences.clock().todayZoned().withHour(9).withMinute(30).plusDays(1)
    val expectedNext =
      followingMonday.toInstant().toEpochMilli() - appPreferences.clock().currentTimeMillis()
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
    val nextDay = appPreferences.clock().todayZoned().withHour(21).withMinute(0)
    val expectedNext =
      nextDay.toInstant().toEpochMilli() - appPreferences.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun flip_testNextAlarmTime_onFriday_pastEndTime() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_5)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingMonday = appPreferences.clock().todayZoned().withHour(21).withMinute(0)
    val expectedNext =
      followingMonday.toInstant().toEpochMilli() - appPreferences.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun flip_testNextAlarmTime_onSaturday() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_2)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingMonday = appPreferences.clock().todayZoned().withHour(21).withMinute(0).plusDays(2)
    val expectedNext =
      followingMonday.toInstant().toEpochMilli() - appPreferences.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun flip_testNextAlarmTime_onSunday() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_3)

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingMonday = appPreferences.clock().todayZoned().withHour(21).withMinute(0).plusDays(1)
    val expectedNext =
      followingMonday.toInstant().toEpochMilli() - appPreferences.clock().currentTimeMillis()
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
    val followingSaturday = appPreferences.clock().todayZoned().withHour(9).withMinute(30).plusDays(1)
    val expectedNext =
      followingSaturday.toInstant().toEpochMilli() - appPreferences.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun testNextAlarmTime_sundaySelected() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_5) // Friday 6pm

    // Select Sunday as an update day
    setSelectDays(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SUNDAY))

    val msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val followingSunday = appPreferences.clock().todayZoned().withHour(9).withMinute(30).plusDays(2)
    val expectedNext =
      followingSunday.toInstant().toEpochMilli() - appPreferences.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test fun flip_testNextAlarmTime_sundaySelected() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_5) // Friday 6am

    // Select Friday Sunday as an update day
    setSelectDays(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SUNDAY))

    var msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val fridayEvening = appPreferences.clock().todayZoned().withHour(21).withMinute(0)
    var expectedNext =
      fridayEvening.toInstant().toEpochMilli() - appPreferences.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)

    // Remove Friday as an update day
    setSelectDays(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, SUNDAY))
    msToNextAlarm = alarmScheduler.msToNextAlarm(0L)
    val sundayEvening = appPreferences.clock().todayZoned().withHour(21).withMinute(0).plusDays(2)
    expectedNext =
      sundayEvening.toInstant().toEpochMilli() - appPreferences.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }
}