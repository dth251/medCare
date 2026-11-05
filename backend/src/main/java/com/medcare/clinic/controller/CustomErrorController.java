package com.medcare.clinic.controller;

import com.medcare.clinic.dto.common.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        String uri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String acceptHeader = request.getHeader("Accept");

        // Nếu request từ trình duyệt yêu cầu HTML và không phải gọi API -> forward về index.html (SPA History mode)
        if (acceptHeader != null && acceptHeader.contains(MediaType.TEXT_HTML_VALUE)
                && (uri == null || !uri.startsWith("/api/"))) {
            return "forward:/index.html";
        }

        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (statusCode != null) {
            try {
                status = Integer.parseInt(statusCode.toString());
            } catch (NumberFormatException ignored) {}
        }

        String errorMsg;
        if (status == 404) {
            errorMsg = "Không tìm thấy tài nguyên yêu cầu (404 Not Found)";
        } else if (status == 403) {
            errorMsg = "Bạn không có quyền truy cập chức năng này (403 Forbidden)";
        } else if (status == 401) {
            errorMsg = "Chưa đăng nhập hoặc phiên làm việc đã hết hạn (401 Unauthorized)";
        } else if (status == 400) {
            errorMsg = message != null && !message.toString().isBlank() ? message.toString() : "Yêu cầu không hợp lệ (400 Bad Request)";
        } else {
            errorMsg = "Đã có lỗi xảy ra trên máy chủ, vui lòng thử lại sau";
        }

        return ResponseEntity.status(status).body(ApiResponse.error(errorMsg));
    }
}
