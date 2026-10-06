package com.vihan.hospital.common.web;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vihan.hospital.common.error.ApiException.TooManyRequestsException;
import com.vihan.hospital.config.AppProperties;

/**
 * Per-IP limits on the public booking endpoints and staff login (DESIGN.md §12.4, §19.2) using Bucket4j token
 * buckets kept in a {@code ConcurrentHashMap} keyed by limit and IP (§7). Idle buckets are dropped
 * every few minutes so the map can't grow without bound. Single server, so in-memory is enough.
 */
@Component
public class RateLimiter {

	public enum Limit { BOOKING, LOOKUP, SLOTS, LOGIN }

	private record Entry(Bucket bucket, AtomicLong lastUsedMillis) {
	}

	private static final Duration IDLE_EXPIRY = Duration.ofMinutes(5);

	private final Map<Limit, Integer> perMinute = new EnumMap<>(Limit.class);
	private final ConcurrentHashMap<String, Entry> buckets = new ConcurrentHashMap<>();
	private final Clock clock;

	public RateLimiter(AppProperties props, Clock clock) {
		this.clock = clock;
		perMinute.put(Limit.BOOKING, props.rateLimit().bookingPerMinute());
		perMinute.put(Limit.LOOKUP, props.rateLimit().lookupPerMinute());
		perMinute.put(Limit.SLOTS, props.rateLimit().slotsPerMinute());
		perMinute.put(Limit.LOGIN, props.rateLimit().loginPerMinute());
	}

	/** Uses one request of the caller's allowance, or throws 429 with the seconds to wait. */
	public void check(Limit limit, String clientIp) {
		String key = limit + "|" + (clientIp == null ? "unknown" : clientIp);
		Entry entry = buckets.computeIfAbsent(key, k -> new Entry(newBucket(perMinute.get(limit)), new AtomicLong()));
		entry.lastUsedMillis().set(clock.millis());
		ConsumptionProbe probe = entry.bucket().tryConsumeAndReturnRemaining(1);
		if (!probe.isConsumed()) {
			long seconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
			throw new TooManyRequestsException(seconds);
		}
	}

	@Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
	void dropIdleBuckets() {
		long cutoff = clock.millis() - IDLE_EXPIRY.toMillis();
		buckets.values().removeIf(e -> e.lastUsedMillis().get() < cutoff);
	}

	int size() {
		return buckets.size();
	}

	private static Bucket newBucket(int perMinute) {
		return Bucket.builder()
			.addLimit(limit -> limit.capacity(perMinute).refillGreedy(perMinute, Duration.ofMinutes(1)))
			.build();
	}

}
