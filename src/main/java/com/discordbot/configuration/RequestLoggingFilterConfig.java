package com.discordbot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestLoggingFilterConfig {

    private static final int MAX_PAYLOAD = 10000;

    /**
     * To automatically log body request
     *
     * <p>required:<br/>
     * application.properties<br/>
     * logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter=DEBUG
     *
     * @return CommonsRequestLoggingFilter
     */
    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        final CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(MAX_PAYLOAD);
        return filter;
    }
}
