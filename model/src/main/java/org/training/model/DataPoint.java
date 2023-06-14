package org.training.model;

import java.time.Instant;
import org.json.JSONObject;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force=true)
@EqualsAndHashCode
@ToString(onlyExplicitlyIncluded=true)
public class DataPoint {

    @ToString.Include
    public final Candle candle;
    @ToString.Include
    public final double value;
   
    @EqualsAndHashCode.Exclude
    private Instant time;

    @ToString.Include
    public Instant time() {

        if (this.time != null) return this.time;

        return (this.time = this.candle.start);

    }

    public JSONObject toJSON() {
        return new JSONObject()
                    .put("candle",this.candle.toJSON())
                    .put("value",this.value);
    }

    public static DataPoint fromJSON(JSONObject dataPoint) {

        Candle candle = Candle.fromJSON(dataPoint.getJSONObject("candle"));
        double value = dataPoint.getDouble("value");
        
        return new DataPoint(candle,value);

    }
    
}
