package org.pgp.scrape;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pgp.CoordinateRange;
import org.pgp.object.Gym;
import org.pgp.object.GymInfo;

import java.beans.ConstructorProperties;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.TreeSet;

public class PineDatabaseScraper implements AreaScraper {
  private static final Logger logger = LogManager.getLogger(PineDatabaseScraper.class);

  private final String host;
  private final String user;
  private final String password;
  private final String schema;

  @ConstructorProperties({"host", "user", "password", "schema"})
  public PineDatabaseScraper(final String host, final String user, final String password, final String schema) {
    this.host = host;
    this.user = user;
    this.password = password;
    this.schema = schema;
  }

  private Connection connect() throws SQLException {
    final MysqlDataSource dataSource = new MysqlDataSource();
    dataSource.setServerName(host);
    dataSource.setUser(user);
    dataSource.setPassword(password);
    dataSource.setDatabaseName(schema);
    dataSource.setServerTimezone("UTC");

    return dataSource.getConnection();
  }

  @Override
  public Set<Gym> scrapeArea(final CoordinateRange coordinateRange) {
    try (final Connection connection = connect();
         final Statement statement = connection.createStatement();
         final ResultSet resultSet = statement.executeQuery("SELECT gymId, name, description, latitude, longitude FROM Gym WHERE " +
             "latitude BETWEEN " + coordinateRange.getMinLat() + " AND " + coordinateRange.getMaxLat() + " AND " +
             "longitude BETWEEN " + coordinateRange.getMinLong() + " AND " + coordinateRange.getMaxLong())) {
      final Set<Gym> gyms = new TreeSet<>();

      while (resultSet.next()) {
        final String gymId = resultSet.getString("gymId");
        final String name = resultSet.getString("name");
        final String description = resultSet.getString("description");
        final BigDecimal latitude = resultSet.getBigDecimal("latitude");
        final BigDecimal longitude = resultSet.getBigDecimal("longitude");

        final Gym gym = new Gym(gymId, name);
        final GymInfo gymInfo = new GymInfo(description, latitude.toString(), longitude.toString());
        gym.setGymInfo(gymInfo);

        gyms.add(gym);

        logger.debug("Added gym '" + name + "'.");
      }

      return gyms;
    } catch (final SQLException e) {
      logger.error(e);
    }

    return null;
  }
}
