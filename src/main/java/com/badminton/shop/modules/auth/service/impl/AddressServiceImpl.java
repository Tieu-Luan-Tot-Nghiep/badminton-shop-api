package com.badminton.shop.modules.auth.service.impl;

import com.badminton.shop.modules.auth.dto.AddressRequest;
import com.badminton.shop.modules.auth.dto.AddressResponse;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.entity.UserAddress;
import com.badminton.shop.modules.auth.repository.UserAddressRepository;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.auth.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AddressResponse createAddress(String email, AddressRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            resetDefaultAddress(user.getId());
        }

        UserAddress address = UserAddress.builder()
                .receiverName(request.getReceiverName())
                .phoneNumber(request.getPhoneNumber())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .specificAddress(request.getSpecificAddress())
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

        address.setReceiverName(request.getReceiverName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setSpecificAddress(request.getSpecificAddress());
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
                .isDefault(address.getIsDefault())
                .build();
    }
}
