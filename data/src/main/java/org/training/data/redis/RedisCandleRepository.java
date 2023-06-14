package org.training.data.redis;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import org.training.data.CandleCache;
import org.training.data.CandleRepository;
import org.training.model.Candle;
import org.training.model.Candles;
import org.json.JSONObject;

import redis.clients.jedis.Jedis;

@NoArgsConstructor
@AllArgsConstructor
public class RedisCandleRepository implements CandleRepository,CandleCache {

    private String host = System.getProperty("redis-host", "localhost");
    private int port = Integer.valueOf(System.getProperty("redis-port", "6379"));
    public final int timeout = 30000;
    
    private final Map<String,Map<Instant,Candle>> cache = new TreeMap<>();

    @Override
    public void initialise(String instrument, Instant from, Instant to) {
        
        Map<Instant,Candle> repo = this.cache.get(instrument);
        if (repo == null) {
            repo = Collections.synchronizedSortedMap(new TreeMap<Instant,Candle>());
            this.cache.put(instrument,repo);
        }
        
        Instant day = from;
        while (day.compareTo(to) <=0) {
            load(instrument,day,repo);
            day = day.plus(1,ChronoUnit.DAYS);    
        }
        
    }

    @Override
    public Candles getCandles(String instrument, Instant from, Instant to) {

        Map<Instant,Candle> candles = this.cache.get(instrument);
        List<Instant> times = new ArrayList<>(candles.keySet());

        List<Candle> filtered = times.stream()
                                     .filter(time -> time.compareTo(from) >=0)
                                     .filter(time -> time.compareTo(to) <0)
                                     .map(time -> candles.get(time))
                                     .toList();

        return new Candles(instrument,filtered);

    }

    @Override
    public boolean updateCandles(Candles candles) {

        Map<Instant,Candle> updates = new TreeMap<Instant,Candle>();

        Map<Instant,Candle> candleMap = this.cache.get(candles.instrument);
        if (candleMap == null) {
            candleMap = new TreeMap<Instant,Candle>();
            this.cache.put(candles.instrument,candleMap);
        }
        
        for (Candle candle: candles.candles) {

            Candle cached = candleMap.get(candle.start);

            if (cached == null || !cached.equals(candle)) updates.put(candle.start,candle);

        }

        candleMap.putAll(updates);
        store(candles.instrument,updates.keySet());
    
        return true;

    }

    private void load(String instrument, Instant today, Map<Instant, Candle> repo) {

        boolean success = false;

        while (!success) {

            try (Jedis jedis = new Jedis(this.host,this.port,this.timeout)) {

                try {

                    String keyPattern = String.format("%s/%s*",instrument,today.toString().substring(0,10));

                    Set<String> keys = jedis.keys(keyPattern);
                    for (String key: keys) {
        
                        JSONObject json = new JSONObject(jedis.get(key));
                        Candle candle = Candle.fromJSON(json);
        
                        repo.put(candle.start,candle);
        
                    }

                    success = true;

                } catch (Exception e) {

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

    private void store(String instrument, Set<Instant> updates) {

        boolean success = false;

        while (!success) {

            try (Jedis jedis = new Jedis(this.host,this.port,this.timeout)) {

                try {

                    for (Instant update: updates) {
                    
                        String key = String.format("%s/%s",instrument,update.toString());
                        String value = this.cache.get(instrument).get(update).toJSON().toString();
                        jedis.set(key,value);
        
                    }
    
                    success = true;
    
                } catch (Exception e) {
    
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

    
    protected boolean deleteAllCandles(String instrument) {

        boolean success = false;

        while (!success) {

            try (Jedis jedis = new Jedis(this.host,this.port,this.timeout)) {

                try {

                    String keyPattern = String.format("%s/*",instrument);

                    Set<String> keys = jedis.keys(keyPattern);
                    for (String key: keys) {
        
                        jedis.del(key);
        
                    }

                    success = true;

                } catch (Exception e) {

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

        return success;
        
    }

}
