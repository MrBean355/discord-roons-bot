package com.github.mrbean355.roons

import java.util.Calendar
import java.util.Date

/**
 * @return the current `Date` that has the [amount] of `Calendar` [field] subtracted.
 */
fun getTimeAgo(amount: Int, field: Int): Date {
    return Calendar.getInstance().run {
        add(field, -amount)
        time
    }
}
