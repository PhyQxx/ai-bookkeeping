package com.aibookkeeping.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * XSS Protection Filter - escapes HTML special characters in request parameters.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XssFilter implements Filter {

    private static final Pattern[] PATTERNS = {
            Pattern.compile("<script.*?>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),  // onclick=, onload=, etc.
            Pattern.compile("<iframe.*?>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<embed.*?>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<object.*?>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            chain.doFilter(new XssHttpServletRequestWrapper((HttpServletRequest) request), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private static class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
        public XssHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return clean(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] cleaned = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleaned[i] = clean(values[i]);
            }
            return cleaned;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return clean(value);
        }

        private String clean(String value) {
            if (value == null) return null;
            for (Pattern p : PATTERNS) {
                value = p.matcher(value).replaceAll("");
            }
            value = value.replace("<", "&lt;").replace(">", "&gt;");
            return value;
        }
    }
}
