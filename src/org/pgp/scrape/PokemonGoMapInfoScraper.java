package org.pgp.scrape;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pgp.CoordinateRange;
import org.pgp.object.Gym;
import org.pgp.object.GymInfo;

import java.beans.ConstructorProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class PokemonGoMapInfoScraper implements AreaScraper {
  private static final Logger logger = LogManager.getLogger(PokemonGoMapInfoScraper.class);

  private final static String COOKIE =
      "updatetoken=0; " +
          "__cfduid={user_id};" +
          "PHPSESSID={session_id}; " +
          "announcementnews4=1;announcementnews6=1;announcementnews8=1;announcementnews9=1; " +
          "mapfilters=0[##split##]1[##split##]1[##split##]0[##split##]0[##split##]0[##split##]0[##split##]0[##split##]1[##split##]1[##split##]1[##split##]0; " +
          "latlngzoom=15[##split##]{curLatCenter}[##split##]{curLongCenter}";

  private final static String GYMS_TEMPLATE = "curl " +
      "\"https://www.pokemongomap.info/includes/it55nmsq9.php\" " +
      "-k " +
      "-H \"Cookie: {cookie}\" " +
      "-H \"dnt: 1\" " +
      "-H \"x-requested-with: XMLHttpRequest\" " +
      "-H \"accept-language: en-US,en;q=0.8\" " +
      "-H \"user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36\" " +
      "-H \"content-type: application/x-www-form-urlencoded; charset=UTF-8\" " +
      "-H \"accept: */*\" " +
      "-H \"referer: https://www.pokemongomap.info/\" " +
      "-H \"authority: www.pokemongomap.info\" " +
      "--data \"fromlat={minLat}&tolat={maxLat}&fromlng={minLong}&tolng={maxLong}&fpoke=0&fgym=1&farm=0&nests=0&raids=0&sponsor=0\"";

  private final static String GYM_TEMPLATE = "curl " +
      "\"https://www.pokemongomap.info/includes/locdata.php\" " +
      "-k " +
      "-H \"Cookie: {cookie}\" " +
      "-H \"Accept-Language: en-US,en;q=0.8\" " +
      "-H \"User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36\" " +
      "-H \"Content-Type: application/x-www-form-urlencoded; charset=UTF-8\" " +
      "-H \"Accept: application/json, text/javascript, */*; q=0.01\" " +
      "-H \"Referer: https://www.pokemongomap.info/\" " +
      "-H \"X-Requested-With: XMLHttpRequest\" " +
      "-H \"DNT: 1\" " +
      "--data \"mid={gymId}\"";

  private final static BigDecimal COORDINATE_SCALE = new BigDecimal(1_852_000);

  private final int divideThreshold;
  private final String userId;
  private final String sessionId;

  private final JsonParser parser = new JsonParser();

  private String generateCommand(final CoordinateRange coordinateRange) {
    return GYMS_TEMPLATE
        .replace("{minLat}", CoordinateRange.formatter.format(coordinateRange.getMinLat()))
        .replace("{maxLat}", CoordinateRange.formatter.format(coordinateRange.getMaxLat()))
        .replace("{minLong}", CoordinateRange.formatter.format(coordinateRange.getMinLong()))
        .replace("{maxLong}", CoordinateRange.formatter.format(coordinateRange.getMaxLong()))
        .replace("{cookie}", COOKIE)
        .replace("{user_id}", userId)
        .replace("{session_id}", sessionId)
        .replace("{curLatCenter}", CoordinateRange.formatter.format(coordinateRange.getMidLat()))
        .replace("{curLongCenter}", CoordinateRange.formatter.format(coordinateRange.getMidLong()));
  }

  private String generateDetailCommand(final Gym gym) {
    return GYM_TEMPLATE
        .replace("{gymId}", String.valueOf(gym.getGymId()))
        .replace("{cookie}", COOKIE)
        .replace("{user_id}", userId)
        .replace("{session_id}", sessionId);
  }

  @ConstructorProperties({"divideThreshold", "userId", "sessionId"})
  public PokemonGoMapInfoScraper(final int divideThreshold, final String userId, final String sessionId) {
    this.divideThreshold = divideThreshold;
    this.userId = userId;
    this.sessionId = sessionId;
  }

  @Override
  public Set<Gym> scrapeArea(final CoordinateRange fullArea) {
    final Stack<CoordinateRange> coordinateRanges = new Stack<>();

    coordinateRanges.push(fullArea);

    final Set<Gym> gyms = new TreeSet<>();

    try {
      // maps from actual gym object to command to get detailed gym info, ordered by gym id's
      final Map<Gym, String> newGymDetailsMap = new TreeMap<>();

      try {
        while (!coordinateRanges.isEmpty()) {
          final CoordinateRange coordinateRange = coordinateRanges.pop();

          if (coordinateRange.area() > 0.05d) {
            logger.info("Subdividing large area...");
            coordinateRange.subDivide()
                .forEach(coordinateRanges::push);
            continue;
          }

          final String command = generateCommand(coordinateRange);

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
                              final Gym gym = new Gym(gymId, siteName);
                              final String latitude = new String(Base64.getDecoder().decode(siteObject.get("f24sfvs").getAsString()));
                              final String longitude = new String(Base64.getDecoder().decode(siteObject.get("z3iafj").getAsString()));

                              final GymInfo gymInfo = new GymInfo();
                              gymInfo.setLatitude(new BigDecimal(latitude).divide(COORDINATE_SCALE));
                              gymInfo.setLongitude(new BigDecimal(longitude).divide(COORDINATE_SCALE));

                              gym.setGymInfo(gymInfo);

                              return gym;
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
                            this::generateDetailCommand)));
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

          Thread.sleep(10_000L);
        }
      } catch (final InterruptedException e) {
        logger.error("InterruptedException caught", e);
      }

      final Stack<Map.Entry<Gym, String>> gymCommands = new Stack<>();

      newGymDetailsMap.entrySet()
          .forEach(gymCommands::push);

      logger.info(gymCommands.size() + " new gyms found.");

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

              gym.getGymInfo().setGymDescription(description);
              gyms.add(gym);
            }
          } else if (!element.isJsonNull()) {
            // empty result set, nothing in this region
            logger.info("  Empty result in getting location information for gym...");
          } else {
            logger.warn("  Null / completely empty result...");
            gymCommands.push(entry);
          }
        }

        Thread.sleep(1_000L);
      }

    } catch (final IOException |
        InterruptedException e) {
      logger.error("Exception caught", e);
    }

    return gyms;
  }
}
