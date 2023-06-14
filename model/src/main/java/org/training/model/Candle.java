package org.training.model;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import org.json.JSONObject;

@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force=true)
@EqualsAndHashCode
@ToString
public class Candle implements Comparable<Candle> {

    public final double low;
    public final double high;
    public final double open;
    public final double close;
    public final Instant start;
    public final Instant end;

    public int compareTo(Candle candle) {
        return this.start.compareTo(candle.start);
    }
    
    public Candle merge(Candle candle) {
        if (candle == null) return new Candle(low,high,open,close,start,end);
        double low = Math.min(this.low,candle.low);
        double high = Math.max(this.high,candle.high);
        double open = (this.start.compareTo(candle.start) < 0) ? this.open : candle.open;
        double close = (this.end.compareTo(candle.end) > 0) ? this.close : candle.close;
        Instant start = (this.start.compareTo(candle.start) < 0) ? this.start : candle.start;
        Instant end = (this.end.compareTo(candle.end) > 0) ? this.end : candle.end;
        return new Candle(low,high,open,close,start,end);
    }

    public Instant getIntervalStart(int interval) {

        ZonedDateTime zonedTime = ZonedDateTime.ofInstant(this.start,TimeZone.getDefault().toZoneId());
        int minutes = zonedTime.getMinute();
        int nearest = ((int)(minutes/interval)*interval);
        return this.start.truncatedTo(ChronoUnit.HOURS).plus(nearest,ChronoUnit.MINUTES);

    }

    public Instant getIntervalEnd(int interval) {

        Instant start = getIntervalStart(interval);
        return start.plus(interval,ChronoUnit.MINUTES);

    }

    public int interval() {

        return (int)(this.end.toEpochMilli() - start.toEpochMilli()) / 60000;

    }

    public JSONObject toJSON() {
        return new JSONObject()
                    .put("low",this.low)
                    .put("high",this.high)
                    .put("open",this.open)
                    .put("close",this.close)
                    .put("start",this.start.toString())
                    .put("end",this.end.toString());
    }

    public static Candle fromJSON(JSONObject candle) {

        double low = candle.getDouble("low");
        double high = candle.getDouble("high");
        double open = candle.getDouble("open");
        double close = candle.getDouble("close");
        Instant start = Instant.parse(candle.getString("start"));
        Instant end = Instant.parse(candle.getString("end"));
        
        return new Candle(low,high,open,close,start,end);

    }

}

