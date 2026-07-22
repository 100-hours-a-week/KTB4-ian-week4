package com.ian.community.image.domain;

/**
 * 이미지 Asset의 수명주기 상태입니다.
 */
public enum ImageStatus {

    ACTIVE,         // 현재 사용자 또는 게시글에서 사용 중인 이미지입니다.
    DELETED,        // 사용자와의 연결은 해제됐지만,  30일 보존 기간이 지나지 않은 이미지입니다.
    DELETE_FAILED   // Hard Delete 과정에서 파일 삭제가 실패한 상태입니다. 다음 Scheduler 실행에서 다시 삭제를 시도합니다.
}