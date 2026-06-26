package com.taf.p2plending.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "p2p.funding")
public record FundingProperties(
        @DefaultValue("14") int windowDays,
        @DefaultValue("0 0 * * * *") String expiryCheckCron) {
}
