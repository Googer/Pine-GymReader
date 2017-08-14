package org.pgp;

import com.google.maps.model.AddressComponent;

import java.util.Objects;

public class Geocode {
  private String formattedAddress;
  private AddressComponent[] addressComponents;

  public Geocode() {
  }

  public Geocode(final String formattedAddress, final AddressComponent[] addressComponents) {
    this.formattedAddress = formattedAddress;
    this.addressComponents = addressComponents;
  }

  public String getFormattedAddress() {
    return formattedAddress;
  }

  public void setFormattedAddress(final String formattedAddress) {
    this.formattedAddress = formattedAddress;
  }

  public AddressComponent[] getAddressComponents() {
    return addressComponents;
  }

  public void setAddressComponents(final AddressComponent[] addressComponents) {
    this.addressComponents = addressComponents;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof Geocode) {
      final Geocode other = (Geocode) obj;

      return Objects.equals(formattedAddress, other.formattedAddress) &&
          Objects.deepEquals(addressComponents, other.addressComponents);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(formattedAddress, addressComponents);
  }
}
