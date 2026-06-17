package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.QuoteDto;
import com.succeshub.appdomain.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    @Override
    @Transactional(readOnly = true)
    public QuoteDto getDailyQuote() {
        return new QuoteDto(
                "The void is not an empty space, but a canvas awaiting the curation of your will.",
                "The Archivist"
        );
    }
}
