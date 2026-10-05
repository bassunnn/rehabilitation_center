package ru.mirea.project;

import ru.mirea.project.repository.PatientRepository;
import ru.mirea.project.repository.ProcedureRepository;
import ru.mirea.project.repository.RehabilitationPlanRepository;
import ru.mirea.project.repository.TherapistRepository;
import ru.mirea.project.service.PatientService;
import ru.mirea.project.service.ProcedureService;
import ru.mirea.project.service.RehabilitationPlanService;
import ru.mirea.project.service.TherapistService;
import ru.mirea.project.ui.ConsoleUi;

public class Main {
    public static void main(String[] args) {
        PatientRepository patientRepository = new PatientRepository();
        TherapistRepository therapistRepository = new TherapistRepository();
        ProcedureRepository procedureRepository = new ProcedureRepository();
        RehabilitationPlanRepository planRepository = new RehabilitationPlanRepository();

        PatientService patientService = new PatientService(patientRepository);
        TherapistService therapistService = new TherapistService(therapistRepository);
        ProcedureService procedureService = new ProcedureService(procedureRepository, patientRepository, therapistRepository);
        RehabilitationPlanService planService = new RehabilitationPlanService(planRepository, patientRepository, therapistRepository);

        new ConsoleUi(patientService, therapistService, procedureService, planService).run();
    }
}
