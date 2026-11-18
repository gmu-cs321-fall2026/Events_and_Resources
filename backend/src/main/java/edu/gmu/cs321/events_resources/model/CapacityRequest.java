package edu.gmu.cs321.events_resources.model;

/**
 * CapacityRequest: the JSON body staff send to PUT /events/{id}/capacity 
 * Integer (not int) so a missing "capacity" arrives as null and we can answer
 * 400 VALIDATION_ERROR instead of silently treating it as 0 (= unlimited).
 */
public record CapacityRequest(Integer capacity) {
}
