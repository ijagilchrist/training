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
@ToString
public class InstrumentData implements Comparable<InstrumentData> {

    public final Instrument instrument;
    public final Instant time;
    public final double spread;

    @Override
    public int compareTo(InstrumentData other) {
        if (this.instrument.compareTo(other.instrument) != 0) return this.instrument.compareTo(other.instrument);
        if (this.time.compareTo(other.time) != 0) return this.time.compareTo(other.time);
        return (int)Math.signum(this.spread-other.spread);
    }
 
    public JSONObject toJSON() {

        return new JSONObject()
                        .put("instrument",this.instrument.toJSON())
                        .put("time",this.time)
                        .put("spread",this.spread);
                    
    }
    
    public static InstrumentData fromJSON(JSONObject json) {

        Instrument instrument = Instrument.fromJSON(json.getJSONObject("instrument"));
        Instant time = json.has("time") ? Instant.parse(json.getString("time")) : Instant.now();
        double spread = json.getDouble("spread");

        return new InstrumentData(instrument,time,spread);
        
    }
    
}
