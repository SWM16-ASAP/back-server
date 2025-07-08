# Ling Level API 명세서 v1.0

'Ling Level' 서비스의 프론트엔드와 백엔드 개발을 위한 API 명세서입니다.

---

## 📝 기본 정보

-   **Base URL**: `/api/v1`
-   **인증**: `/auth/google/login`을 제외한 모든 요청은 HTTP 헤더에 아래와 같이 인증 토큰을 포함해야 합니다.
    ```
    Authorization: Bearer {AccessToken}
    ```

---

## 👤 인증 (Authentication)

### `POST /auth/google/login`

구글 소셜 로그인을 통해 서비스에 인증하고 JWT 토큰을 발급받습니다.

#### **Request Body**

```json
{
  "authCode": "string"
}
```
- `authCode`: 클라이언트에서 구글 로그인을 통해 받은 Authorization Code

#### **Success Response (200 OK)**
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "user": {
    "id": "ObjectId",
    "email": "user@example.com",
    "name": "홍길동",
    "profileImageUrl": "https://path/to/image.jpg"
  }
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "error": "Invalid Google authorization code."
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
  "error": "Invalid or expired refresh token."
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
  "error": "Invalid or expired token."
}
```

### `GET /auth/verify`

현재 Access Token의 유효성을 검증합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109ca",
  "email": "user@example.com",
  "name": "홍길동",
  "profileImageUrl": "https://path/to/image.jpg",
  "role": "user",
  "subscription": "premium"
}
```

#### **Error Response (401 Unauthorized)**
```json
{
  "error": "Invalid or expired token."
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
| `limit`   | Integer | 아니요 (기본값: `10`, 최댓값: `50`)          | 페이지 당 항목 수.                                                 |

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
      "currentReadChapterNumber": 10, // 현재 읽은 챕터 번호
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
  "error": "Invalid sort_by parameter. Must be one of: view_count, average_rating, created_at."
}
```

#### **Error Response (400 Bad Request) - 잘못된 태그 형식**
```json
{
  "error": "Invalid tags format. Tags should be comma-separated strings."
}
```

### `GET /books/{book_id}`

특정 책의 상세 정보를 조회합니다.

#### **Path Parameters**

| 파라미터  | 타입     | 설명             |
| :-------- | :------- | :--------------- |
| `book_id` | String | 조회할 책의 고유 ID |

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
  "error": "Book not found."
}
```

---

## 📖 챕터 (Chapters)

### `GET /books/{book_id}/chapters`

특정 책에 포함된 챕터 목록을 페이지네이션으로 조회합니다.

#### **Path Parameters**

| 파라미터  | 타입     | 설명             |
| :-------- | :------- | :--------------- |
| `book_id` | String | 조회할 책의 고유 ID |

#### **Query Parameters**

| 파라미터 | 타입    | 필수 | 설명                      |
| :------- | :------ | :--- | :------------------------ |
| `page`   | Integer | 아니요 (기본값: `1`)          | 조회할 페이지 번호                |
| `limit`  | Integer | 아니요 (기본값: `10`, 최댓값: `50`) | 페이지 당 항목 수                |

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
  "hasPrevious": false, // 이전 페이지 존재 여부
  "totalChapters": 27, // 전체 챕터 수
  "currentReadChapterNumber": 1 // 현재 읽고 있는 챕터 번호
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
  "error": "Book not found."
}
```

### `GET /books/{book_id}/chapters/{chapter_id}`

특정 챕터의 상세 정보를 조회합니다.

#### **Path Parameters**

| 파라미터     | 타입     | 설명               |
| :----------- | :------- | :----------------- |
| `book_id`    | String | 조회할 책의 고유 ID   |
| `chapter_id` | String | 조회할 챕터의 고유 ID |

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
  "error": "Book not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "error": "Chapter not found."
}
```

---

## 📑 청크 (Chunks)

### `GET /books/{book_id}/chapters/{chapter_id}/chunks`

특정 책의 특정 챕터에 속한 텍스트 청크(Chunk)들을 난이도별로 조회합니다.

#### **Path Parameters**

| 파라미터     | 타입     | 설명               |
| :----------- | :------- | :----------------- |
| `book_id`    | String | 조회할 책의 고유 ID   |
| `chapter_id` | String | 조회할 챕터의 고유 ID |

#### **Query Parameters**

| 파라미터     | 타입    | 필수 | 설명                                   |
| :----------- | :------ | :--- | :------------------------------------- |
| `difficulty` | String  | 예   | `A1`, `A2`, `B1` 등 청크의 난이도. |
| `page`       | Integer | 아니요 | 페이지 번호 (기본값: `1`).                 |
| `limit`      | Integer | 아니요 | 페이지 당 항목 수 (기본값: `10`, 최댓값 `50`).          |

#### **Success Response (200 OK)**
```json
{
  "data": [
    {
      "id": "60d0fe4f5311236168a109cd",
      "chunkNumber": 1,
      "content": "Once when I was six years old I saw a magnificent picture in a book...",
      "isImage": false, // 텍스트 청크인지 이미지 청크인지
      "chunkImageUrl": null, // 이미지 청크일 경우 이미지 URL
      "description": null // 이미지 설명
    },
    {
      "id": "60d0fe4f5311236168a109ce",
      "chunkNumber": 2,
      "content": null, // 이미지 청크일 경우 content는 null
      "isImage": true, // 이미지 청크
      "chunkImageUrl": "https://path/to/boa-constrictor-image.jpg", // 이미지 URL
      "description": "A picture of a boa constrictor swallowing an animal" // 이미지 설명
    }
  ],
  "currentPage": 1, // 현재 페이지
  "totalPages": 5, // 전체 페이지
  "totalCount": 100, // 전체 청크 개수
  "hasNext": true, // 다음 페이지 존재 여부
  "hasPrevious": false, // 이전 페이지 존재 여부
  "totalChunks": 100, // 해당 난이도의 전체 청크 수
  "currentReadChunkNumber": 15, // 현재 읽은 청크 번호
  "progressPercentage": 15.0 // 진행률 (15/100 * 100)
}
```

#### **Error Response (404 Not Found)**
```json
{
  "error": "Chapter not found."
}
```

### `GET /books/{book_id}/chapters/{chapter_id}/chunks/{chunk_id}`

특정 청크의 상세 정보를 조회합니다.

#### **Path Parameters**

| 파라미터     | 타입     | 설명               |
| :----------- | :------- | :----------------- |
| `book_id`    | String | 조회할 책의 고유 ID   |
| `chapter_id` | String | 조회할 챕터의 고유 ID |
| `chunk_id`   | String | 조회할 청크의 고유 ID |

#### **Success Response (200 OK) - 텍스트 청크**
```json
{
  "id": "60d0fe4f5311236168a109cd",
  "chunkNumber": 1,
  "content": "Once when I was six years old I saw a magnificent picture in a book...",
  "isImage": false,
  "chunkImageUrl": null,
  "description": null
}
```

#### **Success Response (200 OK) - 이미지 청크**
```json
{
  "id": "60d0fe4f5311236168a109ce",
  "chunkNumber": 2,
  "content": null,
  "isImage": true,
  "chunkImageUrl": "https://path/to/boa-constrictor-image.jpg",
  "description": "A picture of a boa constrictor swallowing an animal"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "error": "Book not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "error": "Chapter not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "error": "Chunk not found."
}
```

---

## 📈 읽기 진도 (Reading Progress)

### `PUT /books/{book_id}/progress`

사용자의 읽기 진도를 업데이트합니다. 특정 챕터의 특정 청크까지 읽었음을 기록합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터  | 타입     | 설명             |
| :-------- | :------- | :--------------- |
| `book_id` | String | 읽고 있는 책의 고유 ID |

#### **Request Body**

```json
{
  "chapterId": "60d0fe4f5311236168a109cb",
  "chunkNumber": 5
}
```

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109d1",
  "bookId": "60d0fe4f5311236168a109cb", 
  "chapterId": "60d0fe4f5311236168a109cb",
  "totalChunks": 30,
  "currentReadChunkNumber": 5,
  "updatedAt": "2024-01-15T10:30:00"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "error": "Book not found."
}
```

#### **Error Response (404 Not Found)**
```json
{
  "error": "Chapter not found in this book."
}
```

#### **Error Response (400 Bad Request)**
```json
{
  "error": "Invalid chunkNumber. Must be a positive integer."
}
```

### `GET /books/{book_id}/progress`

사용자의 특정 책에 대한 읽기 진도를 조회합니다.

#### **Request Headers**
```
Authorization: Bearer {AccessToken}
```

#### **Path Parameters**

| 파라미터  | 타입     | 설명             |
| :-------- | :------- | :--------------- |
| `book_id` | String | 조회할 책의 고유 ID |

#### **Success Response (200 OK)**
```json
{
  "id": "60d0fe4f5311236168a109d1",
  "bookId": "60d0fe4f5311236168a109cb",
  "chapterId": "60d0fe4f5311236168a109cb",
  "totalChunks": 30,
  "currentReadChunkNumber": 5,
  "updatedAt": "2024-01-15T10:30:00"
}
```

#### **Error Response (404 Not Found)**
```json
{
  "error": "Book not found."
}
```

### `GET /users/me/progress/books`

현재 사용자의 모든 책에 대한 읽기 진도를 조회합니다.

#### **Query Parameters**

| 파라미터 | 타입    | 필수 | 설명                      |
| :------- | :------ | :--- | :------------------------ |
| `page`   | Integer | 아니요 | 페이지 번호 (기본값: `1`)    |
| `limit`  | Integer | 아니요 | 페이지 당 항목 수 (기본값: `10`, 최댓값: `50`) |

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
      "currentReadChapterNumber": 1,
      "currentReadChunkNumber": 5,
      "progressPercentage": 15.5,
      "updatedAt": "2024-01-15T10:30:00"
    }
  ],
  "currentPage": 1, // 현재 페이지
  "totalPages": 3, // 전체 페이지
  "totalCount": 25, // 진도가 있는 전체 책 개수
  "hasNext": true, // 다음 페이지 존재 여부
  "hasPrevious": false // 이전 페이지 존재 여부
}
```