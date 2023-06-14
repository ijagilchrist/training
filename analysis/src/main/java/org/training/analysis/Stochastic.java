package org.training.analysis;

import java.util.ArrayList;
import java.util.List;

import org.training.model.DataPoint;
import org.training.model.DataPoints;

public class Stochastic extends DataPoints {

    private Stochastic(List<DataPoint> dataPoints) {
        super(dataPoints);
    }
   
    public static Stochastic stochastic(DataPoints dataPoints, int period) {
    
        List<DataPoint> stochasticValues = new ArrayList<>();

        List<DataPoint> previousValues = new ArrayList<>();

        for (DataPoint dataPoint: dataPoints.dataPoints) {

            previousValues.add(dataPoint);
            while (previousValues.size() > period) previousValues.remove(0);

            if (previousValues.size() >= period) {

                double min = previousValues.stream().mapToDouble(d -> d.value).min().getAsDouble();
                double max = previousValues.stream().mapToDouble(d -> d.value).max().getAsDouble();

                double stochastic = (dataPoint.value - min) / (max - min) * 100;
                stochasticValues.add(new DataPoint(dataPoint.candle,stochastic));

            }

        }

        return new Stochastic(stochasticValues);
        
    } 
    
}
