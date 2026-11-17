package ru.mirea.rehab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Справочная сущность: Отделение реабилитационного центра.
 */
@Entity
@Table(name = "department")
public class Department extends AuditedVersionedEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 200)
    private String name;

    private Integer capacity;

    @Column(nullable = false)
    private boolean active = true;

    protected Department() {
    }

    public Department(UUID id, String code, String name, Integer capacity) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.capacity = capacity;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

