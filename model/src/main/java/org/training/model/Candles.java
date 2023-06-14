package org.training.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import org.json.JSONArray;
import org.json.JSONObject;

@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force=true)
@EqualsAndHashCode
@ToString(onlyExplicitlyIncluded=true)
public class Candles {

    @ToString.Include
    public final String instrument;
    public final List<Candle> candles;

    @EqualsAndHashCode.Exclude
    private int length;
    @EqualsAndHashCode.Exclude
    private int interval;
    @EqualsAndHashCode.Exclude
    private Instant from;
    @EqualsAndHashCode.Exclude
    private Instant to;
    @EqualsAndHashCode.Exclude
    private List<Instant> times;

    @ToString.Include
    public int length() {

        if (this.length > 0) return this.length;
       
        return (this.length = this.candles.size());

    }

    @ToString.Include
    public int interval() {

        if (this.interval > 0) return this.interval;
        if (this.candles.size() == 0) return 0;

        return (this.interval = this.candles.stream().map(candle -> candle.interval()).max(Integer::max).get());
       
    }
    
    @ToString.Include
    public Instant from() {

        if (this.from != null) return this.from;
        if (this.candles.size() == 0) return null;

        return (this.from = this.candles.stream().map(candle -> candle.start).min(Instant::compareTo).get());

    }
    
    @ToString.Include
    public Instant to() {

        if (this.to != null) return this.to;
        if (this.candles.size() == 0) return null;

        return (this.to = this.candles.stream().map(candle -> candle.end).max(Instant::compareTo).get());

    }

    public List<Instant> times() {

        if (this.times != null) return this.times;

        return (this.times = this.candles.stream().map(candle -> candle.start).toList());

    }

    public Candle get(Instant time) {

        int p = Collections.binarySearch(this.times(),time);
        return (p >= 0) ? this.candles.get(p) : null;
    
    }

    public Candle first() {

        if (this.candles.size() == 0) return null;

        return this.candles.get(0);

    }

    public Candle last() {

        if (this.candles.size() == 0) return null;

        return this.candles.get(this.candles.size()-1);

    }
    
    public Candles merge(int interval) {
        
        List<Candle> merged = new ArrayList<Candle>();
        Candle currentCandle = null;
        Instant currentStart = null;
        for (Candle candle: candles) {
            
            if (currentCandle != null) {

                Instant intervalStart = candle.getIntervalStart(interval);
                if (intervalStart.equals(currentStart)) {
                    currentCandle = currentCandle.merge(candle);
                } else {
                    merged.add(currentCandle);
                    currentCandle = candle;
                    currentStart = intervalStart;
                }
                
            } else {
                currentCandle = candle;
                currentStart = candle.getIntervalStart(interval);
            }

        }

        if (currentCandle != null) merged.add(currentCandle);

        return new Candles(this.instrument,merged);
        
    }

    public Candles getSubset(Instant from, Instant to) {

        List<Candle> subset = new ArrayList<Candle>();
        for (Candle candle: candles) {
            if (candle.start.compareTo(from) < 0) continue;
            if (candle.end.compareTo(to) > 0) break;
            subset.add(candle);
        }

        return new Candles(this.instrument,subset);
                
    }

    public JSONObject toJSON() {

        JSONArray candles = new JSONArray();
        for (Candle candle: this.candles) candles.put(candle.toJSON());

        JSONObject json = new JSONObject();
        json.put("instrument",this.instrument);
        json.put("candles",candles);
        
        return json;

    }

    public static Candles fromJSON(JSONObject json) {

        String instrument = json.getString("instrument");
        JSONArray candlesJSON = json.getJSONArray("candles");
       
        List<Candle> candles = new ArrayList<Candle>();
        for (int i=0; i<candlesJSON.length(); i++) candles.add(Candle.fromJSON(candlesJSON.getJSONObject(i)));
        
        return new Candles(instrument,candles);

    }

}
