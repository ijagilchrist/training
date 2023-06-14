package org.training.simulate.strategy;

import java.time.Instant;

import org.training.model.Candles;
import org.training.model.EntryPoint;

public interface Strategy {
    
    public EntryPoint getEntry(Candles candles, Instant from, Instant to);
    
}
