package ru.mirea.rehab.filter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.rehab.domain.Department;
import ru.mirea.rehab.domain.Patient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Создает параметризованную JPA Specification из разобранного фильтра. */
public final class PatientSpecifications {
    private PatientSpecifications() { }

    public static Specification<Patient> from(PatientFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (filter.archived() != null) predicates.add(cb.equal(root.get("archived"), filter.archived()));
            if (filter.status() != null) predicates.add(cb.equal(root.get("status"), filter.status()));
            if (filter.requiresAssistance() != null) predicates.add(cb.equal(root.get("requiresAssistance"), filter.requiresAssistance()));
            if (filter.risk() != null) predicates.add(cb.equal(root.get("riskLevel"), filter.risk()));

            Join<Patient, Department> departmentJoin = root.join("department", JoinType.LEFT);
            if (filter.department() != null) {
                predicates.add(cb.like(cb.lower(departmentJoin.get("code")), "%" + escape(filter.department()) + "%", '\\'));
            }
            if (filter.tag() != null) {
                Join<Patient, String> tagJoin = root.joinSet("tags", JoinType.LEFT);
                predicates.add(cb.equal(cb.lower(tagJoin), filter.tag().toLowerCase(Locale.ROOT)));
            }
            if (filter.freeText() != null && !filter.freeText().isBlank()) {
                String like = "%" + escape(filter.freeText()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("lastName")), like, '\\'),
                        cb.like(cb.lower(root.get("firstName")), like, '\\'),
                        cb.like(cb.lower(root.get("middleName")), like, '\\'),
                        cb.like(cb.lower(root.get("email")), like, '\\'),
                        cb.like(cb.lower(root.get("phone")), like, '\\'),
                        cb.like(cb.lower(root.get("externalId")), like, '\\'),
                        cb.like(cb.lower(departmentJoin.get("code")), like, '\\'),
                        cb.like(cb.lower(departmentJoin.get("name")), like, '\\')));
            }
            filter.scores().forEach(constraint -> predicates.add(switch (constraint.operator()) {
                case LT -> cb.lessThan(root.get("rehabilitationScore"), constraint.value());
                case LTE -> cb.lessThanOrEqualTo(root.get("rehabilitationScore"), constraint.value());
                case EQ -> cb.equal(root.get("rehabilitationScore"), constraint.value());
                case GTE -> cb.greaterThanOrEqualTo(root.get("rehabilitationScore"), constraint.value());
                case GT -> cb.greaterThan(root.get("rehabilitationScore"), constraint.value());
            }));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escape(String value) {
        return value.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

