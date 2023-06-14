package org.training.data.cache;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.training.model.Instrument;
import org.training.model.InstrumentData;
import org.json.JSONArray;
import org.json.JSONObject;

import redis.clients.jedis.Jedis;

public class InstrumentDataCache {

    private String host = System.getProperty("redis-host", "localhost");
    private int port = Integer.valueOf(System.getProperty("redis-host", "6379"));
    public final int timeout = 30000;

    public static final InstrumentDataCache cache = new InstrumentDataCache();

    private final Set<InstrumentData> instrumentDatas;
    private final Map<Integer,InstrumentData> instrumentDatasByID;
    private final Map<String,InstrumentData> instrumentDatasByName;

    private InstrumentDataCache() {

        this.instrumentDatas = new TreeSet<InstrumentData>();
        this.instrumentDatasByID = new TreeMap<Integer,InstrumentData>();
        this.instrumentDatasByName = new TreeMap<String,InstrumentData>();

        this.refresh();

    }

    public SortedSet<InstrumentData> getInstruments() {

        return new TreeSet<InstrumentData>(this.instrumentDatas);

    }

    public InstrumentData getById(int id) {

        InstrumentData instrument = this.instrumentDatasByID.get(id);
        return instrument;
 
    }
    
    public InstrumentData getByName(String name) {

        InstrumentData instrument = this.instrumentDatasByName.get(name);
        return instrument;
 
    }

    public void refresh() {
    
        for (Instrument instrument: InstrumentCache.cache.getInstruments()) {

            double spread = this.getSpread(instrument.name);

            InstrumentData instrumentData = new InstrumentData(instrument,Instant.now(),spread);

            this.instrumentDatas.add(instrumentData);
            this.instrumentDatasByID.put(instrumentData.instrument.id,instrumentData);
            this.instrumentDatasByName.put(instrumentData.instrument.name,instrumentData);

        }

    }

    public void update(Instant time, JSONArray updates) {

        String keyFormat = "INSTRUMENT/LIVE/%s/%s";
     
        boolean success = false;

        while (!success) {

            try (Jedis jedis = new Jedis(this.host,this.port,this.timeout)) {

                try {

                    for (int i = 0; i<updates.length(); i++) {
                    
                        JSONObject json = updates.getJSONObject(i);

                        String instrumentName = json.getString("instrument");
                        json.remove("instrument");

                        String key = String.format(keyFormat,instrumentName,time);
                        String value = json.toString();

                        jedis.set(key,value);

                    }
                   
                    success = true;

                } catch (Exception e) {

                    e.printStackTrace();
                    
                    synchronized (e) {

                        try {
                            Random random = new Random();
                            int wait = random.nextInt(2000);
                            e.wait(wait);
                        } catch (InterruptedException e1) {
                        }

                    }

                }
            
            }

        }

    }

    private double getSpread(String instrumentName) {

        double spread = -1;

        boolean success = false;

        while (!success) {

            try (Jedis jedis = new Jedis(this.host,this.port,this.timeout)) {

                try {

                    SortedMap<Instant,Double> spreads = new TreeMap<Instant,Double>();
                    
                    String keyPattern = String.format("INSTRUMENT/LIVE/%s/*",instrumentName);
                    
                    Set<String> keys = jedis.keys(keyPattern);
                    for (String key: keys) {
        
                        String[] keyFields = key.split("/");

                        Instant time = Instant.parse(keyFields[3]);
                        JSONObject json = new JSONObject(jedis.get(key));
                        double buy = json.getDouble("buy");
                        double sell = json.getDouble("sell");
                        spreads.put(time,buy-sell);

                    }

                    int sampleSize = Math.min(spreads.size(),10);
                    if (sampleSize > 0) {
                        spread = spreads.entrySet().stream()
                                        .map(e->e.getValue()).skip(Math.max(0,spreads.size()-10))
                                        .collect(Collectors.summingDouble(Double::doubleValue))/sampleSize;
                    }
                    
                    success = true;

                } catch (Exception e) {

                    e.printStackTrace();
                    
                    synchronized (e) {

                        try {
                            Random random = new Random();
                            int wait = random.nextInt(2000);
                            e.wait(wait);
                        } catch (InterruptedException e1) {
                        }

                    }

                }
            
            }


        }

        return spread;
        
    }

}
