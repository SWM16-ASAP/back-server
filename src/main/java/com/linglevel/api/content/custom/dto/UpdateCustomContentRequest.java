package com.linglevel.api.content.custom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "커스텀 콘텐츠 수정 요청")
public class UpdateCustomContentRequest {

    @Schema(description = "새로운 콘텐츠 제목", example = "My Updated Content Title")
    private String title;

    @Schema(description = "업데이트할 태그 목록", example = "[\"technology\", \"artificial-intelligence\"]")
    private List<String> tags;
}
