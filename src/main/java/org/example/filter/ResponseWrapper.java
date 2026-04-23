package org.example.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class ResponseWrapper extends HttpServletResponseWrapper {

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    public ResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = new ServletOutputStream() {
                @Override
                public void write(int b) {
                    buffer.write(b);
                }

                @Override
                public boolean isReady() { return true; }

                @Override
                public void setWriteListener(WriteListener writeListener) {}
            };
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8),true);
        }
        return writer;
    }

    public byte[] getCapturedBytes() {
        if (writer != null) {
            writer.flush();
        }
        return buffer.toByteArray();
    }
}
