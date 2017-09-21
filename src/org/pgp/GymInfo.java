package org.pgp;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

public final class GymInfo {
  private String gymDescription;
  private BigDecimal latitude;
  private BigDecimal longitude;

  private Collection<Geocode> addressComponents;
  private Collection<String> places;

  public GymInfo() {
  }

  public GymInfo(final String gymDescription, final String latitude, final String longitude) {
    this.gymDescription = gymDescription;
    this.latitude = new BigDecimal(latitude);
    this.longitude = new BigDecimal(longitude);
  }

  public String getGymDescription() {
    return gymDescription;
  }

  public void setGymDescription(final String gymDescription) {
    this.gymDescription = gymDescription;
  }

  public BigDecimal getLatitude() {
    return latitude;
  }

  public void setLatitude(final BigDecimal latitude) {
    this.latitude = latitude;
  }

  public BigDecimal getLongitude() {
    return longitude;
  }

  public void setLongitude(final BigDecimal longitude) {
    this.longitude = longitude;
  }

  public Collection<Geocode> getAddressComponents() {
    return addressComponents;
  }

  public void setAddressComponents(final Collection<Geocode> addressComponents) {
    this.addressComponents = addressComponents;
  }

  public Collection<String> getPlaces() {
    return places;
  }

  public void setPlaces(final Collection<String> places) {
    this.places = places;
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
          Objects.equals(longitude, other.longitude) &&
          Objects.equals(addressComponents, other.addressComponents) &&
          Objects.equals(places, other.places);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(gymDescription, latitude, longitude, addressComponents, places);
  }
}
