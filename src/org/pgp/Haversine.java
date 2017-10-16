package org.pgp;

public class Haversine {
  private static final int EARTH_RADIUS = 6371; // Approx Earth radius in KM

  public static double distance(double startLat, final double startLong,
                                double endLat, final double endLong) {

    final double dLat = Math.toRadians((endLat - startLat));
    final double dLong = Math.toRadians((endLong - startLong));

    startLat = Math.toRadians(startLat);
    endLat = Math.toRadians(endLat);

    final double a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLong);
    final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS * c;
  }

  private static double haversin(final double val) {
    return Math.pow(Math.sin(val / 2), 2);
  }
}