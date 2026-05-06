package com.pasoor.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.web.server.Cookie.SameSite;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SessionCookieConfigurationTest {
    @Test
    void productionSessionCookieSettingsCanBeBoundFromEnvironment() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "server.servlet.session.cookie.same-site", "none",
                "server.servlet.session.cookie.secure", "true"
        ));

        ServerProperties properties = new Binder(source)
                .bind("server", Bindable.of(ServerProperties.class))
                .orElseThrow(() -> new AssertionError("server properties should bind"));

        assertThat(properties.getServlet().getSession().getCookie().getSameSite()).isEqualTo(SameSite.NONE);
        assertThat(properties.getServlet().getSession().getCookie().getSecure()).isTrue();
    }
}
