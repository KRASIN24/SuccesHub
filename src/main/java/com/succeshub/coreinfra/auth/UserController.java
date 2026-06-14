package com.succeshub.coreinfra.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes the currently authenticated user to the SPA. The frontend calls this
 * on startup to learn whether there is an active session and who the user is.
 * When there is no session, the security layer returns 401 before this runs.
 */
@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> currentUser(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        String name = principal.getPreferredUsername() != null
                ? principal.getPreferredUsername()
                : principal.getFullName();

        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", principal.getSubject());
        body.put("name", name);
        body.put("email", principal.getEmail());
        body.put("roles", roles);

        return ResponseEntity.ok(body);
    }
}
