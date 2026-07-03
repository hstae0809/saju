package com.saju.fortune.controller;

import com.saju.fortune.dto.FortuneResponseDto;
import com.saju.fortune.dto.SajuRequest;
import com.saju.fortune.service.SajuCalculatorEngine;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") 
public class FortuneController {

    private final SajuCalculatorEngine sajuEngine;

    public FortuneController(SajuCalculatorEngine sajuEngine) {
        this.sajuEngine = sajuEngine;
    }

    @PostMapping("/fortune")
    public FortuneResponseDto getFortuneResult(@RequestBody SajuRequest request) {
        return sajuEngine.getTodayFortune(request);
    }
}