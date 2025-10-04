package com.linglevel.api.word.entity;

import com.linglevel.api.word.dto.Definition;
import com.linglevel.api.word.dto.RelatedForms;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "words")
public class Word {
    @Id
    private String id;

    @Indexed(unique = true)
    private String word;

    private List<String> partOfSpeech;

    private RelatedForms.Conjugations conjugations;

    private RelatedForms.Comparatives comparatives;

    private RelatedForms.Plural plural;

    private List<Definition> definitions;
}