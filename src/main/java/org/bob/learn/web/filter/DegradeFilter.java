package org.bob.learn.web.filter;

import org.bob.learn.util.SystemUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 服务降级过滤器
 * 降级策略：HTTP工作线程数达到配置最大值，且忙碌工作线程数与最大工作线程数的比大于指定阈值
 * 降级过程： 1、请求进入 2、判断是否执行降级策略 3、若是，则转发请求转发至降级控制器，否则，正常处理请求
 * 注：Spring Cloud Hystrix 提供了降级功能，可以作为参考和后期引入Spring Cloud后进行切换
 */
@WebFilter(urlPatterns = "/*", filterName = "degradeFilter")
public class DegradeFilter implements Filter {

    /**
     * 转发路径前缀 转发路径=转发路径前缀+原请求路径
     */
    private final static String DEGRADE_PATH_PREFIX = "/degrade";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if(SystemUtils.isSystemOverload()) {
            HttpServletRequest request = (HttpServletRequest)servletRequest;
            servletRequest.getRequestDispatcher(DEGRADE_PATH_PREFIX+request.getServletPath()).forward(servletRequest, servletResponse);
        }else {
            filterChain.doFilter(servletRequest,servletResponse);
        }
    }

    @Override
    public void destroy() {
    }
}
