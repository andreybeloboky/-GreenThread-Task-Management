package org.example.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class MetadataAuditFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse httpResp = (HttpServletResponse) servletResponse;
        httpResp.setHeader("X-Powered-By", "Jakarta-Servlet-API");
        ResponseWrapper wrapper = new ResponseWrapper(httpResp);
        filterChain.doFilter(servletRequest, wrapper);
        byte[] payload = wrapper.getCapturedBytes();
        int size = payload.length;
        servletResponse.getWriter().write("Response payload size: " + size + " bytes");
        ServletOutputStream out = httpResp.getOutputStream();
        out.write(payload);
        out.flush();
    }
}
