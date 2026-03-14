package com.badminton.shop.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppUtilsTest {

    @Test
    void toSlug_withVietnameseText_shouldConvertCorrectly() {
        assertThat(AppUtils.toSlug("Vợt Cầu Lông")).isEqualTo("vot-cau-long");
        assertThat(AppUtils.toSlug("Giầy Cầu Lông")).isEqualTo("giay-cau-long");
        assertThat(AppUtils.toSlug("Túi Đựng Vợt")).isEqualTo("tui-dung-vot");
    }

    @Test
    void toSlug_withNullOrBlank_shouldReturnEmpty() {
        assertThat(AppUtils.toSlug(null)).isEmpty();
        assertThat(AppUtils.toSlug("")).isEmpty();
        assertThat(AppUtils.toSlug("  ")).isEmpty();
    }

    @Test
    void toSlug_withSpecialCharacters_shouldRemoveThem() {
        assertThat(AppUtils.toSlug("Hello World!")).isEqualTo("hello-world");
        assertThat(AppUtils.toSlug("test@example.com")).isEqualTo("testexamplecom");
    }

    @Test
    void generateOrderCode_shouldStartWithORD() {
        String orderCode = AppUtils.generateOrderCode();
        assertThat(orderCode).startsWith("ORD");
        assertThat(orderCode.length()).isGreaterThan(3);
    }

    @Test
    void generateOrderCode_shouldBeUnique() {
        String code1 = AppUtils.generateOrderCode();
        String code2 = AppUtils.generateOrderCode();
        assertThat(code1).isNotEqualTo(code2);
    }
}
