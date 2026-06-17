package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.LunarDto;

/**
 * Application service that computes the current lunar phase for the astronomy widget.
 */
public interface LunarService {

    /**
     * Returns the current moon phase, illumination, and approximate rise/set times.
     *
     * @return lunar phase data derived from the Conway/Meeus algorithm
     */
    LunarDto getLunarPhase();
}
