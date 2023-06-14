package org.training.analysis;

import java.util.ArrayList;
import java.util.List;

import org.training.model.DataPoint;
import org.training.model.DataPoints;

public class RSI extends DataPoints {

    private RSI(List<DataPoint> dataPoints) {
        super(dataPoints);
    }
   
    public static RSI rsi(DataPoints dataPoints, int period) {
    
        List<DataPoint> rsiValues = new ArrayList<>();

        DataPoint lastDataPoint = null;
        
        double averageProfit = 0;
        double averageLosses = 0;

        int n = 0;
        double totalProfits = 0.0;
        double totalLosses = 0.0;

        for (DataPoint dataPoint: dataPoints.dataPoints) {

            if (lastDataPoint != null) {

                n++;
                
                double profit = Math.max(dataPoint.value-lastDataPoint.value,0);
                double loss = Math.max(lastDataPoint.value-dataPoint.value,0);
                
                if (n <= period) {

                    totalProfits += profit;
                    totalLosses += loss;
                    
                }
                
                if (n == period) {

                    averageProfit = totalProfits/n;
                    averageLosses = totalLosses/n;
                    
                    double rsi = 100.0 - 100.0 / ( 1 + averageProfit/averageLosses );
                    rsiValues.add(new DataPoint(dataPoint.candle,rsi));

                } else if (n > period) {

                    averageProfit = (averageProfit * (period-1) + profit) / period;
                    averageLosses = (averageLosses * (period-1) + loss) / period;
                    
                    double rsi = 100.0 - 100.0 / ( 1 + averageProfit/averageLosses );
                    rsiValues.add(new DataPoint(dataPoint.candle,rsi));

                }

            }

            lastDataPoint = dataPoint;

        }

        return new RSI(rsiValues);
        
    } 
    
}
