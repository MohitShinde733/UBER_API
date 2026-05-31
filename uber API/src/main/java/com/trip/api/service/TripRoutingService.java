package com.trip.api.service;

import java.util.Arrays;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.trip.api.dto.NominatimResult;
import com.trip.api.dto.OsrmRouteResponse;
import com.trip.api.dto.TripDetailsResponse;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TripRoutingService 
{

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String OSRM_ROUTE_BASE = "http://router.project-osrm.org/route/v1/driving";
    private static final String USER_AGENT = "TripDetailsService/1.0 (https://example.com/trip-details)";

    private final RestTemplate restTemplate;

    public TripRoutingService(RestTemplate restTemplate)
    {
        this.restTemplate = restTemplate;
    }

    public TripDetailsResponse getTripDetails(String pickup, String dropoff) 
    {
        double[] pickupCoords = geocode(pickup);
        double[] dropoffCoords = geocode(dropoff);
        double[] route = drivingRoute(pickupCoords, dropoffCoords);
        double distanceKm = route[0];
        double durationMinutes = route[1];

        return new TripDetailsResponse(
                pickup,
                dropoff,
                round3(distanceKm),
                round2(durationMinutes));
    }

    private double[] geocode(String place) 
    {
        String url = UriComponentsBuilder.fromUriString(NOMINATIM_URL)
                .queryParam("q", place)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<NominatimResult[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                NominatimResult[].class);

        NominatimResult[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new ResponseStatusException(NOT_FOUND, "No coordinates found for: " + place);
        }

        NominatimResult first = body[0];
        double lat = Double.parseDouble(first.getLat());
        double lon = Double.parseDouble(first.getLon());
        return new double[] { lat, lon };
    }

    /**
     * @return [distanceKm, durationMinutes]
     */
    private double[] drivingRoute(double[] pickup, double[] dropoff) {
        double lat1 = pickup[0];
        double lon1 = pickup[1];
        double lat2 = dropoff[0];
        double lon2 = dropoff[1];
        // OSRM expects lon,lat order
        String path = lon1 + "," + lat1 + ";" + lon2 + "," + lat2;
        String url = UriComponentsBuilder.fromUriString(OSRM_ROUTE_BASE + "/" + path)
                .queryParam("overview", "false")
                .build()
                .toUriString();

        OsrmRouteResponse data = restTemplate.getForObject(url, OsrmRouteResponse.class);
        if (data == null || !"Ok".equals(data.getCode())
                || data.getRoutes() == null || data.getRoutes().isEmpty()) {
            throw new ResponseStatusException(BAD_GATEWAY, "OSRM could not compute a route between the given points.");
        }

        OsrmRouteResponse.OsrmRoute route = data.getRoutes().get(0);
        double distanceKm = route.getDistance() / 1000.0;
        double durationMinutes = route.getDuration() / 60.0;
        return new double[] { distanceKm, durationMinutes };
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
