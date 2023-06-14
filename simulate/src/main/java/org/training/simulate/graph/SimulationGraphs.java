package org.training.simulate.graph;

import java.util.HashMap;
import java.util.Map;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimulationGraphs extends Application {

    private static Map<String,XYChart.Series<Number,Number>> numericData = new HashMap<>();
    private static Map<String,XYChart.Series<String,Number>> categoryData = new HashMap<>();

    private static Map<String,LineChart<Number,Number>> lineCharts = new HashMap<>();
    private static Map<String,BarChart<String,Number>> barCharts = new HashMap<>();
    
    private static Map<String,Stage> stages = new HashMap<>();

    public static void start() {

        new Thread(() -> {SimulationGraphs.launch(new String[0]);}).start();
        
    }

    @Override public void start(Stage stage) {

        display();

    }

    public static XYChart.Series<Number,Number> getNumericSeries(String name) {
        
        XYChart.Series<Number,Number> series = numericData.get(name);

        if (series == null) {
            series = new XYChart.Series<Number,Number>();
            numericData.put(name,series);
        }

        return series;

    }
    
    public static XYChart.Series<String,Number> getCategorySeries(String name) {
        
        XYChart.Series<String,Number> series = categoryData.get(name);

        if (series == null) {
            series = new XYChart.Series<String,Number>();
            categoryData.put(name,series);
        }

        return series;
        
    }

    public static void display() {
                
        for (String featureName: numericData.keySet()) {
            displayNumericFeature(featureName);
        }
    
        for (String featureName: categoryData.keySet()) {
            displayTimedFeature(featureName);
        }

    }

    private static void displayNumericFeature(String featureName) {
        
        XYChart.Series<Number,Number> series = getNumericSeries(featureName);
        if (!lineCharts.containsKey(featureName)) {
        
            Stage stage = new Stage();

            int width = 800;
            int height = 500;
            String xAxisLabel = featureName;
            String yAxisLabel = "Cum. Profit";
    
            final NumberAxis xAxis = new NumberAxis();
            xAxis.setLabel(xAxisLabel);
            xAxis.setLowerBound(minXValue(series));
            xAxis.setUpperBound(maxXValue(series));
            xAxis.setAutoRanging(false);
            final NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel(yAxisLabel);
            yAxis.setAutoRanging(true);
            
            final LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis,yAxis);
            lineCharts.put(featureName,lineChart);

            lineChart.setTitle(String.format("Cum Profit vs %s",featureName));
            series.setName(featureName);
            
            Scene scene  = new Scene(lineChart,width,height);
            lineChart.getData().add(series);

            stage.setScene(scene);
            stage.show();
                
        } else {

            final LineChart<Number,Number> lineChart = lineCharts.get(featureName);
            final NumberAxis xAxis = (NumberAxis)lineChart.getXAxis();
            xAxis.setLowerBound(minXValue(series));
            xAxis.setUpperBound(maxXValue(series));
            
        }
        
        for (final XYChart.Data<Number,Number> data : series.getData()) {
            Tooltip tooltip = new Tooltip();
            tooltip.setText(String.format("(%.2f,%.2f)",data.getXValue(),data.getYValue()));
            Tooltip.install(data.getNode(), tooltip);
        }       
        
    }

    private static void displayTimedFeature(String featureName) {
        
        XYChart.Series<String,Number> series = getCategorySeries(featureName);
        
        boolean stageExists = stages.containsKey(featureName);
        
        Stage stage = stageExists ? stages.get(featureName) : new Stage();
        stages.put(featureName,stage);

        int width = 1100;
        int height = 500;
        String xAxisLabel = featureName;
        String yAxisLabel = "Cum. Profit";

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xAxisLabel);
        final NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yAxisLabel);
        yAxis.setAutoRanging(true);
    
        BarChart<String,Number> barChart = new BarChart<String,Number>(xAxis,yAxis);
        barCharts.put(featureName,barChart);

        barChart.setTitle(String.format("Cum Profit vs %s",featureName));
        series.setName(featureName);
    
        Scene scene  = new Scene(barChart,width,height);
        barChart.getData().add(series);
    
        stage.setScene(scene);
        stage.show();
    
        for (final XYChart.Data<String,Number> data : series.getData()) {
            Tooltip tooltip = new Tooltip();
            tooltip.setText(String.format("(%s,%.2f)",data.getXValue(),data.getYValue()));
            Tooltip.install(data.getNode(), tooltip);
        }
    
    }

    private static double minXValue(XYChart.Series<Number,Number> series) {

        int n = series.getData().size(); 
        return n > 0 ? series.getData().get(0).getXValue().doubleValue() : 0.0;

    }

    private static double maxXValue(XYChart.Series<Number,Number> series) {

        int n = series.getData().size(); 
        return n > 0 ? series.getData().get(n-1).getXValue().doubleValue() : 100.0;

    }

}
