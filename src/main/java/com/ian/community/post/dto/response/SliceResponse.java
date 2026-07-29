package com.ian.community.post.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
public class SliceResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final boolean hasNext;
    private final String message;

    private SliceResponse(
            List<T> content,
            int page,
            int size,
            boolean hasNext,
            String message
    ) {
        this.content = List.copyOf(content);
        this.page = page;
        this.size = size;
        this.hasNext = hasNext;
        this.message = message;
    }

    public static <T> SliceResponse<T> from(
            Slice<T> slice
    ) {
        return from(slice, null);
    }

    public static <T> SliceResponse<T> from(
            Slice<T> slice,
            String endOfSliceMessage
    ) {
        return new SliceResponse<>(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext(),
                slice.hasNext()
                        ? null
                        : endOfSliceMessage
        );
    }
}