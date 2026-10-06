package com.vihan.hospital.common.web;

import com.vihan.hospital.common.web.RateLimiter.Limit;

/** Lets tests in other packages check the interceptor's path-to-limit mapping. */
public final class RateLimitInterceptorAccess {

	private RateLimitInterceptorAccess() {
	}

	public static Limit limitFor(String method, String path) {
		return RateLimitInterceptor.limitFor(method, path);
	}

}
