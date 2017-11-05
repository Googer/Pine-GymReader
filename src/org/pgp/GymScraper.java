package org.pgp;

import com.google.gson.*;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.NearbySearchRequest;
import com.google.maps.errors.ApiException;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceType;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GymScraper {
  private final static Logger logger = LogManager.getLogger(GymScraper.class);

  private static final class CoordinateRange {
    private final BigDecimal minLat;
    private final BigDecimal maxLat;
    private final BigDecimal minLong;
    private final BigDecimal maxLong;

    private final BigDecimal midLat;
    private final BigDecimal midLong;

    private CoordinateRange(final BigDecimal minLat, final BigDecimal maxLat, final BigDecimal minLong, final BigDecimal maxLong) {
      this.minLat = minLat;
      this.maxLat = maxLat;
      this.minLong = minLong;
      this.maxLong = maxLong;

      midLat = minLat.add(maxLat.subtract(minLat).divide(new BigDecimal("2.0"), 8, RoundingMode.HALF_EVEN));
      midLong = minLong.add(maxLong.subtract(minLong).divide(new BigDecimal("2.0"), 8, BigDecimal.ROUND_HALF_EVEN));
    }

    private double area() {
      return Math.abs(maxLat.subtract(minLat).doubleValue() * maxLong.subtract(minLong).doubleValue());
    }

    private List<CoordinateRange> subDivide() {
      return Arrays.asList(
          new CoordinateRange(minLat, midLat, minLong, midLong),
          new CoordinateRange(midLat, maxLat, minLong, midLong),
          new CoordinateRange(minLat, midLat, midLong, maxLong),
          new CoordinateRange(midLat, maxLat, midLong, maxLong));
    }

    private String generateCommand() {
      return GYMS_TEMPLATE
          .replace("{minLat}", formatter.format(minLat))
          .replace("{maxLat}", formatter.format(maxLat))
          .replace("{minLong}", formatter.format(minLong))
          .replace("{maxLong}", formatter.format(maxLong))
          .replace("{cookie}", COOKIE)
          .replace("{user_id}", USER_ID)
          .replace("{session_id}", SESSION_ID)
          .replace("{curLatCenter}", formatter.format(midLat))
          .replace("{curLongCenter}", formatter.format(midLong));
    }

    @Override
    public String toString() {
      return "<" + formatter.format(minLat) + "," + formatter.format(minLong) + " - " +
          formatter.format(maxLat) + "," + formatter.format(maxLong) + ">";
    }
  }

  private final static DecimalFormat formatter;

  static {
    formatter = new DecimalFormat();
    formatter.setMaximumFractionDigits(8);
    formatter.setMinimumFractionDigits(8);
    formatter.setGroupingUsed(false);
  }

  private static String USER_ID = "d9c8997c8b65f54b4f0f3e938a1b7bcc31509297067";
  private static String SESSION_ID = "74be3fa4e449sa7ulfjl4g3rh7";

  private final static String COOKIE =
      "updatetoken=0; " +
          "__cfduid={user_id};" +
          "PHPSESSID={session_id}; " +
          "announcementnews4=1;announcementnews6=1;announcementnews8=1; " +
          "mapfilters=0[##split##]1[##split##]1[##split##]0[##split##]0[##split##]0[##split##]0[##split##]0[##split##]1[##split##]1[##split##]1[##split##]0; " +
          "latlngzoom=15[##split##]{curLatCenter}[##split##]{curLongCenter}";

  private final static String GYMS_TEMPLATE = "curl " +
      "\"https://www.pokemongomap.info/includes/it43nmsq5.php\" " +
      "-k " +
      "-H \"Cookie: {cookie}\" " +
      "-H \"dnt: 1\" " +
      "-H \"accept-encoding: gzip, deflate, br\" " +
      "-H \"x-requested-with: XMLHttpRequest\" " +
      "-H \"accept-language: en-US,en;q=0.8\" " +
      "-H \"user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36\" " +
      "-H \"content-type: application/x-www-form-urlencoded; charset=UTF-8\" " +
      "-H \"accept: */*\" " +
      "-H \"referer: https://www.pokemongomap.info/\" " +
      "-H \"authority: www.pokemongomap.info\" " +
      "--data \"fromlat={minLat}&tolat={maxLat}&fromlng={minLong}&tolng={maxLong}&fpoke=0&fgym=1&farm=0&nests=0&raids=0&sponsor=0\" " +
      "--compressed";

  private final static String GYM_TEMPLATE = "curl " +
      "\"https://www.pokemongomap.info/includes/locdata.php\" " +
      "-k " +
      "-H \"Cookie: {cookie}\" " +
      "-H \"Accept-Encoding: gzip, deflate, br\" " +
      "-H \"Accept-Language: en-US,en;q=0.8\" " +
      "-H \"User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36\" " +
      "-H \"Content-Type: application/x-www-form-urlencoded; charset=UTF-8\" " +
      "-H \"Accept: application/json, text/javascript, */*; q=0.01\" " +
      "-H \"Referer: https://www.pokemongomap.info/\" " +
      "-H \"X-Requested-With: XMLHttpRequest\" " +
      "-H \"DNT: 1\" " +
      "--data \"mid={gymId}\" " +
      "--compressed";

  private static String getUserId() {
    return USER_ID;
  }

  private static String getSessionId() {
    return SESSION_ID;
  }

  private static final Function<Gym, String> GENERATE_DETAIL_COMMAND = gym ->
      GYM_TEMPLATE
          .replace("{gymId}", String.valueOf(gym.getGymId()))
          .replace("{cookie}", COOKIE)
          .replace("{user_id}", getUserId())
          .replace("{session_id}", getSessionId());

  private static void usage() {
    System.err.println(
        "Usage: GymScraper {-userId=<user id to use for requests>}\n" +
            "                  {-sessionId=<session id to use for requests>}\n" +
            "                  {-minLat=<minimum latitude to scrape>} {-maxLat=<maximum latitude to scrape>}\n" +
            "                  {-minLong=<minimum longitude to scrape>} {-maxLong=<maximum longitude to scrape>}\n" +
            "                  [-googleApiKey=<Google maps API key for reverse geocoding, geocoding not done if omitted>\n" +
            "                  [-existingGyms=<file name of existing gyms output to amend and add to, optional>\n" +
            "                  [-removeMissingGyms=<true|false, defaults to false if omitted>\n" +
            "                  [-geocodeOnly=<true|false, defaults to false if omitted>\n" +
            "                  [-divideThreshold=<number of sites to use as a threshold to subdivide the query,\n" +
            "                   defaults to 200 if omitted>]");
  }

  public static void main(final String[] args) throws InterruptedException, IOException {
    BigDecimal minLat = null;
    BigDecimal maxLat = null;

    BigDecimal minLong = null;
    BigDecimal maxLong = null;

    int divideThreshold = 200;

    boolean _incrementalUpdate = false;
    String existingGymsFilename = null;
    boolean _removeMissingGyms = false;

    boolean geocodeOnly = false;

    String _googleApiKey = null;

    for (final String arg : args) {
      if (!arg.startsWith("-")) {
        continue;
      }

      final String[] splitArg = arg.substring(1).split("=");
      if (splitArg.length != 2) {
        continue;
      }

      final String value = splitArg[1];

      switch (splitArg[0].toLowerCase()) {
        case "minlat": {
          minLat = new BigDecimal(value, MathContext.DECIMAL32);
          break;
        }
        case "maxlat": {
          maxLat = new BigDecimal(value, MathContext.DECIMAL32);
          break;
        }
        case "minlong": {
          minLong = new BigDecimal(value, MathContext.DECIMAL32);
          break;
        }
        case "maxlong": {
          maxLong = new BigDecimal(value, MathContext.DECIMAL32);
          break;
        }
        case "dividethreshold": {
          divideThreshold = Integer.parseInt(value);
          break;
        }
        case "existinggyms": {
          existingGymsFilename = value;
          _incrementalUpdate = true;
          break;
        }
        case "removemissinggyms": {
          _removeMissingGyms = Boolean.parseBoolean(value);
          break;
        }
        case "userid": {
          USER_ID = value;
          break;
        }
        case "sessionid": {
          SESSION_ID = value;
          break;
        }
        case "googleapikey": {
          _googleApiKey = value;
          break;
        }
        case "geocodeonly": {
          geocodeOnly = Boolean.parseBoolean(value);
          break;
        }
      }
    }

    if ((minLat == null || maxLat == null || minLong == null || maxLong == null) && !geocodeOnly) {
      usage();
      System.exit(-1);
    }

    final Set<Gym> existingGyms;
    final Set<Gym> newGyms;
    final Set<Gym> allNewGyms;

    if (existingGymsFilename != null) {
      final File existingGymsFile = Paths.get(existingGymsFilename).toFile();

      if (!existingGymsFile.exists()) {
        System.err.println("File '" + existingGymsFilename + "' does not exist!");
        System.exit(-1);
      }
      try (final BufferedReader input = new BufferedReader(new FileReader(existingGymsFile))) {
        existingGyms = new TreeSet<>(Arrays.asList(new Gson().fromJson(input, Gym[].class)));
      }
      logger.info("Loaded " + existingGyms.size() + " existing gyms.");
    } else {
      existingGyms = new TreeSet<>();
    }

    final boolean incrementalUpdate = _incrementalUpdate;
    final boolean removeMissingGyms = _removeMissingGyms;
    final String googleApiKey = _googleApiKey;

    logger.info("Update mode is " + (incrementalUpdate
        ? "incremental"
        : "full") + ".");

    if (removeMissingGyms) {
      logger.info("Removal of missing gyms from existing gyms in scraped area enabled.");
    }

    final JsonParser parser = new JsonParser();

    if (!geocodeOnly) {
      final Stack<CoordinateRange> coordinateRanges = new Stack<>();

      coordinateRanges.push(new CoordinateRange(minLat, maxLat, minLong, maxLong));

      // maps from actual gym object to command to get detailed gym info, ordered by gym id's
      final Map<Gym, String> newGymDetailsMap = new TreeMap<>();

      while (!coordinateRanges.isEmpty()) {
        final CoordinateRange coordinateRange = coordinateRanges.pop();

        if (coordinateRange.area() > 0.05d) {
          logger.info("Subdividing large area...");
          coordinateRange.subDivide()
              .forEach(coordinateRanges::push);
          continue;
        }

        final String command = coordinateRange.generateCommand();

        logger.debug("Command is: '" + command + "'.");
        logger.info("Processing region " + coordinateRange + ":");

        final Process process = Runtime.getRuntime().exec(command);
        try {
          try (final BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            final String result = IOUtils.toString(input);
            final JsonElement element = parser.parse(result);
            if (element.isJsonObject()) {
              // we got a non-empty response of some sort, so either a spam message or an actual gym list
              final JsonObject object = element.getAsJsonObject();

              if (object.get("spam") != null) {
                // pokemongomapinfo is telling us to cool it, so wait and try again...
                logger.warn("Spam warning detected - sleeping 5 minutes...");
                coordinateRanges.push(coordinateRange);
                Thread.sleep(300_000L);
              } else {
                final int siteCount = object.keySet().size();

                if (siteCount > divideThreshold) {
                  logger.info("  " + siteCount + " sites found - subdividing...");
                  coordinateRange.subDivide()
                      .forEach(coordinateRanges::push);
                } else {
                  // site id's are the keys in the result set
                  final Set<Map.Entry<String, JsonElement>> entries = object.entrySet();

                  logger.info("  " + entries.size() + " site(s) found...");

                  newGymDetailsMap.putAll(entries.stream()
                      .map(entry -> {
                        final String siteId = entry.getKey();
                        final JsonElement site = entry.getValue();

                        if (site.isJsonObject()) {
                          final long gymId = Long.parseLong(siteId);
                          final JsonObject siteObject = site.getAsJsonObject();
                          final String siteName = siteObject.get("rfs21d").getAsString();

                          final int siteType = Integer.parseInt(
                              new String(Base64.getDecoder().decode(siteObject.get("xgxg35").getAsString())));
                          if (siteType > 1) {
                            logger.info("    Found gym '" + siteName + "'.");
                            return new Gym(gymId, siteName);
                          } else {
                            logger.debug("    Skipping site '" + siteName + "'.");
                          }
                        } else {
                          logger.warn("Returned result is not a valid json object!");
                          logger.debug("Result is '" + site.getAsString() + "'.");
                        }
                        return null;
                      })
                      .filter(Objects::nonNull)
                      .collect(Collectors.toMap(
                          Function.identity(),
                          GENERATE_DETAIL_COMMAND)));
                }
              }
            } else if (!element.isJsonNull()) {
              // empty result set, nothing in this region
              logger.info("  0 sites found...");
            } else {
              logger.warn("  Null / completely empty result...");
              coordinateRanges.push(coordinateRange);
            }
          }
        } finally {
          process.destroy();
        }

        Thread.sleep(5_000L);
      }

      final Stack<Map.Entry<Gym, String>> gymCommands = new Stack<>();

      allNewGyms = newGymDetailsMap.keySet();
      newGymDetailsMap.entrySet().stream()
          .filter(entry -> {
            final Gym gym = entry.getKey();

            return !incrementalUpdate || (!existingGyms.contains(gym));
          })
          .forEach(gymCommands::push);

      logger.info(gymCommands.size() + " new gyms found.");
      newGyms = new TreeSet<>();

      // For each gym, now get its detailed information so we can get its location
      logger.info("Retrieving detailed gym information:");
      while (!gymCommands.isEmpty()) {
        final Map.Entry<Gym, String> entry = gymCommands.pop();

        final Gym gym = entry.getKey();
        final String command = entry.getValue();

        logger.debug("Command is: '" + command + "'.");
        final Process process = Runtime.getRuntime().exec(command);

        try (final BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
          final String result = IOUtils.toString(input);
          final JsonElement element = parser.parse(result);

          if (element.isJsonObject()) {
            final JsonObject object = element.getAsJsonObject();

            if (object.get("spam") != null) {
              logger.warn("Spam warning detected - sleeping 5 minutes...");
              gymCommands.push(entry);
              Thread.sleep(300_000L);
            } else {
              logger.info("  Filling in location information for gym '" + gym.getGymName() + "'.");

              final String description = object.get("description").getAsString();
              final String latitude = object.get("markerlat").getAsString();
              final String longitude = object.get("markerlng").getAsString();

              gym.setGymInfo(new GymInfo(description, latitude, longitude));
              newGyms.add(gym);
            }
          } else {
            logger.warn("Empty / invalid result in getting location information for gym...");
          }
        }

        Thread.sleep(1_000L);
      }
    } else {
      newGyms = existingGyms;
      allNewGyms = existingGyms;
    }

    if (googleApiKey != null) {
      logger.info("Reverse geocoding gyms:");
      final GeoApiContext context = new GeoApiContext.Builder()
          .apiKey(googleApiKey)
          .build();

      final Set<String> placeTypes = Arrays.stream(PlaceType.values())
          .map(PlaceType::toString)
          .map(String::toUpperCase)
          .collect(Collectors.toSet());

      newGyms.forEach(gym -> {
        logger.info("  Getting geocode information for gym '" + gym.getGymName() + "'.");

        final int nearestGymDistance = Math.toIntExact(Math.round(
            newGyms.stream()
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

    final Set<Gym> outputGyms;

    if (incrementalUpdate) {
      if (removeMissingGyms) {
        final Set<Gym> missingGyms = new HashSet<>(existingGyms);
        missingGyms.removeAll(allNewGyms);

        existingGyms.removeAll(missingGyms);
      }
      existingGyms.addAll(newGyms);
      outputGyms = existingGyms;
    } else {
      outputGyms = newGyms;
    }

    logger.info("Writing out gym information with locations and descriptions.");
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter("gyms.json"))) {
      writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(outputGyms));
    }
  }
}
