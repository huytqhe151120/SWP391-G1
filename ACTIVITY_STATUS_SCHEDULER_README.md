# Automatic extracurricular activity status synchronization

## Business rules

The scheduler updates `activity_status` according to the activity's approval
status and its start/end times:

| Current activity status | Approval status | Time condition | New activity status |
| --- | --- | --- | --- |
| `UPCOMING` | Any status other than `APPROVED` | `start_time <= now` | `approval_status = REJECTED`; `activity_status = CANCELLED` |
| `UPCOMING` | `APPROVED` | `end_time <= now` | `COMPLETED` |
| `UPCOMING` | `APPROVED` | `start_time <= now < end_time` | `ONGOING` |
| `ONGOING` | `APPROVED` | `end_time <= now` | `COMPLETED` |

The comparisons include the exact boundary time: the start or end transition
is eligible as soon as `now` equals the corresponding timestamp.

The scheduler only updates the source states shown above. It does not change
`COMPLETED` or `CANCELLED` activities, and it does not process rows with
missing start/end times.
When an unapproved activity reaches its start time, all its collaboration
invitations become `CANCELLED`, and its activity-partner-company and
activity-partner-staff rows become `DEACTIVE` in their `collaboration_status`
columns. These changes are committed in the same transaction as the activity
transition. Pending collaboration invitations are also cancelled in the same
synchronization transaction when their activity becomes `ONGOING`,
`COMPLETED`, or `CANCELLED`.

If the application was offline for an entire approved activity, the startup
run catches it up directly from `UPCOMING` to `COMPLETED` when its end time
has passed; it does not first persist an `ONGOING` state.

## Time-zone convention

- Activity form values and application-facing `LocalDateTime` values use
  Vietnam time (`Asia/Ho_Chi_Minh`, UTC+7).
- `start_time` and `end_time` in the database are UTC values. SQL Server
  `DATETIME`/`DATETIME2` columns do not store a time-zone marker, so UTC is an
  application/database convention.
- The application converts form values to UTC when writing activity times
  and converts UTC values back to Vietnam time when reading them.
- The scheduler compares UTC `now` (`DateTimeUtil.nowUtc()`) with the UTC
  database timestamps. Do not manually enter Vietnam-local values into these
  columns.

## Scheduler lifecycle and failure handling

`ActivityStatusScheduler` is registered as a Jakarta Servlet
`ServletContextListener`. When the web application starts, it:

1. Runs one synchronization immediately to reconcile activities whose
   transitions may have been missed while the application was stopped.
2. Runs synchronization again with a one-minute fixed delay between runs.
3. Logs a synchronization failure and continues scheduling subsequent runs.
4. Shuts down its executor when the web application is stopped or undeployed.

Because synchronization runs periodically, a transition can become visible
up to approximately one scheduler interval after its boundary. A failed run
is retried by a later scheduled run; it is not retried immediately.

All activity-status and pending-invitation updates in one synchronization pass run in a single
transaction. They are committed together; if an update fails, the transaction
is rolled back and the failure is logged by the scheduler.

Before deploying this version, run
`src/main/resources/db/migration/V1__add_activity_partner_staff_collaboration_status.sql`
once against the application database. Existing partner-staff assignments
receive `ACTIVE` as their initial collaboration status.

## Requirements

- Store existing and new activity `start_time` and `end_time` values as UTC.
- Keep `end_time` later than `start_time`; activity create/edit validation
  enforces this and requires the start time to be in the future.
- Deploy the WAR to Tomcat 10.1 or another Jakarta Servlet 6-compatible
  container so `@WebListener` is discovered.
- Ensure only one application instance is responsible for scheduling a given
  database if the deployment expects a single scheduler. Multiple instances
  can run the same conditional updates, but each will perform its own
  synchronization pass.
