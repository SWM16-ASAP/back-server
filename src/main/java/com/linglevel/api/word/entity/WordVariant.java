package com.linglevel.api.word.entity;

import com.linglevel.api.word.dto.VariantType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "word_variants")
public class WordVariant {
    @Id
    private String id;

    @Indexed(unique = true)
    private String word;

    private String originalForm;

    private VariantType variantType;
}