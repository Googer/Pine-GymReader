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
import java.util.*;
import java.util.stream.Collectors;

public class GeocodeGyms {
  private final static Logger logger = LogManager.getLogger(GeocodeGyms.class);

  private final String googleApiKey;

  private final class GymWithDistance {
    private final String gymId;
    private final double distance;

    private GymWithDistance(final String gymId, final double distance) {
      this.gymId = gymId;
      this.distance = distance;
    }

    private String getGymId() {
      return gymId;
    }

    private double getDistance() {
      return distance;
    }
  }

  public GeocodeGyms(final String googleApiKey) {
    this.googleApiKey = googleApiKey;
  }

  public void geocode(final Set<Gym> gyms, final boolean incrementalUpdate) {
    logger.info("Reverse geocoding gyms:");
    final GeoApiContext context = new GeoApiContext.Builder()
        .apiKey(googleApiKey)
        .build();

    final Set<String> placeTypes = Arrays.stream(PlaceType.values())
        .map(PlaceType::toString)
        .map(String::toUpperCase)
        .collect(Collectors.toSet());

    gyms.forEach(gym -> {
      final GymWithDistance nearestGymWithDistance = gyms.stream()
          .filter(otherGym -> !otherGym.getGymId().equals(gym.getGymId()))
          .map(otherGym -> new GymWithDistance(otherGym.getGymId(), Haversine.distance(
              gym.getGymInfo().getLatitude().doubleValue(),
              gym.getGymInfo().getLongitude().doubleValue(),
              otherGym.getGymInfo().getLatitude().doubleValue(),
              otherGym.getGymInfo().getLongitude().doubleValue())
              * 1_000d))
          .min(Comparator.comparingDouble(GymWithDistance::getDistance))
          .orElse(new GymWithDistance(null, 30d));

      final GymInfo gymInfo = gym.getGymInfo();
      final String currentNearestGym = gymInfo.getNearestGym();

      if (!incrementalUpdate || currentNearestGym == null || (incrementalUpdate && !currentNearestGym.equals(nearestGymWithDistance.gymId))) {
        logger.info("  Getting geocode information for gym '" + gym.getGymName() + "'.");
        gymInfo.setNearestGym(nearestGymWithDistance.gymId);

        try {
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
                  .radius(Math.toIntExact(Math.round(nearestGymWithDistance.distance)) / 2)
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
      } else {
        logger.info("  Keeping current geocode information for gym '" + gym.getGymName() + "'.");
      }
    });
  }
}
