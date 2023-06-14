package org.training.simulate.config;

import java.util.Map;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force=true)
@Getter
@Builder(toBuilder=true)
@ToString
public class StochasticConfig {

    public enum Type { LONG, SHORT };
    
    private final String instrument;
    private final Type type;
    private final int interval;
    private final String tradeFrom;
    private final String tradeTo;
    private final int tradingPeriod;
    private final double takeProfit;
    private final double stopLoss;
    private final Map<String,Range> filters;

    @RequiredArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PRIVATE, force=true)
    @Getter
    @ToString
    public static class Range {

        private final double lower;
        private final double upper;

    }

}
