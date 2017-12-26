# GymScraper
This is a simple command-line utility to scrape map data for Pokemon Go.

It is extensible to scrape from any source but the only implemented scraper uses PokemonGoMap.info as its source.  It uses a subdividing algorithm to cover the requested latitude-longitude range to fetch the initial list of sites (which are then filtered to just gyms), then issues requests for each discovered gym to obtain additional information about it (exact latitude-longitude coordinates, additional text description, etc.)

The PGMI scrape implementation requires having curl (https://curl.haxx.se/) in the path, as it is what issues the actual requests.

Basic sample usage:

`java -jar GymScraper.jar -scraper=file:scraper.xml -googleApiKey=<google API key> -minLat=40.0 -maxLat=41.0 -minLong=-80.5 -maxLong=-79.5`

scraper.xml contains scraper configuration, including its class and any parameters it needs to initialize.

The Google API key is a Google Maps key (one can be generated for free) for use with their Maps client services.  If omitted, reverse geocoding for address lookup, nearby places, etc., is not performed or therefore in the output json file.
