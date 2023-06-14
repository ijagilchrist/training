package org.training.analysis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.training.model.DataPoint;
import org.training.model.DataPoints;

public class StochasticMomentum extends DataPoints {

    private StochasticMomentum(List<DataPoint> dataPoints) {
        super(dataPoints);
    }
    public static StochasticMomentum sm(DataPoints dataPoints, int period1, int period2, int period3, double smoothing) {
    
        List<DataPoint> dsValues = new ArrayList<>();
        List<DataPoint> dhlValues = new ArrayList<>();

        List<DataPoint> previousValues = new ArrayList<>();
        
        for (DataPoint dataPoint: dataPoints.dataPoints) {

            previousValues.add(dataPoint);
            while (previousValues.size() > period1) previousValues.remove(0);

            if (previousValues.size() >= period1) {

                double min = previousValues.stream().mapToDouble(d -> d.value).min().getAsDouble();
                double max = previousValues.stream().mapToDouble(d -> d.value).max().getAsDouble();

                double ds = (dataPoint.value - (max+min)/2);
                dsValues.add(new DataPoint(dataPoint.candle,ds));
                
                double dhl = max-min;
                dhlValues.add(new DataPoint(dataPoint.candle,dhl));
                
            }

        }

        EMA ds = EMA.ema(EMA.ema(new DataPoints(dsValues){},period2,smoothing),period3,smoothing);
        EMA dhl = EMA.ema(EMA.ema(new DataPoints(dhlValues){},period2,smoothing),period3,smoothing);

        List<DataPoint> smValues = new ArrayList<>();
        for (Instant time: ds.times()) {
            DataPoint dsValue = ds.get(time);
            DataPoint dhlValue = dhl.get(time);
            smValues.add(new DataPoint(dsValue.candle,dsValue.value/dhlValue.value*100.0+50.0));
        }

        return new StochasticMomentum(smValues);
        
    } 
    
}
