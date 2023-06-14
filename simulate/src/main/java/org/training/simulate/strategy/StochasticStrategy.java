package org.training.simulate.strategy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.training.analysis.EMA;
import org.training.analysis.Graph;
import org.training.analysis.Price;
import org.training.analysis.StochasticMomentum;
import org.training.model.Candles;
import org.training.model.DataPoint;
import org.training.model.EntryPoint;
import org.training.simulate.config.StochasticConfig.Range;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class StochasticStrategy implements Strategy {
    
    private final boolean isLong;
    private final int interval;
    private final Map<String,Range> filter;
    
    @Override
    public EntryPoint getEntry(Candles candles, Instant from, Instant to) {

        Candles merged = candles.merge(interval);
        
        Price closePrices = Price.close(merged);
        StochasticMomentum k = StochasticMomentum.sm(closePrices, 10, 3, 3, 2.0);
        EMA d = EMA.ema(k, 10, 2.0);
        Graph ema200 = Graph.gradient(EMA.ema(closePrices,200,2.0));

        Instant entry = null;
        Map<String,Double> features = new HashMap<>();
        for (Instant t1 = from; !t1.isAfter(to); t1 = t1.plus(interval, ChronoUnit.MINUTES)) {
            
            Instant t0 = t1.minus(interval, ChronoUnit.MINUTES);
            DataPoint k0 = k.get(t0);
            DataPoint k1 = k.get(t1);
            DataPoint d0 = d.get(t0);
            DataPoint d1 = d.get(t1);
            DataPoint e200 = ema200.get(t1);
            if (k0 == null || k1 == null || d0 == null || d1 == null || e200 == null) continue;
            
            boolean isEntry;
            if (!this.isLong) {
                isEntry = k0.value > d0.value && k1.value < d1.value;
            } else {
                isEntry = k0.value < d0.value && k1.value > d1.value;
            }
            if (!isEntry) continue;

            double offset = (t1.getEpochSecond()-from.getEpochSecond())/60;

            // double yi = this.isLong ? yIntercept(k0.value,k1.value,d0.value,d1.value) : 100.0 - yIntercept(k0.value,k1.value,d0.value,d1.value);
            // double dy = this.isLong ? k1.value - yIntercept(k0.value,k1.value,d0.value,d1.value) : -(k1.value - yIntercept(k0.value,k1.value,d0.value,d1.value));
            // double ema = this.isLong ? e200.value/d0.value*1.0e8 : -e200.value/d0.value*1.0e8;
            double yi = yIntercept(k0.value,k1.value,d0.value,d1.value);
            double dy = k1.value - yIntercept(k0.value,k1.value,d0.value,d1.value);
            double ema = e200.value/d0.value*1.0e8;
            features.put("d0",round(d0.value,4));
            features.put("d1",round(d1.value,4));
            features.put("k0",round(k0.value,4));
            features.put("k1",round(k1.value,4));
            features.put("yi",round(yi,4));
            features.put("dy",round(dy,4));
            features.put("dk",round(Math.abs(k1.value-k0.value),4));
            features.put("dd",round(Math.abs(d1.value-d0.value),4));
            features.put("e200",round(ema,4));
            features.put("offset",offset);
    
            Optional<Map.Entry<String,Range>> filtered = this.filter.entrySet().stream().filter(e -> filter(e,features)).findFirst();
            if (filtered.isPresent()) continue;

            entry = t1.plus(interval,ChronoUnit.MINUTES);
            break;
            
        }
        
        return entry != null ? new EntryPoint(entry,features) : null;

    }
    
    private boolean filter(Map.Entry<String,Range> entry, Map<String,Double> features) {

        Double feature = features.get(entry.getKey());
        if (feature == null) return true;

        Range range = entry.getValue();
        if (range == null) return true;

        return feature < range.getLower() || feature > range.getUpper();
        
    }

    private double yIntercept(double k0, double k1, double d0, double d1) {

        double m1 = m(0.0,1.0,k0,k1);
        double m2 = m(0.0,1.0,d0,d1);
        double c1 = c(0.0,k0,m1);
        double c2 = c(0.0,d0,m2);

        double xIntercept = (c2-c1)/(m1-m2);

        double y1 = y(xIntercept,m1,c1);
        double y2 = y(xIntercept,m2,c2);
        if (Math.abs(y2-y1) > 1.0e-6) {
            System.err.println(String.format("Screwed %.3e",Math.abs(y2-y1)));
        }

        return m1*xIntercept + c1;

    }

    private double m(double x1, double x2, double y1, double y2) {

        return (y2-y1)/(x2-x1);

    }

    private double c(double x, double y, double m) {

        return y - m*x;

    }

    private double y(double x, double m, double c) {

        return m*x + c;

    }
    
    private double round(double d, int sf) {
        BigDecimal bd = new BigDecimal(d);
        bd = bd.round(new MathContext(sf));
        return bd.doubleValue();
    }

}
