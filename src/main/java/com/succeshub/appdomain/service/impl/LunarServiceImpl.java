package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.LunarDto;
import com.succeshub.appdomain.service.LunarService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class LunarServiceImpl implements LunarService {

    private static final String[] PHASE_NAMES = {
            "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous",
            "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"
    };

    @Override
    @Cacheable("lunar-phase")
    public LunarDto getLunarPhase() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        double jd = toJulianDay(today);

        double moonAge = jd - 2451549.5;
        double cycles = moonAge / 29.53;
        double phaseAge = (cycles - Math.floor(cycles)) * 29.53;

        int phaseIndex = (int) Math.floor((phaseAge / 29.53) * 8) % 8;
        String phaseName = PHASE_NAMES[phaseIndex];

        int illumination = (int) Math.round(50 * (1 - Math.cos((phaseAge / 29.53) * 2 * Math.PI)));
        illumination = Math.max(0, Math.min(100, illumination));

        int riseMinutes = (int) ((phaseAge / 29.53) * 24 * 60) % (24 * 60);
        int setMinutes = (riseMinutes + 12 * 60) % (24 * 60);

        String riseTime = minutesToTime(riseMinutes);
        String setTime = minutesToTime(setMinutes);

        return new LunarDto(phaseName, illumination, riseTime, setTime);
    }

    private double toJulianDay(LocalDate date) {
        int y = date.getYear();
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();
        if (m <= 2) { y--; m += 12; }
        int a = y / 100;
        int b = 2 - a + a / 4;
        return Math.floor(365.25 * (y + 4716))
                + Math.floor(30.6001 * (m + 1))
                + d + b - 1524.5;
    }

    private String minutesToTime(int totalMinutes) {
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        return LocalTime.of(h, m).format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
