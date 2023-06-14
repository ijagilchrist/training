package org.training.data;

import java.time.Instant;

public interface CandleCache {
    
    public void initialise(String instrument, Instant from, Instant to);

}
