package com.badminton.shop.modules.auth.controller;

import com.badminton.shop.modules.auth.dto.AddressRequest;
import com.badminton.shop.modules.auth.dto.AddressResponse;
import com.badminton.shop.modules.auth.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<AddressResponse> createAddress(@Valid @RequestBody AddressRequest request, Principal principal) {
        return ResponseEntity.ok(addressService.createAddress(principal.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            Principal principal) {
        return ResponseEntity.ok(addressService.updateAddress(id, principal.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id, Principal principal) {
        addressService.deleteAddress(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAllAddresses(Principal principal) {
        return ResponseEntity.ok(addressService.getAllAddresses(principal.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddressById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(addressService.getAddressById(id, principal.getName()));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<Void> setDefaultAddress(@PathVariable Long id, Principal principal) {
        addressService.setDefaultAddress(id, principal.getName());
        return ResponseEntity.ok().build();
    }
}
