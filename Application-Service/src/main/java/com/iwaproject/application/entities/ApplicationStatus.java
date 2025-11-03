package com.iwaproject.application.entities;

/**
 * Enumeration of application statuses.
 */
public enum ApplicationStatus {
    /**
     * Application has been sent.
     */
    SENT("envoyee"),

    /**
     * Application has been accepted.
     */
    ACCEPTED("acceptee"),

    /**
     * Application has been refused.
     */
    REFUSED("refusee");

    /**
     * French status value.
     */
    private final String value;

    /**
     * Constructor for ApplicationStatus.
     *
     * @param statusValue the French status value
     */
    ApplicationStatus(final String statusValue) {
        this.value = statusValue;
    }

    /**
     * Gets the French status value.
     *
     * @return the status value
     */
    public String getValue() {
        return value;
    }

    /**
     * Converts a French status value to an ApplicationStatus.
     *
     * @param statusValue the French status value
     * @return the corresponding ApplicationStatus
     * @throws IllegalArgumentException if the value is unknown
     */
    public static ApplicationStatus fromValue(final String statusValue) {
        for (ApplicationStatus status : ApplicationStatus.values()) {
            if (status.value.equalsIgnoreCase(statusValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + statusValue);
    }
}
