package com.badminton.shop.modules.auth.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.auth.dto.AddressRequest;
import com.badminton.shop.modules.auth.dto.AddressResponse;
import com.badminton.shop.modules.auth.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@Valid @RequestBody AddressRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED,
                        "Address created successfully.",
                        addressService.createAddress(principal.getName(), request)
                ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Address updated successfully.",
                addressService.updateAddress(id, principal.getName(), request)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteAddress(@PathVariable Long id, Principal principal) {
        addressService.deleteAddress(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully.", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAllAddresses(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Addresses fetched successfully.",
                addressService.getAllAddresses(principal.getName())
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Address fetched successfully.",
                addressService.getAddressById(id, principal.getName())
        ));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Object>> setDefaultAddress(@PathVariable Long id, Principal principal) {
        addressService.setDefaultAddress(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Default address updated successfully.", null));
    }
}
