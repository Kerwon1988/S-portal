package com.simbio.core.messaging.application;

public record ContactAttachmentInput(
        String submittedName,
        long declaredSize,
        byte[] content,
        Integer audioDurationSeconds) {

    public ContactAttachmentInput {
        if (content != null) {
            content = content.clone();
        }
    }

    @Override
    public byte[] content() {
        return content == null ? null : content.clone();
    }
}
