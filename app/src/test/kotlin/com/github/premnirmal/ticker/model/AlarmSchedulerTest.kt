package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.BaseUnitTest
import com.github.premnirmal.ticker.Tools
import org.junit.Test
import org.mockito.Mockito.`when`
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class AlarmSchedulerTest : BaseUnitTest() {

  companion object {
    val TIME_1 = 1494857100000L // Monday 05/15/2017 10:05
    val TIME_2 = 1494684300000L // Saturday 05/13/2017 10:05
    val TIME_3 = 1494770700000L // Sunday 05/14/2017 10:05
    val TIME_4 = 1494972000000L // Tuesday 05/16/2017 18:00
    val TIME_5 = 1494626400000L // Friday 05/12/2017 18:00

    val FLIP_TIME_1 = 1494900000000L // Monday 05/15/2017 22:00
    val FLIP_TIME_2 = 1494727200000L // Saturday 05/13/2017 22:00
    val FLIP_TIME_3 = 1494813600000L // Sunday 05/14/2017 22:00
    val FLIP_TIME_4 = 1494928800000L // Tuesday 05/16/2017 06:00
    val FLIP_TIME_5 = 1494583200000L // Friday 05/12/2017 06:00
  }

  private fun setStartAndEndTime(startTime: String, endTime: String) {
    val preferences = Tools.instance.sharedPreferences
    preferences.edit().putString(Tools.START_TIME, startTime).apply()
    preferences.edit().putString(Tools.END_TIME, endTime).apply()
  }

  private fun setNow(now: ZonedDateTime) {
    setNow(now.toInstant().toEpochMilli())
  }

  private fun setNow(now: Long) {
    val instant = Instant.ofEpochMilli(now)
    val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
    val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val clock = Tools.clock()
    `when`(clock.currentTimeMillis()).thenReturn(now)
    `when`(clock.elapsedRealtime()).thenReturn(now)
    `when`(clock.todayLocal()).thenReturn(localDateTime)
    `when`(clock.todayZoned()).thenReturn(zonedDateTime)
  }

  @Test
  fun testNextAlarmTime_whenBetweenStartAndEndTime() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_1)

    val msToNextAlarm = AlarmScheduler.msToNextAlarm()
    assertEquals(Tools.updateInterval, msToNextAlarm)
  }

  @Test
  fun testNextAlarmTime_whenPastEndTime() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_4)

    val msToNextAlarm = AlarmScheduler.msToNextAlarm()
    val nextDay = Tools.clock().todayZoned().withHour(9).withMinute(30).plusDays(1)
    val expectedNext = nextDay.toInstant().toEpochMilli() -
        Tools.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test
  fun testNextAlarmTime_onFriday_pastEndTime() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_5)

    val msToNextAlarm = AlarmScheduler.msToNextAlarm()
    val followingMonday = Tools.clock().todayZoned().withHour(9).withMinute(30).plusDays(3)
    val expectedNext = followingMonday.toInstant().toEpochMilli() -
        Tools.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test
  fun testNextAlarmTime_onSaturday() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_2)

    val msToNextAlarm = AlarmScheduler.msToNextAlarm()
    val followingMonday = Tools.clock().todayZoned().withHour(9).withMinute(30).plusDays(2)
    val expectedNext = followingMonday.toInstant().toEpochMilli() -
          Tools.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test
  fun testNextAlarmTime_onSunday() {
    setStartAndEndTime("09:30", "16:30")
    setNow(TIME_3)

    val msToNextAlarm = AlarmScheduler.msToNextAlarm()
    val followingMonday = Tools.clock().todayZoned().withHour(9).withMinute(30).plusDays(1)
    val expectedNext = followingMonday.toInstant().toEpochMilli() -
        Tools.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  //////////////////////////////////////////////////////////////
  // Tests for when start time is later than the end time.
  //////////////////////////////////////////////////////////////

  @Test
  fun flip_testNextAlarmTime_whenBetweenStartAndEndTime() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_1)

    val msToNextAlarm = AlarmScheduler.msToNextAlarm()
    assertEquals(Tools.updateInterval, msToNextAlarm)
  }

  @Test
  fun flip_testNextAlarmTime_whenPastEndTime() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_4)

    val msToNextAlarm = AlarmScheduler.msToNextAlarm()
    val nextDay = Tools.clock().todayZoned().withHour(21).withMinute(0)
    val expectedNext = nextDay.toInstant().toEpochMilli() -
        Tools.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test
  fun flip_testNextAlarmTime_onFriday_pastEndTime() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_5)

    val msToNextAlarm = AlarmScheduler.msToNextAlarm()
    val followingMonday = Tools.clock().todayZoned().withHour(21).withMinute(0).plusDays(3)
    val expectedNext = followingMonday.toInstant().toEpochMilli() -
        Tools.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test
  fun flip_testNextAlarmTime_onSaturday() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_2)

    val msToNextAlarm = AlarmScheduler.msToNextAlarm()
    val followingMonday = Tools.clock().todayZoned().withHour(21).withMinute(0).plusDays(2)
    val expectedNext = followingMonday.toInstant().toEpochMilli() -
        Tools.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

  @Test
  fun flip_testNextAlarmTime_onSunday() {
    setStartAndEndTime("21:00", "04:00")
    setNow(FLIP_TIME_3)

    val msToNextAlarm = AlarmScheduler.msToNextAlarm()
    val followingMonday = Tools.clock().todayZoned().withHour(21).withMinute(0).plusDays(1)
    val expectedNext = followingMonday.toInstant().toEpochMilli() -
        Tools.clock().currentTimeMillis()
    assertEquals(expectedNext, msToNextAlarm)
  }

}