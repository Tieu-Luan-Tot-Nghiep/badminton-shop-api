package com.badminton.shop.modules.shipping.service.impl;

import com.badminton.shop.modules.auth.entity.UserAddress;
import com.badminton.shop.modules.shipping.config.GHNProperties;
import com.badminton.shop.modules.shipping.dto.ghn.GHNShippingFeeRequest;
import com.badminton.shop.modules.shipping.dto.request.ShippingFeeCalculationRequest;
import com.badminton.shop.modules.shipping.dto.response.DistrictResponse;
import com.badminton.shop.modules.shipping.dto.response.ProvinceResponse;
import com.badminton.shop.modules.shipping.dto.response.WardResponse;
import com.badminton.shop.modules.shipping.service.ShippingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingServiceImplTest {

    @Mock
    private ShippingProvider shippingProvider;

    private GHNProperties ghnProperties;
    private ShippingServiceImpl shippingService;

    @BeforeEach
    void setUp() {
        ghnProperties = new GHNProperties();
        ghnProperties.setFromDistrictId(1444);
        ghnProperties.setServiceTypeId(2);
        ghnProperties.setDefaultWeightGrams(1000);
        ghnProperties.setDefaultLengthCm(35);
        ghnProperties.setDefaultWidthCm(10);
        ghnProperties.setDefaultHeightCm(5);

        shippingService = new ShippingServiceImpl(shippingProvider, ghnProperties);
    }

    @Test
    void calculateShippingFee_MapsAddressByNameAndUsesResolvedIds() {
        UserAddress address = UserAddress.builder()
                .province("TP. HCM")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .specificAddress("123 Nguyen Hue")
                .build();

        ShippingFeeCalculationRequest request = ShippingFeeCalculationRequest.builder()
                .address(address)
                .insuranceValue(BigDecimal.valueOf(1500000))
                .items(List.of(
                        ShippingFeeCalculationRequest.ShippingItemRequest.builder()
                                .name("Racquet")
                                .sku("RACKET-01")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(1500000))
                                .weightGrams(1200)
                                .lengthCm(68)
                                .widthCm(24)
                                .heightCm(6)
                                .build()
                ))
                .build();

        when(shippingProvider.getProvinces()).thenReturn(List.of(
                ProvinceResponse.builder().provinceId(79).provinceName("Ho Chi Minh").build()
        ));
        when(shippingProvider.getDistricts(79)).thenReturn(List.of(
                DistrictResponse.builder().districtId(1442).districtName("Binh Thanh").provinceId(79).build(),
                DistrictResponse.builder().districtId(1444).districtName("District 1").provinceId(79).build()
        ));
        when(shippingProvider.getWards(1444)).thenReturn(List.of(
                WardResponse.builder().wardCode("20108").wardName("Ben Nghe").districtId(1444).build()
        ));
        when(shippingProvider.calculateShippingFee(any())).thenReturn(BigDecimal.valueOf(35000));

        BigDecimal fee = shippingService.calculateShippingFee(request);

        assertEquals(BigDecimal.valueOf(35000), fee);

        ArgumentCaptor<GHNShippingFeeRequest> captor = ArgumentCaptor.forClass(GHNShippingFeeRequest.class);
        verify(shippingProvider).calculateShippingFee(captor.capture());

        GHNShippingFeeRequest mapped = captor.getValue();
        assertEquals(1444, mapped.getToDistrictId());
        assertEquals("20108", mapped.getToWardCode());
        assertEquals(1444, mapped.getFromDistrictId());
        assertEquals(1200, mapped.getWeight());
        assertEquals(68, mapped.getLength());
        assertEquals(24, mapped.getWidth());
        assertEquals(6, mapped.getHeight());

        verify(shippingProvider).getDistricts(eq(79));
        verify(shippingProvider).getWards(eq(1444));
    }

    @Test
    void calculateShippingFee_UsesFallbackDimensionsWhenVariantDataMissing() {
        UserAddress address = UserAddress.builder()
                .province("Ho Chi Minh")
                .district("District 1")
                .ward("Ben Nghe")
                .specificAddress("123 Nguyen Hue")
                .build();

        ShippingFeeCalculationRequest request = ShippingFeeCalculationRequest.builder()
                .address(address)
                .insuranceValue(BigDecimal.valueOf(800000))
                .items(List.of(
                        ShippingFeeCalculationRequest.ShippingItemRequest.builder()
                                .name("Racquet")
                                .sku("RACKET-02")
                                .quantity(2)
                                .unitPrice(BigDecimal.valueOf(400000))
                                .weightGrams(null)
                                .lengthCm(null)
                                .widthCm(null)
                                .heightCm(null)
                                .build()
                ))
                .build();

        when(shippingProvider.getProvinces()).thenReturn(List.of(
                ProvinceResponse.builder().provinceId(79).provinceName("Ho Chi Minh").build()
        ));
        when(shippingProvider.getDistricts(79)).thenReturn(List.of(
                DistrictResponse.builder().districtId(1444).districtName("District 1").provinceId(79).build()
        ));
        when(shippingProvider.getWards(1444)).thenReturn(List.of(
                WardResponse.builder().wardCode("20108").wardName("Ben Nghe").districtId(1444).build()
        ));
        when(shippingProvider.calculateShippingFee(any())).thenReturn(BigDecimal.valueOf(30000));

        shippingService.calculateShippingFee(request);

        ArgumentCaptor<GHNShippingFeeRequest> captor = ArgumentCaptor.forClass(GHNShippingFeeRequest.class);
        verify(shippingProvider).calculateShippingFee(captor.capture());

        GHNShippingFeeRequest mapped = captor.getValue();
        assertEquals(2000, mapped.getWeight());
        assertEquals(35, mapped.getLength());
        assertEquals(10, mapped.getWidth());
        assertEquals(10, mapped.getHeight());
    }

    @Test
    void calculateShippingFee_ComputesPackageFromMultipleItems() {
        UserAddress address = UserAddress.builder()
                .province("Ho Chi Minh")
                .district("District 1")
                .ward("Ben Nghe")
                .specificAddress("123 Nguyen Hue")
                .build();

        ShippingFeeCalculationRequest request = ShippingFeeCalculationRequest.builder()
                .address(address)
                .insuranceValue(BigDecimal.valueOf(2100000))
                .items(List.of(
                        ShippingFeeCalculationRequest.ShippingItemRequest.builder()
                                .name("Racquet A")
                                .sku("RAC-A")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(900000))
                                .weightGrams(1000)
                                .lengthCm(70)
                                .widthCm(22)
                                .heightCm(5)
                                .build(),
                        ShippingFeeCalculationRequest.ShippingItemRequest.builder()
                                .name("Racquet B")
                                .sku("RAC-B")
                                .quantity(2)
                                .unitPrice(BigDecimal.valueOf(600000))
                                .weightGrams(800)
                                .lengthCm(68)
                                .widthCm(24)
                                .heightCm(6)
                                .build()
                ))
                .build();

        when(shippingProvider.getProvinces()).thenReturn(List.of(
                ProvinceResponse.builder().provinceId(79).provinceName("Ho Chi Minh").build()
        ));
        when(shippingProvider.getDistricts(79)).thenReturn(List.of(
                DistrictResponse.builder().districtId(1444).districtName("District 1").provinceId(79).build()
        ));
        when(shippingProvider.getWards(1444)).thenReturn(List.of(
                WardResponse.builder().wardCode("20108").wardName("Ben Nghe").districtId(1444).build()
        ));
        when(shippingProvider.calculateShippingFee(any())).thenReturn(BigDecimal.valueOf(45000));

        shippingService.calculateShippingFee(request);

        ArgumentCaptor<GHNShippingFeeRequest> captor = ArgumentCaptor.forClass(GHNShippingFeeRequest.class);
        verify(shippingProvider).calculateShippingFee(captor.capture());

        GHNShippingFeeRequest mapped = captor.getValue();
        assertEquals(2600, mapped.getWeight());
        assertEquals(70, mapped.getLength());
        assertEquals(24, mapped.getWidth());
        assertEquals(17, mapped.getHeight());
        assertEquals(2, mapped.getItems().size());
    }

    @Test
    void calculateShippingFee_AcceptsAbbreviatedDistrictAndWardNames() {
        UserAddress address = UserAddress.builder()
                .province("TP.HCM")
                .district("Q1")
                .ward("P. Bến Nghé")
                .specificAddress("123 Nguyen Hue")
                .build();

        ShippingFeeCalculationRequest request = ShippingFeeCalculationRequest.builder()
                .address(address)
                .insuranceValue(BigDecimal.valueOf(500000))
                .items(List.of(
                        ShippingFeeCalculationRequest.ShippingItemRequest.builder()
                                .name("Racquet")
                                .sku("RACKET-03")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(500000))
                                .weightGrams(900)
                                .lengthCm(65)
                                .widthCm(22)
                                .heightCm(5)
                                .build()
                ))
                .build();

        when(shippingProvider.getProvinces()).thenReturn(List.of(
                ProvinceResponse.builder().provinceId(79).provinceName("Ho Chi Minh").build()
        ));
        when(shippingProvider.getDistricts(79)).thenReturn(List.of(
                DistrictResponse.builder().districtId(1444).districtName("District 1").provinceId(79).build()
        ));
        when(shippingProvider.getWards(1444)).thenReturn(List.of(
                WardResponse.builder().wardCode("20108").wardName("Ben Nghe").districtId(1444).build()
        ));
        when(shippingProvider.calculateShippingFee(any())).thenReturn(BigDecimal.valueOf(32000));

        BigDecimal fee = shippingService.calculateShippingFee(request);

        assertEquals(BigDecimal.valueOf(32000), fee);
        ArgumentCaptor<GHNShippingFeeRequest> captor = ArgumentCaptor.forClass(GHNShippingFeeRequest.class);
        verify(shippingProvider).calculateShippingFee(captor.capture());
        assertEquals(1444, captor.getValue().getToDistrictId());
        assertEquals("20108", captor.getValue().getToWardCode());
    }

    @Test
    void calculateShippingFee_ThrowsBadRequestWhenDistrictCannotBeMapped() {
        UserAddress address = UserAddress.builder()
                .province("Ho Chi Minh")
                .district("District Unknown")
                .ward("Ben Nghe")
                .specificAddress("123 Nguyen Hue")
                .build();

        ShippingFeeCalculationRequest request = ShippingFeeCalculationRequest.builder()
                .address(address)
                .insuranceValue(BigDecimal.valueOf(100000))
                .items(List.of(
                        ShippingFeeCalculationRequest.ShippingItemRequest.builder()
                                .name("Grip")
                                .sku("GRIP-01")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(100000))
                                .build()
                ))
                .build();

        when(shippingProvider.getProvinces()).thenReturn(List.of(
                ProvinceResponse.builder().provinceId(79).provinceName("Ho Chi Minh").build()
        ));
        when(shippingProvider.getDistricts(79)).thenReturn(List.of(
                DistrictResponse.builder().districtId(1444).districtName("District 1").provinceId(79).build()
        ));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> shippingService.calculateShippingFee(request));

        assertTrue(ex.getMessage().contains("Không map được quận/huyện GHN"));
        verify(shippingProvider, never()).calculateShippingFee(any());
    }

        @Test
        void calculateShippingFee_UsesCachedGhnMetadataWithoutMasterDataLookup() {
                UserAddress address = UserAddress.builder()
                                .province("Ho Chi Minh")
                                .district("District 1")
                                .ward("Ben Nghe")
                                .specificAddress("123 Nguyen Hue")
                                .ghnProvinceId(202)
                                .ghnDistrictId(1444)
                                .ghnWardCode("20108")
                                .build();

                ShippingFeeCalculationRequest request = ShippingFeeCalculationRequest.builder()
                                .address(address)
                                .insuranceValue(BigDecimal.valueOf(300000))
                                .items(List.of(
                                                ShippingFeeCalculationRequest.ShippingItemRequest.builder()
                                                                .name("Bag")
                                                                .sku("BAG-01")
                                                                .quantity(1)
                                                                .unitPrice(BigDecimal.valueOf(300000))
                                                                .weightGrams(700)
                                                                .lengthCm(40)
                                                                .widthCm(20)
                                                                .heightCm(10)
                                                                .build()
                                ))
                                .build();

                when(shippingProvider.calculateShippingFee(any())).thenReturn(BigDecimal.valueOf(28000));

                BigDecimal fee = shippingService.calculateShippingFee(request);

                assertEquals(BigDecimal.valueOf(28000), fee);

                ArgumentCaptor<GHNShippingFeeRequest> captor = ArgumentCaptor.forClass(GHNShippingFeeRequest.class);
                verify(shippingProvider).calculateShippingFee(captor.capture());
                GHNShippingFeeRequest mapped = captor.getValue();
                assertEquals(1444, mapped.getToDistrictId());
                assertEquals("20108", mapped.getToWardCode());

                verify(shippingProvider, never()).getProvinces();
                verify(shippingProvider, never()).getDistricts(any());
                verify(shippingProvider, never()).getWards(any());
        }
}
