package com.trip.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TripDetailsResponse(
        String pickup,
        String dropoff,
        @JsonProperty("distance_km") double distanceKm,
        @JsonProperty("duration_minutes") double durationMinutes)
        {
                        

        }
