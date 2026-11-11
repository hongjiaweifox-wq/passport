package com.tuya.passport.cloud.common.support;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ActorContext {
    public Actor actor(String orgHeader, String userHeader, String roleHeader) {
        String org = normalize(orgHeader, "DEMO-ORG").toUpperCase(Locale.ROOT);
        String user = normalize(userHeader, "alice");
        String role = normalize(roleHeader, "ADMIN").toUpperCase(Locale.ROOT);
        if (!org.matches("[A-Z0-9_-]{1,64}")) throw new ApiException("COMMON_INVALID_ORG", "Invalid organization code", HttpStatus.BAD_REQUEST);
        if (!user.matches("[A-Za-z0-9_.@-]{1,128}")) throw new ApiException("COMMON_INVALID_USER", "Invalid user", HttpStatus.BAD_REQUEST);
        if (!Set.of("DATA_EDITOR", "COMPLIANCE_MAKER", "COMPLIANCE_CHECKER", "PASSPORT_OPERATOR", "ADMIN").contains(role)) {
            throw new ApiException("COMMON_INVALID_ROLE", "Unknown demo role", HttpStatus.BAD_REQUEST);
        }
        return new Actor(org, user, role);
    }

    public void requireAnyRole(Actor actor, String... roles) {
        Set<String> allowed = Arrays.stream(roles).collect(Collectors.toSet());
        if (!allowed.contains(actor.role())) throw ApiException.forbidden("COMMON_FORBIDDEN", "This demo role cannot perform the operation");
    }

    private static String normalize(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    public record Actor(String orgCode, String userId, String role) {}
}
