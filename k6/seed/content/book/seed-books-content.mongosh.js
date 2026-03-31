/*
 * K6 content seed for local MongoDB.
 *
 * Usage:
 *   docker compose exec -T mongo mongosh llv_api_local < k6/seed/content/book/seed-books-content.mongosh.js
 *
 * Optional env vars:
 *   SEED_PREFIX=k6seed
 *   BOOK_COUNT=240
 *   USER_COUNT=4
 *   EXTRA_DIFFICULTY_RATIO=0.3
 *   RESET_EXISTING=false
 */

(function seedBooksContent() {
  const env = typeof process !== 'undefined' ? process.env : {};

  const config = {
    seedPrefix: env.SEED_PREFIX || 'k6seed',
    bookCount: positiveInt(env.BOOK_COUNT, 240),
    userCount: positiveInt(env.USER_COUNT, 4),
    extraDifficultyRatio: boundedNumber(env.EXTRA_DIFFICULTY_RATIO, 0.3, 0, 1),
    resetExisting: parseBoolean(env.RESET_EXISTING || 'false'),
    now: new Date(),
  };

  const random = createRandom(config.seedPrefix);
  const databaseName = db.getName();

  const collections = {
    users: db.getCollection('user'),
    books: db.getCollection('books'),
    chapters: db.getCollection('chapters'),
    chunks: db.getCollection('chunks'),
  };

  print(`[seed] Database: ${databaseName}`);
  print(`[seed] Prefix: ${config.seedPrefix}`);
  print(`[seed] Books: ${config.bookCount}, Users: ${config.userCount}`);

  if (config.resetExisting) {
    resetExistingSeed(collections, config.seedPrefix);
  }

  const users = buildUsers(config);
  const content = buildContentGraph(config, random);

  upsertUsers(collections.users, users);
  upsertBooks(collections.books, content.books);
  upsertChapters(collections.chapters, content.chapters);
  upsertChunks(collections.chunks, content.chunks);

  print('[seed] Completed successfully');
  printjson({
    users: users.length,
    books: content.books.length,
    chapters: content.chapters.length,
    chunks: content.chunks.length,
  });
})();

function resetExistingSeed(collections, seedPrefix) {
  const idRegex = new RegExp(`^${escapeRegex(seedPrefix)}-`);
  const usernameRegex = new RegExp(`^${escapeRegex(seedPrefix)}-user-`);

  const deletedChunks = collections.chunks.deleteMany({ id: idRegex }).deletedCount;
  const deletedChapters = collections.chapters.deleteMany({ id: idRegex }).deletedCount;
  const deletedBooks = collections.books.deleteMany({ id: idRegex }).deletedCount;
  const deletedUsers = collections.users.deleteMany({ username: usernameRegex }).deletedCount;

  print(`[seed] Reset existing seed docs: users=${deletedUsers}, books=${deletedBooks}, chapters=${deletedChapters}, chunks=${deletedChunks}`);
}

function buildUsers(config) {
  const users = [];

  for (let index = 1; index <= config.userCount; index += 1) {
    const username = `${config.seedPrefix}-user-${pad(index, 2)}`;

    users.push({
      id: `${config.seedPrefix}-user-doc-${pad(index, 2)}`,
      username,
      email: `${username}@example.local`,
      displayName: `K6 Seed User ${index}`,
      provider: 'test',
      profileImageUrl: `https://static.linglevel.local/profiles/${config.seedPrefix}/${pad(index, 2)}.png`,
      role: 'USER',
      deleted: false,
      createdAt: daysAgo(index * 7),
      deletedAt: null,
    });
  }

  return users;
}

function buildContentGraph(config, random) {
  const books = [];
  const chapters = [];
  const chunks = [];

  for (let bookIndex = 1; bookIndex <= config.bookCount; bookIndex += 1) {
    const profile = pickBookProfile(random);
    const bookId = `${config.seedPrefix}-book-${pad(bookIndex, 4)}`;
    const createdAt = daysAgo(randomInt(random, 0, 365));
    const titleSeed = buildTitleSeed(bookIndex);
    const primaryDifficulty = pickPrimaryDifficulty(random);
    const difficultyLevels = buildDifficultyLevels(primaryDifficulty, config.extraDifficultyRatio, random);

    let totalReadingTime = 0;
    const chapterCount = randomInt(random, profile.chapterRange.min, profile.chapterRange.max);

    for (let chapterNumber = 1; chapterNumber <= chapterCount; chapterNumber += 1) {
      const chapterId = `${bookId}-chapter-${pad(chapterNumber, 2)}`;
      const chunkPlan = buildChunkPlan(profile, chapterNumber, random);
      const chapterReadingTime = estimateChapterReadingTime(chunkPlan.primaryChunkCount, difficultyLevels.length);

      chapters.push({
        id: chapterId,
        bookId,
        chapterNumber,
        title: `Chapter ${chapterNumber}. ${buildChapterTitle(titleSeed.baseNoun, chapterNumber)}`,
        chapterImageUrl: chapterNumber % 5 === 0
          ? `https://static.linglevel.local/books/${bookId}/chapters/${pad(chapterNumber, 2)}.jpg`
          : null,
        description: `${titleSeed.baseAdjective} events unfold around ${titleSeed.baseNoun.toLowerCase()} in chapter ${chapterNumber}.`,
        readingTime: chapterReadingTime,
      });

      totalReadingTime += chapterReadingTime;

      for (let levelIndex = 0; levelIndex < difficultyLevels.length; levelIndex += 1) {
        const difficultyLevel = difficultyLevels[levelIndex];
        const chunkCount = adjustChunkCountByLevel(chunkPlan.primaryChunkCount, levelIndex, random);

        for (let chunkNumber = 1; chunkNumber <= chunkCount; chunkNumber += 1) {
          const chunkId = `${chapterId}-${difficultyLevel.toLowerCase()}-chunk-${pad(chunkNumber, 2)}`;
          const isImage = shouldCreateImageChunk(chunkNumber, chapterNumber, random);

          chunks.push({
            id: chunkId,
            chapterId,
            chunkNumber,
            difficultyLevel,
            type: isImage ? 'IMAGE' : 'TEXT',
            content: isImage
              ? `https://static.linglevel.local/books/${bookId}/chapters/${pad(chapterNumber, 2)}/images/${difficultyLevel.toLowerCase()}-${pad(chunkNumber, 2)}.jpg`
              : buildChunkText(titleSeed, chapterNumber, chunkNumber, difficultyLevel),
            description: isImage
              ? `${titleSeed.baseAdjective} illustration for chapter ${chapterNumber}, chunk ${chunkNumber}.`
              : null,
          });
        }
      }
    }

    books.push({
      id: bookId,
      title: `${titleSeed.baseAdjective} ${titleSeed.baseNoun}`,
      titleTranslations: {
        ko: `${titleSeed.baseNounKo}의 ${titleSeed.baseAdjectiveKo}`,
        ja: `${titleSeed.baseAdjectiveJa} ${titleSeed.baseNounJa}`,
      },
      author: buildAuthorName(bookIndex),
      coverImageUrl: `https://static.linglevel.local/books/${bookId}/cover-small.jpg`,
      difficultyLevel: primaryDifficulty,
      chapterCount,
      readingTime: totalReadingTime,
      averageRating: buildAverageRating(random),
      reviewCount: buildReviewCount(profile, random),
      viewCount: buildViewCount(profile, random),
      tags: buildTags(profile, random),
      createdAt,
    });
  }

  return { books, chapters, chunks };
}

function upsertUsers(collection, users) {
  collection.bulkWrite(
    users.map((user) => ({
      updateOne: {
        filter: { username: user.username },
        update: { $set: user },
        upsert: true,
      },
    })),
    { ordered: false }
  );
}

function upsertBooks(collection, books) {
  collection.bulkWrite(
    books.map((book) => ({
      updateOne: {
        filter: { id: book.id },
        update: { $set: book },
        upsert: true,
      },
    })),
    { ordered: false }
  );
}

function upsertChapters(collection, chapters) {
  collection.bulkWrite(
    chapters.map((chapter) => ({
      updateOne: {
        filter: { id: chapter.id },
        update: { $set: chapter },
        upsert: true,
      },
    })),
    { ordered: false }
  );
}

function upsertChunks(collection, chunks) {
  const batchSize = 1000;

  for (let index = 0; index < chunks.length; index += batchSize) {
    const batch = chunks.slice(index, index + batchSize);

    collection.bulkWrite(
      batch.map((chunk) => ({
        updateOne: {
          filter: { id: chunk.id },
          update: { $set: chunk },
          upsert: true,
        },
      })),
      { ordered: false }
    );
  }
}

function pickBookProfile(random) {
  const value = random();

  if (value < 0.2) {
    return {
      name: 'short',
      chapterRange: { min: 6, max: 8 },
      chunkRange: { min: 18, max: 24 },
      tags: ['starter', 'dialogue', 'daily-life', 'school'],
    };
  }

  if (value < 0.8) {
    return {
      name: 'medium',
      chapterRange: { min: 10, max: 15 },
      chunkRange: { min: 26, max: 34 },
      tags: ['classic', 'growth', 'friendship', 'mystery', 'travel'],
    };
  }

  return {
    name: 'long',
    chapterRange: { min: 20, max: 30 },
    chunkRange: { min: 34, max: 42 },
    tags: ['epic', 'history', 'adventure', 'war', 'politics'],
  };
}

function buildChunkPlan(profile, chapterNumber, random) {
  const primaryChunkCount = randomInt(random, profile.chunkRange.min, profile.chunkRange.max);
  const chapterWeight = chapterNumber % 7 === 0 ? 1 : 0;

  return {
    primaryChunkCount: primaryChunkCount + chapterWeight,
  };
}

function pickPrimaryDifficulty(random) {
  const levels = ['A2', 'B1', 'B2', 'C1'];
  return levels[randomInt(random, 0, levels.length - 1)];
}

function buildDifficultyLevels(primaryDifficulty, extraDifficultyRatio, random) {
  const order = ['A0', 'A1', 'A2', 'B1', 'B2', 'C1', 'C2'];
  const primaryIndex = order.indexOf(primaryDifficulty);
  const levels = [primaryDifficulty];

  if (random() >= extraDifficultyRatio) {
    return levels;
  }

  const candidates = [];

  if (primaryIndex > 0) {
    candidates.push(order[primaryIndex - 1]);
  }
  if (primaryIndex < order.length - 1) {
    candidates.push(order[primaryIndex + 1]);
  }

  if (candidates.length > 0) {
    levels.push(candidates[randomInt(random, 0, candidates.length - 1)]);
  }

  return levels;
}

function adjustChunkCountByLevel(primaryChunkCount, levelIndex, random) {
  if (levelIndex === 0) {
    return primaryChunkCount;
  }

  return Math.max(3, primaryChunkCount + randomInt(random, -1, 1));
}

function shouldCreateImageChunk(chunkNumber, chapterNumber, random) {
  if ((chapterNumber + chunkNumber) % 11 === 0) {
    return true;
  }

  return random() < 0.03;
}

function buildChunkText(titleSeed, chapterNumber, chunkNumber, difficultyLevel) {
  const sentence = `${titleSeed.baseAdjective} ${titleSeed.baseNoun.toLowerCase()} moves through chapter ${chapterNumber}, section ${chunkNumber}, at ${difficultyLevel} pace.`;
  return [
    sentence,
    'The character observes small details, reacts to change, and keeps the scene moving with clear narrative beats.',
    'This placeholder text is intentionally stable so local k6 comparisons focus on query cost rather than random payload drift.',
  ].join(' ');
}

function buildTitleSeed(bookIndex) {
  const adjectives = ['Silent', 'Hidden', 'Burning', 'Golden', 'Fading', 'Northern', 'Restless', 'Last'];
  const nouns = ['Garden', 'Harbor', 'Compass', 'Archive', 'Forest', 'Letters', 'Skyline', 'Bridge'];
  const adjectivesKo = ['조용한', '숨겨진', '타오르는', '황금빛', '희미한', '북쪽의', '불안한', '마지막'];
  const nounsKo = ['정원', '항구', '나침반', '기록보관소', '숲', '편지', '스카이라인', '다리'];
  const adjectivesJa = ['静かな', '隠された', '燃える', '黄金の', '薄れる', '北の', '落ち着かない', '最後の'];
  const nounsJa = ['庭', '港', '羅針盤', '記録庫', '森', '手紙', 'スカイライン', '橋'];

  const adjectiveIndex = bookIndex % adjectives.length;
  const nounIndex = Math.floor(bookIndex / adjectives.length) % nouns.length;

  return {
    baseAdjective: adjectives[adjectiveIndex],
    baseNoun: nouns[nounIndex],
    baseAdjectiveKo: adjectivesKo[adjectiveIndex],
    baseNounKo: nounsKo[nounIndex],
    baseAdjectiveJa: adjectivesJa[adjectiveIndex],
    baseNounJa: nounsJa[nounIndex],
  };
}

function buildChapterTitle(baseNoun, chapterNumber) {
  const patterns = ['Arrival', 'Signal', 'Detour', 'Witness', 'Crossing', 'Turn', 'Distance', 'Echo'];
  return `${patterns[(chapterNumber - 1) % patterns.length]} of the ${baseNoun}`;
}

function buildAuthorName(bookIndex) {
  const firstNames = ['Mina', 'Elias', 'Harper', 'Jun', 'Noah', 'Sora', 'Lena', 'Theo'];
  const lastNames = ['Park', 'Rivera', 'Bennett', 'Tanaka', 'Kim', 'Silva', 'Walker', 'Ito'];

  return `${firstNames[bookIndex % firstNames.length]} ${lastNames[Math.floor(bookIndex / firstNames.length) % lastNames.length]}`;
}

function buildAverageRating(random) {
  return Number((3.4 + random() * 1.4).toFixed(1));
}

function buildReviewCount(profile, random) {
  const multiplier = profile.name === 'long' ? 1.4 : profile.name === 'short' ? 0.7 : 1;
  return Math.round((20 + random() * 180) * multiplier);
}

function buildViewCount(profile, random) {
  const base = profile.name === 'long' ? 600 : profile.name === 'short' ? 80 : 250;
  const heavyTail = Math.pow(random(), 0.35);
  return Math.round(base + heavyTail * 6000);
}

function buildTags(profile, random) {
  const tags = [];
  const candidates = profile.tags.slice();
  const tagCount = randomInt(random, 1, Math.min(3, candidates.length));

  while (tags.length < tagCount) {
    const index = randomInt(random, 0, candidates.length - 1);
    tags.push(candidates.splice(index, 1)[0]);
  }

  return tags;
}

function estimateChapterReadingTime(primaryChunkCount, difficultyCount) {
  return Math.max(3, Math.round((primaryChunkCount * (difficultyCount > 1 ? 1.15 : 1)) * 1.1));
}

function createRandom(seedString) {
  let seed = 0;

  for (let index = 0; index < seedString.length; index += 1) {
    seed = (seed * 31 + seedString.charCodeAt(index)) >>> 0;
  }

  return function next() {
    seed = (seed + 0x6D2B79F5) >>> 0;
    let value = seed;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  };
}

function randomInt(random, min, max) {
  return Math.floor(random() * (max - min + 1)) + min;
}

function positiveInt(value, fallback) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function boundedNumber(value, fallback, min, max) {
  const parsed = Number(value);

  if (!Number.isFinite(parsed)) {
    return fallback;
  }

  return Math.min(max, Math.max(min, parsed));
}

function parseBoolean(value) {
  return ['1', 'true', 'yes', 'on'].includes(String(value).toLowerCase());
}

function pad(value, length) {
  return String(value).padStart(length, '0');
}

function daysAgo(days) {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() - days);
  return date;
}

function escapeRegex(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
