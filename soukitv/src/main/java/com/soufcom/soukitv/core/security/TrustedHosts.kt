package com.soufcom.soukitv.core.security

fun defaultTrustedHosts(securityVault: SecurityVault?): Set<String> =
    securityVault?.trustedHosts().orEmpty()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map(String::lowercase)
        .toSet()
