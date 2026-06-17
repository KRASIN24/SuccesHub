package com.succeshub.appdomain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "daily_quote", schema = "succeshub")
@Getter
@Setter
@NoArgsConstructor
public class DailyQuote {

    @Id
    private UUID id;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "author", nullable = false)
    private String author;
}
