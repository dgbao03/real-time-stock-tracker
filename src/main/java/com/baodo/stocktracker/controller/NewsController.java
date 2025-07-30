package com.baodo.stocktracker.controller;

import com.baodo.stocktracker.dto.response.NewsItemResponse;
import com.baodo.stocktracker.service.NewsService;
import io.lettuce.core.dynamic.annotation.Param;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@Slf4j
@RequestMapping("/company-news")
@AllArgsConstructor
public class NewsController {
    private NewsService newsService;

    @GetMapping
    public ResponseEntity<?> getCompanyNews(@RequestParam String symbol) {
        log.info("Received request to get company news for symbol [{}]", symbol);

        List<NewsItemResponse> newsResponse = this.newsService.getCompanyNews(symbol);

        log.info("Financial news request for symbol [{}] has completed", symbol);
        return ResponseEntity.ok(newsResponse);
    }

}
