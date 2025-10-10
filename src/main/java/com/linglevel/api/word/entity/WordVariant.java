package com.linglevel.api.word.entity;

import com.linglevel.api.word.dto.VariantType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "word_variants")
@CompoundIndex(name = "word_original_idx", def = "{'word': 1, 'originalForm': 1}", unique = true)
public class WordVariant {
    @Id
    private String id;

    @Indexed
    private String word;

    private String originalForm;

    private VariantType variantType;
}