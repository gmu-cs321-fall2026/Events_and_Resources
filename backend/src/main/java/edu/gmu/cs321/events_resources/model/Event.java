package edu.gmu.cs321.events_resources.model;

public record Event(
    Long id,
    String eventTitle,
    String host,
    String date,
    String time,
    String location,
	String category,
	String audience,
	String format,
    String registerLink,
    int capacity,
    int registeredCount
) {}
