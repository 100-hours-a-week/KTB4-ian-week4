package com.ian.community.user.dto.response;

public record ProfileImageResponse(
        String highUrl,
        String mediumUrl,
        String smallUrl,
        boolean defaultImage
) {
    private static final String DEFAULT_URL =
            "/images/profile-default.svg";

    public static ProfileImageResponse from(ImageAsset imageAsset) {
        if (imageAsset == null) {
            return new ProfileImageResponse(
                    DEFAULT_URL,
                    DEFAULT_URL,
                    DEFAULT_URL,
                    true
            );
        }

        String base = "/api/images/" + imageAsset.getImageAssetId();

        return new ProfileImageResponse(
                base + "/profile-high",
                base + "/profile-160",
                base + "/profile-34",
                false
        );
    }
}
