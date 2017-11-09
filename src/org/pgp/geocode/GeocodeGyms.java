package org.pgp.geocode;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.NearbySearchRequest;
import com.google.maps.errors.ApiException;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pgp.object.Geocode;
import org.pgp.object.Gym;
import org.pgp.object.GymInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class GeocodeGyms {
  private final static Logger logger = LogManager.getLogger(GeocodeGyms.class);

  private final String googleApiKey;

  public GeocodeGyms(final String googleApiKey) {
    this.googleApiKey = googleApiKey;
  }

  public void geocode(final Set<Gym> gyms) {
    logger.info("Reverse geocoding gyms:");
    final GeoApiContext context = new GeoApiContext.Builder()
        .apiKey(googleApiKey)
        .build();

    final Set<String> placeTypes = Arrays.stream(PlaceType.values())
        .map(PlaceType::toString)
        .map(String::toUpperCase)
        .collect(Collectors.toSet());

    gyms.forEach(gym -> {
      logger.info("  Getting geocode information for gym '" + gym.getGymName() + "'.");

      final int nearestGymDistance = Math.toIntExact(Math.round(
          gyms.stream()
              .filter(otherGym -> otherGym.getGymId() != gym.getGymId())
              .mapToDouble(otherGym ->
                  Haversine.distance(
                      gym.getGymInfo().getLatitude().doubleValue(),
                      gym.getGymInfo().getLongitude().doubleValue(),
                      otherGym.getGymInfo().getLatitude().doubleValue(),
                      otherGym.getGymInfo().getLongitude().doubleValue())
                      * 1_000d)
              .min()
              .orElse(30)));
      try {
        final GymInfo gymInfo = gym.getGymInfo();
        final LatLng location = new LatLng(gymInfo.getLatitude().doubleValue(), gymInfo.getLongitude().doubleValue());

        gymInfo.setAddressComponents(Arrays.stream(
            GeocodingApi.newRequest(context)
                .latlng(location)
                .await())
            .map(result -> new Geocode(result.formattedAddress, result.addressComponents))
            .collect(Collectors.toCollection(LinkedHashSet::new)));

        gymInfo.setPlaces(Arrays.stream(
            new NearbySearchRequest(context)
                .location(location)
                .radius(nearestGymDistance / 2)
                .await()
                .results)
            .filter(place -> Arrays.stream(place.types)
                .map(String::toUpperCase)
                .anyMatch(placeTypes::contains))
            .map(place -> place.name)
            .collect(Collectors.toCollection(TreeSet::new)));
      } catch (final ApiException
          | InterruptedException
          | IOException e) {
        logger.error("Exception caught", e);
      }
    });
  }
}
