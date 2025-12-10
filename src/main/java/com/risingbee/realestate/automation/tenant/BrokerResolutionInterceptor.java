package com.risingbee.realestate.automation.tenant;

import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.repo.BrokerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Resolve broker per request:
 * 1) header X-Broker-ApiKey
 * 2) query param broker
 * 3) session attribute "brokerId"
 *
 * If API key present but invalid => respond 401 (for APIs/webhooks).
 * Otherwise leave context null (some public endpoints possible).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BrokerResolutionInterceptor implements HandlerInterceptor {

    public static final String HEADER_API_KEY = "X-Broker-ApiKey";
    public static final String PARAM_BROKER = "broker";
    public static final String SESSION_BROKER_ID = "brokerId";

    private final BrokerRepository brokerRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            String apiKey = request.getHeader(HEADER_API_KEY);
            if (apiKey == null || apiKey.isBlank()) {
                apiKey = request.getParameter(PARAM_BROKER);
            }

            if (apiKey != null && !apiKey.isBlank()) {
                Optional<Broker> ob = brokerRepository.findByApiKey(apiKey);
                if (ob.isEmpty()) {
                    log.warn("Invalid API Key used: {}", apiKey);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return false;
                }
                BrokerContext.set(ob.get());
                return true;
            }

            HttpSession session = request.getSession(false);
            if (session != null) {
                Object raw = session.getAttribute(SESSION_BROKER_ID);
                if (raw instanceof Long) {
                    Long brokerId = (Long) raw;
                    brokerRepository.findById(brokerId).ifPresent(BrokerContext::set);
                } else if (raw instanceof Integer) {
                    // just in case session stored Integer
                    Long brokerId = ((Integer) raw).longValue();
                    brokerRepository.findById(brokerId).ifPresent(BrokerContext::set);
                }
            }

            return true;
        } catch (Exception ex) {
            log.error("Error resolving broker", ex);
            // allow request to proceed without tenant rather than blocking system (depends on policy)
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BrokerContext.clear();
    }
}
