package ru.mirea.project.model;

public enum TherapistSpecialization {
    PHYSIOTHERAPIST("Физиотерапевт"),
    MASSAGE_THERAPIST("Массажист"),
    SPEECH_THERAPIST("Логопед"),
    OCCUPATIONAL_THERAPIST("Эрготерапевт"),
    PSYCHOLOGIST("Психолог");

    private final String title;

    TherapistSpecialization(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
