package org.training.data;

import java.time.Instant;

import org.training.model.Candles;

public interface CandleRepository {
    
    public Candles getCandles(String instrument, Instant from, Instant to);

    public boolean updateCandles(Candles candles);

}
