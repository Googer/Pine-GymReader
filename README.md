# Pine-GymReader
This is a simple command-line utility to generate the map data for the Professor Pine raid coordination bot.

It is extensible to generate gym data from any source.

The PGMI scrape implementation (deprecated) requires having curl (https://curl.haxx.se/) in the path, as it is what issues the actual requests.

Basic sample usage:

`java -jar GymScraper.jar -scraper=file:scraper.xml -googleApiKey=<google API key> -minLat=40.0 -maxLat=41.0 -minLong=-80.5 -maxLong=-79.5`

scraper.xml contains scraper configuration, including its class and any parameters it needs to initialize.

The Google API key is a Google Maps key (one can be generated for free) for use with their Maps client services.  If omitted, reverse geocoding for address lookup, nearby places, etc., is not performed or therefore in the output json file.
