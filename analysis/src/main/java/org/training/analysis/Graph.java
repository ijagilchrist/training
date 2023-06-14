package org.training.analysis;

import java.util.ArrayList;
import java.util.List;

import org.training.model.DataPoint;
import org.training.model.DataPoints;

public class Graph extends DataPoints {

    private Graph(List<DataPoint> dataPoints) {
        super(dataPoints);
    }
   
    public static Graph gradient(DataPoints dataPoints) {

        List<DataPoint> gradientValues = new ArrayList<>();

        DataPoint lastPoint = null;

        for (DataPoint dataPoint: dataPoints.dataPoints) {

            if (lastPoint != null) {

                double interval = (dataPoint.time().getEpochSecond() - lastPoint.time().getEpochSecond())/60.0; 
                double gradient = (dataPoint.value - lastPoint.value)/interval;
                gradientValues.add(new DataPoint(dataPoint.candle,gradient));

            }

            lastPoint = dataPoint;

        }

        return new Graph(gradientValues);
        
    } 
    
}
