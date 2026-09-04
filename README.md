# Vaping Duty
This is the backend microservice for the Vaping Duty service.

Frontend: https://github.com/hmrc/vaping-duty-frontend

Stub: https://github.com/hmrc/vaping-duty-stubs

## Requirements
Written in Scala 3 with Play Framework and suitable to be run on JRE 21 or later.

## Features

### Non-Repudiation Service (NRS) Integration
This service integrates with HMRC's Non-Repudiation Service to provide an audit trail for vaping duty return submissions.

**Key Features:**
- **Asynchronous Processing**: NRS submissions are queued and processed in the background, ensuring they don't impact the primary returns submission flow
- **Work Item Queue**: Uses MongoDB work item repository for durable, retryable queue management
- **Circuit Breaker**: Implements circuit breaker pattern to protect against downstream NRS service failures
- **Scheduled Processing**: Background scheduler processes queued NRS submissions with automatic retry
- **Failure Isolation**: NRS failures do not affect returns submission - submissions succeed even if NRS queueing fails

**Architecture:**
```
Returns Submission (Primary Flow):
ReturnsController → SubmitReturnsConnector → ETMP API

NRS Submission (Parallel Flow):
ReturnsController → NrsService.makeWorkItemAndQueue()
                         ↓
                    NrsWorkItemRepository (MongoDB)
                         ↓
                    NrsScheduledService (Background)
                         ↓
                    NrsConnector (with Circuit Breaker)
                         ↓
                    NRS API
```

**Monitoring:**

The NRS integration includes comprehensive monitoring via circuit breaker metrics:

*Available Metrics:*
- `nrs.circuit-breaker.state` - Current state (0=Closed, 1=HalfOpen, 2=Open)
- `nrs.circuit-breaker.successes` - Total successful submissions
- `nrs.circuit-breaker.failures` - Total failed submissions
- `nrs.circuit-breaker.calls-rejected` - Calls rejected due to circuit being open
- `nrs.circuit-breaker.state-changes` - Number of state transitions

*Circuit Breaker Behavior:*
- **Closed (0)**: Normal operation, all requests allowed
- **HalfOpen (1)**: Testing phase after timeout, limited requests to test recovery
- **Open (2)**: Circuit open, requests rejected to protect downstream service

*Configuration:*
- Max failures before opening: 5
- Reset timeout: 60 seconds (with exponential backoff up to 300s)
- Half-open test calls: 3

*Alerting Thresholds:*
- **Critical**: Circuit open for > 5 minutes
- **Warning**: Success rate < 95% over 15 minutes
- **Warning**: > 5 state changes in 30 minutes

**Configuration:**
All NRS configuration is in `conf/application.conf` under `microservice.services.nrs`. Key settings include:
- Circuit breaker thresholds (max failures, timeouts)
- Work item retry intervals and TTL
- Scheduler intervals
- NRS API endpoint and credentials

**Troubleshooting:**

*Circuit Breaker Stuck Open:*
1. Check NRS service status
2. Review NRS connector logs for error details
3. Verify work item queue is accumulating items
4. Check network connectivity

*Frequent State Flapping:*
1. Review timing of state changes for patterns
2. Check NRS response times
3. Verify circuit breaker timeout configuration
4. Check if correlated with high load periods

*Integration with Work Item Queue:*
- Circuit Closed: Items processed normally
- Circuit Open: New items queued, existing items remain in queue
- Circuit Half-Open: Scheduled service processes queued items as test calls
- Circuit Closed Again: All queued items processed normally

This ensures no submissions are lost even when NRS is temporarily unavailable.

## Running the service

### To run entirely under Service Manager
```
sm2 --start VAPING_DUTY_ALL
```

### To run locally
Launch as if running entirely under service manager above.

Stop the backend service running under Service Manager:
```
sm2 --stop VAPING_DUTY 
```

Start the service running locally:
```
sbt run
```

## Test the application

To run the full set of test suites with coverage reports:

```
sbt runAllChecks
```
To run the unit test suites with coverage reports:

```
sbt runLocalChecks
```

### License

This code is open source software licensed under the [Apache 2.0 License].

