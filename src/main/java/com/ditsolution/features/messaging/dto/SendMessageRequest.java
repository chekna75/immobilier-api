package com.ditsolution.features.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SendMessageRequest {
    
    @NotBlank(message = "Le contenu du message est requis")
    private String content;
    
    private String messageType = "TEXT";
    
    // Constructeurs
    public SendMessageRequest() {}
    
    public SendMessageRequest(String content) {
        this.content = content;
    }
    
    public SendMessageRequest(String content, String messageType) {
        this.content = content;
        this.messageType = messageType;
    }
    
    // Getters et Setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
