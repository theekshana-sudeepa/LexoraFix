package com.lexoratech.lexorafix.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a service booking made by a customer.
 * Lombok generates all getters and setters automatically.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBooking {
    private int id;
    private int userId;
    private String orderId;
    private String serviceType;
    private String date;
    private String time;
    private String notes;
    private String status;
    private String timestamp;
    // Worker details (populated when a worker accepts the booking)
    private String assignedWorkerName;
    private String assignedWorkerPhone;
    private String assignedWorkerEmail;
    
    // Ratings
    private Float rating;
    private String reviewText;
    private Boolean isRated;
}
