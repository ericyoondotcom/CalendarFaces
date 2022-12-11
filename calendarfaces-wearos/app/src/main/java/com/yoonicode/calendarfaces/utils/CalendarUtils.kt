package com.yoonicode.calendarfaces.utils

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.util.*

data class CalendarEntry (
    var startTime: Calendar,
    var title: String
)

class CalendarUtils {
    fun getFirstEvent(
        context: Context
    ): CalendarEntry? {
        val INSTANCE_PROJECTION: Array<String> = arrayOf(
            CalendarContract.Instances.TITLE, // 0
            CalendarContract.Instances.BEGIN, // 1
            CalendarContract.Instances.END, // 2
        )
        // The indices for the projection array above.
        val PROJECTION_TITLE_INDEX: Int = 0
        val PROJECTION_BEGIN_INDEX: Int = 1
        val PROJECTION_END_INDEX: Int = 2

        val startMillis = Calendar.getInstance().timeInMillis
        val endMillis = startMillis + 1000 * 60 * 60 * 24

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val i = context.contentResolver.query(
            builder.build(),
            INSTANCE_PROJECTION,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC, ${CalendarContract.Instances.END} ASC"
        ) ?: return null
        while(i.moveToNext()) {
            val beginVal = i.getLong(PROJECTION_BEGIN_INDEX)
            val endVal = i.getLong(PROJECTION_END_INDEX)
            val title = i.getString(PROJECTION_TITLE_INDEX)
            if(beginVal < Calendar.getInstance().timeInMillis) continue
            val beginCalendar = Calendar.getInstance().apply { timeInMillis = beginVal }
            return CalendarEntry(beginCalendar, title)
        }
        return null
    }
}
