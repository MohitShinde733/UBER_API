package com.trip.api.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trip.api.dto.TripDetailsResponse;
import com.trip.api.service.TripRoutingService;

@RestController
public class TripDetailsController 
{

    private final TripRoutingService tripRoutingService;

    public TripDetailsController(TripRoutingService tripRoutingService) 
    {
        this.tripRoutingService = tripRoutingService;
    }

    @GetMapping("/trip/details")
    public TripDetailsResponse tripDetails
    (@RequestParam String pickup,@RequestParam String dropoff) 
    {
        return tripRoutingService.getTripDetails(pickup, dropoff);
    }
}


