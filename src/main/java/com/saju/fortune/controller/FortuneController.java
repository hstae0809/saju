package com.saju.fortune.controller;

import com.saju.fortune.dto.FortuneResponseDto;
import com.saju.fortune.dto.SajuRequest;
import com.saju.fortune.service.SajuCalculatorEngine;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {
    "http://localhost:8080",
    "http://127.0.0.1:8080",
    "https://luckycat-tau.vercel.app"
})
public class FortuneController {

    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private final SajuCalculatorEngine sajuEngine;
    private final Map<String, RateWindow> rateLimitMap = new ConcurrentHashMap<>();

    public FortuneController(SajuCalculatorEngine sajuEngine) {
        this.sajuEngine = sajuEngine;
    }

    @PostMapping("/fortune")
    public ResponseEntity<?> getFortuneResult(@RequestBody SajuRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        if (!allowRequest(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
        }

        if (request == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "잘못된 요청입니다."));
        }

        try {
            request.validate();
            FortuneResponseDto response = sajuEngine.getTodayFortune(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "운세를 처리하는 중 문제가 발생했습니다."));
        }
    }

    private boolean allowRequest(String ip) {
        long now = System.currentTimeMillis();
        if (rateLimitMap.size() > 5000) {
            rateLimitMap.clear();
        }
        RateWindow window = rateLimitMap.computeIfAbsent(ip, k -> new RateWindow(now));
        synchronized (window) {
            if (now - window.windowStart > 60_000L) {
                window.windowStart = now;
                window.count.set(1);
                return true;
            }
            return window.count.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    private static class RateWindow {
        long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        RateWindow(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}