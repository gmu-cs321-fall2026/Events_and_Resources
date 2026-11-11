\\## Asked



Asked Claude to suggest an approach to the issue of the default JavaScript dates UTC, because when putting events on calendar, it should be local time instead of UTC.







\\## Produced



Suggestion to pass the year, month, and day as separate numbers, JavaScript will use local time, rather than sending dates as plain text which JavaScript treats as UTC.



\\## Changed or rejected



Accepted. Troubleshoot the wrong dates that put the events on calendar on UTC