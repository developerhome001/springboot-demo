package com.keray.common.handler;


import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

public class IHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String[]> paramMap = new LinkedHashMap<>(32);
    private byte[] body = null;

    public IHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String[] o = paramMap.get(name);
        return o == null || o.length == 0 ? null : o[0];
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(paramMap.keySet());
    }

    /**
     * 重写getParameterMap方法，原生调用HTTPRequestServlet#getParameterMap被锁定，无法调用
     * 这里重写了只对HandlerMethodArgumentResolver使用getParameterMap解析参数有效，其他情况下改装载器无法装载
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        return paramMap;
    }

    @Override
    public String[] getParameterValues(String name) {
        return paramMap.get(name);
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (this.body == null) {
            return super.getInputStream();
        }
        return new ICoyoteInputStream(new ByteArrayInputStream(this.body), super.getInputStream());
    }
}
class ICoyoteInputStream extends ServletInputStream {

    private final ByteArrayInputStream byteArrayInputStream;

    private final ServletInputStream old;

    public ICoyoteInputStream(ByteArrayInputStream byteArrayInputStream, ServletInputStream old) {
        this.byteArrayInputStream = byteArrayInputStream;
        this.old = old;
    }

    @Override
    public boolean isFinished() {
        return old.isFinished();
    }

    @Override
    public boolean isReady() {
        return old.isReady();
    }

    @Override
    public void setReadListener(ReadListener listener) {
        old.setReadListener(listener);
    }


    @Override
    public int read(byte[] b) throws IOException {
        return byteArrayInputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return byteArrayInputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return byteArrayInputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return byteArrayInputStream.available();
    }

    @Override
    public void close() throws IOException {
        byteArrayInputStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        byteArrayInputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        byteArrayInputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return byteArrayInputStream.markSupported();
    }

    @Override
    public int read() throws IOException {
        return byteArrayInputStream.read();
    }
}

