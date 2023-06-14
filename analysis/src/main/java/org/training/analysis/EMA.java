package org.training.analysis;

import java.util.ArrayList;
import java.util.List;

import org.training.model.DataPoint;
import org.training.model.DataPoints;

public class EMA extends DataPoints {

    private EMA(List<DataPoint> dataPoints) {
        super(dataPoints);
    }
   
    public static EMA ema(DataPoints dataPoints, int period, double smoothing) {
    
        double multiplier = smoothing/(1.0+(double)period);

        List<DataPoint> emaValues = new ArrayList<>();

        List<DataPoint> previousValues = new ArrayList<>();

        Double value = null;

        for (DataPoint dataPoint: dataPoints.dataPoints) {

            if (value != null) {

                value = dataPoint.value * multiplier + value * (1 - multiplier);
                emaValues.add(new DataPoint(dataPoint.candle,value));

            } else {

                previousValues.add(dataPoint);
                if (previousValues.size() == period) value = previousValues.stream().mapToDouble(d -> d.value).average().getAsDouble();

            }

        }

        return new EMA(emaValues);
        
    } 
    
}
