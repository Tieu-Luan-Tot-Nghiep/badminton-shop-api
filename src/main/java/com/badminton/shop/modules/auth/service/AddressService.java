package com.badminton.shop.modules.auth.service;

import com.badminton.shop.modules.auth.dto.AddressRequest;
import com.badminton.shop.modules.auth.dto.AddressResponse;

import java.util.List;

public interface AddressService {
    AddressResponse createAddress(String email, AddressRequest request);
    AddressResponse updateAddress(Long addressId, String email, AddressRequest request);
    void deleteAddress(Long addressId, String email);
    List<AddressResponse> getAllAddresses(String email);
    AddressResponse getAddressById(Long addressId, String email);
    void setDefaultAddress(Long addressId, String email);
}
