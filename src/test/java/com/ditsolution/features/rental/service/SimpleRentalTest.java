package com.ditsolution.features.rental.service;

import com.ditsolution.features.rental.dto.CreateContractRequest;
import com.ditsolution.features.rental.dto.InitiatePaymentRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleRentalTest {

    @Test
    void testCreateContractRequest() {
        // Test de création d'une requête de contrat
        CreateContractRequest request = new CreateContractRequest();
        request.setPropertyId(UUID.randomUUID());
        request.setTenantId(UUID.randomUUID());
        request.setMonthlyRent(new BigDecimal("1200.00"));
        request.setPaymentDueDay(5);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusYears(1));

        // Vérifications
        assertNotNull(request);
        assertNotNull(request.getPropertyId());
        assertNotNull(request.getTenantId());
        assertEquals(new BigDecimal("1200.00"), request.getMonthlyRent());
        assertEquals(5, request.getPaymentDueDay());
        assertNotNull(request.getStartDate());
        assertNotNull(request.getEndDate());
    }

    @Test
    void testInitiatePaymentRequest() {
        // Test de création d'une requête de paiement
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setRentPaymentId(1L);
        request.setPaymentMethod("CARD");
        request.setPhoneNumber("+33123456789");
        request.setOperator("ORANGE");
        request.setClientIp("192.168.1.1");
        request.setUserAgent("Mozilla/5.0");

        // Vérifications
        assertNotNull(request);
        assertEquals(1L, request.getRentPaymentId());
        assertEquals("CARD", request.getPaymentMethod());
        assertEquals("+33123456789", request.getPhoneNumber());
        assertEquals("ORANGE", request.getOperator());
        assertEquals("192.168.1.1", request.getClientIp());
        assertEquals("Mozilla/5.0", request.getUserAgent());
    }

    @Test
    void testBigDecimalOperations() {
        // Test des opérations BigDecimal
        BigDecimal amount1 = new BigDecimal("1200.00");
        BigDecimal amount2 = new BigDecimal("100.00");
        BigDecimal total = amount1.add(amount2);

        assertEquals(new BigDecimal("1300.00"), total);
        assertTrue(amount1.compareTo(amount2) > 0);
    }

    @Test
    void testLocalDateOperations() {
        // Test des opérations LocalDate
        LocalDate today = LocalDate.now();
        LocalDate future = today.plusYears(1);
        LocalDate past = today.minusMonths(1);

        assertTrue(future.isAfter(today));
        assertTrue(past.isBefore(today));
        assertEquals(1, future.getYear() - today.getYear());
    }

    @Test
    void testUUIDGeneration() {
        // Test de génération d'UUID
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        assertNotNull(uuid1);
        assertNotNull(uuid2);
        assertNotEquals(uuid1, uuid2);
        assertEquals(36, uuid1.toString().length());
    }
}
