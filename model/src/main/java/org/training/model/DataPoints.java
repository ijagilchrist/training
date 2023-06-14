package org.training.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
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
public abstract class DataPoints {

    public final List<DataPoint> dataPoints;
    
    @EqualsAndHashCode.Exclude
    private int length;
    
    @EqualsAndHashCode.Exclude
    private Instant from;
    
    @EqualsAndHashCode.Exclude
    private Instant to;
    
    @EqualsAndHashCode.Exclude
    private List<Instant> times;
    
    @ToString.Include
    public int length() {

        if (this.length > 0) return this.length;
       
        return (this.length = this.dataPoints.size());

    }
    
    @ToString.Include
    public Instant from() {

        if (this.from != null) return this.from;
        if (this.dataPoints.size() == 0) return null;

        return (this.from = this.dataPoints.stream().map(dataPoint -> dataPoint.candle.start).min(Instant::compareTo).get());

    }
    
    @ToString.Include
    public Instant to() {

        if (this.to != null) return this.to;
        if (this.dataPoints.size() == 0) return null;

        return (this.to = this.dataPoints.stream().map(dataPoint -> dataPoint.candle.end).max(Instant::compareTo).get());

    }

    public List<Instant> times() {

        if (this.times != null) return this.times;

        return (this.times = this.dataPoints.stream().map(dataPoint -> dataPoint.time()).toList());

    }

    public DataPoint get(Instant time) {

        int p = Collections.binarySearch(this.times(),time);
        return (p >= 0) ? this.dataPoints.get(p) : null;
    
    }
     
    public JSONObject toJSON() {

        JSONArray dataPoints = new JSONArray();
        for (DataPoint dataPoint: this.dataPoints) dataPoints.put(dataPoint.toJSON());

        JSONObject json = new JSONObject();
        json.put("dataPoints",dataPoints);

        return json;

    }

    public static DataPoints fromJSON(JSONObject json) {

        JSONArray array = json.getJSONArray("dataPoints");

        List<DataPoint> dataPoints = new ArrayList<DataPoint>();
        for (int i=0; i<array.length(); i++) dataPoints.add(DataPoint.fromJSON(array.getJSONObject(i)));

        return new DataPoints(dataPoints) {};

    }

}
