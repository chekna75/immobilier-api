package com.ditsolution.features.messaging.dto;

import jakarta.validation.constraints.NotNull;

public class CreateConversationRequest {
    
    @NotNull(message = "L'ID de la propriété est requis")
    private String propertyId;
    
    private String initialMessage;
    
    // Constructeurs
    public CreateConversationRequest() {}
    
    public CreateConversationRequest(String propertyId, String initialMessage) {
        this.propertyId = propertyId;
        this.initialMessage = initialMessage;
    }
    
    // Getters et Setters
    public String getPropertyId() {
        return propertyId;
    }
    
    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }
    
    public String getInitialMessage() {
        return initialMessage;
    }
    
    public void setInitialMessage(String initialMessage) {
        this.initialMessage = initialMessage;
    }
}
