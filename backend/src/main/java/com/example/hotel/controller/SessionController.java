package com.example.hotel.controller;

import com.example.hotel.dto.*;
import com.example.hotel.exception.BusinessException;
import com.example.hotel.repository.StaffUserRepository;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff/session")
public class SessionController {
    private final AuthenticationManager authManager;
    private final StaffUserRepository staff;

    public SessionController(AuthenticationManager a, StaffUserRepository s) {
        authManager = a;
        staff = s;
    }

    @GetMapping
    SessionResponse session(Authentication auth, CsrfToken csrf) {
        return response(auth, csrf);
    }

    @PostMapping
    SessionResponse login(
            @Valid @RequestBody LoginRequest req, HttpServletRequest request, CsrfToken csrf) {
        try {
            Authentication auth =
                    authManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    req.username(), req.password()));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            request.getSession(true)
                    .setAttribute(
                            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                            context);
            return response(auth, csrf);
        } catch (AuthenticationException e) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "账号或密码错误");
        }
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest req) {
        var session = req.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
    }

    private SessionResponse response(Authentication auth, CsrfToken csrf) {
        boolean ok =
                auth != null
                        && auth.isAuthenticated()
                        && !(auth
                                instanceof
                                org.springframework.security.authentication
                                        .AnonymousAuthenticationToken);
        SessionResponse.Staff s =
                ok
                        ? staff.findByUsername(auth.getName())
                                .map(x -> new SessionResponse.Staff(x.getDisplayName()))
                                .orElse(null)
                        : null;
        return new SessionResponse(
                ok, s, new SessionResponse.Csrf(csrf.getHeaderName(), csrf.getToken()));
    }
}
