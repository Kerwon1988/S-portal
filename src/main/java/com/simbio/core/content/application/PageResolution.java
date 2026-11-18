package com.simbio.core.content.application;

public record PageResolution(PublishedPageView page, String redirectPath) {

    public static PageResolution found(PublishedPageView page) {
        return new PageResolution(page, null);
    }

    public static PageResolution redirected(String path) {
        return new PageResolution(null, path);
    }
}
