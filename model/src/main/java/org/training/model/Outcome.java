package org.training.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.NoArgsConstructor;

@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force=true)
@EqualsAndHashCode
@Getter
@ToString
public class Outcome {

    private final int elapsed;
    private final double profit;
    private final double maxProfit;
    private final double maxLoss;
    private final Candle candle;
    
}
