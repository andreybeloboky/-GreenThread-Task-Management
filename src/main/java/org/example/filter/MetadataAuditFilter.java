package org.example.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@WebFilter("/*")
public class MetadataAuditFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse httpResp = (HttpServletResponse) servletResponse;
        ResponseWrapper wrapper = new ResponseWrapper(httpResp);

        httpResp.setHeader("X-Powered-By", "Jakarta-Servlet-API");

        filterChain.doFilter(servletRequest, wrapper);

        byte[] payload = wrapper.getCapturedBytes();
        int size = payload.length;

        log.info("Response payload size: {}", size);

        ServletOutputStream out = httpResp.getOutputStream();
        out.write(payload);
        out.flush();
        wrapper.close();
    }
}
