package com.ResumeParser;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class ResumeFileDto {
  private String name;

  private String fileName;
  private String email;
  private String phoneNumber;
  private Float experience;
  private List<String> designation;
  private List<String> skills = new ArrayList<>();
  private String city;

  public void setName(String name) {
    this.name = name;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public void setExperience(Float experience) {
    this.experience = experience;
  }

  public void setDesignation(List<String> designation) {
    this.designation = designation;
  }

  public void setSkills(List<String> skills) {
    this.skills = skills;
  }

  public void setCity(String city) {
    this.city = city;
  }
}
