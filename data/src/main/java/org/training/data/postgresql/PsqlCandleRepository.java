package org.training.data.postgresql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.training.data.CandleRepository;
import org.training.model.Candle;
import org.training.model.Candles;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class PsqlCandleRepository implements CandleRepository {

    private static final int INTERVAL = 1;

    private String host = System.getProperty("postgresql-host", "localhost");
    private String port = System.getProperty("postgresql-port", "5432");
    private String user = "postgres";
    private String password = "ZimZam32!";

    @Override
    public Candles getCandles(String instrument, Instant from, Instant to) {

        List<Candle> candles = new ArrayList<>();

        Connection connection = null;
        try {
            Class.forName("org.postgresql.Driver");
            Properties parameters = new Properties();
            parameters.put("user", this.user);
            parameters.put("password", this.password);
            connection = DriverManager.getConnection(this.connectionURL(), parameters);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        if (connection != null) {

            try {

                String sql = "SELECT \"start\",\"end\",\"interval\",\"high\",\"low\",\"open\",\"close\" FROM \"%s\" " +
                             "WHERE \"start\">=? AND \"end\" <=? ORDER BY \"start\"";

                String statement = String.format(sql,instrument);

                PreparedStatement s = connection.prepareStatement(statement);
                s.setTimestamp(1, Timestamp.from(from));
                s.setTimestamp(2, Timestamp.from(to));
                ResultSet rs = s.executeQuery();
                while (rs.next()) {
                    Instant start = rs.getTimestamp(1).toInstant();
                    Instant end = rs.getTimestamp(2).toInstant();
                    double high = rs.getDouble(4);
                    double low = rs.getDouble(5);
                    double open = rs.getDouble(6);
                    double close = rs.getDouble(7);
                    Candle candle = new Candle(low, high, open, close, start, end);
                    candles.add(candle);
                }
                s.close();
                connection.close();

                return new Candles(instrument, candles);

            } catch (SQLException e) {
                if (e.getSQLState().equals("42X05")) {
                    System.err.println("No Table for " + instrument);
                } else {
                    e.printStackTrace();
                }
            }

            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

        return null;

    }

    @Override
    public boolean updateCandles(Candles candles) {

        Connection connection = null;
        try {
            Class.forName("org.postgresql.Driver");
            Properties parameters = new Properties();
            parameters.put("user", this.user);
            parameters.put("password", this.password);
            connection = DriverManager.getConnection(this.connectionURL(), parameters);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (connection != null) {

            try {

                for (Candle candle : candles.candles) {

                    String sql = "INSERT INTO \"%s\" (\"start\",\"end\",\"interval\",\"high\",\"low\",\"open\",\"close\") VALUES(?,?,?,?,?,?,?) " +
                                 "ON CONFLICT(\"start\") DO " +
                                 "UPDATE SET \"end\"=?,\"interval\"=?,\"high\"=?,\"low\"=?,\"open\"=?,\"close\"=?";
                    
                    String statement = String.format(sql, candles.instrument, candles.instrument);

                    PreparedStatement s = connection.prepareStatement(statement);

                    s.setTimestamp(1, Timestamp.from(candle.start));
                    s.setTimestamp(2, Timestamp.from(candle.end));
                    s.setInt(3, INTERVAL);
                    s.setDouble(4, candle.high);
                    s.setDouble(5, candle.low);
                    s.setDouble(6, candle.open);
                    s.setDouble(7, candle.close);
                    s.setTimestamp(8, Timestamp.from(candle.end));
                    s.setInt(9, INTERVAL);
                    s.setDouble(10, candle.high);
                    s.setDouble(11, candle.low);
                    s.setDouble(12, candle.open);
                    s.setDouble(13, candle.close);
                    
                    s.executeUpdate();

                    s.close();

                }

            } catch (SQLException e) {
                if (e.getSQLState().equals("42X05")) {
                    System.err.println("No Table for " + candles.instrument);
                } else {
                    e.printStackTrace();
                }
                return false;
            }

            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

        }

        return true;

    }

    protected boolean deleteAllCandles(String instrument) {

        Connection connection = null;
        try {
            Class.forName("org.postgresql.Driver");
            Properties parameters = new Properties();
            parameters.put("user", this.user);
            parameters.put("password", this.password);
            connection = DriverManager.getConnection(this.connectionURL(), parameters);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (connection != null) {

            try {

                String sql = "DELETE FROM \"%s\"";
                String statement = String.format(sql, instrument);

                PreparedStatement s = connection.prepareStatement(statement);
                
                s.executeUpdate();

                s.close();

            } catch (SQLException e) {
                if (e.getSQLState().equals("42X05")) {
                    System.err.println("No Table for " + instrument);
                } else {
                    e.printStackTrace();
                }
                return false;
            }

            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

        }

        return true;

    }

    private String connectionURL() {

        return String.format("jdbc:postgresql://%s:%s/forex", this.host, this.port);

    }

}
