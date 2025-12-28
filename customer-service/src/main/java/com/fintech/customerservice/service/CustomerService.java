package com.fintech.customerservice.service;

import com.fintech.customerservice.domain.Address;
import com.fintech.customerservice.domain.Customer;
import com.fintech.customerservice.dto.request.AddressRequest;
import com.fintech.customerservice.dto.request.CreateCustomerRequest;
import com.fintech.customerservice.dto.request.UpdateCustomerRequest;
import com.fintech.customerservice.exception.CustomerAlreadyExistsException;
import com.fintech.customerservice.exception.CustomerNotFoundException;
import com.fintech.customerservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * Create a new customer profile.
     */
    @Transactional
    public Customer createCustomer(CreateCustomerRequest request) {
        // Check if customer already exists for this user
        if (customerRepository.existsByUserId(request.userId())) {
            throw new CustomerAlreadyExistsException(
                    "Customer profile already exists for user: " + request.userId()
            );
        }

        // Check phone number uniqueness if provided
        if (request.phoneNumber() != null &&
            customerRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new CustomerAlreadyExistsException(
                    "Phone number already in use: " + request.phoneNumber()
            );
        }

        Customer customer = Customer.builder()
                .userId(request.userId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Created customer profile {} for user {}", saved.getId(), request.userId());
        return saved;
    }

    /**
     * Get customer by ID.
     */
    @Transactional(readOnly = true)
    public Customer getCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found: " + customerId
                ));
    }

    /**
     * Get customer by user ID (from Auth Service).
     */
    @Transactional(readOnly = true)
    public Customer getCustomerByUserId(UUID userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found for user: " + userId
                ));
    }

    /**
     * Update customer profile.
     */
    @Transactional
    public Customer updateCustomer(UUID customerId, UpdateCustomerRequest request) {
        Customer customer = getCustomer(customerId);

        if (request.firstName() != null) {
            customer.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            customer.setLastName(request.lastName());
        }
        if (request.phoneNumber() != null) {
            // Check uniqueness if changing phone
            if (!request.phoneNumber().equals(customer.getPhoneNumber()) &&
                customerRepository.existsByPhoneNumber(request.phoneNumber())) {
                throw new CustomerAlreadyExistsException(
                        "Phone number already in use: " + request.phoneNumber()
                );
            }
            customer.setPhoneNumber(request.phoneNumber());
        }
        if (request.dateOfBirth() != null) {
            customer.setDateOfBirth(request.dateOfBirth());
        }
        if (request.address() != null) {
            customer.setAddress(mapAddress(request.address()));
        }

        Customer saved = customerRepository.save(customer);
        log.info("Updated customer profile {}", customerId);
        return saved;
    }

    /**
     * Update KYC status (admin operation).
     */
    @Transactional
    public Customer updateKycStatus(UUID customerId, Customer.KycStatus status) {
        Customer customer = getCustomer(customerId);
        customer.setKycStatus(status);
        Customer saved = customerRepository.save(customer);
        log.info("Updated KYC status for customer {} to {}", customerId, status);
        return saved;
    }

    private Address mapAddress(AddressRequest request) {
        return Address.builder()
                .street(request.street())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country())
                .build();
    }
}

