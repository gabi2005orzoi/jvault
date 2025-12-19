package com.jvault.jvault.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class IpUtils {
    public static String getIpClient(HttpServletRequest request){
        String ipAddress = request.getHeader("X-Forwarded-For");
        if(ipAddress == null || ipAddress.isEmpty() || "unknow".equalsIgnoreCase(ipAddress))
            ipAddress = request.getHeader("Proxy-Client-IP");
        if(ipAddress == null || ipAddress.isEmpty() || "unknow".equalsIgnoreCase(ipAddress))
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        if(ipAddress == null || ipAddress.isEmpty() || "unknow".equalsIgnoreCase(ipAddress))
            ipAddress = request.getRemoteAddr();
        if(ipAddress != null && ipAddress.contains(","))
            ipAddress = ipAddress.split(",")[0].trim();
        return ipAddress;
    }

    public static String getCurrentIp(){
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if(attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if(ip == null || ip.isEmpty())
                    return request.getRemoteAddr();
                return ip;
            }
        } catch (Exception e){
            //
        }
        return "UNKNOWN";
    }
}
