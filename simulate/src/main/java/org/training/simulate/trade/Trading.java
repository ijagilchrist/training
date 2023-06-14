package org.training.simulate.trade;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.training.model.Candle;
import org.training.model.Candles;
import org.training.model.EntryPoint;
import org.training.model.Outcome;
import org.training.model.Trade;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Trading {

    private final String instrument;
    private final boolean isLong;
    private final int leverage;
    private final double spread;
    private final int reportAt;
    
    public Trade trade(EntryPoint entryPoint, Candles candles) {
        
        Instant start = candles.first().start;
        Instant end = candles.last().start;
        double open = candles.first().open;

        List<Outcome> events = new ArrayList<>();
        
        Trade trade = new Trade(instrument,isLong,entryPoint,events);
        
        double maxProfit = -Double.MAX_VALUE;
        double maxLoss = Double.MAX_VALUE;
        for (Instant time=start; !time.isAfter(end); time=time.plus(1,ChronoUnit.MINUTES)) {

            Candle next = candles.get(time);
            if (next == null) continue;

            double profit = ((this.isLong ? next.high-open : open-next.low)-this.spread)/open*this.leverage*100;
            double loss = ((this.isLong ? next.low-open : open-next.high)-this.spread)/open*this.leverage*100;
            double close = ((this.isLong ? next.close-open : open-next.close)-this.spread)/open*this.leverage*100;
            
            int elapsed = (int)(time.getEpochSecond() - start.getEpochSecond())/60;
            if (loss < maxLoss || profit > maxProfit || elapsed%reportAt == 0 || next.start.equals(end)) {

                maxProfit = Math.max(profit,maxProfit);
                maxLoss = Math.min(loss,maxLoss);
                
                Outcome event = new Outcome(elapsed,round(close,4),round(maxProfit,4),round(maxLoss,4),next);
                events.add(event);
                
            }

        }

        return trade;
    
    }

    public static Trade filter(Trade trade, double takeProfit, double stopLoss, int maxElapsed) {

        List<Outcome> outcomes = new ArrayList<>();
        
        for (Outcome outcome: trade.getOutcomes()) {

            if (outcome.getElapsed() > maxElapsed) {
            
                break;

            } else if (outcome.getMaxLoss() < stopLoss) {

                outcomes.add(new Outcome(outcome.getElapsed(),stopLoss,outcome.getMaxProfit(),stopLoss,outcome.getCandle()));
                break;

            } else if (outcome.getMaxProfit() > takeProfit) {

                outcomes.add(new Outcome(outcome.getElapsed(),takeProfit,takeProfit,outcome.getMaxLoss(),outcome.getCandle()));
                break;

            } else {

                outcomes.add(outcome);

            }

        }

        return new Trade(trade.getInstrument(),trade.isTradeLong(),trade.getEntryPoint(),outcomes);

    }


    private double round(double d, int sf) {
        BigDecimal bd = new BigDecimal(d);
        bd = bd.round(new MathContext(sf));
        return bd.doubleValue();
    }}
