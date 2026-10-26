package com.campus.lostandfound;

/** Public-safe view of an Item. Never contains the secret answer. */
public record ItemView(
        Long id,
        String type,
        String title,
        String category,
        String location,
        String contactInfo,
        String securityQuestion,
        String status,
        int attemptsLeft) {

    public static final int MAX_ATTEMPTS = 3;

    public static ItemView of(Item i) {
        boolean hideContact = "FOUND".equalsIgnoreCase(i.getType())
                && "OPEN".equalsIgnoreCase(i.getStatus());
        return new ItemView(
                i.getId(), i.getType(), i.getTitle(), i.getCategory(), i.getLocation(),
                hideContact ? "Hidden until security question is answered" : i.getContactInfo(),
                i.getSecurityQuestion(), i.getStatus(),
                Math.max(0, MAX_ATTEMPTS - i.getFailedAttempts()));
    }
}
