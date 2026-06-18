package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.QuoteDto;

/**
 * Application service for the daily inspirational quote shown on the dashboard.
 */
public interface QuoteService {

    /**
     * Returns the quote of the day for display in the UI.
     *
     * @return quote text and attributed author
     */
    QuoteDto getDailyQuote();
}
