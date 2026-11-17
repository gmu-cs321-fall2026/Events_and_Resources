package edu.gmu.cs321.events_resources.model;
/**
 * ReminderRequest: the JSON body of PUT /events/{id}/register/reminder.
 *
 *   {"remind": true}    turn the reminder on
 *   {"remind": false}   turn it off
 *
 * Boolean (capital B) instead of boolean, so a missing "remind" arrives as null
 * and the controller can answer 400 instead of quietly treating it as false.
 */

public record ReminderRequest(Boolean remind) {
}
