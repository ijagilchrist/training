package org.training.simulate.trade;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.training.data.CandleRepository;
import org.training.data.cache.InstrumentDataCache;
import org.training.data.postgresql.PsqlCandleRepository;
import org.training.data.redis.RedisCandleRepository;
import org.training.model.Candle;
import org.training.model.Candles;
import org.training.model.EntryPoint;
import org.training.model.InstrumentData;
import org.training.model.LocalTimeSupport;
import org.training.model.Trade;
import org.training.simulate.config.StochasticConfig;
import org.training.simulate.config.StochasticConfig.Type;
import org.training.simulate.strategy.Strategy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DailyTrading implements LocalTimeSupport {

    public enum DataSource { POSTGRES, REDIS };
    
    private final StochasticConfig config;
    private final DataSource dataSource;

    public List<Trade> trade(Strategy strategy, Instant from, Instant to) {
        
        String instrument = config.getInstrument();
        boolean isLong = config.getType() == Type.LONG;
        InstrumentData instrumentData = InstrumentDataCache.cache.getByName(instrument);
        int leverage = instrumentData.instrument.leverage;
        double spread = instrumentData.spread;

        List<Trade> trades = new ArrayList<>();

        CandleRepository repo = this.getDataSource();
      
        for (Instant day=from; day.compareTo(to)<=0; day=day.plus(1,ChronoUnit.DAYS)) {
    
            if (this.isWeekend(day)) continue;
    
            Instant start = this.getLocalTime(day,config.getTradeFrom());
            Instant end = this.getLocalTime(day,config.getTradeTo());
    
            Candles candles = repo.getCandles(instrument,start.minus(6*1440,ChronoUnit.MINUTES),end.plus(config.getTradingPeriod()+1,ChronoUnit.MINUTES));
            if (candles == null || candles.length() == 0) continue;
        
            EntryPoint entryPoint = strategy.getEntry(candles,start,end);
            while(entryPoint != null && entryPoint.getEntry().isBefore(start)) {
                Instant next = entryPoint.getEntry().plus(config.getInterval(),ChronoUnit.MINUTES);
                entryPoint = strategy.getEntry(candles,next,end);
            }
            
            while (entryPoint != null && !entryPoint.getEntry().isAfter(end)) {
                
                Instant tradeFrom = entryPoint.getEntry();
                Instant tradeTo = tradeFrom.plus(config.getTradingPeriod()+1,ChronoUnit.MINUTES);
        
                Candle first = candles.get(tradeFrom);
                if (first != null) {

                    Trading tradeEvents = new Trading(instrument,isLong,leverage,spread,config.getInterval());
                    Trade trade = tradeEvents.trade(entryPoint,candles.getSubset(tradeFrom,tradeTo));
                    trades.add(trade);
                
                }
            
                Instant next = entryPoint.getEntry().plus(config.getInterval(),ChronoUnit.MINUTES);
                entryPoint = strategy.getEntry(candles,next,end);

            }
            
        }

        return trades;
        
    }

    public static List<Trade> sort(List<Trade> trades) {

        List<Trade> sorted = new ArrayList<>(trades);
        sorted.sort((a,b) -> Long.compare(a.getEntryPoint().getEntry().getEpochSecond(),b.getEntryPoint().getEntry().getEpochSecond()));
        return sorted;

    }

    public static List<Trade> deconflict(List<Trade> trades, int gap) {

        List<Trade> deconflicted = new ArrayList<>();
        
        Instant startFrom = Instant.EPOCH;
        for (Trade trade: sort(trades)) {

            Instant start = trade.getEntryPoint().getEntry();
            if (start.isBefore(startFrom)) continue;

            int elapsed = trade.outcome().getElapsed();
            startFrom = start.plus(elapsed+gap,ChronoUnit.MINUTES);

            deconflicted.add(trade);

        }

        return deconflicted;

    }

    private CandleRepository getDataSource() {
       
        switch (this.dataSource) {

            case POSTGRES:
                return new PsqlCandleRepository();

            case REDIS:
                return new RedisCandleRepository();

            default:
                return null;
                
        }

    }

}
