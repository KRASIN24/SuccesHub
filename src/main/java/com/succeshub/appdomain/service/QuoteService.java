package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.QuoteDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuoteService {

    @Transactional(readOnly = true)
    public QuoteDto getDailyQuote() {
        return new QuoteDto(
                        "The void is not an empty space, but a canvas awaiting the curation of your will.",
                        "The Archivist"
                );
    }
}
