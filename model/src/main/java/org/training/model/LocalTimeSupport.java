package org.training.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

public interface LocalTimeSupport {
    
    default Instant getLocalTime(Instant day, String time) {

        String zonedDateTime = ZonedDateTime.ofInstant(day.truncatedTo(ChronoUnit.MINUTES),TimeZone.getDefault().toZoneId()).toString();
        String amendedDateTime = String.format("%s%s%s",zonedDateTime.substring(0,11),time,zonedDateTime.substring(16));
        return ZonedDateTime.parse(amendedDateTime).toInstant();

    }

    default String getTime(Instant datetime, String format) {

        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(datetime,TimeZone.getDefault().toZoneId());
        return zonedDateTime.format(DateTimeFormatter.ofPattern(format));

    }
    
    default DayOfWeek getDayOfWeek(Instant day) {

        ZonedDateTime date = ZonedDateTime.ofInstant(day,TimeZone.getDefault().toZoneId());
        return date.getDayOfWeek();

    }

    default boolean isWeekend(Instant day) {

        DayOfWeek dayOfWeek = this.getDayOfWeek(day);
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;

    }

}
