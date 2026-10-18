package edu.gmu.cs321.events_resources.model;

public record Event(
    long id,
    String eventTitle,
    String host,
    String date,
    String time,
    String location,
    String tag,
    String registerLink,
    int capacity,
    int registeredCount
) {}
