package com.yoonicode.calendarfaces.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.wear.provider.WearableCalendarContract
import java.util.*

data class CalendarEntry (
    var startTime: Calendar,
    var title: String
)

class CalendarUtils {
    companion object { // this is the Kotlin way of making a method static
        fun getSortedEvents(
            context: Context
        ): ArrayList<CalendarEntry> {
            val INSTANCE_PROJECTION: Array<String> = arrayOf(
                CalendarContract.Instances.ALL_DAY, // 0
                CalendarContract.Instances.BEGIN, // 1
                CalendarContract.Instances.TITLE // 2
            )
            val PROJECTION_ALL_DAY: Int = 0
            val PROJECTION_BEGIN_INDEX: Int = 1
            val PROJECTION_TITLE_INDEX: Int = 2

            val startMillis = Calendar.getInstance().timeInMillis
            val endMillis = startMillis + 1000 * 60 * 60 * 24

            val entries = ArrayList<CalendarEntry>()

            val builder = WearableCalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)
            context.contentResolver.query(
                builder.build(), INSTANCE_PROJECTION, null, null, null
            ).use {
                if(it == null) return ArrayList<CalendarEntry>();
                while(it.moveToNext()) {
                    val allDay = it.getInt(PROJECTION_ALL_DAY)
                    val beginVal = it.getLong(PROJECTION_BEGIN_INDEX)
                    val title = it.getString(PROJECTION_TITLE_INDEX)
                    if(beginVal < Calendar.getInstance().timeInMillis) continue
                    if(allDay == 1) continue
                    val beginCalendar = Calendar.getInstance().apply { timeInMillis = beginVal }
                    entries.add(CalendarEntry(beginCalendar, title))
                }
            }

            entries.sortBy { i -> i.startTime }
            return entries
        }
    }
}
