package org.pgp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pgp.geocode.GeocodeGyms;
import org.pgp.object.Gym;
import org.pgp.scrape.AreaScraper;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public final class GymScraper {
  private final static Logger logger = LogManager.getLogger(GymScraper.class);

  private static void usage() {
    System.err.println(
        "Usage: GymScraper {-scraper=<scraper configuration file>}\n" +
            "                  {-minLat=<minimum latitude to scrape>} {-maxLat=<maximum latitude to scrape>}\n" +
            "                  {-minLong=<minimum longitude to scrape>} {-maxLong=<maximum longitude to scrape>}\n" +
            "                  [-googleApiKey=<Google maps API key for reverse geocoding, geocoding not done if omitted>\n" +
            "                  [-existingGyms=<file name of existing gyms to amend and add to, optional>\n" +
            "                  [-removeMissingGyms=<true|false, defaults to false if omitted>\n" +
            "                  [-geocodeOnly=<true|false, defaults to false if omitted>");
  }

  private static AreaScraper from(final String scraperConfiguration) {
    try (final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(scraperConfiguration)) {
      return context.getBean("scraper", AreaScraper.class);
    }
  }

  public static void main(final String[] args) throws IOException {
    BigDecimal minLat = null;
    BigDecimal maxLat = null;

    BigDecimal minLong = null;
    BigDecimal maxLong = null;

    String _scraperConfiguration = null;

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
        case "scraper": {
          _scraperConfiguration = value;
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

    final String googleApiKey = _googleApiKey;
    final String scraperConfiguration = _scraperConfiguration;

    if ((scraperConfiguration == null || minLat == null || maxLat == null || minLong == null || maxLong == null) && !geocodeOnly) {
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

    logger.info("Update mode is " + (incrementalUpdate
        ? "incremental"
        : "full") + ".");

    if (removeMissingGyms) {
      logger.info("Removal of missing gyms from existing gyms in scraped area enabled.");
    }

    if (!geocodeOnly) {
      newGyms = GymScraper.from(scraperConfiguration)
          .scrapeArea(new CoordinateRange(minLat, maxLat, minLong, maxLong));
      allNewGyms = newGyms;
    } else {
      newGyms = existingGyms;
      allNewGyms = existingGyms;
    }

    if (googleApiKey != null) {
      new GeocodeGyms(googleApiKey).geocode(newGyms);
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
