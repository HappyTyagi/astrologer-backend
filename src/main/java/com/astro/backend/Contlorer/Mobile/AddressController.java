package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Entity.Address;
import com.astro.backend.Repositry.AddressRepository;
import com.astro.backend.RequestDTO.CreateAddressRequest;
import com.astro.backend.RequestDTO.UpdateAddressRequest;
import com.astro.backend.ResponseDTO.AddressResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressRepository addressRepository;
    private static final int MAX_ADDRESSES_PER_USER = 5;

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(@Valid @RequestBody CreateAddressRequest request) {
        try {
            final String mobileNumber = request.getUserMobileNumber() == null
                    ? ""
                    : request.getUserMobileNumber().trim();
            if (mobileNumber.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AddressResponse.builder()
                                .status(false)
                                .message("User mobile number is required")
                                .build());
            }

            long addressCount = addressRepository.countByUserMobileNumber(mobileNumber);
            if (addressCount >= MAX_ADDRESSES_PER_USER) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AddressResponse.builder()
                                .status(false)
                                .message("Maximum 5 addresses allowed per user")
                                .build());
            }

            Address address = Address.builder()
                    .userMobileNumber(mobileNumber)
                    .name(request.getName())
                    .addressLine1(request.getAddressLine1())
                    .addressLine2(request.getAddressLine2())
                    .city(request.getCity())
                    .state(request.getState())
                    .district(request.getDistrict())
                    .pincode(request.getPincode())
                    .landmark(request.getLandmark())
                    .addressType(request.getAddressType())
                    .isDefault(request.getIsDefault())
                    .build();

            Address saved = addressRepository.save(Objects.requireNonNull(address, "address"));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toResponse(saved, true, "Address created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AddressResponse.builder()
                            .status(false)
                            .message("Failed to create address: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddressById(@PathVariable Long id) {
        Long addressId = Objects.requireNonNull(id, "id");
        return addressRepository.findById(addressId)
                .map(address -> ResponseEntity.ok(toResponse(address, true, "Address fetched successfully")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AddressResponse.builder()
                                .status(false)
                                .message("Address not found with ID: " + addressId)
                                .build()));
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@RequestParam(value = "userMobileNumber", required = false) String userMobileNumber) {
        List<Address> addresses = (userMobileNumber == null || userMobileNumber.isBlank())
                ? addressRepository.findAll()
                : addressRepository.findByUserMobileNumber(userMobileNumber);

        List<AddressResponse> response = addresses.stream()
                .map(address -> toResponse(address, true, "Address fetched successfully"))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(@PathVariable Long id, @Valid @RequestBody UpdateAddressRequest request) {
        try {
            Long addressId = Objects.requireNonNull(id, "id");
            Address address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("Address not found with ID: " + addressId));

            if (request.getUserMobileNumber() != null && !request.getUserMobileNumber().isBlank()) {
                address.setUserMobileNumber(request.getUserMobileNumber());
            }
            if (request.getName() != null) {
                address.setName(request.getName());
            }
            if (request.getAddressLine1() != null) {
                address.setAddressLine1(request.getAddressLine1());
            }
            if (request.getAddressLine2() != null) {
                address.setAddressLine2(request.getAddressLine2());
            }
            if (request.getCity() != null) {
                address.setCity(request.getCity());
            }
            if (request.getState() != null) {
                address.setState(request.getState());
            }
            if (request.getDistrict() != null) {
                address.setDistrict(request.getDistrict());
            }
            if (request.getPincode() != null) {
                address.setPincode(request.getPincode());
            }
            if (request.getLandmark() != null) {
                address.setLandmark(request.getLandmark());
            }
            if (request.getAddressType() != null) {
                address.setAddressType(request.getAddressType());
            }
            if (request.getIsDefault() != null) {
                address.setIsDefault(request.getIsDefault());
            }

            Address updated = addressRepository.save(Objects.requireNonNull(address, "address"));
            return ResponseEntity.ok(toResponse(updated, true, "Address updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AddressResponse.builder()
                            .status(false)
                            .message("Failed to update address: " + e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AddressResponse> deleteAddress(@PathVariable Long id) {
        try {
            Long addressId = Objects.requireNonNull(id, "id");
            if (!addressRepository.existsById(addressId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AddressResponse.builder()
                                .status(false)
                                .message("Address not found with ID: " + addressId)
                                .build());
            }
            addressRepository.deleteById(addressId);
            return ResponseEntity.ok(AddressResponse.builder()
                    .status(true)
                    .message("Address deleted successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AddressResponse.builder()
                            .status(false)
                            .message("Failed to delete address: " + e.getMessage())
                            .build());
        }
    }

    private AddressResponse toResponse(Address address, boolean status, String message) {
        return AddressResponse.builder()
                .id(address.getId())
                .userMobileNumber(address.getUserMobileNumber())
                .name(address.getName())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .district(address.getDistrict())
                .pincode(address.getPincode())
                .landmark(address.getLandmark())
                .addressType(address.getAddressType())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .status(status)
                .message(message)
                .build();
    }
}
