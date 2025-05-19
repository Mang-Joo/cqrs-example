package cqrs.user.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;


@Entity
public class User {
    @Id
    private UUID userId;
    private String name;

    protected User() {
    }

    public User(UUID userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }
}