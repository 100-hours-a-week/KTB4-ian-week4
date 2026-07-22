package com.ian.community.image.domain;

/**
 * 하나의 ImageAsset으로부터 생성되는 이미지 크기와 용도를 구분합니다.
 */
public enum ImageVariantType {

    PROFILE_HIGH, // 프로필 편집 화면 등에서 사용할 고해상도 이미지입니다. 최대 크기는 2048 x 2048입니다.
    PROFILE_160,  // 비교적 크게 표시되는 프로필 이미지입니다
    PROFILE_34,   // 피드 작성자, 댓글 작성자 등에 사용하는 작은 프로필 이미지입니다.
    FEED_SOURCE,  //원본 해상도를 유지하면서 WebP로 변환한 피드 이미지입니다.
    FEED_DISPLAY  //실제 피드 화면에 표시하는 리사이징 이미지입니다.
}