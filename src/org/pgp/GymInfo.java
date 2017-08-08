package org.pgp;

import java.math.BigDecimal;
import java.util.Objects;

public final class GymInfo {
  private final String gymDescription;
  private final BigDecimal latitude;
  private final BigDecimal longitude;

  public GymInfo(final String gymDescription, final String latitude, final String longitude) {
    this.gymDescription = gymDescription;
    this.latitude = new BigDecimal(latitude);
    this.longitude = new BigDecimal(longitude);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof GymInfo) {
      final GymInfo other = (GymInfo) obj;

      return Objects.equals(gymDescription, other.gymDescription) &&
          Objects.equals(latitude, other.latitude) &&
          Objects.equals(longitude, other.longitude);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(gymDescription, latitude, longitude);
  }
}
