package org.training.analysis;

import java.util.ArrayList;
import java.util.List;

import org.training.model.DataPoint;
import org.training.model.DataPoints;

public class SMA extends DataPoints {

    private SMA(List<DataPoint> dataPoints) {
        super(dataPoints);
    }
   
    public static SMA sma(DataPoints dataPoints, int period) {
    
        List<DataPoint> smaValues = new ArrayList<>();

        List<DataPoint> previousValues = new ArrayList<>();

        for (DataPoint dataPoint: dataPoints.dataPoints) {

            previousValues.add(dataPoint);
            while (previousValues.size() > period) previousValues.remove(0);

            if (previousValues.size() >= period) {

                double average = previousValues.stream().mapToDouble(d -> d.value).average().getAsDouble();
                smaValues.add(new DataPoint(dataPoint.candle,average));

            }

        }

        return new SMA(smaValues);
        
    } 
    
}
