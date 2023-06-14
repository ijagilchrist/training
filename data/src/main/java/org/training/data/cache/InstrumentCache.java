package org.training.data.cache;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.training.model.Instrument;

import org.json.JSONObject;

import redis.clients.jedis.Jedis;

public class InstrumentCache {

    private String host = System.getProperty("redis-host", "localhost");
    private int port = Integer.valueOf(System.getProperty("redis-host", "6379"));
    public final int timeout = 30000;

    public static final InstrumentCache cache = new InstrumentCache();

    private final Set<Instrument> instruments;
    private final Map<Integer,Instrument> instrumentsByID;
    private final Map<String,Instrument> instrumentsByName;


    private InstrumentCache() {

        this.instruments = new TreeSet<Instrument>();
        this.instrumentsByID = new TreeMap<Integer,Instrument>();
        this.instrumentsByName = new TreeMap<String,Instrument>();
    
        boolean success = false;

        while (!success) {

            try (Jedis jedis = new Jedis(this.host,this.port,this.timeout)) {

                try {

                    String keyPattern = String.format("INSTRUMENT/DATA/*");

                    Set<String> keys = jedis.keys(keyPattern);
                    for (String key: keys) {
        
                        JSONObject json = new JSONObject(jedis.get(key));
                        Instrument instrument = Instrument.fromJSON(json);
                        
                        if (instrument.enabled) {

                            this.instruments.add(instrument);
                            this.instrumentsByID.put(instrument.id,instrument);
                            this.instrumentsByName.put(instrument.name,instrument);
    
                        }

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

    public SortedSet<Instrument> getInstruments() {

        return new TreeSet<Instrument>(this.instruments);

    }

    public Instrument getById(int id) {

        Instrument instrument = this.instrumentsByID.get(id);
        return instrument;
 
    }
    
    public Instrument getByName(String name) {

        Instrument instrument = this.instrumentsByName.get(name);
        return instrument;
 
    }

}
