package io.github.dzw1113.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;


/**
 * 从流获取请求
 * @author dzw
 */
public class BodyRequestWrapper extends HttpServletRequestWrapper {

    private byte[] bytes;
    private PushBackServletInputStream pushBackServletInputStream;

    public BodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 读取输入流里的请求参数，并保存到bytes里
        try (InputStream in = request.getInputStream()) {
            bytes = IOUtils.toByteArray(in);
        }
        this.pushBackServletInputStream = new PushBackServletInputStream(new ByteArrayInputStream(bytes));
        reWriteInputStream();
    }

    @Override
    public PushBackServletInputStream getInputStream() throws IOException {
        reWriteInputStream();
        return pushBackServletInputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    public String getRequestParams() throws IOException {
        return new String(bytes, this.getCharacterEncoding());
    }

    public void reWriteInputStream() {
        pushBackServletInputStream.setStream(new ByteArrayInputStream(bytes != null ? bytes : new byte[0]));
    }

    public static class PushBackServletInputStream extends ServletInputStream {

        private InputStream stream;

        public PushBackServletInputStream(InputStream stream) {
            this.stream = stream;
        }

        public void setStream(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public int read() throws IOException {
            return stream.read();
        }

        @Override
        public boolean isFinished() {
            return true;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

        }
    }

}