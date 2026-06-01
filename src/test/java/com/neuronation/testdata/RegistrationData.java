package com.neuronation.testdata;

import java.util.concurrent.atomic.AtomicLong;

public class RegistrationData {
    private static final AtomicLong COUNTER = new AtomicLong();

    private String emailPrefix;
    private String emailDomain;
    private String password;
    private String name;

    public String getEmailPrefix() { return emailPrefix; }
    public String getEmailDomain() { return emailDomain; }
    public String getPassword() { return password; }
    public String getName() { return name; }

    /**
     * Generates a unique email address per test run. Timestamp + atomic counter
     * guarantees uniqueness across parallel threads even when two threads call
     * this within the same millisecond.
     */
    public String generateEmail() {
        return emailPrefix + "+" + System.currentTimeMillis()
                + "-" + COUNTER.incrementAndGet()
                + "@" + emailDomain;
    }
}
