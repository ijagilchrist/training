package org.training.analysis;

import java.util.List;
import org.training.model.Candles;
import org.training.model.DataPoint;
import org.training.model.DataPoints;

public class Price extends DataPoints {

    private Price(List<DataPoint> dataPoints) {
        super(dataPoints);
    }
    
    public static Price open(Candles candles) {

        return new Price(candles.candles.stream()
                                .map(candle -> new DataPoint(candle,candle.open))
                                .toList());
                     
    }
    
    public static Price high(Candles candles) {

        return new Price(candles.candles.stream()
                                .map(candle -> new DataPoint(candle,candle.high))
                                .toList());
                     
    }
    
    public static Price low(Candles candles) {

        return new Price(candles.candles.stream()
                                .map(candle -> new DataPoint(candle,candle.low))
                                .toList());
                     
    }
    
    public static Price close(Candles candles) {

        return new Price(candles.candles.stream()
                                .map(candle -> new DataPoint(candle,candle.close))
                                .toList());
                     
    }

}
