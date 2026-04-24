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

function seedBooksContent() {
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
    bookProgresses: db.getCollection('bookProgress'),
  };

  print(`[seed] Database: ${databaseName}`);
  print(`[seed] Prefix: ${config.seedPrefix}`);
  print(`[seed] Books: ${config.bookCount}, Users: ${config.userCount}`);

  if (config.resetExisting) {
    resetExistingSeed(collections, config.seedPrefix);
  }

  const users = buildUsers(config);
  const content = buildContentGraph(config, random);
  const progressSeed = buildBookProgresses(config, users, content.bookCatalog, createRandom(`${config.seedPrefix}:progress`));
  print(`[seed] Prepared bookProgress documents: ${progressSeed.bookProgresses.length}`);

  upsertUsers(collections.users, users);
  upsertBooks(collections.books, content.books);
  upsertChapters(collections.chapters, content.chapters);
  upsertChunks(collections.chunks, content.chunks);
  print('[seed] Chunk upserts completed');
  upsertBookProgresses(collections.bookProgresses, progressSeed.bookProgresses);
  print('[seed] Book progress upserts completed');

  print('[seed] Completed successfully');
  printjson({
    users: users.length,
    books: content.books.length,
    chapters: content.chapters.length,
    chunks: content.chunks.length,
    bookProgresses: progressSeed.bookProgresses.length,
    progressProfiles: progressSeed.summaryByUser,
  });

  if (typeof quit === 'function') {
    quit(0);
  }
}

function resetExistingSeed(collections, seedPrefix) {
  const idRegex = new RegExp(`^${escapeRegex(seedPrefix)}-`);
  const usernameRegex = new RegExp(`^${escapeRegex(seedPrefix)}-user-`);

  const deletedProgresses = collections.bookProgresses.deleteMany({
    $or: [
      { _id: idRegex },
      { id: idRegex },
      { userId: idRegex },
      { bookId: idRegex },
    ],
  }).deletedCount;
  const deletedChunks = collections.chunks.deleteMany({
    $or: [
      { _id: idRegex },
      { id: idRegex },
    ],
  }).deletedCount;
  const deletedChapters = collections.chapters.deleteMany({
    $or: [
      { _id: idRegex },
      { id: idRegex },
    ],
  }).deletedCount;
  const deletedBooks = collections.books.deleteMany({
    $or: [
      { _id: idRegex },
      { id: idRegex },
    ],
  }).deletedCount;
  const deletedUsers = collections.users.deleteMany({
    $or: [
      { _id: idRegex },
      { username: usernameRegex },
    ],
  }).deletedCount;

  print(`[seed] Reset existing seed docs: users=${deletedUsers}, books=${deletedBooks}, chapters=${deletedChapters}, chunks=${deletedChunks}, bookProgresses=${deletedProgresses}`);
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
  const bookCatalog = [];

  for (let bookIndex = 1; bookIndex <= config.bookCount; bookIndex += 1) {
    const profile = pickBookProfile(random);
    const bookId = `${config.seedPrefix}-book-${pad(bookIndex, 4)}`;
    const createdAt = daysAgo(randomInt(random, 0, 365));
    const titleSeed = buildTitleSeed(bookIndex);
    const primaryDifficulty = pickPrimaryDifficulty(random);
    const difficultyLevels = buildDifficultyLevels(primaryDifficulty, config.extraDifficultyRatio, random);
    const bookChapterCatalog = [];

    let totalReadingTime = 0;
    const chapterCount = randomInt(random, profile.chapterRange.min, profile.chapterRange.max);

    for (let chapterNumber = 1; chapterNumber <= chapterCount; chapterNumber += 1) {
      const chapterId = `${bookId}-chapter-${pad(chapterNumber, 2)}`;
      const chunkPlan = buildChunkPlan(profile, chapterNumber, random);
      const chapterReadingTime = estimateChapterReadingTime(chunkPlan.primaryChunkCount, difficultyLevels.length);
      const chapterCatalog = {
        id: chapterId,
        chapterNumber,
        chunkIdsByDifficulty: {},
      };

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

          if (!chapterCatalog.chunkIdsByDifficulty[difficultyLevel]) {
            chapterCatalog.chunkIdsByDifficulty[difficultyLevel] = [];
          }
          chapterCatalog.chunkIdsByDifficulty[difficultyLevel].push(chunkId);

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

      bookChapterCatalog.push(chapterCatalog);
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

    bookCatalog.push({
      id: bookId,
      primaryDifficulty,
      chapterCount,
      chapters: bookChapterCatalog,
    });
  }

  return { books, chapters, chunks, bookCatalog };
}

function upsertUsers(collection, users) {
  collection.bulkWrite(
    users.map((user) => ({
      updateOne: {
        filter: { _id: user.id },
        update: { $set: toPersistedDocument(user) },
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
        filter: { _id: book.id },
        update: { $set: toPersistedDocument(book) },
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
        filter: { _id: chapter.id },
        update: { $set: toPersistedDocument(chapter) },
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
          filter: { _id: chunk.id },
          update: { $set: toPersistedDocument(chunk) },
          upsert: true,
        },
      })),
      { ordered: false }
    );
  }
}

function upsertBookProgresses(collection, bookProgresses) {
  if (bookProgresses.length === 0) {
    return;
  }

  collection.bulkWrite(
    bookProgresses.map((progress) => ({
      updateOne: {
        filter: { _id: progress.id },
        update: { $set: toPersistedDocument(progress) },
        upsert: true,
      },
    })),
    { ordered: false }
  );
}

function buildBookProgresses(config, users, bookCatalog, random) {
  const progressProfiles = [
    {
      name: 'mostly-unread',
      weights: { NOT_STARTED: 0.72, IN_PROGRESS: 0.18, COMPLETED: 0.10 },
    },
    {
      name: 'balanced',
      weights: { NOT_STARTED: 0.45, IN_PROGRESS: 0.35, COMPLETED: 0.20 },
    },
    {
      name: 'active-reader',
      weights: { NOT_STARTED: 0.25, IN_PROGRESS: 0.45, COMPLETED: 0.30 },
    },
    {
      name: 'completion-heavy',
      weights: { NOT_STARTED: 0.12, IN_PROGRESS: 0.28, COMPLETED: 0.60 },
    },
  ];

  const bookProgresses = [];
  const summaryByUser = [];

  users.forEach((user, userIndex) => {
    const profile = progressProfiles[userIndex % progressProfiles.length];
    const counts = {
      NOT_STARTED: 0,
      IN_PROGRESS: 0,
      COMPLETED: 0,
    };

    bookCatalog.forEach((bookEntry, bookIndex) => {
      const status = pickWeightedProgressStatus(profile.weights, random);
      counts[status] += 1;

      if (status === 'NOT_STARTED') {
        return;
      }

      const progressId = `${config.seedPrefix}-book-progress-${pad(userIndex + 1, 2)}-${pad(bookIndex + 1, 4)}`;

      if (status === 'COMPLETED') {
        bookProgresses.push(buildCompletedBookProgress(progressId, user, bookEntry, random));
        return;
      }

      bookProgresses.push(buildInProgressBookProgress(progressId, user, bookEntry, random));
    });

    summaryByUser.push({
      username: user.username,
      profile: profile.name,
      counts,
    });
  });

  return { bookProgresses, summaryByUser };
}

function toPersistedDocument(entity) {
  const document = Object.assign({}, entity);
  delete document.id;
  return document;
}

function buildCompletedBookProgress(progressId, user, bookEntry, random) {
  const completedAgo = randomInt(random, 7, 90);
  const completedAt = daysAgo(completedAgo);
  const updatedAt = daysAgo(randomInt(random, 0, completedAgo));
  const lastChapter = bookEntry.chapters[bookEntry.chapters.length - 1];
  const lastChunkIds = getChunkIdsForProgress(lastChapter, bookEntry.primaryDifficulty);
  const lastChunkNumber = lastChunkIds.length;

  return {
    id: progressId,
    userId: user.id,
    bookId: bookEntry.id,
    chapterId: lastChapter.id,
    chunkId: lastChunkIds[lastChunkIds.length - 1],
    currentReadChapterNumber: bookEntry.chapterCount,
    maxReadChapterNumber: bookEntry.chapterCount,
    currentReadChunkNumber: lastChunkNumber,
    maxReadChunkNumber: encodeChapterFirstPosition(bookEntry.chapterCount, lastChunkNumber),
    normalizedProgress: 100,
    maxNormalizedProgress: 100,
    currentDifficultyLevel: bookEntry.primaryDifficulty,
    chapterProgresses: bookEntry.chapters.map((chapter, index) => ({
      chapterNumber: chapter.chapterNumber,
      progressPercentage: 100,
      isCompleted: true,
      completedAt: daysAgo(completedAgo + (bookEntry.chapterCount - index - 1)),
    })),
    isCompleted: true,
    completedAt,
    updatedAt,
  };
}

function buildInProgressBookProgress(progressId, user, bookEntry, random) {
  const chapterCount = bookEntry.chapterCount;
  const minimumCompletedChapters = chapterCount >= 5 ? Math.floor(chapterCount * 0.2) : 0;
  const maximumCompletedChapters = Math.max(
    minimumCompletedChapters,
    Math.min(chapterCount - 1, Math.floor(chapterCount * 0.75))
  );
  const completedChapterCount = randomInt(random, minimumCompletedChapters, maximumCompletedChapters);
  const currentChapterNumber = Math.min(chapterCount, completedChapterCount + 1);
  const currentChapter = bookEntry.chapters[currentChapterNumber - 1];
  const currentChunkIds = getChunkIdsForProgress(currentChapter, bookEntry.primaryDifficulty);
  const minimumChunkNumber = Math.max(1, Math.floor(currentChunkIds.length * 0.25));
  const maximumChunkNumber = Math.max(
    minimumChunkNumber,
    Math.min(currentChunkIds.length - 1, Math.ceil(currentChunkIds.length * 0.85))
  );
  const currentReadChunkNumber = randomInt(random, minimumChunkNumber, maximumChunkNumber);
  const currentChunkId = currentChunkIds[currentReadChunkNumber - 1];
  const currentChapterProgress = roundToOneDecimal((currentReadChunkNumber * 100) / currentChunkIds.length);
  const updatedAt = daysAgo(randomInt(random, 0, 21));

  return {
    id: progressId,
    userId: user.id,
    bookId: bookEntry.id,
    chapterId: currentChapter.id,
    chunkId: currentChunkId,
    currentReadChapterNumber: currentChapterNumber,
    maxReadChapterNumber: currentChapterNumber,
    currentReadChunkNumber,
    maxReadChunkNumber: encodeChapterFirstPosition(currentChapterNumber, currentReadChunkNumber),
    normalizedProgress: roundToOneDecimal((completedChapterCount * 100) / chapterCount),
    maxNormalizedProgress: roundToOneDecimal((completedChapterCount * 100) / chapterCount),
    currentDifficultyLevel: bookEntry.primaryDifficulty,
    chapterProgresses: buildInProgressChapterProgresses(
      bookEntry.chapters,
      completedChapterCount,
      currentChapterNumber,
      currentChapterProgress,
      random
    ),
    isCompleted: false,
    completedAt: null,
    updatedAt,
  };
}

function buildInProgressChapterProgresses(chapters, completedChapterCount, currentChapterNumber, currentChapterProgress, random) {
  const chapterProgresses = [];

  for (let index = 0; index < completedChapterCount; index += 1) {
    chapterProgresses.push({
      chapterNumber: chapters[index].chapterNumber,
      progressPercentage: 100,
      isCompleted: true,
      completedAt: daysAgo(randomInt(random, 2, 45)),
    });
  }

  chapterProgresses.push({
    chapterNumber: currentChapterNumber,
    progressPercentage: currentChapterProgress,
    isCompleted: false,
    completedAt: null,
  });

  return chapterProgresses;
}

function pickWeightedProgressStatus(weights, random) {
  const value = random();

  if (value < weights.NOT_STARTED) {
    return 'NOT_STARTED';
  }

  if (value < weights.NOT_STARTED + weights.IN_PROGRESS) {
    return 'IN_PROGRESS';
  }

  return 'COMPLETED';
}

function getChunkIdsForProgress(chapter, difficultyLevel) {
  const chunkIds = chapter.chunkIdsByDifficulty[difficultyLevel] || [];

  if (chunkIds.length === 0) {
    throw new Error(`No chunks found for chapter=${chapter.id}, difficulty=${difficultyLevel}`);
  }

  return chunkIds;
}

function encodeChapterFirstPosition(chapterNumber, chunkNumber) {
  const safeChapterNumber = Math.max(1, Number(chapterNumber) || 1);
  const safeChunkNumber = Math.max(0, Number(chunkNumber) || 0);
  return (safeChapterNumber * 65536) + safeChunkNumber;
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

function roundToOneDecimal(value) {
  return Math.round(value * 10) / 10;
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

seedBooksContent();
