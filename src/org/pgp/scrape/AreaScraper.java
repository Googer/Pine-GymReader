package org.pgp.scrape;

import org.pgp.CoordinateRange;
import org.pgp.object.Gym;

import java.util.Set;

public interface AreaScraper {
  Set<Gym> scrapeArea(CoordinateRange coordinateRange);
}
