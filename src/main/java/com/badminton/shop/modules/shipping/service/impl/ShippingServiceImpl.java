package com.badminton.shop.modules.shipping.service.impl;

import com.badminton.shop.modules.auth.entity.UserAddress;
import com.badminton.shop.modules.shipping.config.GHNProperties;
import com.badminton.shop.modules.shipping.dto.ghn.GHNItem;
import com.badminton.shop.modules.shipping.dto.ghn.GHNOrderRequest;
import com.badminton.shop.modules.shipping.dto.ghn.GHNShippingFeeRequest;
import com.badminton.shop.modules.shipping.dto.request.ShippingFeeCalculationRequest;
import com.badminton.shop.modules.shipping.dto.request.ShippingOrderCreationRequest;
import com.badminton.shop.modules.shipping.dto.response.DistrictResponse;
import com.badminton.shop.modules.shipping.dto.response.ProvinceResponse;
import com.badminton.shop.modules.shipping.dto.response.ShippingOrderResponse;
import com.badminton.shop.modules.shipping.dto.response.WardResponse;
import com.badminton.shop.modules.shipping.exception.ShippingIntegrationException;
import com.badminton.shop.modules.shipping.service.ShippingProvider;
import com.badminton.shop.modules.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private final ShippingProvider shippingProvider;
    private final GHNProperties ghnProperties;

    @Override
    public BigDecimal calculateShippingFee(ShippingFeeCalculationRequest request) {
        ResolvedAddress resolvedAddress = resolveAddress(request.getAddress());
        int fromDistrictId = requirePositive("app.shipping.ghn.from-district-id", ghnProperties.getFromDistrictId());
        int serviceTypeId = requirePositive("app.shipping.ghn.service-type-id", ghnProperties.getServiceTypeId());

        GHNShippingFeeRequest ghnRequest = GHNShippingFeeRequest.builder()
            .fromDistrictId(fromDistrictId)
                .toDistrictId(resolvedAddress.districtId())
                .toWardCode(resolvedAddress.wardCode())
            .serviceTypeId(serviceTypeId)
                .insuranceValue(safeAmount(request.getInsuranceValue()))
                .weight(totalWeight(request.getItems()))
                .length(packageLength(request.getItems()))
                .width(packageWidth(request.getItems()))
                .height(packageHeight(request.getItems()))
                .items(toGHNItems(request.getItems()))
                .build();

        return shippingProvider.calculateShippingFee(ghnRequest);
    }

    @Override
    public ShippingOrderResponse createShippingOrder(ShippingOrderCreationRequest request) {
        if (request.getFeeCalculationRequest() == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu tính phí vận chuyển để tạo vận đơn.");
        }

        ResolvedAddress resolvedAddress = resolveAddress(request.getAddress());
        List<ShippingFeeCalculationRequest.ShippingItemRequest> items = request.getFeeCalculationRequest().getItems();
        int fromDistrictId = requirePositive("app.shipping.ghn.from-district-id", ghnProperties.getFromDistrictId());
        int serviceTypeId = requirePositive("app.shipping.ghn.service-type-id", ghnProperties.getServiceTypeId());

        GHNOrderRequest ghnRequest = GHNOrderRequest.builder()
            .paymentTypeId(2)
                .note(request.getNote())
                .requiredNote("KHONGCHOXEMHANG")
                .toName(request.getReceiverName())
                .toPhone(request.getReceiverPhone())
            .toAddress(buildDetailedAddress(request.getAddress()))
                .toWardCode(resolvedAddress.wardCode())
                .toDistrictId(resolvedAddress.districtId())
                .fromDistrictId(fromDistrictId)
                .codAmount(safeAmount(request.getCodAmount()))
                .insuranceValue(safeAmount(request.getInsuranceValue()))
                .serviceTypeId(serviceTypeId)
                .weight(totalWeight(items))
                .length(packageLength(items))
                .width(packageWidth(items))
                .height(packageHeight(items))
                .clientOrderCode(request.getClientOrderCode())
                .items(toGHNItems(items))
                .build();

        return shippingProvider.createShippingOrder(ghnRequest);
    }

    @Override
    public ShippingOrderResponse getShippingDetail(String shippingCode) {
        return shippingProvider.getShippingDetail(shippingCode);
    }

    @Override
    public List<ProvinceResponse> getProvinces() {
        return shippingProvider.getProvinces();
    }

    @Override
    public List<DistrictResponse> getDistricts(Integer provinceId) {
        return shippingProvider.getDistricts(provinceId);
    }

    @Override
    public List<WardResponse> getWards(Integer districtId) {
        return shippingProvider.getWards(districtId);
    }

    private ResolvedAddress resolveAddress(UserAddress address) {
        if (address == null) {
            throw new IllegalArgumentException("Địa chỉ giao hàng không được để trống.");
        }

        Integer cachedDistrictId = address.getGhnDistrictId();
        String cachedWardCode = safeValue(address.getGhnWardCode());
        if (cachedDistrictId != null && cachedDistrictId > 0 && !cachedWardCode.isBlank()) {
            Integer cachedProvinceId = address.getGhnProvinceId();
            return new ResolvedAddress(cachedProvinceId, cachedDistrictId, cachedWardCode);
        }

        String provinceName = safeValue(address.getProvince());
        String districtName = safeValue(address.getDistrict());
        String wardName = safeValue(address.getWard());

        if (provinceName.isBlank() || districtName.isBlank() || wardName.isBlank()) {
            throw new IllegalArgumentException(
                    "Địa chỉ giao hàng phải có đủ tỉnh/thành, quận/huyện, phường/xã để tính phí GHN.");
        }

        ProvinceResponse province = findProvinceByName(provinceName);
        DistrictResponse district = findDistrictByName(province.getProvinceId(), districtName);
        WardResponse ward = findWardByName(district.getDistrictId(), wardName);

        if (district.getDistrictId() == null || district.getDistrictId() <= 0) {
            throw new IllegalArgumentException("Không map được DistrictID GHN hợp lệ từ địa chỉ quận/huyện: " + districtName);
        }
        if (ward.getWardCode() == null || ward.getWardCode().isBlank()) {
            throw new IllegalArgumentException("Không map được WardCode GHN hợp lệ từ địa chỉ phường/xã: " + wardName);
        }

        return new ResolvedAddress(province.getProvinceId(), district.getDistrictId(), ward.getWardCode());
    }

    private ProvinceResponse findProvinceByName(String provinceName) {
        List<ProvinceResponse> provinces = shippingProvider.getProvinces();
        return selectBestMatch(
                provinces,
                provinceName,
                ProvinceResponse::getProvinceName,
                "Không map được tỉnh/thành GHN từ địa chỉ: " + provinceName +
                        ". Vui lòng dùng dữ liệu từ API /api/shipping/provinces.");
    }

    private DistrictResponse findDistrictByName(Integer provinceId, String districtName) {
        List<DistrictResponse> districts = shippingProvider.getDistricts(provinceId);

        Integer strictId = parseStrictInteger(districtName);
        if (strictId != null) {
            return districts.stream()
                    .filter(d -> strictId.equals(d.getDistrictId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "DistrictID GHN không tồn tại trong tỉnh đã chọn: " + strictId));
        }

        return selectBestMatch(
                districts,
                districtName,
                DistrictResponse::getDistrictName,
                "Không map được quận/huyện GHN từ địa chỉ: " + districtName +
                        ". Vui lòng dùng dữ liệu từ API /api/shipping/provinces/{provinceId}/districts hoặc lưu DistrictID.");
    }

    private WardResponse findWardByName(Integer districtId, String wardName) {
        List<WardResponse> wards = shippingProvider.getWards(districtId);

        String rawWard = safeValue(wardName);
        WardResponse byCode = wards.stream()
                .filter(w -> safeValue(w.getWardCode()).equalsIgnoreCase(rawWard))
                .findFirst()
                .orElse(null);
        if (byCode != null) {
            return byCode;
        }

        return selectBestMatch(
                wards,
                wardName,
                WardResponse::getWardName,
                "Không map được phường/xã GHN từ địa chỉ: " + wardName +
                        ". Vui lòng dùng dữ liệu từ API /api/shipping/districts/{districtId}/wards hoặc lưu WardCode.");
    }

    private <T> T selectBestMatch(List<T> options, String input, Function<T, String> nameExtractor, String notFoundMessage) {
        String normalizedInput = normalizeAddressName(input);
        if (normalizedInput.isBlank()) {
            throw new IllegalArgumentException(notFoundMessage);
        }

        T exact = options.stream()
                .filter(option -> normalizeAddressName(nameExtractor.apply(option)).equals(normalizedInput))
                .findFirst()
                .orElse(null);
        if (exact != null) {
            return exact;
        }

        T contains = options.stream()
                .filter(option -> {
                    String target = normalizeAddressName(nameExtractor.apply(option));
                    return !target.isBlank() && (target.contains(normalizedInput) || normalizedInput.contains(target));
                })
                .findFirst()
                .orElse(null);
        if (contains != null) {
            return contains;
        }

        throw new IllegalArgumentException(notFoundMessage);
    }

    private List<GHNItem> toGHNItems(List<ShippingFeeCalculationRequest.ShippingItemRequest> items) {
        return items.stream()
                .map(item -> GHNItem.builder()
                        .name(item.getName())
                        .code(item.getSku())
                        .quantity(item.getQuantity())
                        .price(safeAmount(item.getUnitPrice()))
                        .weight(safeWeight(item.getWeightGrams()))
                        .length(safeLength(item.getLengthCm()))
                        .width(safeWidth(item.getWidthCm()))
                        .height(safeHeight(item.getHeightCm()))
                        .build())
                .toList();
    }

    private int totalWeight(List<ShippingFeeCalculationRequest.ShippingItemRequest> items) {
        int sum = items.stream()
                .mapToInt(item -> safeWeight(item.getWeightGrams()) * safeQuantity(item.getQuantity()))
                .sum();
        return Math.max(sum, safeWeight(null));
    }

    private int packageLength(List<ShippingFeeCalculationRequest.ShippingItemRequest> items) {
        int max = items.stream().map(ShippingFeeCalculationRequest.ShippingItemRequest::getLengthCm)
                .mapToInt(this::safeLength)
                .max()
                .orElse(safeLength(null));
        return Math.max(max, 1);
    }

    private int packageWidth(List<ShippingFeeCalculationRequest.ShippingItemRequest> items) {
        int max = items.stream().map(ShippingFeeCalculationRequest.ShippingItemRequest::getWidthCm)
                .mapToInt(this::safeWidth)
                .max()
                .orElse(safeWidth(null));
        return Math.max(max, 1);
    }

    private int packageHeight(List<ShippingFeeCalculationRequest.ShippingItemRequest> items) {
        int sum = items.stream()
                .mapToInt(item -> safeHeight(item.getHeightCm()) * safeQuantity(item.getQuantity()))
                .sum();
        return Math.max(sum, safeHeight(null));
    }

    private int safeQuantity(Integer quantity) {
        return quantity == null || quantity <= 0 ? 1 : quantity;
    }

    private int safeWeight(Integer weight) {
        int fallback = ghnProperties.getDefaultWeightGrams() == null ? 1000 : ghnProperties.getDefaultWeightGrams();
        return weight == null || weight <= 0 ? fallback : weight;
    }

    private int safeLength(Integer value) {
        int fallback = ghnProperties.getDefaultLengthCm() == null ? 35 : ghnProperties.getDefaultLengthCm();
        return value == null || value <= 0 ? fallback : value;
    }

    private int safeWidth(Integer value) {
        int fallback = ghnProperties.getDefaultWidthCm() == null ? 10 : ghnProperties.getDefaultWidthCm();
        return value == null || value <= 0 ? fallback : value;
    }

    private int safeHeight(Integer value) {
        int fallback = ghnProperties.getDefaultHeightCm() == null ? 5 : ghnProperties.getDefaultHeightCm();
        return value == null || value <= 0 ? fallback : value;
    }

    private int safeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return 0;
        }
        return amount.setScale(0, java.math.RoundingMode.HALF_UP).intValueExact();
    }

    private String normalizeAddressName(String value) {
        String normalized = Normalizer.normalize(safeValue(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);

        normalized = normalized
                .replaceAll("\\btp\\.?\\s*", "thanh pho ")
                .replaceAll("\\bq\\.?\\s*(\\d+)\\b", "quan $1")
                .replaceAll("\\bp\\.?\\s*(\\d+)\\b", "phuong $1")
                .replace("tphcm", "thanh pho ho chi minh")
                .replace("tp hcm", "thanh pho ho chi minh")
                .replace("hcm", "ho chi minh");

        return normalized
                .replace("thanh pho", "")
                .replace("tinh", "")
                .replace("quan", "")
                .replace("huyen", "")
                .replace("district", "")
                .replace("thi xa", "")
                .replace("thi tran", "")
                .replace("phuong", "")
                .replace("ward", "")
                .replace("xa", "")
                .replaceAll("[^a-z0-9]", "");
    }

    private Integer parseStrictInteger(String value) {
        String raw = safeValue(value);
        if (raw.isBlank() || !raw.matches("\\d+")) {
            return null;
        }
        return Integer.parseInt(raw);
    }

    private int requirePositive(String key, Integer value) {
        if (value == null || value <= 0) {
            throw new IllegalStateException("Thiếu cấu hình hợp lệ cho " + key + ".");
        }
        return value;
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildDetailedAddress(UserAddress address) {
        return String.join(", ",
                safeValue(address.getSpecificAddress()),
                safeValue(address.getWard()),
                safeValue(address.getDistrict()),
                safeValue(address.getProvince()));
    }

    private record ResolvedAddress(Integer provinceId, Integer districtId, String wardCode) {
    }
}
