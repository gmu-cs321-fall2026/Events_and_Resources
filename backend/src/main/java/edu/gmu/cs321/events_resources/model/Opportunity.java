package edu.gmu.cs321.events_resources.model;

public record Opportunity(
		Long id,
		String title,
		String employer,
		String type,
		String description,
		String link,
		String deadline,
		String datePosted) {
}