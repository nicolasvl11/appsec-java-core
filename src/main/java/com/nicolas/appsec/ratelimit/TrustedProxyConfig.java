package com.nicolas.appsec.ratelimit;

import java.util.Set;

public record TrustedProxyConfig(Set<String> trustedProxyIps) {

    public boolean isTrusted(String remoteAddr) {
        return remoteAddr != null && trustedProxyIps.contains(remoteAddr);
    }
}