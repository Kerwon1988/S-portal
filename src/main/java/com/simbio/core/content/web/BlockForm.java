package com.simbio.core.content.web;

import java.util.UUID;

import com.simbio.core.content.application.BlockInput;
import com.simbio.core.content.application.ContentValidationException;

public class BlockForm {

    private String type = "";
    private int position;
    private String heading = "";
    private String bodyMarkdown = "";
    private String targetUrl = "";
    private String mediaPublicId = "";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null ? "" : type;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getBodyMarkdown() {
        return bodyMarkdown;
    }

    public void setBodyMarkdown(String bodyMarkdown) {
        this.bodyMarkdown = bodyMarkdown;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getMediaPublicId() {
        return mediaPublicId;
    }

    public void setMediaPublicId(String mediaPublicId) {
        this.mediaPublicId = mediaPublicId;
    }

    BlockInput toInput() {
        try {
            UUID mediaId = mediaPublicId == null || mediaPublicId.isBlank()
                    ? null : UUID.fromString(mediaPublicId);
            return new BlockInput(type, position, heading, bodyMarkdown, targetUrl, mediaId);
        } catch (IllegalArgumentException exception) {
            throw new ContentValidationException("Invalid media identifier");
        }
    }
}
