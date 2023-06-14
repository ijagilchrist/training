package org.training.model;

import java.util.List;

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
public class Trade {

    private final String instrument;
    private final boolean tradeLong;
    private final EntryPoint entryPoint;
    private final List<Outcome> outcomes;
    
    public Outcome outcome() {

        return outcomes.size() > 0 ? outcomes.get(outcomes.size()-1) : null;

    }
    
}
