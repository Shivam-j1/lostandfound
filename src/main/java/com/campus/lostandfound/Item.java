package com.campus.lostandfound;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "ITEMS")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Report type is required")
    @Pattern(regexp = "(?i)LOST|FOUND", message = "Type must be LOST or FOUND")
    private String type;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be 3 to 100 characters")
    private String title;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Location is required")
    @Size(max = 150, message = "Location is too long")
    private String location;

    @NotBlank(message = "Contact info is required")
    @Size(max = 100, message = "Contact info is too long")
    private String contactInfo;

    @Size(max = 200, message = "Question is too long")
    private String securityQuestion;

    // Write-only: accepted from the client, NEVER sent back in any JSON response
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 100, message = "Answer is too long")
    private String securityAnswer;

    private String status = "OPEN";     // OPEN, CLAIMED, LOCKED
    private int failedAttempts = 0;

    public Item() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public String getSecurityQuestion() { return securityQuestion; }
    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }

    public String getSecurityAnswer() { return securityAnswer; }
    public void setSecurityAnswer(String securityAnswer) { this.securityAnswer = securityAnswer; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
}
