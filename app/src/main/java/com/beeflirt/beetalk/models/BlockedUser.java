package com.beeflirt.beetalk.models;

public class BlockedUser {
    private int id;
    private int blockedId;
    private String name;
    private String email;
    private String gender;
    private int age;
    private String primaryPhoto;
    private String reason;
    private String createdAt;

    // Constructors
    public BlockedUser() {
    }

    public BlockedUser(int id, int blockedId, String name, String email, String gender,
            int age, String primaryPhoto, String reason, String createdAt) {
        this.id = id;
        this.blockedId = blockedId;
        this.name = name;
        this.email = email;
        this.gender = gender;
        this.age = age;
        this.primaryPhoto = primaryPhoto;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBlockedId() {
        return blockedId;
    }

    public void setBlockedId(int blockedId) {
        this.blockedId = blockedId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPrimaryPhoto() {
        return primaryPhoto;
    }

    public void setPrimaryPhoto(String primaryPhoto) {
        this.primaryPhoto = primaryPhoto;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
