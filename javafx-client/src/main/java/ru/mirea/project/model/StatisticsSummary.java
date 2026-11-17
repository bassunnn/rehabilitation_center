package ru.mirea.project.model;

import java.util.EnumMap;
import java.util.Map;

public class StatisticsSummary {
    private int totalPatients;
    private int totalProcedures;
    private Map<ProcedureStatus, Integer> proceduresByStatus = new EnumMap<>(ProcedureStatus.class);
    private double completionRate;
    private double averagePatientAge;
    private int minPatientAge;
    private int maxPatientAge;
    private int proceduresThisMonth;
    private double averageProceduresPerPatient;

    public int getTotalPatients() {
        return totalPatients;
    }

    public void setTotalPatients(int totalPatients) {
        this.totalPatients = totalPatients;
    }

    public int getTotalProcedures() {
        return totalProcedures;
    }

    public void setTotalProcedures(int totalProcedures) {
        this.totalProcedures = totalProcedures;
    }

    public Map<ProcedureStatus, Integer> getProceduresByStatus() {
        return proceduresByStatus;
    }

    public void setProceduresByStatus(Map<ProcedureStatus, Integer> proceduresByStatus) {
        this.proceduresByStatus = proceduresByStatus;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public double getAveragePatientAge() {
        return averagePatientAge;
    }

    public void setAveragePatientAge(double averagePatientAge) {
        this.averagePatientAge = averagePatientAge;
    }

    public int getMinPatientAge() {
        return minPatientAge;
    }

    public void setMinPatientAge(int minPatientAge) {
        this.minPatientAge = minPatientAge;
    }

    public int getMaxPatientAge() {
        return maxPatientAge;
    }

    public void setMaxPatientAge(int maxPatientAge) {
        this.maxPatientAge = maxPatientAge;
    }

    public int getProceduresThisMonth() {
        return proceduresThisMonth;
    }

    public void setProceduresThisMonth(int proceduresThisMonth) {
        this.proceduresThisMonth = proceduresThisMonth;
    }

    public double getAverageProceduresPerPatient() {
        return averageProceduresPerPatient;
    }

    public void setAverageProceduresPerPatient(double averageProceduresPerPatient) {
        this.averageProceduresPerPatient = averageProceduresPerPatient;
    }

    public int getCountForStatus(ProcedureStatus status) {
        return proceduresByStatus.getOrDefault(status, 0);
    }
}

