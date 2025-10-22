package org.suffers.aag.demo.entities;

public class User {
    private int id;
    private final String username;
    private String password;
    private String role;
    private int superiorId;
    private boolean isNew;
    private boolean modified;

    public User(int id, String username, String password, String role, int superiorId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.superiorId = superiorId;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public int getSuperiorId() {
        return superiorId;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return username;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public void setSuperiorId(int superiorId) {
        this.superiorId = superiorId;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        User user = (User) obj;
        return this.id == user.getId();
    }

}
