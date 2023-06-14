package org.training.simulate.command;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.training.data.yaml.YAML;
import org.training.model.LocalTimeSupport;
import org.training.model.Outcome;
import org.training.model.Trade;
import org.training.simulate.config.StochasticConfig;
import org.training.simulate.config.StochasticConfig.Range;
import org.training.simulate.config.StochasticConfig.Type;
import org.training.simulate.graph.SimulationGraphs;
import org.training.simulate.trade.DailyTrading;
import org.training.simulate.trade.Trading;
import org.springframework.boot.CommandLineRunner;

import javafx.application.Platform;
import javafx.scene.chart.XYChart;

public class OptimiseTrades implements CommandLineRunner,LocalTimeSupport {

    private Settings initialSettings;
    private Settings currentSettings;
    private List<Trade> allTrades;
    private File configFile;
                
    @Override
    public void run(String... args) throws Exception {


        String instrument = args.length > 0 ? args[0].toUpperCase() : "EURUSD";
        Type type = args.length > 1 ? Type.valueOf(args[1].toUpperCase()) : Type.LONG;
        String inputName = String.format("%s-%s",instrument,type.name().toLowerCase());
        String initialCommands = args.length > 2 ? args[2] : "optimise";
        int interval = args.length > 3 ? Integer.valueOf(args[3]) : 15;
        Instant from = args.length > 4 ? Instant.parse(String.format("%sT01:00:00Z", args[4])) : Instant.parse("2021-04-25T01:00:00Z");
        Instant to = args.length > 5 ? Instant.parse(String.format("%sT01:00:00Z", args[5])) : Instant.now().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS);
        
        
        this.configFile = getFile(inputName,"datastore/config/",".yaml");
        StochasticConfig config;
        if (configFile.exists()) {
            config = YAML.read(configFile,StochasticConfig.class).toBuilder().interval(interval).build();
        } else {
            configFile = getFile("default","datastore/base-config/",".yaml");
            StochasticConfig defaultConfig = YAML.read(configFile,StochasticConfig.class);
            config = defaultConfig.toBuilder().instrument(instrument).type(type).interval(interval).build();
        }
        
        String entriesName = String.format("%s-%s-%d",config.getInstrument(),config.getType().name(),config.getInterval());
        File entriesFile = getFile(entriesName,"datastore/entries/",".yaml");

        this.initialSettings = new Settings(from,to,config);
        this.currentSettings = initialSettings;

        this.allTrades = YAML.readList(entriesFile,Trade.class);
        
        processCommands(initialCommands);
        List<Trade> trades = trade(this.currentSettings);
        if (trades !=null) new Renderer(trades).run();

        SimulationGraphs.start();

        try (Scanner scanner = new Scanner(System.in)) {

            while (true) {

                System.out.print("* ");
                String commands = scanner.nextLine().trim().toLowerCase();
                boolean redraw = processCommands(commands);

                if (redraw) {
                    trades = trade(this.currentSettings);
                    if (trades != null) Platform.runLater(new Renderer(trades));
                    sleep(5000);
                }

            }
    
        }

    }

    private void printSettings(Settings settings) {
        
        String ANSI_RED = "\u001B[31;47m";
        String ANSI_RESET = "\u001B[0m";
        
        String stringFormat = ANSI_RED+"> %-12s: %s"+ANSI_RESET;
        String intFormat = ANSI_RED+"> %-12s: %d"+ANSI_RESET;
        String percentageFormat = ANSI_RED+"> %-12s: %.2f%%"+ANSI_RESET;
        String rangeFormat = ANSI_RED+"> %-12s: >=%.2f, <=%.2f"+ANSI_RESET;

        System.out.println(String.format(stringFormat,"From",this.getTime(settings.getFrom(),"dd-MM-YYY")));
        System.out.println(String.format(stringFormat,"To",this.getTime(settings.getTo(),"dd-MM-YYY")));
        
        StochasticConfig config = settings.getConfig();
        System.out.println(String.format(intFormat,"Interval",config.getInterval()));
        System.out.println(String.format(stringFormat,"Trade From",config.getTradeFrom()));
        System.out.println(String.format(stringFormat,"Trade To",config.getTradeTo()));
        System.out.println(String.format(intFormat,"Trade Period",config.getTradingPeriod()));
        System.out.println(String.format(percentageFormat,"Take Profit",config.getTakeProfit()));
        System.out.println(String.format(percentageFormat,"Stop Loss",config.getStopLoss()));
        
        Map<String,Range> filters = config.getFilters();
        for (String name: filters.keySet()) {
            Range range = filters.get(name);
            System.out.println(String.format(rangeFormat,name,range.getLower(),range.getUpper()));        
        }
        
    }

    private void printResults(int n, double totalProfit, double cumProfit) {
        
        String ANSI_BLUE = "\u001B[34;47m";
        String ANSI_RESET = "\u001B[0m";
        
        String intFormat = ANSI_BLUE+"> %-12s: %d"+ANSI_RESET;
        String percentageFormat = ANSI_BLUE+"> %-12s: %.1f%%"+ANSI_RESET;
        String percentageFormat2 = ANSI_BLUE+"> %-12s: %.2f%%"+ANSI_RESET;
        String currencyFormat = ANSI_BLUE+"> %-12s: $%.2f"+ANSI_RESET;
        
        double avgProfit = n>0 ? totalProfit/n : 0.0;

        System.out.println(String.format(intFormat,"No trades",n));
        System.out.println(String.format(percentageFormat,"Total Profit",totalProfit));
        System.out.println(String.format(percentageFormat2,"Avg Profit",avgProfit));
        System.out.println(String.format(currencyFormat,"Cum Profit",cumProfit-1000.0));
        
    }

    private boolean processCommands(String commands) {

        boolean redraw = false;
        for (String command: commands.split(";")) {

            redraw = processCommand(command) || redraw; 
                    
        }

        return redraw;

    }
    private boolean processCommand(String rawCommand) {

        String command = rawCommand.trim().toLowerCase();

        System.out.println("Command> "+command);

        Pattern setValuePattern = Pattern.compile("([A-Za-z]+)\\s*=\\s*(.+)");
        Matcher setValueMatcher = setValuePattern.matcher(command);
    
        Pattern setRangePattern = Pattern.compile("([A-Za-z0-9]+)\\s*([<>])\\s*(.+)");
        Matcher setRangeMatcher = setRangePattern.matcher(command);
        
        if (command.equals("exit")) System.exit(1);

        boolean redraw = true;
        if (command.equals("clear")) {

            this.currentSettings = this.initialSettings;

        } else if (command.equals("reset")) {

            reset();

        } else if (command.equals("save")) {

            redraw = save();

        } else if (command.equals("optimise")) {
        
            this.currentSettings = optimise(this.currentSettings);
            
        } else if (setValueMatcher.matches()) {

            redraw = setValue(setValueMatcher.group(1),setValueMatcher.group(2));
            
        } else if (setRangeMatcher.matches()) {

            redraw = setRange(setRangeMatcher.group(1),setRangeMatcher.group(3),setRangeMatcher.group(2));
        
        } else if (command.equals("")) {

            redraw = false;

        } else {

            System.err.println(String.format("Unknown command %s",command));
            redraw = false;

        }

        return redraw;

    }

    private void reset() {
        
        Map<String,Range> newFilters = new HashMap<>();
        newFilters.put("yi",new Range(0.0,100.0));
        if (this.currentSettings.getConfig().getType() == Type.LONG) {
            newFilters.put("dy",new Range(0.0,100.0));
        } else {
            newFilters.put("dy",new Range(-100.0,0.0));
        }
        newFilters.put("e200",new Range(-100.0,100.0));
        StochasticConfig config = this.currentSettings.getConfig().toBuilder().filters(newFilters).build();
        this.currentSettings = this.currentSettings.toBuilder().config(config).build();

    }

    private boolean save() {

        try {
            YAML.write(this.currentSettings.getConfig(),this.configFile);
        } catch (IOException e) {
            System.err.println(String.format("Could not save %s: %s",this.configFile,e.getMessage()));
        }
        return false;

    }

    private boolean setValue(String name, String value) {
        
        switch (name) {

            case "from":
                Instant from = Instant.parse(String.format("%sT01:00:00Z",value));
                this.currentSettings = this.currentSettings.toBuilder().from(from).build();
                return true;

            case "to":
                Instant to = Instant.parse(String.format("%sT01:00:00Z",value));
                this.currentSettings = this.currentSettings.toBuilder().to(to).build();
                return true;

            case "tradefrom":
                {
                    StochasticConfig config = this.currentSettings.getConfig().toBuilder().tradeFrom(value).build();
                    this.currentSettings = this.currentSettings.toBuilder().config(config).build();
                    return true;    
                }
                
            case "tradeto":
                {
                    StochasticConfig config = this.currentSettings.getConfig().toBuilder().tradeTo(value).build();
                    this.currentSettings = this.currentSettings.toBuilder().config(config).build();
                    return true;    
                }
                
            case "takeprofit":
                {
                    double takeProfit = Double.valueOf(value);
                    StochasticConfig config = this.currentSettings.getConfig().toBuilder().takeProfit(takeProfit).build();
                    this.currentSettings = this.currentSettings.toBuilder().config(config).build();
                    return true;    
                }
                
            case "stoploss":
                {
                    double stopLoss = Double.valueOf(value);
                    StochasticConfig config = this.currentSettings.getConfig().toBuilder().stopLoss(stopLoss).build();
                    this.currentSettings = this.currentSettings.toBuilder().config(config).build();
                    return true;    
                }


            default:
                System.err.println(String.format("Unknown property %s",name));
                return false;
                
        }
    }
    
    private boolean setRange(String name, String value, String bound) {
        
        Map<String,Range> filters = this.currentSettings.getConfig().getFilters();
        Range range = filters.get(name);
        if (range == null) {
            System.err.println(String.format("Unknown feature %s",name));
            return false;
        }
        
        Double doubleValue;
        try {
            doubleValue = Double.valueOf(value);
        } catch (NumberFormatException e) {
            System.err.println(String.format("Invalid value %s for %s",value,name));
            return false;
        }

        if (bound.equals(">")) {

            Range newRange = new Range(doubleValue,range.getUpper());
            Map<String,Range> newFilters = new HashMap<>(filters);
            newFilters.put(name,newRange);
            StochasticConfig config = this.currentSettings.getConfig().toBuilder().filters(newFilters).build();
            this.currentSettings = this.currentSettings.toBuilder().config(config).build();
            return true;
            
        } else if (bound.equals("<")) {

            Range newRange = new Range(range.getLower(),doubleValue);
            Map<String,Range> newFilters = new HashMap<>(filters);
            newFilters.put(name,newRange);
            StochasticConfig config = this.currentSettings.getConfig().toBuilder().filters(newFilters).build();
            this.currentSettings = this.currentSettings.toBuilder().config(config).build();
            return true;

        } else {

            return false;

        }

    }

    private void sleep(int ms) {

        Object lock = new Object();
        synchronized (lock) {
            try {
                lock.wait(ms);
            } catch (InterruptedException e) {

            }
        }

    }

    private boolean filter(Map.Entry<String,Range> entry, Map<String,Double> features) {

        Double feature = features.get(entry.getKey());
        if (feature == null) return true;

        Range range = entry.getValue();
        if (range == null) return true;

        return feature < range.getLower() || feature > range.getUpper();
        
    }

    private boolean filter(Trade trade, Instant from, Instant to) {

        Instant entryAt = trade.getEntryPoint().getEntry();
        if (entryAt.isBefore(from)) return false;
        if (entryAt.isAfter(to)) return false;
        return true;
        
    }

    private double inc(double value, double increment) {
        return ((double)Math.round((value+increment)*1.0e6))/1.0e6;
    }
    
    private double round(double d, int sf) {
        BigDecimal bd = new BigDecimal(d);
        bd = bd.round(new MathContext(sf));
        return bd.doubleValue();
    }

    private File getFile(String filename, String defaultDirectory, String defaultExtension) {

        if (filename == null) return null;

        filename = filename + (filename.indexOf(".") == -1 ? defaultExtension : "");
        filename = (filename.indexOf("/") == -1 ? defaultDirectory : "") + filename;

        return new File(filename);

    }

    public Settings optimise(Settings settings) {

        List<Trade> trades = this.allTrades.stream().filter(trade -> filter(trade, settings.getFrom(), settings.getTo())).toList();
        StochasticConfig config = settings.getConfig();
        
        Best best = null;

        for (double takeProfit = 2.0; takeProfit <= 8.0; takeProfit = inc(takeProfit, 0.25)) {

            for (double stopLoss = -2.0; stopLoss >= round(-takeProfit, 3); stopLoss = inc(stopLoss, -0.25)) {

                for (int maxDuration = 15; maxDuration <= 360; maxDuration += 15) {
                    List<Trade> filteredTrades = new ArrayList<>();
                    for (Trade trade : trades) {

                        Instant entryAt = trade.getEntryPoint().getEntry();

                        String entryAtTime = this.getTime(entryAt, "HH:mm");
                        if (entryAtTime.compareTo(config.getTradeFrom()) < 0)
                            continue;
                        if (entryAtTime.compareTo(config.getTradeTo()) > 0)
                            continue;

                        Optional<Map.Entry<String, Range>> filtered = config.getFilters().entrySet().stream()
                                .filter(e -> filter(e, trade.getEntryPoint().getFeatures())).findFirst();
                        if (filtered.isPresent())
                            continue;

                        Trade filteredTrade = Trading.filter(trade, takeProfit, stopLoss, maxDuration);
                        filteredTrades.add(filteredTrade);

                    }

                    filteredTrades = DailyTrading.deconflict(filteredTrades, 30);
                    int n = (int) filteredTrades.stream().count();
                    double profit = filteredTrades.stream().mapToDouble(trade -> trade.outcome().getProfit()).sum();

                    double average = n > 0 ? profit / n : 0.0;
                    if (best == null || best.getProfit() < profit) {
                        best = new Best(n, profit, average, takeProfit, stopLoss, maxDuration, filteredTrades);
                    }

                }

            }
        }

        if (best != null) {

            StochasticConfig newConfig = config.toBuilder().takeProfit(best.getTakeProfit()).stopLoss(best.getStopLoss()).tradingPeriod(best.getMaxDuration()).build();
            return this.currentSettings.toBuilder().config(newConfig).build();

        } else {

            return null;

        }

    }
    
    public List<Trade> trade(Settings settings) {

        List<Trade> trades = allTrades.stream().filter(trade -> filter(trade, settings.getFrom(), settings.getTo())).toList();
        StochasticConfig config = settings.getConfig();
        
        List<Trade> filteredTrades = new ArrayList<>();
        for (Trade trade : trades) {

            Instant entryAt = trade.getEntryPoint().getEntry();

            String entryAtTime = this.getTime(entryAt, "HH:mm");
            if (entryAtTime.compareTo(config.getTradeFrom()) < 0) continue;
            if (entryAtTime.compareTo(config.getTradeTo()) > 0) continue;

            Optional<Map.Entry<String, Range>> filtered = config.getFilters().entrySet().stream().filter(e -> filter(e, trade.getEntryPoint().getFeatures())).findFirst();
            if (filtered.isPresent()) continue;

            Trade filteredTrade = Trading.filter(trade,config.getTakeProfit(),config.getStopLoss(),config.getTradingPeriod());
            filteredTrades.add(filteredTrade);

        }

        filteredTrades = DailyTrading.deconflict(filteredTrades, 30);

        int n = 0;
        double totalProfit = 0.0;
        double cumProfit = 1000.0;
        for (Trade trade: filteredTrades) {
            
            n++;
            
            double profit = trade.outcome().getProfit(); 
            totalProfit += profit;
            cumProfit *= (1 + profit / 100);
        
            // Map<String,Double> features = trade.getEntryPoint().getFeatures();
            
        }
        
        printSettings(settings);
        printResults(n,totalProfit,cumProfit);
    
        return filteredTrades;

    }
    
    @RequiredArgsConstructor
    class Renderer implements Runnable,LocalTimeSupport {

        private final List<Trade> unsortedTrades;
        
        @Override
        public void run() {

            List<Trade> trades = new ArrayList<>(unsortedTrades);
            
            String[] featureNames = new String[] { "yi", "e200", "dy" };
            for (String featureName : featureNames) {

                trades.sort((a, b) -> Double.compare(a.getEntryPoint().getFeatures().get(featureName),
                        b.getEntryPoint().getFeatures().get(featureName)));
                double pl = 0.0;
                XYChart.Series<Number, Number> series = SimulationGraphs.getNumericSeries(featureName);
                series.getData().clear();
                List<Double> emas = new ArrayList<Double>();
                for (Trade trade : trades) {

                    Outcome last = trade.outcome();
                    Map<String, Double> features = trade.getEntryPoint().getFeatures();
                    double featureValue = features.get(featureName);

                    pl += last.getProfit();
                    emas.add(pl);
                    while (emas.size()>10) emas.remove(0);
                    // double ema = emas.stream().mapToDouble(Double::valueOf).average().getAsDouble();
                    series.getData().add(new XYChart.Data<Number, Number>(featureValue, pl));

                }

            }

            {

                SortedMap<String, Double> pl = new TreeMap<>();
                SortedMap<String, Double> n = new TreeMap<>();
                for (Trade trade : trades) {
                    String time = this.getTime(trade.getEntryPoint().getEntry(), "HH:mm");
                    if (pl.containsKey(time)) {
                        pl.put(time, pl.get(time) + trade.outcome().getProfit());
                        n.put(time, n.get(time) + 1.0);
                    } else {
                        pl.put(time, trade.outcome().getProfit());
                        n.put(time, 1.0);
                    }
                }

                XYChart.Series<String, Number> series = SimulationGraphs.getCategorySeries("Time");
                series.getData().clear();
                for (String time : pl.keySet()) {
                    double average = pl.get(time) / n.get(time);
                    series.getData().add(new XYChart.Data<String, Number>(time, average));
                }

            }

            {

                trades.sort((a, b) -> a.getEntryPoint().getEntry().compareTo(b.getEntryPoint().getEntry()));
                XYChart.Series<String, Number> series = SimulationGraphs.getCategorySeries("Time 2");
                series.getData().clear();
                for (Trade trade : trades) {
                    String time = this.getTime(trade.getEntryPoint().getEntry(), "YYYY-MM-dd HH:mm");
                    double profit = trade.outcome().getProfit();
                    XYChart.Data<String, Number> data = new XYChart.Data<String, Number>(time, profit);
                    series.getData().add(data);
                }

            }

            if (Platform.isFxApplicationThread())
                SimulationGraphs.display();

        }

    }
    
    @RequiredArgsConstructor
    @Getter
    class Best {

        private final int n;
        private final double profit;
        private final double average;
        private final double takeProfit;
        private final double stopLoss;
        private final int maxDuration;
        private final List<Trade> trades;
    
    }

    @RequiredArgsConstructor
    @Getter
    @Builder(toBuilder=true)
    static class Settings {

        private final Instant from;
        private final Instant to;
        private final StochasticConfig config;
        
    }   

}
