package com.badminton.shop.modules.shipping.service.impl;

import com.badminton.shop.modules.shipping.dto.ghn.GHNOrderRequest;
import com.badminton.shop.modules.shipping.dto.ghn.GHNShippingFeeRequest;
import com.badminton.shop.modules.shipping.dto.response.DistrictResponse;
import com.badminton.shop.modules.shipping.dto.response.ProvinceResponse;
import com.badminton.shop.modules.shipping.dto.response.ShippingOrderResponse;
import com.badminton.shop.modules.shipping.dto.response.WardResponse;
import com.badminton.shop.modules.shipping.config.GHNProperties;
import com.badminton.shop.modules.shipping.exception.ShippingIntegrationException;
import com.badminton.shop.modules.shipping.service.ShippingProvider;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GHNShippingService implements ShippingProvider {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    @Qualifier("ghnWebClient")
    private final WebClient webClient;
    private final GHNProperties ghnProperties;

    @Override
    public BigDecimal calculateShippingFee(GHNShippingFeeRequest request) {
        JsonNode data = postForData("/v2/shipping-order/fee", request);
        JsonNode totalFeeNode = data.path("total");
        if (!totalFeeNode.isNumber()) {
            totalFeeNode = data.path("service_fee");
        }
        if (!totalFeeNode.isNumber()) {
            throw new ShippingIntegrationException("GHN không trả về thông tin phí vận chuyển hợp lệ.");
        }
        return BigDecimal.valueOf(totalFeeNode.asLong());
    }

    @Override
    public ShippingOrderResponse createShippingOrder(GHNOrderRequest request) {
        JsonNode data = postForData("/v2/shipping-order/create", request);
        return toShippingOrderResponse(data);
    }

    @Override
    public ShippingOrderResponse getShippingDetail(String shippingCode) {
        JsonNode data = postForData("/v2/shipping-order/detail", Map.of("order_code", shippingCode));
        return toShippingOrderResponse(data);
    }

    @Override
    public List<ProvinceResponse> getProvinces() {
        JsonNode data = getForData("/master-data/province");
        List<ProvinceResponse> provinces = new ArrayList<>();
        for (JsonNode item : data) {
            Integer provinceId = readInt(item, "ProvinceID", "ProvinceId", "province_id");
            String provinceName = readText(item, "ProvinceName", "ProvinceNameEn", "province_name");
            if (provinceId == null || provinceId <= 0 || provinceName.isBlank()) {
                continue;
            }
            provinces.add(ProvinceResponse.builder()
                    .provinceId(provinceId)
                    .provinceName(provinceName)
                    .build());
        }
        return provinces;
    }

    @Override
    public List<DistrictResponse> getDistricts(Integer provinceId) {
        JsonNode data = postForData("/master-data/district", Map.of("province_id", provinceId));
        List<DistrictResponse> districts = new ArrayList<>();
        for (JsonNode item : data) {
            Integer districtId = readInt(item, "DistrictID", "DistrictId", "district_id");
            String districtName = readText(item, "DistrictName", "DistrictNameEN", "district_name");
            Integer districtProvinceId = readInt(item, "ProvinceID", "ProvinceId", "province_id");
            if (districtId == null || districtId <= 0 || districtName.isBlank()) {
                continue;
            }
            districts.add(DistrictResponse.builder()
                    .districtId(districtId)
                    .districtName(districtName)
                    .provinceId(districtProvinceId)
                    .build());
        }
        return districts;
    }

    @Override
    public List<WardResponse> getWards(Integer districtId) {
        JsonNode data = postForData("/master-data/ward", Map.of("district_id", districtId));
        List<WardResponse> wards = new ArrayList<>();
        for (JsonNode item : data) {
            String wardCode = readText(item, "WardCode", "WardCodeV2", "ward_code");
            String wardName = readText(item, "WardName", "WardNameEN", "ward_name");
            if (wardCode.isBlank() || wardName.isBlank()) {
                continue;
            }
            wards.add(WardResponse.builder()
                    .wardCode(wardCode)
                    .wardName(wardName)
                    .districtId(districtId)
                    .build());
        }
        return wards;
    }

    private JsonNode getForData(String path) {
        JsonNode root = executeGet(path);
        validateResponse(root);
        return root.path("data");
    }

    private JsonNode postForData(String path, Object body) {
        JsonNode root = executePost(path, body);
        validateResponse(root);
        return root.path("data");
    }

    private JsonNode executeGet(String path) {
        String normalizedPath = normalizePath(path);
        String debugUrl = resolveDebugUrl(normalizedPath);
        try {
            JsonNode node = webClient.get()
                    .uri(normalizedPath)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(REQUEST_TIMEOUT);
            if (node == null) {
                throw new ShippingIntegrationException("GHN trả về dữ liệu rỗng.");
            }
            return node;
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 401) {
                throw new ShippingIntegrationException(
                        "GHN authentication failed (401) at " + debugUrl + ". Please verify app.shipping.ghn.token / GHN_TOKEN and ensure the token belongs to GHN Staging.",
                        ex);
            }
            throw new ShippingIntegrationException("Lỗi GHN API GET " + debugUrl + ": " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new ShippingIntegrationException("Không thể gọi GHN API GET " + debugUrl, ex);
        }
    }

    private JsonNode executePost(String path, Object body) {
        String normalizedPath = normalizePath(path);
        String debugUrl = resolveDebugUrl(normalizedPath);
        try {
            JsonNode node = webClient.post()
                    .uri(normalizedPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(REQUEST_TIMEOUT);
            if (node == null) {
                throw new ShippingIntegrationException("GHN trả về dữ liệu rỗng.");
            }
            return node;
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 401) {
                throw new ShippingIntegrationException(
                        "GHN authentication failed (401) at " + debugUrl + ". Please verify app.shipping.ghn.token / GHN_TOKEN and ensure the token belongs to GHN Staging.",
                        ex);
            }
            throw new ShippingIntegrationException("Lỗi GHN API POST " + debugUrl + ": " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new ShippingIntegrationException("Không thể gọi GHN API POST " + debugUrl, ex);
        }
    }

    private void validateResponse(JsonNode root) {
        int code = root.path("code").asInt(-1);
        if (code != 200) {
            String message = root.path("message").asText("Unknown GHN error");
            throw new ShippingIntegrationException("GHN API lỗi: " + message);
        }
    }

    private ShippingOrderResponse toShippingOrderResponse(JsonNode data) {
        String expected = data.path("expected_delivery_time").asText(null);
        return ShippingOrderResponse.builder()
                .shippingCode(data.path("order_code").asText(null))
                .shippingFee(BigDecimal.valueOf(data.path("total_fee").asLong(0)))
                .expectedDeliveryTime(parseExpectedTime(expected))
                .status(data.path("status").asText(null))
                .sortCode(data.path("sort_code").asText(null))
                .build();
    }

    private LocalDateTime parseExpectedTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String resolveDebugUrl(String normalizedPath) {
        try {
            URI base = URI.create(normalizeBaseUrl(ghnProperties.getBaseUrl()));
            return base.resolve(normalizedPath).toString();
        } catch (Exception ignored) {
            return normalizedPath;
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        if (normalized.isEmpty()) {
            normalized = "https://dev-online-gateway.ghn.vn/shiip/public-api";
        }
        if (!normalized.contains("/shiip/public-api")) {
            if (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            normalized = normalized + "/shiip/public-api";
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    private Integer readInt(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (value.isNumber()) {
                return value.asInt();
            }
            if (value.isTextual()) {
                String text = value.asText("").trim();
                if (text.matches("\\d+")) {
                    return Integer.parseInt(text);
                }
            }
        }
        return null;
    }

    private String readText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (value.isTextual()) {
                String text = value.asText("").trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

}
