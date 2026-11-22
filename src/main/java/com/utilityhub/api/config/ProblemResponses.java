package com.utilityhub.api.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import tools.jackson.databind.json.JsonMapper;

/**
 * Writes an RFC 9457 problem detail from a security filter, which runs before Spring MVC
 * (and its exception handler), in the same format as every other API error.
 */
final class ProblemResponses {

    private ProblemResponses() {}

    static void write(
            HttpServletResponse response,
            JsonMapper jsonMapper,
            HttpServletRequest request,
            HttpStatus status,
            String detail)
            throws IOException {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "about:blank");
        problem.put("title", status.getReasonPhrase());
        problem.put("status", status.value());
        problem.put("detail", detail);
        problem.put("instance", request.getRequestURI());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(problem));
    }
}
