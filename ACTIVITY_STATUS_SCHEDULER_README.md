# Automatic extracurricular activity status synchronization

## What it does
- DRAFT/PENDING/REJECTED + UPCOMING + start_time <= now
  -> CANCELLED
- APPROVED + UPCOMING + end_time <= now
  -> COMPLETED (catch-up if Tomcat was off)
- APPROVED + UPCOMING + start_time <= now < end_time
  -> ONGOING
- APPROVED + ONGOING + end_time <= now
  -> COMPLETED
- COMPLETED and CANCELLED are not changed by this scheduler.

## Before running
1. Run database/fix_activity_status_logic_constraint.sql once
   against the project database.
2. Ensure start_time and end_time are stored as Vietnam local
   wall-clock times, matching DateTimeUtil.ZONE
   (Asia/Ho_Chi_Minh) and the datetime-local form input.
3. Build and run the WAR on Tomcat 10.1 / Jakarta Servlet 6.

The scheduler runs once at application startup, then every minute.
If Tomcat is stopped for the whole activity window, the startup
run reconciles the state after it comes back.