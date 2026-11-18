package edu.gmu.cs321.events_resources.model;

/**
 * EventSeats: just the two numbers the waitlist rules need from one event row.
 * WHO USES IT
 *   WaitlistRepository.findSeats(...) / lockSeats(...) build one.
 *   WaitlistService asks it "is the event full?" and "how many seats are open?".
 *
 * capacity 0 means unlimited, so an unlimited event is never full.
 */
public record EventSeats(int capacity, int registeredCount) {

    /** True when every seat is taken. Unlimited events (capacity 0) are never full. */
    public boolean isFull() {
        return capacity > 0 && registeredCount >= capacity;
    }
    /**
     * How many people can still get a seat right now.
     * Unlimited events answer Integer.MAX_VALUE ("as many as you like").
     */
    public int openSeats() {
        if (capacity == 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, capacity - registeredCount);
    }
}
