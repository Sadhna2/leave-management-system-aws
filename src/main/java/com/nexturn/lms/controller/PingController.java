package com.nexturn.lms.controller;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
/** Public liveness probe without database or credential details. */
@RestController
public class PingController {
    @GetMapping("/ping")
    public Map<String, String> ping() { return Map.of("status", "UP"); }
}
