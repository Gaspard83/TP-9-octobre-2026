package com.boutique.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Accès à l'utilisateur authentifié (issu du JWT) et contrôles de propriété des ressources. */
@Component
public class CurrentUserService {

    public Long currentCustomerId() {
        Jwt jwt = currentJwt();
        Number id = jwt.getClaim(JwtService.CLAIM_CUSTOMER_ID);
        return id.longValue();
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** Un client ne peut accéder qu'à ses propres données ; un administrateur accède à tout. */
    public void checkAccessTo(Long customerId) {
        if (!isAdmin() && !currentCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Accès refusé à une ressource d'un autre client");
        }
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        throw new AccessDeniedException("Aucun utilisateur authentifié par JWT");
    }
}
