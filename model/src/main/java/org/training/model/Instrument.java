package org.training.model;

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
public class Instrument implements Comparable<Instrument> {

    public final int id;
    public final String name;
    public final int leverage;
    public final boolean enabled;
 
    public JSONObject toJSON() {

        return new JSONObject()
                        .put("id",this.id)
                        .put("name",this.name)
                        .put("leverage",this.leverage)
                        .put("enabled",this.enabled);
                    
    }
    
    public static Instrument fromJSON(JSONObject json) {

        int id = json.getInt("id");
        String name = json.getString("name");
        int leverage = json.getInt("leverage");
        boolean enabled = json.getBoolean("enabled");

        return new Instrument(id,name,leverage,enabled);
        
    }

    @Override
    public int compareTo(Instrument other) {
        return this.name.compareTo(other.name);
    }

}

