package com.beeflirt.beetalk.models;

import com.google.gson.annotations.SerializedName;

public class ExploreUser {
    @SerializedName("user_id")
    private int userId;

    @SerializedName("name")
    private String name;

    @SerializedName("username")
    private String username;

    @SerializedName("primary_photo")
    private String primaryPhoto;

    @SerializedName("age")
    private int age;

    @SerializedName("gender")
    private String gender;

    @SerializedName("bio")
    private String bio;

    @SerializedName("hobbies")
    private String hobbies;

    @SerializedName("dreams")
    private String dreams;

    @SerializedName("city")
    private String city;

    @SerializedName("country")
    private String country;

    @SerializedName("distance")
    private double distance;

    @SerializedName("is_online")
    private int isOnline;

    @SerializedName("last_seen")
    private String lastSeen;

    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getPrimaryPhoto() { return primaryPhoto; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getBio() { return bio; }
    public String getHobbies() { return hobbies; }
    public String getDreams() { return dreams; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public double getDistance() { return distance; }
    public int getIsOnline() { return isOnline; }
    public String getLastSeen() { return lastSeen; }
}
