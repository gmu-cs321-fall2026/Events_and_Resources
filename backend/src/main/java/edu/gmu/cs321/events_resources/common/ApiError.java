package edu.gmu.cs321.events_resources.common;

/**
 * The JSON body of every error response, in the format all subsystems share (SOW §6):
 * {"error": "...", "code": "...", "detail": "..."}
 *
 * error  = short message a person can read, e.g. "This event is full"
 * code   = stable label code can check, e.g. "EVENT_FULL"
 * detail = extra information, or "" if there is none
 */
public record ApiError(String error, String code, String detail) {
}
