package edu.gmu.cs321.events_resources.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * ReminderScheduler: the "alarm clock" that runs ReminderService on a timer.
 *
 * @EnableScheduling turns on Spring's timer, and @Scheduled marks the method it calls:
 *   initialDelay = wait this long after startup before the first check (default 1 minute)
 *   fixedDelay   = then wait this long after each check finishes (default 5 minutes)
 * Both come from application.properties (reminders.first-check-after-ms, reminders.check-every-ms).
 *
 * Turn reminders off with REMINDERS_ENABLED=false: @ConditionalOnProperty then skips
 * creating this class, so no timer runs at all.
 *
 * A failed check (e.g. the database is restarting) is logged and never stops the
 * timer: the next check simply runs again.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "reminders.enabled", havingValue = "true", matchIfMissing = true)

public class ReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderService reminders;

    public ReminderScheduler(ReminderService reminders) {
        this.reminders = reminders;
    }

    @Scheduled(initialDelayString = "${reminders.first-check-after-ms:60000}",
               fixedDelayString = "${reminders.check-every-ms:300000}")
    public void checkReminders() {
        try {
            int sent = reminders.sendDueReminders();
            if (sent > 0) {
                log.info("Sent {} event reminder(s)", sent);
            }
        } catch (RuntimeException e) {
            log.warn("Reminder check failed; trying again at the next check", e);
        }
    }
}
