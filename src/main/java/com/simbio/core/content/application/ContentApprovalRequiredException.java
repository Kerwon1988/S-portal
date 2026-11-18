package com.simbio.core.content.application;

public class ContentApprovalRequiredException extends RuntimeException {

    public ContentApprovalRequiredException() {
        super("Required business or legal approval is pending");
    }
}
