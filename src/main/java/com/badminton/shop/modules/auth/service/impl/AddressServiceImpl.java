package com.badminton.shop.modules.auth.service.impl;

import com.badminton.shop.modules.auth.dto.AddressRequest;
import com.badminton.shop.modules.auth.dto.AddressResponse;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.entity.UserAddress;
import com.badminton.shop.modules.auth.repository.UserAddressRepository;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.auth.service.AddressService;
import com.badminton.shop.modules.shipping.dto.response.DistrictResponse;
import com.badminton.shop.modules.shipping.dto.response.ProvinceResponse;
import com.badminton.shop.modules.shipping.dto.response.WardResponse;
import com.badminton.shop.modules.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ShippingService shippingService;

    @Override
    @Transactional
    public AddressResponse createAddress(String email, AddressRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        ResolvedAddressMetadata resolved = resolveAddressMetadata(request);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            resetDefaultAddress(user.getId());
        }

        UserAddress address = UserAddress.builder()
            .receiverName(safeValue(request.getReceiverName()))
            .phoneNumber(safeValue(request.getPhoneNumber()))
            .province(resolved.provinceName())
            .district(resolved.districtName())
            .ward(resolved.wardName())
            .specificAddress(safeValue(request.getSpecificAddress()))
            .ghnProvinceId(resolved.provinceId())
            .ghnDistrictId(resolved.districtId())
            .ghnWardCode(resolved.wardCode())
                .isDefault(request.getIsDefault())
                .user(user)
                .build();

        return mapToResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long addressId, String email, AddressRequest request) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        if (!address.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền cập nhật địa chỉ này");
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            resetDefaultAddress(address.getUser().getId());
        }

        ResolvedAddressMetadata resolved = resolveAddressMetadata(request);

        address.setReceiverName(safeValue(request.getReceiverName()));
        address.setPhoneNumber(safeValue(request.getPhoneNumber()));
        address.setProvince(resolved.provinceName());
        address.setDistrict(resolved.districtName());
        address.setWard(resolved.wardName());
        address.setSpecificAddress(safeValue(request.getSpecificAddress()));
        address.setGhnProvinceId(resolved.provinceId());
        address.setGhnDistrictId(resolved.districtId());
        address.setGhnWardCode(resolved.wardCode());
        address.setIsDefault(request.getIsDefault());

        return mapToResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId, String email) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        if (!address.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền xóa địa chỉ này");
        }

        addressRepository.delete(address);
    }

    @Override
    public List<AddressResponse> getAllAddresses(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return addressRepository.findAllByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse getAddressById(Long addressId, String email) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        if (!address.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền xem địa chỉ này");
        }

        return mapToResponse(address);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long addressId, String email) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        if (!address.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền thao tác trên địa chỉ này");
        }

        resetDefaultAddress(address.getUser().getId());
        address.setIsDefault(true);
        addressRepository.save(address);
    }

    private void resetDefaultAddress(Long userId) {
        List<UserAddress> addresses = addressRepository.findAllByUserId(userId);
        addresses.forEach(a -> a.setIsDefault(false));
        addressRepository.saveAll(addresses);
    }

    private AddressResponse mapToResponse(UserAddress address) {
        return AddressResponse.builder()
                .id(address.getId())
                .receiverName(address.getReceiverName())
                .phoneNumber(address.getPhoneNumber())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .specificAddress(address.getSpecificAddress())
                .ghnProvinceId(address.getGhnProvinceId())
                .ghnDistrictId(address.getGhnDistrictId())
                .ghnWardCode(address.getGhnWardCode())
                .isDefault(address.getIsDefault())
                .build();
    }

    private ResolvedAddressMetadata resolveAddressMetadata(AddressRequest request) {
        String provinceInput = safeValue(request.getProvince());
        String districtInput = safeValue(request.getDistrict());
        String wardInput = safeValue(request.getWard());

        ProvinceResponse province = selectBestMatch(
                shippingService.getProvinces(),
                provinceInput,
                ProvinceResponse::getProvinceName,
                "Tỉnh/Thành không hợp lệ theo danh mục GHN: " + provinceInput);

        List<DistrictResponse> districts = shippingService.getDistricts(province.getProvinceId());
        DistrictResponse district = findDistrict(districts, districtInput, provinceInput);

        List<WardResponse> wards = shippingService.getWards(district.getDistrictId());
        WardResponse ward = findWard(wards, wardInput, district.getDistrictName());

        return new ResolvedAddressMetadata(
                province.getProvinceId(),
                province.getProvinceName(),
                district.getDistrictId(),
                district.getDistrictName(),
                ward.getWardCode(),
                ward.getWardName());
    }

    private DistrictResponse findDistrict(List<DistrictResponse> districts, String districtInput, String provinceName) {
        Integer districtId = parseStrictInteger(districtInput);
        if (districtId != null) {
            return districts.stream()
                    .filter(d -> districtId.equals(d.getDistrictId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Quận/Huyện không tồn tại trong GHN cho tỉnh " + provinceName + ": " + districtInput));
        }

        return selectBestMatch(
                districts,
                districtInput,
                DistrictResponse::getDistrictName,
                "Quận/Huyện không hợp lệ theo danh mục GHN: " + districtInput);
    }

    private WardResponse findWard(List<WardResponse> wards, String wardInput, String districtName) {
        WardResponse byCode = wards.stream()
                .filter(w -> safeValue(w.getWardCode()).equalsIgnoreCase(wardInput))
                .findFirst()
                .orElse(null);
        if (byCode != null) {
            return byCode;
        }

        return selectBestMatch(
                wards,
                wardInput,
                WardResponse::getWardName,
                "Phường/Xã không hợp lệ theo danh mục GHN cho quận/huyện " + districtName + ": " + wardInput);
    }

    private <T> T selectBestMatch(List<T> options, String input, Function<T, String> nameExtractor, String errorMessage) {
        String normalizedInput = normalizeAddressName(input);
        if (normalizedInput.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
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

        throw new IllegalArgumentException(errorMessage);
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

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private record ResolvedAddressMetadata(
            Integer provinceId,
            String provinceName,
            Integer districtId,
            String districtName,
            String wardCode,
            String wardName) {
    }
}
