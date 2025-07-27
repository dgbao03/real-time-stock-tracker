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
    public ResponseEntity<?> getCompanyNews (
            @RequestParam String symbol,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "true") boolean isPaginate
    ){
        log.info("Received request to get company news for symbol [{}]", symbol);
        Object newsResponse = this.newsService.getCompanyNews(symbol,  pageNo, pageSize, isPaginate);

        String resultMessage = (newsResponse != null) ? "Contains news data" : "No news data";
        log.info("Financial news request for symbol [{}] has completed. Response data: [{}]", symbol, resultMessage);

        return ResponseEntity.ok(newsResponse);
    }

}
