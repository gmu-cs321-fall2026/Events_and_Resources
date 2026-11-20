package edu.gmu.cs321.events_resources.model;

import java.util.List;

/**
 * ResourceRequest (MASON-7): the body of POST /resources and PUT /resources/{id}.
 *
 *   {"title": "Behavioral questions I got at Capital One",
 *    "description": "Three questions they asked and how I used STAR to answer.",
 *    "type": "Interview Questions", "industry": "Technology",
 *    "link": "https://example.com/my-notes", "majors": ["CS Majors", "Juniors"]}
 *
 * link is optional (a tip can be just the description). Everything else is required.
 * There is no status field on purpose: the server decides the status.
 */
public record ResourceRequest(
        String title,
        String description,
        String type,
        String industry,
        String link,
        List<String> majors) {
}
