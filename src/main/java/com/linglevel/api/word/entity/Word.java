package com.linglevel.api.word.entity;

import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.model.Meaning;
import com.linglevel.api.word.model.RelatedForms;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * 단어 엔티티 (원형 단어만 저장) 변형 형태는 WordVariant에 별도 저장
 *
 * 같은 단어도 언어 쌍별로 여러 개 저장 가능 예: "run" EN->KO, "run" EN->JA는 별도 행
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "words")
public class Word {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 원형 단어 (예: "pretty", "see", "run") 복합 unique 제약의 일부 (word + targetLanguageCode +
	 * sourceLanguageCode)
	 */
	@Column(nullable = false, length = 255)
	private String word;

	/**
	 * 원본 언어 코드
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private LanguageCode sourceLanguageCode;

	/**
	 * 번역 대상 언어 코드
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private LanguageCode targetLanguageCode;

	/**
	 * 자주 쓰이는 뜻 3개 요약 (대상 언어)
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> summary;

	/**
	 * 품사별 의미 목록 AI로부터 받은 Meaning을 그대로 저장
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	private List<Meaning> meanings;

	/**
	 * 관련 변형 형태들 (동사 활용형, 비교급, 복수형 등)
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	private RelatedForms relatedForms;

	@Builder.Default
	@Column(nullable = false)
	private Boolean isEssential = false;

}
