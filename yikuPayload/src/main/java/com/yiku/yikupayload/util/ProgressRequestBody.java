package com.yiku.yikupayload.util;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class ProgressRequestBody extends RequestBody {
    private MediaType contentType;
    private File file;
    private ProgressCallback mCallback;

    public ProgressRequestBody(MediaType contentType, File file, ProgressCallback mCallback) {
        this.contentType = contentType;
        this.file = file;
        this.mCallback = mCallback;
    }

    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(file);
            long totalBytesRead = 0;
            for (long readCount; (readCount = source.read(sink.getBuffer(), 8192)) != -1; ) {
                totalBytesRead += readCount;
                mCallback.uploadProgress(totalBytesRead);
            }
        } finally {
            Util.closeQuietly(source);
        }
    }

    public interface ProgressCallback {
        void uploadProgress(long totalBytesRead);
    }
}