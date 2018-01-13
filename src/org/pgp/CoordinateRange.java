package org.pgp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public final class CoordinateRange {
  public final static DecimalFormat formatter;

  static {
    final NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.US);
    formatter = (DecimalFormat) numberFormatter;

    formatter.setMaximumFractionDigits(8);
    formatter.setMinimumFractionDigits(8);
    formatter.setGroupingUsed(false);
  }

  public BigDecimal getMinLat() {
    return minLat;
  }

  public BigDecimal getMaxLat() {
    return maxLat;
  }

  public BigDecimal getMinLong() {
    return minLong;
  }

  public BigDecimal getMaxLong() {
    return maxLong;
  }

  public BigDecimal getMidLat() {
    return midLat;
  }

  public BigDecimal getMidLong() {
    return midLong;
  }

  private final BigDecimal minLat;
  private final BigDecimal maxLat;
  private final BigDecimal minLong;
  private final BigDecimal maxLong;

  private final BigDecimal midLat;
  private final BigDecimal midLong;

  public CoordinateRange(final BigDecimal minLat, final BigDecimal maxLat, final BigDecimal minLong, final BigDecimal maxLong) {
    this.minLat = minLat;
    this.maxLat = maxLat;
    this.minLong = minLong;
    this.maxLong = maxLong;

    midLat = minLat.add(maxLat.subtract(minLat).divide(new BigDecimal("2.0"), 8, RoundingMode.HALF_EVEN));
    midLong = minLong.add(maxLong.subtract(minLong).divide(new BigDecimal("2.0"), 8, RoundingMode.HALF_EVEN));
  }

  public double area() {
    return Math.abs(maxLat.subtract(minLat).doubleValue() * maxLong.subtract(minLong).doubleValue());
  }

  public List<CoordinateRange> subDivide() {
    return List.of(
        new CoordinateRange(minLat, midLat, minLong, midLong),
        new CoordinateRange(midLat, maxLat, minLong, midLong),
        new CoordinateRange(minLat, midLat, midLong, maxLong),
        new CoordinateRange(midLat, maxLat, midLong, maxLong));
  }

  @Override
  public String toString() {
    return "<" + formatter.format(minLat) + "," + formatter.format(minLong) + " - " +
        formatter.format(maxLat) + "," + formatter.format(maxLong) + ">";
  }
}
