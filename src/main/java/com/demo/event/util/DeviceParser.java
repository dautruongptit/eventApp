package com.demo.event.util;

import org.springframework.stereotype.Component;

@Component
public class DeviceParser {

    /**
     * Xac dinh loai thiet bi tu User-Agent string.
     * @return "Mobile" | "Tablet" | "Desktop" | "Unknown"
     */
    public String parseDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown";
        String ua = userAgent.toLowerCase();

        if (ua.contains("tablet") || ua.contains("ipad"))
            return "Tablet";
        if (ua.contains("mobile") || ua.contains("iphone") ||
                ua.contains("android") && !ua.contains("tablet"))
            return "Mobile";
        return "Desktop";
    }

    /**
     * Xac dinh he dieu hanh.
     * @return "Windows 11" | "macOS" | "Android" | "iOS" | "Linux" | "Unknown"
     */
    public String parseOs(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown";
        String ua = userAgent.toLowerCase();

        if (ua.contains("windows nt 10.0")) return "Windows 10/11";
        if (ua.contains("windows"))         return "Windows";
        if (ua.contains("mac os x"))        return "macOS";
        if (ua.contains("iphone") ||
                ua.contains("ipad"))             return "iOS";
        if (ua.contains("android"))         return "Android";
        if (ua.contains("linux"))           return "Linux";
        return "Unknown";
    }

    /**
     * Xac dinh trinh duyet.
     * @return "Chrome" | "Safari" | "Firefox" | "Edge" | "Unknown"
     */
    public String parseBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown";
        String ua = userAgent.toLowerCase();

        // Thu tu QUAN TRONG: Edge phai check truoc Chrome
        if (ua.contains("edg/") || ua.contains("edge/")) return "Edge";
        if (ua.contains("opr/") || ua.contains("opera")) return "Opera";
        if (ua.contains("chrome"))   return "Chrome";
        if (ua.contains("firefox"))  return "Firefox";
        if (ua.contains("safari"))   return "Safari";
        return "Unknown";
    }

    /**
     * Lay IP that tu request (xu ly X-Forwarded-For khi dung proxy/load balancer).
     */
    public String extractIp(jakarta.servlet.http.HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For co the chua nhieu IP: "client, proxy1, proxy2"
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }
}
