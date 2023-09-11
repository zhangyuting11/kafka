package com.zhangyuting.kafkademo;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import org.apache.logging.log4j.ThreadContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;


/**
 * @author: zhangyuting
 * @Project: easy_project
 * @Pcakage: com.lyh.ecology.base.aop.LogAop
 * @Date: 2023年01月09日 14:48
 * @Description:
 */
@Aspect
@Component
public class MyLogAop {
    private static final Logger log = LoggerFactory.getLogger(MyLogAop.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MyLogAop(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    @Pointcut("execution(* com.zhangyuting.kafkademo.controller.*..*(..))")
    public void cutService() {
    }

    @Around("cutService()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        HashMap<String, Object> map = new HashMap<>(10);
        try {
            HttpServletRequest request = getRequest();
            if (request == null) {
                log.error("获取请求失败。");
                return point.proceed();
            } else {
                MDC.put("traceId", request.getHeader("traceId"));
                MDC.put("traceOrder", request.getHeader("traceOrder"));
                Signature signature = point.getSignature();
                MethodSignature methodSignature = (MethodSignature) signature;
                Method method = methodSignature.getMethod();
                map.put("requestHost", request.getRemoteHost());
                map.put("url", request.getRequestURI());
                String className = point.getTarget().getClass().getName();
                String methodName = className + "#" + point.getSignature().getName();
                map.put("targetClassName", className);
                map.put("targetMethodName", point.getSignature().toString());
                Map<String, String[]> parameterMap = request.getParameterMap();
                Set<Map.Entry<String, String[]>> entry = parameterMap.entrySet();
                Iterator<Map.Entry<String, String[]>> it = entry.iterator();
                String urlParam = "";
                while (it.hasNext()) {
                    Map.Entry<String, String[]> me = it.next();
                    String key = me.getKey();
                    String value = me.getValue()[0];
                    urlParam += "&" + key + "=" + value;
                }
                map.put("url", request.getRequestURL().toString() + urlParam.replaceFirst("&", "?"));
                map.put("uri", request.getRequestURI());
                map.put("params", JSONUtil.toJsonStr(getParameter(method, point.getArgs())));
                map.put("method", request.getMethod());
                Object result = point.proceed();
                map.put("spendTime", (int) (System.currentTimeMillis() - startTime));
                map.put("result", "");
                log.error(JSONUtil.toJsonStr(map));
                return result;
            }
        } catch (Throwable var18) {
            map.put("spendTime", (int) (System.currentTimeMillis() - startTime));
            map.put("result", var18.toString());
            log.error(JSONUtil.toJsonStr(map));
            throw var18;
        } finally {
            addSysLog(map);
            ThreadContext.clearAll();
        }
    }

    public static HttpServletRequest getRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return requestAttributes == null ? null : requestAttributes.getRequest();
    }

    /**
     * 根据方法和传入的参数获取请求参数
     */
    private Object getParameter(Method method, Object[] args) {
        List<Object> argList = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            //将RequestBody注解修饰的参数作为请求参数
            RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
            if (requestBody != null) {
                argList.add(args[i]);
            }
            //将RequestParam注解修饰的参数作为请求参数
            RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
            if (requestParam != null) {
                Map<String, Object> map = new HashMap<>();
                String key = parameters[i].getName();
                if (!StringUtils.isEmpty(requestParam.value())) {
                    key = requestParam.value();
                }
                map.put(key, args[i]);
                argList.add(map);
            }
        }
        if (argList.size() == 0) {
            return null;
        } else if (argList.size() == 1) {
            return argList.get(0);
        } else {
            return argList;
        }
    }

    /**
     * 上传系统请求日志到mongo
     *
     * @param map
     */
    private void addSysLog(Map map) {
        ThreadUtil.execute(()->{
            kafkaTemplate.send("kafka_demo", JSONUtil.toJsonStr(map));
        });
    }
}