package org.pgp;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.math.BigDecimal;
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

      midLat = minLat.add(maxLat.subtract(minLat).divide(new BigDecimal("2.0")));
      midLong = minLong.add(maxLong.subtract(minLong).divide(new BigDecimal("2.0")));
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
          .replace("{session_id}", SESSION_ID)
          .replace("{curLatCenter}", formatter.format(midLat))
          .replace("{curLongCenter}", formatter.format(midLong));
    }

    @Override
    public String toString() {
      return "<" + minLat + "," + minLong + " - " + maxLat + "," + maxLong + ">";
    }
  }

  private final static DecimalFormat formatter;

  static {
    formatter = new DecimalFormat();
    formatter.setMaximumFractionDigits(8);
    formatter.setMinimumFractionDigits(8);
    formatter.setGroupingUsed(false);
  }

  private static String SESSION_ID = "fd1lms5pd02cka2fkung48c123";

  private final static String COOKIE =
      "mapfilters=0[##split##]1[##split##]1[##split##]0[##split##]0[##split##]0[##split##]0[##split##]0; " +
          "PHPSESSID={session_id}; " +
          "announcementnews4=1; " +
          "latlngzoom=15^[^#^#split^#^#^]{curLatCenter}^[^#^#split^#^#^]{curLongCenter}";

  private final static String GYMS_TEMPLATE = "curl " +
      "\"https://www.pokemongomap.info/includes/uy22ewsd1.php\" " +
      "-k " +
      "-H \"Origin: https://www.pokemongomap.info\" " +
      "-H \"Cookie: {cookie}\" " +
      "-H \"Accept-Encoding: gzip, deflate, br\" " +
      "-H \"Accept-Language: en-US,en;q=0.8\" " +
      "-H \"User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36\" " +
      "-H \"Content-Type: application/x-www-form-urlencoded; charset=UTF-8\" " +
      "-H \"Accept: */*\" " +
      "-H \"Referer: https://www.pokemongomap.info/\" " +
      "-H \"X-Requested-With: XMLHttpRequest\" " +
      "-H \"DNT: 1\" " +
      "--data \"fromlat={minLat}^&tolat={maxLat}^&fromlng={minLong}^&tolng={maxLong}^&fpoke=0^&fgym=1^&farm=0^&nests=0^&raids=0\" " +
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

  private static String getSessionId() {
    return SESSION_ID;
  }

  private static final Function<Gym, String> GENERATE_DETAIL_COMMAND = gym ->
      GYM_TEMPLATE.replace("{gymId}", String.valueOf(gym.getGymId()))
          .replace("{cookie}", COOKIE)
          .replace("{session_id}", getSessionId());

  private static void usage() {
    System.err.println(
        "Usage: GymScraper {-sessionId=<session id to use for requests>\n" +
            "                  {-minLat=<minimum latitude to scrape>} {-maxLat=<maximum latitude to scrape>}\n" +
            "                  {-minLong=<minimum longitude to scrape>} {-maxLong=<maximum longitude to scrape>}\n" +
            "                  [-divideThreshold=<number of sites to use as a threshold to subdivide the query,\n" +
            "                   defaults to 200 if omitted>]");
  }

  public static void main(final String[] args) throws InterruptedException, IOException {
    BigDecimal minLat = null;
    BigDecimal maxLat = null;

    BigDecimal minLong = null;
    BigDecimal maxLong = null;

    int divideThreshold = 200;

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
          minLat = new BigDecimal(value);
          break;
        }
        case "maxlat": {
          maxLat = new BigDecimal(value);
          break;
        }
        case "minlong": {
          minLong = new BigDecimal(value);
          break;
        }
        case "maxlong": {
          maxLong = new BigDecimal(value);
          break;
        }
        case "dividethreshold": {
          divideThreshold = Integer.parseInt(value);
          break;
        }
        case "sessionid": {
          SESSION_ID = value;
        }
      }
    }

    if (minLat == null || maxLat == null || minLong == null || maxLong == null) {
      usage();
      System.exit(-1);
    }

    final Stack<CoordinateRange> coordinateRanges = new Stack<>();
    coordinateRanges.push(new CoordinateRange(minLat, maxLat, minLong, maxLong));

    // maps from actual gym object to command to get detailed gym info, ordered by gym id's
    final Map<Gym, String> gymDetailsMap = new TreeMap<>();
    final Set<Gym> gyms = gymDetailsMap.keySet();

    final JsonParser parser = new JsonParser();

    while (!coordinateRanges.isEmpty()) {
      final CoordinateRange coordinateRange = coordinateRanges.pop();
      final String command = coordinateRange.generateCommand();

      logger.debug("Command is: '" + command + "'.");
      logger.info("Processing region " + coordinateRange + ":");

      final Process process = Runtime.getRuntime().exec(command);

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

              gymDetailsMap.putAll(entries.stream()
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
        }
      }

      Thread.sleep(30_000L);
    }

    logger.info("Writing out initial gym information.");
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter("gyms-nolocation.json"))) {
      writer.write(new Gson().toJson(gyms));
    }

    final Stack<Map.Entry<Gym, String>> gymCommands = new Stack<>();
    gymDetailsMap.entrySet().forEach(gymCommands::push);

    final Set<Gym> gymsWithInfo = new TreeSet<>();

    // For each gym, now get its detailed information so we can get its location
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
            logger.info("Filling in location information for gym '" + gym.getGymName() + "'.");

            final String description = object.get("description").getAsString();
            final String latitude = object.get("markerlat").getAsString();
            final String longitude = object.get("markerlng").getAsString();

            gymsWithInfo.add(gym.withGymInfo(new GymInfo(description, latitude, longitude)));
          }
        } else {
          logger.warn("Empty / invalid result in getting location information for gym...");
        }
      }

      Thread.sleep(1_000L);
    }

    logger.info("Writing out gym information with locations and descriptions.");
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter("gyms.json"))) {
      writer.write(new Gson().toJson(gymsWithInfo));
    }
  }
}
