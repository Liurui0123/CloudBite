package com.cloudbite.ai.controller;

import com.cloudbite.ai.service.AiService;
import com.cloudbite.common.ApiResult;
import com.cloudbite.common.AuthHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService service;

    public AiController(AiService service) {
        this.service = service;
    }

    @PostMapping("/recommend")
    public ApiResult<Map<String, Object>> recommend(@RequestHeader(value = AuthHeaders.USER_ID, required = false) Long userId,
                                                    @RequestBody Map<String, String> body) {
        return ApiResult.ok(service.recommend(userId, body.get("preference")));
    }

    @PostMapping("/chat")
    public ApiResult<Map<String, Object>> chat(@RequestHeader(value = AuthHeaders.USER_ID, required = false) Long userId,
                                               @RequestBody Map<String, String> body) {
        return ApiResult.ok(service.chat(userId, body.get("message"), body.get("orderNo")));
    }

    @PostMapping("/description")
    public ApiResult<Map<String, Object>> description(@RequestHeader(value = AuthHeaders.USER_ID, required = false) Long userId,
                                                      @RequestBody Map<String, String> body) {
        return ApiResult.ok(service.describeDish(userId, body.get("dishName"), body.get("ingredients")));
    }
}