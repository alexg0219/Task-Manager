package org.suffers.aag.demo.entities;

import java.time.LocalDateTime;

public class Task {
    private int id;
    private String title;
    private String description;
    private String status;
    private final LocalDateTime createdAt;
    private LocalDateTime deadline;
    private User creator;
    private User assignee;
    private boolean modified;
    private LocalDateTime closeAt;
    private boolean isNew;

    public Task(int id, String title, String description, String status, LocalDateTime createdAt,
                User creator, User assignee, boolean modified, LocalDateTime deadline, LocalDateTime closeAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.creator = creator;
        this.assignee = assignee;
        this.modified = modified;
        this.deadline = deadline;
        this.isNew = false;
        this.closeAt = closeAt;
    }

    public int getId() {
        return id;
    }

    public int getAssigneeId() {
        return assignee.getId();
    }

    public int getCreatorId() {
        return creator.getId();
    }

    public User getCreator() {
        return creator;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public void markModified() {
        modified = true;
    }

    public boolean isModified() {
        return modified;
    }

    public void resetModified() {
        modified = false;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public void setCloseAt(LocalDateTime closeAt) {
        this.closeAt = closeAt;
    }

    public LocalDateTime getCloseAt() {
        return closeAt;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public boolean isNew() {
        return isNew;
    }
}

