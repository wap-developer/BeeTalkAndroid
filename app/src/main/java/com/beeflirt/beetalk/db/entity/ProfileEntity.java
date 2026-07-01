package com.beeflirt.beetalk.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "profiles")
public class ProfileEntity {
    @PrimaryKey
    private int userId;
    private String name;
    private String gender;
    private String birthDate;
    private int age;
    private String bio;
    private String username;
    private String hobbies;
    private String dreams;
    private double latitude;
    private double longitude;
    private String city;
    private String country;
    private double distance;
    private String primaryPhoto;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getHobbies() { return hobbies; }
    public void setHobbies(String hobbies) { this.hobbies = hobbies; }

    public String getDreams() { return dreams; }
    public void setDreams(String dreams) { this.dreams = dreams; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public String getPrimaryPhoto() { return primaryPhoto; }
    public void setPrimaryPhoto(String primaryPhoto) { this.primaryPhoto = primaryPhoto; }
}
