package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.Tools
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDateTime

/**
 * This class tells us whether the current time is within the refresh hours that the user has set.
 *
 * Created by premnirmal on 4/6/17.
 */
object Market {

  /**
   * @return whether the current time is within the refresh hours that the user has set.
   */
  fun isOpen(): Boolean {
    val hourOfDay = LocalDateTime.now().hour
    val minuteOfHour = LocalDateTime.now().minute
    val dayOfWeek = LocalDateTime.now().dayOfWeek

    val startTimez = Tools.startTime()
    val endTimez = Tools.endTime()

    run({
      // don't allow end time less than start time. reset to default time if so
      Tools.validateTimeSet(endTimez, startTimez)
    })

    var isOpen = false
    isOpen = dayOfWeek <= DayOfWeek.FRIDAY && dayOfWeek >= DayOfWeek.MONDAY
    if (isOpen) {
      isOpen = hourOfDay >= startTimez[0] && hourOfDay <= endTimez[0]
    }
    if (isOpen) {
      isOpen = minuteOfHour >= startTimez[1] && hourOfDay <= endTimez[1]
    }
    return isOpen
  }

}