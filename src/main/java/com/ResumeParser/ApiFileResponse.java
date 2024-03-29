package com.ResumeParser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFileResponse {

  @JsonProperty("label")
  private Label label;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Label {

    @JsonProperty("Person_Details")
    private PersonDetails personDetails;

    @JsonProperty("Experience_Details")
    private EmploymentDetails employmentDetails;

    @JsonProperty("refined_experience_details")
    private List<RefinedExperienceDetails> refinedExperienceDetails;

    @JsonProperty("Education_Details")
    private EducationDetails educationDetails;

    @JsonProperty("career_growth_score")
    private String careerGrowthScore;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PersonDetails {

      private String name;

      @JsonProperty("phoneno")
      private String phoneNumber;

      private String email;

      @JsonProperty("totalexp")
      private TotalExperience totalExperience;

      @JsonProperty("linkedin_url")
      private String linkedInUrl;

      @JsonProperty("city")
      private String city;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EducationDetails {
      @JsonProperty("educational_institution")
      private List<String> institution;

      @JsonProperty("high_education")
      private List<String> qualification;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TotalExperience {

      @JsonProperty("year")
      private Float experience;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmploymentDetails {

      @JsonProperty("skills")
      private List<String> skills = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RefinedExperienceDetails {

      @JsonProperty("start_date")
      @JsonFormat(pattern = "dd/MM/yyyy")
      private Date startDate;

      @JsonProperty("end_date")
      @JsonFormat(pattern = "dd/MM/yyyy")
      private Date endDate;

      @JsonProperty("company")
      private String companyName;

      @JsonProperty("company_loc")
      private String companyLocation;

      @JsonProperty("designation")
      private String designation;
    }
  }
}
