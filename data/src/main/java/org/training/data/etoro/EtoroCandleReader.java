package org.training.data.etoro;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.training.model.Candle;
import org.training.model.Candles;
import org.json.JSONArray;
import org.json.JSONObject;

public class EtoroCandleReader {
    
    public Candles read(String instrument, JSONObject json) {

        List<Candle> candles = new ArrayList<Candle>();
        
        int interval = this.getInterval(json.getString("Interval"));

        JSONArray candles1 = json.getJSONArray("Candles");

        JSONArray candles2 = candles1.getJSONObject(0).getJSONArray("Candles");

        for (int i=0; i<candles2.length(); i++) {

            JSONObject candleJSON = candles2.getJSONObject(i);

            double low = candleJSON.getDouble("Low");
            double high = candleJSON.getDouble("High");
            double open = candleJSON.getDouble("Open");
            double close = candleJSON.getDouble("Close");
            Instant start = Instant.parse(candleJSON.getString("FromDate"));
            Instant end = start.plus(interval,ChronoUnit.MINUTES);

            Candle candle = new Candle(low,high,open,close,start,end);

            candles.add(candle);

        }
        
        return new Candles(instrument,candles);
        
    }
   
    private int getInterval(String interval) {

        switch (interval) {
            case "OneMinute":
                return 1;
            case "FiveMinutes":
                return 5;
            case "TenMinutes":
                return 10;
            case "FifteenMinutes":
                return 15;
            case "ThirtyMinutes":
                return 30;
            default:
                return 1;
        }
        
    }
 
}
