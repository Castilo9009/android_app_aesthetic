package com.dancingqueen.walladog.aws.downloader;

public interface ResponseHandler {
    void onSuccess(long downloadId);
    void onError(String errorMessage);
}
