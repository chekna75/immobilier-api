package com.ditsolution.features.messaging.dto;

import jakarta.validation.constraints.NotNull;

public class CreateConversationRequest {
    
    @NotNull(message = "L'ID de la propriété est requis")
    private Long propertyId;
    
    private String initialMessage;
    
    // Constructeurs
    public CreateConversationRequest() {}
    
    public CreateConversationRequest(Long propertyId, String initialMessage) {
        this.propertyId = propertyId;
        this.initialMessage = initialMessage;
    }
    
    // Getters et Setters
    public Long getPropertyId() {
        return propertyId;
    }
    
    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }
    
    public String getInitialMessage() {
        return initialMessage;
    }
    
    public void setInitialMessage(String initialMessage) {
        this.initialMessage = initialMessage;
    }
}
