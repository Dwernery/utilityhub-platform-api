package com.utilityhub.api.config;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import tools.jackson.databind.json.JsonMapper;

/**
 * CSRF protection for the session cookie. A browser attaches cookies by itself, so
 * another website could try to make it send a write (POST, PUT, ...) to this API.
 * {@code SameSite=Strict} already stops that in current browsers; this filter is the
 * second line: a cookie-carrying write is refused unless the browser says it comes from
 * this site ({@code Origin}, or {@code Sec-Fetch-Site} when there's no Origin).
 *
 * <p>Requests with {@code Authorization: Bearer} are not affected: a browser never adds
 * that header on its own, so it can't be forged that way.
 *
 * <p>Deliberately not a {@code @Component} (see {@link SessionRefreshFilter}).
 */
class CookieOriginFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final List<String> alsoAllowed;
    private final JsonMapper jsonMapper;

    /** @param alsoAllowed other origins to accept, i.e. the CORS origins (the Vite dev server) */
    CookieOriginFilter(List<String> alsoAllowed, JsonMapper jsonMapper) {
        this.alsoAllowed = alsoAllowed.stream()
                .map(origin -> origin.toLowerCase(Locale.ROOT))
                .toList();
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (needsCheck(request) && !fromThisSite(request)) {
            ProblemResponses.write(
                    response,
                    jsonMapper,
                    request,
                    HttpStatus.FORBIDDEN,
                    "This request did not come from the UtilityHub web app.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean needsCheck(HttpServletRequest request) {
        return !SAFE_METHODS.contains(request.getMethod())
                && request.getHeader(HttpHeaders.AUTHORIZATION) == null
                && WebUtils.getCookie(request, SessionCookies.NAME) != null;
    }

    private boolean fromThisSite(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null) {
            String normalized = origin.toLowerCase(Locale.ROOT);
            return normalized.equals(ownOrigin(request)) || alsoAllowed.contains(normalized);
        }
        // No Origin (rare for a write): fall back to the browser's own verdict, if it gave one.
        String site = request.getHeader("Sec-Fetch-Site");
        return site == null || site.equals("same-origin") || site.equals("none");
    }

    /** This server's own origin as the browser sees it (nginx's X-Forwarded-* already applied). */
    private static String ownOrigin(HttpServletRequest request) {
        String scheme = request.getScheme().toLowerCase(Locale.ROOT);
        int port = request.getServerPort();
        boolean defaultPort = (scheme.equals("https") && port == 443) || (scheme.equals("http") && port == 80);
        return scheme + "://" + request.getServerName().toLowerCase(Locale.ROOT) + (defaultPort ? "" : ":" + port);
    }
}
