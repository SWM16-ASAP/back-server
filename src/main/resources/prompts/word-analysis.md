Word: {word} | Target: {targetLanguage}

**CRITICAL: If '{word}' is nonsensical/gibberish/typo, return []**
**CRITICAL: ALL fields (summary, meaning, example, exampleTranslation) MUST have meaningful content. NEVER leave empty strings.**
**If you cannot provide meaningful content, return [] instead.**

HOMOGRAPH CHECK: Same spelling, multiple DISTINCT ORIGINS or DIFFERENT ORIGINAL FORMS?
(e.g., "saw"=past of "see" + noun "saw"톱, "left"=past of "leave" + adj "left"왼쪽, "rose"=past of "rise" + noun "rose"장미)
**CRITICAL**: If input word IS the original form (e.g., "run", "book"), return SINGLE entry with variantTypes=[ORIGINAL_FORM]
- "run" → Single entry: originalForm="run", variantTypes=[ORIGINAL_FORM] (DO NOT split into ORIGINAL_FORM and PAST_PARTICIPLE)
- "books" → Single entry: originalForm="book", variantTypes=[PLURAL, THIRD_PERSON]
→ YES (different origins): Return array with separate entries | NO: Single-element array

**CRITICAL MERGING RULE:**
- If input word has SAME originalForm but DIFFERENT variantTypes, MERGE into ONE ENTRY with multiple variantTypes
- Example "books":
  Single entry: originalForm="book", variantTypes=[PLURAL, THIRD_PERSON], meanings include BOTH noun meanings AND verb meanings
- Each meaning should specify its partOfSpeech clearly

STRUCTURE:
**CRITICAL: Only variantTypes describes the INPUT word. Everything else (summary, meanings, examples) describes the ORIGINAL FORM.**

1. sourceLanguageCode/targetLanguageCode: "EN", "KO", etc.
2. originalForm: Base form (verbs→infinitive, adj→positive, nouns→singular)
   **CRITICAL: For adverbs ending in "-ly":**
   - The adverb itself IS the original form
   - Do NOT remove "-ly" to get the base adjective
   - "carefully" → originalForm="carefully" (NOT "careful")
   - "absolutely" → originalForm="absolutely" (NOT "absolute")
3. variantTypes: **ARRAY** of relationships between INPUT word and originalForm
   variantTypes = ONLY morphological relationship (변형 관계만!)
   ✅ VALID VALUES: ORIGINAL_FORM, PAST_TENSE, PAST_PARTICIPLE, PRESENT_PARTICIPLE, THIRD_PERSON, COMPARATIVE, SUPERLATIVE, PLURAL, UNDEFINED

   **CRITICAL: Special cases**
   - Pronouns (them, him, whom, etc.): variantTypes=[ORIGINAL_FORM], partOfSpeech="pronoun"
   - Past participles used as adjectives (confused, interested, etc.): variantTypes=[PAST_PARTICIPLE], add BOTH verb and adjective meanings
   - Words without inflection (adverbs, prepositions, etc.): variantTypes=[ORIGINAL_FORM]

4. partOfSpeech = Grammatical category (품사)
   - Goes INSIDE meanings array (meanings 배열 안에!)
   ✅ VALID VALUES: verb, noun, adjective, adverb, pronoun, preposition, conjunction, interjection, determiner, article, numeral

   - If input="ran" and originalForm="run", then variantTypes=[PAST_TENSE]
   - If input="books" and originalForm="book", then variantTypes=[PLURAL, THIRD_PERSON] (both noun plural AND verb 3rd person)
5. summary: Max 3 common translations of the ORIGINAL FORM
   - Input "ran" → summary of "run": ["달리다","운영하다"]
   - Input "prettiest" → summary of "pretty": ["예쁜","아름다운"]
6. meanings: All meanings describe the ORIGINAL FORM (not the input word)
   - Max 15 objects (common→rare, omit obscure ones)
   - partOfSpeech: verb, noun, adjective, adverb, etc.
   - meaning: Detailed explanation in target language
   - example: **CRITICAL RULES:**
     1. ALWAYS use the ORIGINAL FORM in the example (NOT the input word!)
        - If originalForm="book" (input was "books"), use "book" in example
        - If originalForm="run" (input was "ran"), use "run" in example
     2. **PART OF SPEECH MUST MATCH**: The word in the example MUST be used as the specified partOfSpeech
        - If partOfSpeech="noun", the word must function as a noun in the example
        - If partOfSpeech="verb", the word must function as a verb in the example
        - WRONG: partOfSpeech="noun" but example has "I need to book a flight" (book is verb here)
        - CORRECT: partOfSpeech="noun" and example has "I love reading a book" (book is noun here)
     3. Grammar: Ensure grammatically correct sentences (e.g., "I/You/We/They run" ✓, "She runs" ✓)
     4. QUALITY: Natural, practical sentences used in real-life contexts
     5. CLARITY: Sentence must clearly demonstrate the word's meaning
     6. LENGTH: 5-12 words (not too short, not too long)
     7. AVOID: Generic phrases like "I need...", "This is...", "It is..." - be creative!
     GOOD: "I love reading a good book." (book as noun, matches partOfSpeech)
     GOOD: "We run a small bakery in downtown." (run as verb, matches partOfSpeech)
     BAD: "I need to book a flight." (if partOfSpeech is noun - book is verb here!)
   - exampleTranslation: Translation in target language
7. conjugations: (verbs only) present, past, pastParticiple, presentParticiple, thirdPerson
8. comparatives: (adj only) positive, comparative, superlative
9. plural: (nouns only) singular, plural

EXAMPLE - "saw" homograph:
[
  {{
    "originalForm": "see",
    "variantTypes": ["PAST_TENSE"],
    "sourceLanguageCode": "EN",
    "targetLanguageCode": "KO",
    "summary": ["보다", "알다"],
    "meanings": [
      {{
        "partOfSpeech": "verb",
        "meaning": "시각적으로 인지하다",
        "example": "We see the mountains clearly from our window.",
        "exampleTranslation": "우리는 창문에서 산이 선명하게 보입니다."
      }}
    ],
    "conjugations": {{"present": "see", "past": "saw", "pastParticiple": "seen", "presentParticiple": "seeing", "thirdPerson": "sees"}},
    "comparatives": null,
    "plural": null
  }},
  {{
    "originalForm": "saw",
    "variantTypes": ["ORIGINAL_FORM"],
    "sourceLanguageCode": "EN",
    "targetLanguageCode": "KO",
    "summary": ["톱"],
    "meanings": [
      {{
        "partOfSpeech": "noun",
        "meaning": "톱 (자르는 도구)",
        "example": "The carpenter used a saw to cut the wood.",
        "exampleTranslation": "목수는 톱을 사용하여 나무를 잘랐습니다."
      }}
    ],
    "conjugations": null,
    "comparatives": null,
    "plural": {{"singular": "saw", "plural": "saws"}}
  }}
]

{format}
