package com.badminton.shop.util;

import org.springframework.data.domain.Page;
import com.badminton.shop.dto.response.PageResponse;

import java.text.Normalizer;
import java.util.Random;
import java.util.regex.Pattern;

public final class AppUtils {

    private static final Random RANDOM = new Random();

    private AppUtils() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("")
                .toLowerCase()
                .replaceAll("[đĐ]", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    public static String generateOrderCode() {
        long timestamp = System.currentTimeMillis();
        int random = RANDOM.nextInt(1000);
        return String.format("ORD%d%03d", timestamp, random);
    }

    public static <T, R> PageResponse<R> toPageResponse(Page<T> page, java.util.List<R> content) {
        return PageResponse.<R>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
