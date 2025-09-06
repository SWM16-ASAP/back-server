yp# Ling Level API 명세서 v1.0

'Ling Level' 서비스의 프론트엔드와 백엔드 개발을 위한 API 명세서입니다.

---

## 📝 기본 정보

-   **Base URL**: `/api/v1`
-   **인증**: `/oauth/login`을 제외한 모든 요청은 HTTP 헤더에 아래와 같이 인증 토큰을 포함해야 합니다.
    ```
    Authorization: Bearer {AccessToken}
    ```

---

## 👤 인증 (Authentication)

### `POST /oauth/login`

Oauth Firebase 로그인을 통해 서비스에 인증하고 JWT 토큰을 발급받습니다.

#### **Request Body**

```json
{
  "authCode": "string"
}
```
- `authCode`: 클라이언트에서  Authorization Code

#### **Success Response (200 OK)**
```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalidauthorization code."
}
```

---

## 🔐 인가 (Authorization)

### `POST /auth/refresh`

Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.

#### **Request Body**

```json
{
  "refreshToken": "string"
}
```

#### **Success Response (200 OK)**
```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid or expired refresh token."
}
```

### `POST /auth/logout`

현재 세션을 종료하고 토큰을 무효화합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Success Response (200 OK)**
```json
{
  "message": "Successfully logged out."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid or expired token."
}
```

### `GET /auth/me`

현재 Access Token에 포함된 JWT Claims 정보를 추출하여 반환합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Success Response (200 OK)**
```json
{
  "userId": "60d0fe4f5311236168a109ca",
  "username": "google_123456789",
  "email": "user@example.com",
  "role": "USER",
  "provider": "google",
  "displayName": "홍길동",
  "issuedAt": "2025-08-04T09:30:00",
  "expiresAt": "2025-08-04T19:30:00"
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid or expired token"
}
```

---

## 📚 도서 (Books)

### `GET /books`

책 목록을 조건에 따라 조회합니다. 기본적으로 최신순으로 정렬되며, 선택적으로 태그나 키워드 필터를 적용할 수 있습니다.

#### **Query Parameters**

| 파라미터  | 타입    | 필수 | 설명                                                               |
| :-------- | :------ | :--- | :----------------------------------------------------------------- |
| `sort_by` | String  | 아니요 (기본값: `created_at`) | `view_count` (조회수순), `average_rating` (평점순), `created_at` (최신순) 중 하나. |
| `tags`    | String  | 아니요                       | 검색할 태그들 (쉼표로 구분, 예: "philosophy,children"). 제공 시 해당 태그가 포함된 책만 조회. |
| `keyword` | String  | 아니요                       | 검색할 책 제목 또는 작가 이름 (부분 일치 검색). 제공 시 키워드가 포함된 책만 조회. |
| `page`    | Integer | 아니요 (기본값: `1`)           | 조회할 페이지 번호.                                                |
| `limit`   | Integer | 아니요 (기본값: `10`, 최댓값: `100`)          | 페이지 당 항목 수.                                                 |

#### **Success Response (200 OK)**
```json
{
  "data": [
    {
      "id": "60d0fe4f5311236168a109ca", // 식별자
      "title": "The Little Prince", // 책 제목
      "author": "Antoine de Saint-Exupéry", // 책 작가
      "coverImageUrl": "https://path/to/cover.jpg", // 표지 이미지 주소
      "difficultyLevel": "A1", // 책 기본 난이도
      "chapterCount": 27, // 책 챕터 수
      "currentReadChapterNumber": 10, // 현재 읽은 챕터 번호 (완료된 챕터를 기준으로 하며, 기본값은 0)
      "progressPercentage": 37.0, // 진행률 (10/27 * 100)
      "readingTime": 120, // 읽기 시간 (분 단위)
      "averageRating": 4.8, // 리뷰 평점
      "reviewCount": 1500, // 리뷰어 수
      "viewCount": 25000, // 조회수
      "tags": ["philosophy", "children"], // 태그 목록
      "createdAt": "2024-01-15T00:00:00" // 생성 날짜
    }
    ...
  ],
  "currentPage": 1, // 현재 페이지
  "totalPages": 10, // 전체 페이지
  "totalCount": 100, // 전체 책 개수
  "hasNext": true, // 다음 페이지 존재 여부
  "hasPrevious": false, // 이전 페이지 존재 여부
}
```

#### **API 사용 예시**

**1. 기본 조회 (최신순 정렬)**
```
GET /api/v1/books
```

**2. 조회수 높은 순으로 정렬**
```
GET /api/v1/books?sort_by=view_count
```

**3. 태그 필터링 (여러 태그)**
```
GET /api/v1/books?tags=philosophy,adventure
```

**4. 키워드 검색 (제목/작가)**
```
GET /api/v1/books?keyword=prince
```

**5. 복합 조건 (태그 + 정렬)**
```
GET /api/v1/books?tags=philosophy&sort_by=average_rating
```

**6. 복합 조건 (키워드 + 태그 + 정렬)**
```
GET /api/v1/books?keyword=prince&tags=children&sort_by=view_count
```

#### **Error Response (400 Bad Request)**
```json
{
  "message": "Invalid sort_by parameter. Must be one of: view_count, average_rating, created_at."
}
```

#### **Error Response (400 Bad Request) - 잘못된 태그 형식**
```json
{
  "message": "Invalid tags format. Tags should be comma-separated strings."
}
```

### `GET /books/{bookId}`

특정 책의 상세 정보를 조회합니다.

#### **Path Parameters**

| 파라미터  | 타입     | 설명             |
| :-------- | :------- | :--------------- |
| `bookId` | String | 조회할 책의 고유 ID |

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109ca",
  "title": "The Little Prince",
  "author": "Antoine de Saint-Exupéry",
  "coverImageUrl": "https://path/to/cover.jpg",
  "difficultyLevel": "A1",
  "chapterCount": 27,
  "currentReadChapterNumber": 10,
  "progressPercentage": 37.0,
  "readingTime": 120,
  "averageRating": 4.8,
  "reviewCount": 1500,
  "viewCount": 25000,
  "tags": ["philosophy", "children"],
  "createdAt": "2024-01-15T00:00:00"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Book not found."
}
```

### `POST /books/import`

S3에 저장된 JSON 파일을 읽어서 새로운 책과 관련 챕터, 청크 데이터를 생성합니다. 이 API는 임시 API 키를 사용하여 인증합니다.

#### **Request Headers**
```
X-API-Key: {TempApiKey}
```

#### **Request Body**

```json
{
  "requestId": "60d0fe4f5311236168a109ca"
}
```
- `requestId`: 처리 요청의 고유 ID (필수)

#### **Success Response (201 Created)**
```json
{
  "id": "60d0fe4f5311236168a109ca" // 생성된 책의 식별자
}
```

#### **예상 JSON 파일 구조**
```json
{
  "novel_id": "uuid-here",
  "title": "소설 제목",
  "author": "작가명",
  "original_text_level": "B2",
  "chapter_metadata": [
    {
      "chapterNum": 1,
      "title": "첫 번째 챕터 제목",
      "summary": "첫 번째 챕터의 내용 요약..."
    },
    {
      "chapterNum": 2,
      "title": "두 번째 챕터 제목",
      "summary": "두 번째 챕터의 내용 요약..."
    }
  ],
  "leveled_results": [
    {
      "textLevel": "A1",
      "chapters": [
        {
          "chapterNum": 1,
          "chunks": [
            {
              "chunkNum": 1,
              "chunkText": "A1 레벨로 변환된 텍스트..."
            }
          ]
        }
      ]
    }
  ]
}
```

---

## 📖 챕터 (Chapters)

### `GET /books/{bookId}/chapters`

특정 책에 포함된 챕터 목록을 페이지네이션으로 조회합니다.

#### **Path Parameters**

| 파라미터  | 타입     | 설명             |
| :-------- | :------- | :--------------- |
| `bookId` | String | 조회할 책의 고유 ID |

#### **Query Parameters**

| 파라미터 | 타입    | 필수 | 설명                      |
| :------- | :------ | :--- | :------------------------ |
| `page`   | Integer | 아니요 (기본값: `1`)          | 조회할 페이지 번호                |
| `limit`  | Integer | 아니요 (기본값: `10`, 최댓값: `100`) | 페이지 당 항목 수                |

#### **Success Response (200 OK)**
```json
{
  "data": [
    {
      "id": "60d0fe4f5311236168a109cb",
      "chapterNumber": 1,
      "title": "The Drawing",
      "chapterImageUrl": "https://path/to/chapter-image.jpg", // 챕터 이미지
      "description": "A brief summary of the first chapter.",
      "chunkCount": 10, // 챕터 내부 전체 청크
      "currentReadChunkNumber": 8, // 현재 읽은 청크 번호
      "progressPercentage": 80.0, // 진행률 (8/10 * 100)
      "readingTime": 15
    },
    {
      "id": "60d0fe4f5311236168a109cc",
      "chapterNumber": 2,
      "title": "The Boa Constrictor",
      "chapterImageUrl": "https://path/to/chapter2-image.jpg", // 챕터 이미지
      "description": "The pilot's encounter with the boa constrictor.",
      "chunkCount": 20,
      "currentReadChunkNumber": 0, // 아직 읽지 않음
      "progressPercentage": 0.0, // 진행률
      "readingTime": 20
    }
  ],
  "currentPage": 1, // 현재 페이지
  "totalPages": 3, // 전체 페이지
  "totalCount": 27, // 전체 챕터 항목 수 (페이지네이션 기준)
  "hasNext": true, // 다음 페이지 존재 여부
  "hasPrevious": false // 이전 페이지 존재 여부
}
```

#### **API 사용 예시**

**1. 기본 조회 (첫 번째 페이지)**
```
GET /api/v1/books/60d0fe4f5311236168a109ca/chapters
```

**2. 특정 페이지 조회**
```
GET /api/v1/books/60d0fe4f5311236168a109ca/chapters?page=2
```

**3. 페이지 크기 조정**
```
GET /api/v1/books/60d0fe4f5311236168a109ca/chapters?page=1&limit=20
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Book not found."
}
```

### `GET /books/{bookId}/chapters/{chapterId}`

특정 챕터의 상세 정보를 조회합니다.

#### **Path Parameters**

| 파라미터     | 타입     | 설명               |
| :----------- | :------- | :----------------- |
| `bookId`    | String | 조회할 책의 고유 ID   |
| `chapterId` | String | 조회할 챕터의 고유 ID |

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109cb",
  "chapterNumber": 1,
  "title": "The Drawing",
  "chapterImageUrl": "https://path/to/chapter-image.jpg",
  "description": "A brief summary of the first chapter.",
  "chunkCount": 10,
  "currentReadChunkNumber": 8,
  "progressPercentage": 80.0,
  "readingTime": 15
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Book not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Chapter not found."
}
```

---

## 📑 청크 (Chunks)

### `GET /books/{bookId}/chapters/{chapterId}/chunks`

특정 책의 특정 챕터에 속한 텍스트 청크(Chunk)들을 난이도별로 조회합니다.

#### **Path Parameters**

| 파라미터     | 타입     | 설명               |
| :----------- | :------- | :----------------- |
| `bookId`    | String | 조회할 책의 고유 ID   |
| `chapterId` | String | 조회할 챕터의 고유 ID |

#### **Query Parameters**

| 파라미터     | 타입    | 필수 | 설명                                                  |
| :----------- | :------ | :--- |:----------------------------------------------------|
| `difficulty` | String  | 예   | `a0`, `a1`, `a2`, `b1`, `b2`, `c1`, `c2` 등 청크의 난이도. |
| `page`       | Integer | 아니요 | 페이지 번호 (기본값: `1`).                                  |
| `limit`      | Integer | 아니요 | 페이지 당 항목 수 (기본값: `10`, 최댓값 `200`).                  |

#### **Success Response (200 OK)**
```json
{
  "data": [
    {
      "id": "60d0fe4f5311236168a109cd",
      "chunkNumber": 1,
      "difficulty": "A1",
      "type": "TEXT", // "TEXT" 또는 "IMAGE"
      "content": "Once when I was six years old I saw a magnificent picture in a book...", // TEXT 타입일 경우 텍스트 내용
      "description": null // 이미지 설명 (IMAGE 타입일 경우)
    },
    {
      "id": "60d0fe4f5311236168a109ce", 
      "chunkNumber": 2,
      "difficulty": "A1",
      "type": "IMAGE", // 이미지 청크
      "content": "https://img.linglevel.com/images/boa-constrictor.jpg", // IMAGE 타입일 경우 이미지 URL
      "description": "A picture of a boa constrictor swallowing an animal" // 이미지 설명
    }
  ],
  "currentPage": 1, // 현재 페이지
  "totalPages": 5, // 전체 페이지
  "totalCount": 100, // 전체 청크 개수
  "hasNext": true, // 다음 페이지 존재 여부
  "hasPrevious": false, // 이전 페이지 존재 여부
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Chapter not found."
}
```

### `GET /books/{bookId}/chapters/{chapterId}/chunks/{chunkId}`

특정 청크의 상세 정보를 조회합니다.

#### **Path Parameters**

| 파라미터     | 타입     | 설명               |
| :----------- | :------- | :----------------- |
| `bookId`    | String | 조회할 책의 고유 ID   |
| `chapterId` | String | 조회할 챕터의 고유 ID |
| `chunkId`   | String | 조회할 청크의 고유 ID |

#### **Success Response (200 OK) - 텍스트 청크**
```json
{
  "id": "60d0fe4f5311236168a109cd",
  "chunkNumber": 1,
  "difficulty": "A1",
  "type": "TEXT",
  "content": "Once when I was six years old I saw a magnificent picture in a book...",
  "description": null
}
```

#### **Success Response (200 OK) - 이미지 청크**
```json
{
  "id": "60d0fe4f5311236168a109ce",
  "chunkNumber": 2,
  "difficulty": "A1", 
  "type": "IMAGE",
  "content": "https://img.linglevel.com/images/boa-constrictor.jpg",
  "description": "A picture of a boa constrictor swallowing an animal"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Book not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Chapter not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Chunk not found."
}
```

---

## 📈 읽기 진도 (Reading Progress)

### `PUT /books/{bookId}/progress`

사용자의 읽기 진도를 업데이트합니다. 특정 챕터의 특정 청크까지 읽었음을 기록합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터  | 타입     | 설명             |
| :-------- | :------- | :--------------- |
| `bookId` | String | 읽고 있는 책의 고유 ID |

#### **Request Body**

```json
{
  "chapterId": "60d0fe4f5311236168a109cb",
  "chunkId": "60d0fe4f5311236168c172db"
}
```

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109d1",
  "bookId": "60d0fe4f5311236168a109cb", 
  "chapterId": "60d0fe4f5311236168a109cb",
  "chunkId": "60d0fe4f5311236168c172db",
  "currentReadChapterNumber": 1,
  "currentReadChunkNumber": 5,
  "updatedAt": "2024-01-15T10:30:00"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Book not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Chapter not found in this book."
}
```

#### **Error Response (400 Bad Request)**
```json
{
  "message": "Invalid chunkId. Must be a valid chunk identifier."
}
```

### `GET /books/{bookId}/progress`

사용자의 특정 책에 대한 읽기 진도를 조회합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터  | 타입     | 설명             |
| :-------- | :------- | :--------------- |
| `bookId` | String | 조회할 책의 고유 ID |

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109d1",
  "bookId": "60d0fe4f5311236168a109cb",
  "chapterId": "60d0fe4f5311236168a109cb",
  "chunkId": "60d0fe4f53112389248a182db",
  "currentReadChapterNumber": 1,
  "currentReadChunkNumber": 5,
  "updatedAt": "2024-01-15T10:30:00"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Book not found."
}
```

### `GET /books/progress`

현재 사용자의 모든 책에 대한 읽기 진도를 조회합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Query Parameters**

| 파라미터 | 타입    | 필수 | 설명                      |
| :------- | :------ | :--- | :------------------------ |
| `page`   | Integer | 아니요 (기본값: `1`) | 조회할 페이지 번호 |
| `limit`  | Integer | 아니요 (기본값: `10`, 최댓값: `100`) | 페이지 당 항목 수 |

#### **Success Response (200 OK)**
```json
{
  "data": [
    {
      "id": "60d0fe4f5311236168a109d1",
      "book": {
        "id": "60d0fe4f5311236168a109cb",
        "title": "The Little Prince",
        "author": "Antoine de Saint-Exupéry",
        "coverImageUrl": "https://path/to/cover.jpg",
        "totalChapters": 27
      },
      "chapterId": "60d0fe4f5311236168a109cb",
      "chunkId": "60d0fe4f53112389248a182db",
      "currentReadChapterNumber": 1,
      "progressPercentage": 15.5,
      "updatedAt": "2024-01-15T10:30:00"
    }
  ],
  "currentPage": 1,
  "totalPages": 3,
  "totalCount": 25,
  "hasNext": true,
  "hasPrevious": false 
}
```

#### **API 사용 예시**

**1. 기본 조회 (첫 번째 페이지)**
```
GET /api/v1/books/progress
```

**2. 특정 페이지 조회**
```
GET /api/v1/books/progress?page=2
```

**3. 페이지 크기 조정**
```
GET /api/v1/books/progress?page=1&limit=20
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid or expired token."
}
```

---

## 👤 사용자 관리 (User Management)

### `DELETE /users/me`

현재 인증된 사용자의 계정을 삭제합니다. JWT 토큰을 통해 사용자를 식별하며, 관련된 모든 사용자 데이터가 삭제됩니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Success Response (200 OK)**
```json
{
  "message": "User account deleted successfully."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid or expired token."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "User not found."
}
```

---

## 📚 단어장 (Words & Bookmarks)

### `GET /words`

전체 단어 목록을 페이지네이션으로 조회합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Query Parameters**

| 파라미터 | 타입    | 필수 | 설명                      |
| :------- | :------ | :--- | :------------------------ |
| `page`   | Integer | 아니요 (기본값: `1`)          | 조회할 페이지 번호                |
| `limit`  | Integer | 아니요 (기본값: `10`, 최댓값: `100`) | 페이지 당 항목 수                |
| `search` | String  | 아니요                       | 검색할 단어 (부분 일치 검색)         |

#### **Success Response (200 OK)**
```json
{
  "data": [
    {
      "id": "60d0fe4f5311236168a109ca",
      "word": "magnificent"
    },
    {
      "id": "60d0fe4f5311236168a109cb", 
      "word": "picture"
    }
  ],
  "currentPage": 1,
  "totalPages": 10,
  "totalCount": 100,
  "hasNext": true,
  "hasPrevious": false
}
```

#### **API 사용 예시**

**1. 기본 조회**
```
GET /api/v1/words
```

**2. 단어 검색**
```
GET /api/v1/words?search=magn
```

**3. 페이지네이션**
```
GET /api/v1/words?page=2&limit=20
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid or expired token."
}
```

### `GET /words/{word}`

특정 단어의 상세 정보를 조회합니다. 현재 사용자의 북마크 상태도 함께 반환됩니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터   | 타입     | 설명      |
|:-------| :------- | :-------- |
| `word` | String | 조회할 단어 |

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109ca",
  "word": "magnificent",
  "bookmarked": true
}
```

- `bookmarked`: 현재 사용자가 해당 단어를 북마크했는지 여부

#### **Error Response (404 Not Found)**
```json
{
  "message": "Word not found."
}
```


---

## 📖 북마크 (Bookmarks)

### `GET /bookmarks/words`

현재 사용자가 북마크한 단어 목록을 조회합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Query Parameters**

| 파라미터 | 타입    | 필수 | 설명                      |
| :------- | :------ | :--- | :------------------------ |
| `page`   | Integer | 아니요 (기본값: `1`)          | 조회할 페이지 번호                |
| `limit`  | Integer | 아니요 (기본값: `10`, 최댓값: `100`) | 페이지 당 항목 수                |
| `search` | String  | 아니요                       | 검색할 단어 (부분 일치 검색)         |

#### **Success Response (200 OK)**
```json
{
  "data": [
    {
      "id": "60d0fe4f5311236168a109ca",
      "word": "magnificent",
      "bookmarkedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "60d0fe4f5311236168a109cb",
      "word": "adventure", 
      "bookmarkedAt": "2024-01-14T15:45:00"
    }
  ],
  "currentPage": 1,
  "totalPages": 5,
  "totalCount": 45,
  "hasNext": true,
  "hasPrevious": false
}
```

#### **API 사용 예시**

**1. 기본 조회**
```
GET /api/v1/bookmarks/words
```

**2. 북마크된 단어 검색**
```
GET /api/v1/bookmarks/words?search=magn
```

**3. 페이지네이션**
```
GET /api/v1/bookmarks/words?page=2&limit=20
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid or expired token."
}
```

### `POST /bookmarks/words/{word}`

특정 단어를 북마크에 추가합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터   | 타입     | 설명      |
|:-------| :------- |:--------|
| `word` | String | 북마크할 단어 |

#### **Success Response (201 Created)**
```json
{
  "message": "Word bookmarked successfully."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Word not found."
}
```

#### **Error Response (409 Conflict)**
```json
{
  "message": "Word is already bookmarked."
}
```

### `DELETE /bookmarks/words/{word}`

특정 단어를 북마크에서 제거합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터   | 타입     | 설명         |
|:-------| :------- |:-----------|
| `word` | String | 북마크 해제할 단어 |

#### **Success Response (200 OK)**
```json
{
  "message": "Word bookmark removed successfully."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Word not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Word bookmark not found."
}
```

### `PUT /bookmarks/words/{word}/toggle`

특정 단어의 북마크 상태를 토글합니다. 북마크되어 있으면 제거하고, 북마크되어 있지 않으면 추가합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터   | 타입     | 설명       |
|:-------| :------- |:---------|
| `word` | String | 토글할 단어 |

#### **Success Response (200 OK)**
```json
{
  "bookmarked": true
}
```

- `bookmarked`: 토글 후의 북마크 상태 (true: 북마크됨, false: 북마크 해제됨)
- `message`: 수행된 작업에 대한 메시지

#### **Success Response (200 OK) - 북마크 해제 시**
```json
{
  "bookmarked": false
}
```

---

## 📰 기사 (Articles)

### `GET /articles`

기사 목록을 조건에 따라 조회합니다. 기본적으로 최신순으로 정렬되며, 선택적으로 태그나 키워드 필터를 적용할 수 있습니다.

#### **Query Parameters**

| 파라미터  | 타입    | 필수 | 설명                                                                    |
| :-------- | :------ | :--- |:----------------------------------------------------------------------|
| `sort_by` | String  | 아니요 (기본값: `created_at`) | `view_count` (조회수순), `average_rating` (평점순), `created_at` (최신순) 중 하나. |
| `tags`    | String  | 아니요                       | 검색할 태그들 (쉼표로 구분, 예: "technology,business"). 제공 시 해당 태그가 포함된 기사만 조회.   |
| `keyword` | String  | 아니요                       | 검색할 기사 제목 또는 작가 이름 (부분 일치 검색). 제공 시 키워드가 포함된 기사만 조회.                  |
| `page`    | Integer | 아니요 (기본값: `1`)           | 조회할 페이지 번호.                                                           |
| `limit`   | Integer | 아니요 (기본값: `10`, 최댓값: `100`)          | 페이지 당 항목 수.                                                           |

#### **Success Response (200 OK)**
```json
{
  "data": [
    {
      "id": "60d0fe4f5311236168a109ca",
      "title": "Viking King's Bizarre Legacy: The Shocking Truth Behind Your Phone's Most Mysterious Feature!",
      "author": "",
      "coverImageUrl": "https://path/to/cover.jpg",
      "difficultyLevel": "C1",
      "chunkCount": 15,
      "readingTime": 8,
      "averageRating": 4.5,
      "reviewCount": 230,
      "viewCount": 15000,
      "tags": ["technology", "history"],
      "createdAt": "2024-01-15T00:00:00"
    }
  ],
  "currentPage": 1,
  "totalPages": 5,
  "totalCount": 50,
  "hasNext": true,
  "hasPrevious": false
}
```

#### **API 사용 예시**

**1. 기본 조회 (최신순 정렬)**
```
GET /api/v1/articles
```

**2. 조회수 높은 순으로 정렬**
```
GET /api/v1/articles?sort_by=view_count
```

**3. 태그 필터링 (여러 태그)**
```
GET /api/v1/articles?tags=technology,business
```

**4. 키워드 검색 (제목/작가)**
```
GET /api/v1/articles?keyword=viking
```

#### **Error Response (400 Bad Request)**
```json
{
  "message": "Invalid sort_by parameter. Must be one of: view_count, average_rating, created_at."
}
```

### `GET /articles/{articleId}`

특정 기사의 상세 정보를 조회합니다.

#### **Path Parameters**

| 파라미터        | 타입     | 설명            |
|:------------| :------- |:--------------|
| `articleId` | String | 조회할 기사의 고유 ID |

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109ca",
  "title": "Viking King's Bizarre Legacy: The Shocking Truth Behind Your Phone's Most Mysterious Feature!",
  "author": "",
  "coverImageUrl": "https://path/to/cover.jpg",
  "difficultyLevel": "C1",
  "chunkCount": 15,
  "readingTime": 8,
  "averageRating": 4.5,
  "reviewCount": 230,
  "viewCount": 15000,
  "tags": ["technology", "history"],
  "createdAt": "2024-01-15T00:00:00"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Article not found."
}
```

### `POST /articles/import`

S3에 저장된 JSON 파일을 읽어서 새로운 기사와 관련 청크 데이터를 생성합니다. 이 API는 임시 API 키를 사용하여 인증합니다.

#### **Request Headers**
```
X-API-Key: {TempApiKey}
```

#### **Request Body**

```json
{
  "id": "string"
}
```
- `id`: S3에 저장된 JSON 파일의 식별자 (예: "86781f8a-cb42-4fa1-865e-0e8e20d903d8" → "86781f8a-cb42-4fa1-865e-0e8e20d903d8.json" 파일을 조회)

#### **Success Response (201 Created)**
```json
{
  "id": "60d0fe4f5311236168a109ca"
}
```

#### **예상 JSON 파일 구조**
```json
{
  "id": "86781f8a-cb42-4fa1-865e-0e8e20d903d8",
  "content_type": "article",
  "title": "Viking King's Bizarre Legacy: The Shocking Truth Behind Your Phone's Most Mysterious Feature!",
  "author": "",
  "cover_image_url": "s3://leveling-dev-output-bucket/article/86781f8a-cb42-4fa1-865e-0e8e20d903d8/images/cover.jpg",
  "original_text_level": "C1",
  "leveled_results": [
    {
      "textLevel": "A0",
      "chapters": [
        {
          "chapterNum": 1,
          "chunks": [
            {
              "chunkNum": 1,
              "isImage": false,
              "chunkText": "You have a phone. The phone has a sign...",
              "description": null
            }
          ]
        }
      ]
    }
  ]
}
```

---

## 📑 기사 청크 (Articles Chunks)

### `GET /articles/{articleId}/chunks`

특정 기사에 속한 텍스트 청크(Chunk)들을 난이도별로 조회합니다.

#### **Path Parameters**

| 파라미터        | 타입     | 설명            |
|:------------| :------- |:--------------|
| `articleId` | String | 조회할 기사의 고유 ID |

#### **Query Parameters**

| 파라미터     | 타입    | 필수 | 설명                                   |
| :----------- | :------ | :--- | :------------------------------------- |
| `difficulty` | String  | 예   | `a0`, `a1`, `a2`, `b1`, `b2`, `c1`, `c2` 등 청크의 난이도. |
| `page`       | Integer | 아니요 | 페이지 번호 (기본값: `1`).                 |
| `limit`      | Integer | 아니요 | 페이지 당 항목 수 (기본값: `10`, 최댓값 `100`).          |

#### **Success Response (200 OK)**
```json
{
  "data": [
    {
      "id": "60d0fe4f5311236168a109cd",
      "chunkNumber": 1,
      "difficulty": "A1",
      "type": "TEXT",
      "content": "You have a phone. The phone has a sign. The sign is called Bluetooth...",
      "description": null
    },
    {
      "id": "60d0fe4f5311236168a109ce", 
      "chunkNumber": 2,
      "difficulty": "A1",
      "type": "IMAGE",
      "content": "https://static.linglevel.com/article/86781f8a-cb42-4fa1-865e-0e8e20d903d8/images/bluetooth-logo.jpg",
      "description": "Bluetooth logo symbol"
    }
  ],
  "currentPage": 1,
  "totalPages": 3,
  "totalCount": 25,
  "hasNext": true,
  "hasPrevious": false
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Article not found."
}
```

### `GET /articles/{articleId}/chunks/{chunkId}`

특정 기사 청크의 상세 정보를 조회합니다.

#### **Path Parameters**

| 파라미터        | 타입     | 설명            |
|:------------| :------- |:--------------|
| `articleId` | String | 조회할 기사의 고유 ID |
| `chunkId`   | String | 조회할 청크의 고유 ID |

#### **Success Response (200 OK) - 텍스트 청크**
```json
{
  "id": "60d0fe4f5311236168a109cd",
  "chunkNumber": 1,
  "difficulty": "A1",
  "type": "TEXT",
  "content": "You have a phone. The phone has a sign. The sign is called Bluetooth...",
  "description": null
}
```

#### **Success Response (200 OK) - 이미지 청크**
```json
{
  "id": "60d0fe4f5311236168a109ce",
  "chunkNumber": 2,
  "difficulty": "A1", 
  "type": "IMAGE",
  "content": "https://static.linglevel.com/article/86781f8a-cb42-4fa1-865e-0e8e20d903d8/images/bluetooth-logo.jpg",
  "description": "Bluetooth logo symbol"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Article not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Chunk not found."
}
```

---

## 🔧 어드민 관리 (Admin Management)

### `PUT /admin/books/{bookId}/chapters/{chapterId}/chunks/{chunkId}`

어드민 권한으로 특정 책의 청크 내용을 수정합니다. 이 API는 임시 API 키를 사용하여 인증합니다.

#### **Request Headers**
```
X-API-Key: {TempApiKey}
```

#### **Path Parameters**

| 파라미터     | 타입     | 설명               |
| :----------- | :------- | :----------------- |
| `bookId`    | String | 수정할 책의 고유 ID   |
| `chapterId` | String | 수정할 챕터의 고유 ID |
| `chunkId`   | String | 수정할 청크의 고유 ID |

#### **Request Body**

```json
{
  "content": "Updated chunk content...",
  "description": "Updated description for image chunks"
}
```
- `content`: 수정할 청크 내용 (텍스트 청크의 경우 텍스트, 이미지 청크의 경우 이미지 URL)
- `description`: 이미지 청크의 설명 (선택사항, 이미지 청크인 경우에만 사용)

#### **API 사용 예시**

**특정 책의 청크 수정**
```
PUT /api/v1/admin/books/60d0fe4f5311236168a109ca/chapters/60d0fe4f5311236168a109cb/chunks/60d0fe4f5311236168a109cd
```

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109cd",
  "chunkNumber": 1,
  "difficulty": "A1",
  "type": "TEXT",
  "content": "Updated chunk content...",
  "description": null
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Book not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Chapter not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Chunk not found."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid API key."
}
```

### `DELETE /admin/books/{bookId}`

어드민 권한으로 특정 책과 관련된 모든 데이터(챕터, 청크, 진도 등)를 삭제합니다. 이 API는 임시 API 키를 사용하여 인증합니다.

#### **Request Headers**
```
X-API-Key: {TempApiKey}
```

#### **Path Parameters**

| 파라미터  | 타입     | 설명             |
| :-------- | :------- | :--------------- |
| `bookId` | String | 삭제할 책의 고유 ID |

#### **Success Response (200 OK)**
```json
{
  "message": "Book and all related data deleted successfully."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Book not found."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid API key."
}
```

### `PUT /admin/articles/{articleId}/chunks/{chunkId}`

어드민 권한으로 특정 기사의 청크 내용을 수정합니다. 이 API는 임시 API 키를 사용하여 인증합니다.

#### **Request Headers**
```
X-API-Key: {TempApiKey}
```

#### **Path Parameters**

| 파라미터        | 타입     | 설명            |
|:------------| :------- |:--------------|
| `articleId` | String | 수정할 기사의 고유 ID |
| `chunkId`   | String | 수정할 청크의 고유 ID |

#### **Request Body**

```json
{
  "content": "Updated article chunk content...",
  "description": "Updated description for image chunks"
}
```
- `content`: 수정할 청크 내용 (텍스트 청크의 경우 텍스트, 이미지 청크의 경우 이미지 URL)
- `description`: 이미지 청크의 설명 (선택사항, 이미지 청크인 경우에만 사용)

#### **API 사용 예시**

**특정 기사의 청크 수정**
```
PUT /api/v1/admin/articles/60d0fe4f5311236168a109ca/chunks/60d0fe4f5311236168a109cd
```

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109cd",
  "chunkNumber": 1,
  "difficulty": "A1",
  "type": "TEXT",
  "content": "Updated article chunk content...",
  "description": null
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Article not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Chunk not found."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid API key."
}
```

### `DELETE /admin/articles/{articleId}`

어드민 권한으로 특정 기사와 관련된 모든 데이터(청크 등)를 삭제합니다. 이 API는 임시 API 키를 사용하여 인증합니다.

#### **Request Headers**
```
X-API-Key: {TempApiKey}
```

#### **Path Parameters**

| 파라미터        | 타입     | 설명            |
|:------------| :------- |:--------------|
| `articleId` | String | 삭제할 기사의 고유 ID |

#### **Success Response (200 OK)**
```json
{
  "message": "Article and all related data deleted successfully."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Article not found."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid API key."
}
```

---

## 📱 앱 버전 관리 (App Version Management)

### `GET /version`

클라이언트 앱에서 버전 확인을 위한 API입니다. 현재 최신 버전과 최소 요구 버전을 반환합니다.

#### **Success Response (200 OK)**
```json
{
  "latestVersion": "1.2.3",
  "minimumVersion": "1.1.0"
}
```

- `latestVersion`: 스토어에 배포된 최신 앱 버전
- `minimumVersion`: 앱 사용을 위한 최소 요구 버전

#### **API 사용 예시**

**버전 정보 조회**
```
GET /api/v1/version
```

클라이언트는 이 정보를 사용하여:
- 최신 버전과 비교하여 업데이트 안내 표시
- 최소 버전 미달 시 강제 업데이트 요구

---

## 🔧 어드민 - 앱 버전 관리 (Admin App Version Management)

### `PATCH /admin/version`

어드민 권한으로 앱의 최신 버전 및 최소 요구 버전을 부분 업데이트합니다. 이 API는 임시 API 키를 사용하여 인증합니다.

#### **Request Headers**
```
X-API-Key: {TempApiKey}
```

#### **Request Body**

```json
{
  "latestVersion": "1.2.3"
}
```

- `latestVersion`: 업데이트할 최신 버전 (선택사항)
- `minimumVersion`: 업데이트할 최소 요구 버전 (선택사항)

#### **Success Response (200 OK)**
```json
{
  "latestVersion": "1.2.3",
  "minimumVersion": "1.1.0",
  "updatedAt": "2024-01-15T10:30:00"
}
```

#### **API 사용 예시**

**최신 버전만 업데이트**
```
PATCH /api/v1/admin/version
{
  "latestVersion": "1.2.4"
}
```

#### **Error Response (400 Bad Request)**
```json
{
  "message": "At least one version field (latestVersion or minimumVersion) must be provided."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid API key."
}
```

---

## 🎨 커스텀 콘텐츠 (Custom Contents)

### `POST /custom-contents/requests`

사용자가 텍스트를 입력하여 AI 콘텐츠 처리 요청을 생성합니다. 클립보드 텍스트나 웹 링크 크롤링을 통한 콘텐츠 처리를 요청할 수 있습니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Request Body**

```json
{
  "title": "My Custom Article",
  "contentType": "TEXT",
  "originalContent": "Once upon a time, there was a little prince who lived on a small planet...",
  "targetDifficultyLevels": ["A1", "B1"],
  "originUrl": null,
  "originAuthor": null
}
```

- `title`: 콘텐츠 제목 (필수)
- `contentType`: 콘텐츠 타입 (필수) - `TEXT` 또는 `LINK`
- `originalContent`: 처리할 원본 텍스트 (TEXT 타입인 경우 필수)
- `targetDifficultyLevels`: 목표 난이도 배열 - `["A1", "B1"]` 형태, 각 항목은 `A0`, `A1`, `A2`, `B1`, `B2`, `C1`, `C2` 중 하나 (선택사항)
- `originUrl`: 원본 링크 URL (링크 타입인 경우)
- `originAuthor`: 원본 저자 (선택사항)

#### **Success Response (201 Created)**
```json
{
  "requestId": "60d0fe4f5311236168a109ca",
  "title": "My Custom Article",
  "status": "PENDING",
  "createdAt": "2024-01-15T10:00:00"
}
```

#### **Error Response (400 Bad Request)**
```json
{
  "message": "Title and contentType are required."
}
```

### `GET /custom-contents/requests`

사용자의 콘텐츠 처리 요청 목록을 조회합니다. 진행 중이거나 완료된 요청들을 상태별로 확인할 수 있습니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Query Parameters**

| 파라미터  | 타입    | 필수 | 설명                                                 |
| :-------- | :------ | :--- | :--------------------------------------------------- |
| `status`  | String  | 아니요 | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` 상태별 필터링 |
| `page`    | Integer | 아니요 (기본값: `1`) | 조회할 페이지 번호 |
| `limit`   | Integer | 아니요 (기본값: `10`, 최댓값: `100`) | 페이지 당 항목 수 |

#### **Success Response (200 OK)**
```json
{
  "data": [
    {
      "id": "60d0fe4f5311236168a109ca",
      "title": "My Custom Article",
      "contentType": "TEXT",
      "targetDifficultyLevels": ["A1", "B1"],
      "originUrl": null,
      "originDomain": null,
      "originAuthor": null,
      "status": "PROCESSING",
      "progress": 45,
      "createdAt": "2024-01-15T10:00:00",
      "completedAt": null,
      "errorMessage": null,
      "resultCustomContentId": null
    },
    {
      "id": "60d0fe4f5311236168a109cb",
      "title": "Tech News Article",
      "contentType": "TEXT",
      "targetDifficultyLevels": ["B1"],
      "originUrl": "https://techcrunch.com/example-article",
      "originDomain": "techcrunch.com",
      "originAuthor": "TechCrunch",
      "status": "COMPLETED",
      "progress": 100,
      "createdAt": "2024-01-14T15:30:00",
      "completedAt": "2024-01-14T15:35:00",
      "errorMessage": null,
      "resultCustomContentId": "60d0fe4f5311236168a109cc"
    }
  ],
  "currentPage": 1,
  "totalPages": 3,
  "totalCount": 25,
  "hasNext": true,
  "hasPrevious": false
}
```

#### **API 사용 예시**

**1. 기본 조회 (모든 요청)**
```
GET /api/v1/custom-contents/requests
```

**2. 완료된 요청만 조회**
```
GET /api/v1/custom-contents/requests?status=COMPLETED
```

**3. 진행 중인 요청만 조회**
```
GET /api/v1/custom-contents/requests?status=PROCESSING
```

### `GET /custom-contents/requests/{requestId}`

특정 콘텐츠 처리 요청의 상세 정보를 조회합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터    | 타입   | 설명                   |
| :---------- | :----- | :--------------------- |
| `requestId` | String | 조회할 요청의 고유 ID |

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109ca",
  "title": "My Custom Article",
  "contentType": "TEXT",
  "targetDifficultyLevels": ["A1", "B1"],
  "originUrl": null,
  "originDomain": null,
  "originAuthor": null,
  "status": "COMPLETED",
  "progress": 100,
  "createdAt": "2024-01-15T10:00:00",
  "completedAt": "2024-01-15T10:05:00",
  "errorMessage": null,
  "resultCustomContentId": "60d0fe4f5311236168a109cc"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Content request not found."
}
```

### `GET /custom-contents`

완료된 커스텀 콘텐츠 목록을 조회합니다. 기본적으로 최신순으로 정렬되며, 선택적으로 태그나 키워드 필터를 적용할 수 있습니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Query Parameters**

| 파라미터  | 타입    | 필수 | 설명                                                               |
| :-------- | :------ | :--- | :----------------------------------------------------------------- |
| `sort_by` | String  | 아니요 (기본값: `created_at`) | `view_count` (조회수순), `average_rating` (평점순), `created_at` (최신순) 중 하나 |
| `tags`    | String  | 아니요                       | 검색할 태그들 (쉼표로 구분, 예: \"technology,beginner\"). 제공 시 해당 태그가 포함된 콘텐츠만 조회 |
| `keyword` | String  | 아니요                       | 검색할 콘텐츠 제목 또는 작가 이름 (부분 일치 검색) |
| `page`    | Integer | 아니요 (기본값: `1`)           | 조회할 페이지 번호 |
| `limit`   | Integer | 아니요 (기본값: `10`, 최댓값: `100`)          | 페이지 당 항목 수 |

#### **Success Response (200 OK)**
```json
{
  "data": [
    {
      "id": "60d0fe4f5311236168a109ca",
      "title": "My Custom Article",
      "author": "AI Generated",
      "coverImageUrl": "https://path/to/cover.jpg",
      "difficultyLevel": "A1",
      "targetDifficultyLevels": ["A1", "B1"],
      "chunkCount": 12,
      "readingTime": 8,
      "averageRating": 4.2,
      "reviewCount": 15,
      "viewCount": 150,
      "tags": ["technology", "beginner"],
      "originUrl": null,
      "originDomain": null,
      "createdAt": "2024-01-15T10:05:00",
      "updatedAt": "2024-01-15T10:05:00"
    }
  ],
  "currentPage": 1,
  "totalPages": 5,
  "totalCount": 45,
  "hasNext": true,
  "hasPrevious": false
}
```

#### **API 사용 예시**

**1. 기본 조회 (최신순 정렬)**
```
GET /api/v1/custom-contents
```

**2. 조회수 높은 순으로 정렬**
```
GET /api/v1/custom-contents?sort_by=view_count
```

**3. 태그 필터링**
```
GET /api/v1/custom-contents?tags=technology,beginner
```

### `GET /custom-contents/{customContentId}`

특정 커스텀 콘텐츠의 상세 정보를 조회합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터            | 타입   | 설명                       |
| :------------------ | :----- | :------------------------- |
| `customContentId` | String | 조회할 커스텀 콘텐츠의 고유 ID |

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109ca",
  "title": "My Custom Article",
  "author": "AI Generated",
  "coverImageUrl": "https://path/to/cover.jpg",
  "difficultyLevel": "A1",
  "targetDifficultyLevels": ["A1", "B1"],
  "chunkCount": 12,
  "readingTime": 8,
  "averageRating": 4.2,
  "reviewCount": 15,
  "viewCount": 150,
  "tags": ["technology", "beginner"],
  "originUrl": null,
  "originDomain": null,
  "createdAt": "2024-01-15T10:05:00",
  "updatedAt": "2024-01-15T10:05:00"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Custom content not found."
}
```

### `PATCH /custom-contents/{customContentId}`

커스텀 콘텐츠의 제목이나 태그를 수정합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터 | 타입 | 설명 |
|:---------|:-----|:-----|
| `customContentId` | String | 수정할 커스텀 콘텐츠의 고유 ID |

#### **Request Body**
```json
{
  "title": "Updated Custom Article Title",
  "tags": ["technology", "updated", "beginner"]
}
```
- `title`: 수정할 제목 (선택사항)
- `tags`: 수정할 태그 배열 (선택사항)

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109ca",
  "title": "Updated Custom Article Title",
  "author": "AI Generated",
  "coverImageUrl": "https://path/to/cover.jpg",
  "difficultyLevel": "A1",
  "targetDifficultyLevels": ["A1", "B1"],
  "chunkCount": 12,
  "readingTime": 8,
  "averageRating": 4.2,
  "reviewCount": 15,
  "viewCount": 150,
  "tags": ["technology", "updated", "beginner"],
  "originUrl": null,
  "originDomain": null,
  "createdAt": "2024-01-15T10:05:00",
  "updatedAt": "2024-01-15T11:30:00"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Custom content not found."
}
```

#### **Error Response (403 Forbidden)**
```json
{
  "message": "You can only modify your own custom content."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid or expired token."
}
```

#### **Error Response (400 Bad Request)**
```json
{
  "message": "At least one field (title or tags) must be provided."
}
```

### `GET /custom-contents/{customContentId}/chunks`

특정 커스텀 콘텐츠에 속한 텍스트 청크(Chunk)들을 난이도별로 조회합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터            | 타입   | 설명                       |
| :------------------ | :----- | :------------------------- |
| `customContentId` | String | 조회할 커스텀 콘텐츠의 고유 ID |

#### **Query Parameters**

| 파라미터     | 타입    | 필수 | 설명                                                  |
| :----------- | :------ | :--- |:-------------------------------------------------------|
| `difficulty` | String  | 예   | `a0`, `a1`, `a2`, `b1`, `b2`, `c1`, `c2` 등 청크의 난이도 |
| `page`       | Integer | 아니요 | 페이지 번호 (기본값: `1`) |
| `limit`      | Integer | 아니요 | 페이지 당 항목 수 (기본값: `10`, 최댓값 `100`) |

#### **Success Response (200 OK)**
```json
{
  "data": [
    {
      "id": "60d0fe4f5311236168a109cd",
      "chunkNumber": 1,
      "difficulty": "A1",
      "type": "TEXT",
      "content": "Once upon a time, there was a little prince. He lived on a small planet...",
      "description": null
    },
    {
      "id": "60d0fe4f5311236168a109ce", 
      "chunkNumber": 2,
      "difficulty": "A1",
      "type": "IMAGE",
      "content": "https://static.linglevel.com/custom/60d0fe4f5311236168a109ca/images/prince-planet.jpg",
      "description": "The little prince on his planet"
    }
  ],
  "currentPage": 1,
  "totalPages": 3,
  "totalCount": 25,
  "hasNext": true,
  "hasPrevious": false
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Custom content not found."
}
```

### `GET /custom-contents/{customContentId}/chunks/{chunkId}`

특정 커스텀 콘텐츠 청크의 상세 정보를 조회합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터            | 타입   | 설명                       |
| :------------------ | :----- | :------------------------- |
| `customContentId` | String | 조회할 커스텀 콘텐츠의 고유 ID |
| `chunkId`         | String | 조회할 청크의 고유 ID |

#### **Success Response (200 OK) - 텍스트 청크**
```json
{
  "id": "60d0fe4f5311236168a109cd",
  "chunkNumber": 1,
  "difficulty": "A1",
  "type": "TEXT",
  "content": "Once upon a time, there was a little prince. He lived on a small planet...",
  "description": null
}
```

#### **Success Response (200 OK) - 이미지 청크**
```json
{
  "id": "60d0fe4f5311236168a109ce",
  "chunkNumber": 2,
  "difficulty": "A1", 
  "type": "IMAGE",
  "content": "https://static.linglevel.com/custom/60d0fe4f5311236168a109ca/images/prince-planet.jpg",
  "description": "The little prince on his planet"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Custom content not found."
}
```

### `DELETE /custom-contents/{customContentId}`

사용자가 본인이 생성한 커스텀 콘텐츠를 삭제합니다. 콘텐츠와 관련된 모든 청크 데이터도 함께 삭제됩니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터            | 타입   | 설명                       |
| :------------------ | :----- | :------------------------- |
| `customContentId` | String | 삭제할 커스텀 콘텐츠의 고유 ID |

#### **Success Response (200 OK)**
```json
{
  "message": "Custom content deleted successfully."
}
```

**동작:**
- 커스텀 콘텐츠와 관련된 모든 청크 데이터를 soft delete 처리합니다 (isDeleted: true)
- 연관된 콘텐츠 요청(contentRequest)의 상태를 DELETED로 변경합니다
- contentRequestId를 통해 사용자 소유권을 확인합니다
- 실제 데이터는 보존되며, 조회 API에서는 제외됩니다

#### **Error Response (404 Not Found)**
```json
{
  "message": "Custom content not found."
}
```

#### **Error Response (403 Forbidden)**
```json
{
  "message": "You can only delete your own custom content."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid or expired token."
}
```

---

## 🤖 웹훅 (Webhooks) - AI 처리 결과

### `POST /webhooks/custom-contents/completed`

AI가 콘텐츠 처리를 완료했을 때 결과 JSON 파일의 위치를 전달하여 백엔드에서 처리하도록 하는 웹훅 API입니다.

#### **Request Headers**
```
X-API-Key: {TempApiKey}
```

#### **Request Body**

```json
{
  "requestId": "60d0fe4f5311236168a109ca"
}
```

- `requestId`: 처리 요청의 고유 ID (필수)

#### **Success Response (200 OK)**
```json
{
  "requestId": "60d0fe4f5311236168a109ca",
  "status": "completed"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Content request not found."
}
```

#### **Error Response (400 Bad Request)**
```json
{
  "message": "Request is not in PROCESSING status."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid API key."
}
```

### `POST /webhooks/custom-contents/failed`

AI 콘텐츠 처리가 실패했을 때 요청 상태를 업데이트하고 사용자에게 실패 알림을 발송하는 웹훅 API입니다.

#### **Request Headers**
```
X-API-Key: {TempApiKey}
```

#### **Request Body**

```json
{
  "requestId": "60d0fe4f5311236168a109ca",
  "errorMessage": "Content processing failed due to unsupported format"
}
```

- `requestId`: 처리 요청의 고유 ID (필수)
- `errorMessage`: 사용자에게 표시할 에러 메시지 (필수)

#### **Success Response (200 OK)**
```json
{
  "message": "Content request marked as failed successfully"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Content request not found."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid API key."
}
```

### `POST /webhooks/custom-contents/progress`

AI 콘텐츠 처리 중 진행률을 업데이트하는 웹훅 API입니다.

#### **Request Headers**
```
X-API-Key: {TempApiKey}
```

#### **Request Body**

```json
{
  "requestId": "60d0fe4f5311236168a109ca",
  "progress": 75,
  "status": "PROCESSING"
}
```

- `requestId`: 처리 요청의 고유 ID (필수)
- `progress`: 진행률 0-100 (필수)
- `status`: 현재 상태 - `PROCESSING` (기본값)

#### **Success Response (200 OK)**
```json
{
  "message": "Progress updated successfully"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "message": "Content request not found."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid API key."
}
```

---

## 💡 고객 건의 (Suggestions)

### `POST /suggestions`

고객의 건의사항을 제출받습니다.

#### **Request Body**

```json
{
  "email": "user@example.com",
  "tags": "bug, ui, feature",
  "content": "이런이런 기능이 추가되었으면 좋겠습니다."
}
```
- `email`: 건의자를 식별하기 위한 이메일 주소
- `tags`: 건의 내용의 분류를 위한 태그 (쉼표로 구분)
- `content`: 건의 내용 본문

#### **Success Response (201 Created)**
```json
{
  "message": "Suggestion submitted successfully."
}
```

#### **Error Response (400 Bad Request)**
```json
{
  "message": "Email and content are required."
}
```

---

## 🔔 FCM 토큰 관리 (FCM Token Management)

### `PUT /fcm/token`

사용자의 FCM 토큰을 등록하거나 업데이트합니다. 동일한 사용자+디바이스 조합이 이미 존재하는 경우 토큰을 업데이트하고, 존재하지 않는 경우 새로 생성합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Request Body**

```json
{
  "fcmToken": "string",
  "deviceId": "string",
  "platform": "string"
}
```
- `fcmToken`: Firebase Cloud Messaging 토큰
- `deviceId`: 디바이스 고유 식별자
- `platform`: 플랫폼 종류 ("ANDROID", "IOS", "WEB")

#### **Success Response (200 OK)**
```json
{
  "message": "FCM token updated successfully.",
  "tokenId": "string"
}
```

#### **Success Response (201 Created)**
```json
{
  "message": "FCM token created successfully.",
  "tokenId": "string"
}
```

#### **Error Response (400 Bad Request)**
```json
{
  "message": "fcmToken, deviceId, and platform are required."
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "message": "Invalid or expired token."
}
```