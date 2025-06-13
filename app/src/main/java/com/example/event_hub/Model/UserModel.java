package com.example.event_hub.Model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
public class UserModel {

    @SerializedName("id")
    private Long id;

    @SerializedName("login")
    private String login;

    @SerializedName("name") // Added based on API docs
    private String name;

    @SerializedName("role")
    private String role;

    @SerializedName("status")
    private String status;

    @SerializedName("profileImageUrl") // Added based on API docs
    private String profileImageUrl;

    @SerializedName("description") // Added based on API docs
    private String description;

    @SerializedName("createdAt")
    private Date createdAt;

    // Constructor with all fields from API documentation for UserModel
    public UserModel(Long userId, String login, String name, String role, String status, String profileImageUrl, String description, Date createdAt) {
        this.id = userId;
        this.login = login;
        this.name = name;
        this.role = role;
        this.status = status;
        this.profileImageUrl = profileImageUrl;
        this.description = description;
        this.createdAt = createdAt;
    }
}