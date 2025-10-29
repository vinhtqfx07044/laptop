package com.laptoprepair.service.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import com.laptoprepair.utils.TimeUtils;

/**
 * AI Tool service for getting current time information.
 */
@Service
public class TimeTools {

    private static final Logger logger = LoggerFactory.getLogger(TimeTools.class);

    @Tool(description = "Lấy thời gian hiện tại tại Việt Nam. " +
            "Sử dụng để hiểu ngữ cảnh thời gian khi người dùng hỏi về ngày, giờ, hoặc khoảng thời gian. " +
            "Ví dụ: 'hôm nay', 'tuần này', 'tháng này', 'hôm qua', v.v.")
    public String getCurrentTime() {
        try {
            String formattedTime = TimeUtils.getCurrentTimeFormatted();

            logger.info("LLM requested current time: {}", formattedTime);

            return String.format("Thời gian hiện tại tại Việt Nam:%n%s%n(Định dạng: Năm-Tháng-Ngày Giờ:Phút:Giây Thứ)",
                    formattedTime);

        } catch (Exception e) {
            logger.error("Error getting current time: {}", e.getMessage(), e);
            return "Không thể lấy thời gian hiện tại.";
        }
    }
}
