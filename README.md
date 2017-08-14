# GymScraper
Gym scraper for data from pokemongomap.info

This is a simple command-line utility to scrape data from pokemongomap.info.  It uses a subdividing algorithm to cover the requested latitude-longitude range to fetch the initial list of sites (which are then filtered to just gyms), then issues requests for each discovered gym to obtain additional information about it (exact latitude-longitude coordinates, additional text description, etc.)

This utility requires having curl (https://curl.haxx.se/) in the path, as it is what issues the actual requests.

Basic sample usage:

`java -jar GymScraper.jar -sessionId=<session Id> -googleApiKey=<google API key> -minLat=40.0 -maxLat=41.0 -minLong=-80.5 -maxLong=-79.5`

The session id is a unique key to send to pokemongomap.info.  To generate a valid one for use, simply visit pokemongomap.info and get some sites / gyms with network monitoring enabled in your web browser.  The key is the value from the PHPSESSID cookie (it should look like a random string of numbers and letters 26 characters or so in length).

The Google API key is a valid Google API key (one can be generated for free) for use with Google Maps's client services.  If omitted, reverse geocoding for address lookup, etc., is not performed or therefore in the output json file.
