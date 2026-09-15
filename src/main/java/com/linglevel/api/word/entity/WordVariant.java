package com.linglevel.api.word.entity;

import com.linglevel.api.word.dto.VariantType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "word_variants")
public class WordVariant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String word;

	@Column(nullable = false, length = 255)
	private String originalForm;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<VariantType> variantTypes;

}
