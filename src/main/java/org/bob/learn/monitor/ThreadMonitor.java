package org.bob.learn.monitor;

import lombok.extern.slf4j.Slf4j;
import org.bob.learn.common.constant.WebServerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public final class ThreadMonitor {

    /**
     * Undertow服务器工作线程名称正则表达式
     */
    private final static String  WORK_THREAD_NAME_PATTERN_FOR_UNDERTOW = "^XNIO-(.*)(task)(.*)";

    /**
     * Tomcat服务器工作线程名称正则表达式
     */
    private final static String  WORK_THREAD_NAME_PATTERN_FOR_TOMCAT = "^http-nio-(.*)(exec)(.*)";

    /**
     * 服务器类型与服务器工作线程名称正则表达式映射
     */
    private final static Map<WebServerType,String> WEB_SERVER_WORK_THREAD_NAME_PATTERN_MAP = new HashMap<>(4);

    /**
     * JVM线程管理器
     */
    private final static ThreadMXBean THREAD_MX_BEAN;

    /**
     * 服务器工作线程名称模式
     */
    private final static Pattern WEB_SERVER_WORK_THREAD_NAME_PATTERN;

    /**
     * 服务器工作线程数上限
     */
    private static double SERVER_WORK_THREAD_MAX_LIMIT;

    /**
     * 服务器忙碌工作线程数上限
     */
    private static double SERVER_BUSY_WORK_THREAD_NUM_LIMIT;

    /**
     * 服务器过载标识
     */
    private static volatile boolean systemOverloadFlag = false;

    static {
        WEB_SERVER_WORK_THREAD_NAME_PATTERN_MAP.put(WebServerType.TOMCAT,WORK_THREAD_NAME_PATTERN_FOR_TOMCAT);
        WEB_SERVER_WORK_THREAD_NAME_PATTERN_MAP.put(WebServerType.UNDERTOW,WORK_THREAD_NAME_PATTERN_FOR_UNDERTOW);
        WEB_SERVER_WORK_THREAD_NAME_PATTERN = Pattern.compile(WEB_SERVER_WORK_THREAD_NAME_PATTERN_MAP.get(WebServerType.serverType()));
        THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
    }

    /**
     * 默认为0.85
     */
    @Value("${tomcat.thread.busyRatioLimit:0.85}")
    private double busyRatioLimit;

    @Autowired
    private ServerProperties serverProperties;

    @PostConstruct
    public void init(){
        WebServerType webServerType = WebServerType.serverType();
        if(WebServerType.UNSUPPORTED.equals(webServerType)){
            throw new IllegalStateException("不支持的web服务器");
        }
        if(WebServerType.TOMCAT.equals(webServerType)){
            SERVER_WORK_THREAD_MAX_LIMIT =  serverProperties.getTomcat().getMaxThreads();
        }
        if(WebServerType.UNDERTOW.equals(WebServerType.serverType())){
            SERVER_WORK_THREAD_MAX_LIMIT =  serverProperties.getUndertow().getWorkerThreads()!=null? serverProperties.getUndertow().getWorkerThreads(): Math.max(Runtime.getRuntime().availableProcessors(), 2) *8;
        }
        SERVER_BUSY_WORK_THREAD_NUM_LIMIT = SERVER_WORK_THREAD_MAX_LIMIT * busyRatioLimit;
    }


    public static void checkForSystemOverload() {
        ThreadInfo[] threadInfoArray = THREAD_MX_BEAN.getThreadInfo(THREAD_MX_BEAN.getAllThreadIds());
        if(threadInfoArray!=null) {
            double busyThreadNum = Arrays.asList(threadInfoArray).stream().filter(ThreadMonitor::matchBusyWorkThreat).count();
            if(busyThreadNum>SERVER_BUSY_WORK_THREAD_NUM_LIMIT){
                systemOverloadFlag = true;
                log.info("http忙碌工作线程数为[{}]，超过阈值[{}],将执行接口降级逻辑",busyThreadNum,SERVER_BUSY_WORK_THREAD_NUM_LIMIT);
            }else {
                systemOverloadFlag = false;
                log.info("http忙线程数为[{}]，未超过阈值[{}],将执行接口正常逻辑",busyThreadNum,SERVER_BUSY_WORK_THREAD_NUM_LIMIT);
            }
        }
    }

    public static boolean isSystemOverload() {
        return systemOverloadFlag;
    }

    private static boolean matchBusyWorkThreat(ThreadInfo threadInfo) {
        return WEB_SERVER_WORK_THREAD_NAME_PATTERN.matcher(threadInfo.getThreadName()).find()&& Thread.State.RUNNABLE.name().equalsIgnoreCase(threadInfo.getThreadState().name());
    }
}
