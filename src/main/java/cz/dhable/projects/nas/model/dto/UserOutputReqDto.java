package cz.dhable.projects.nas.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserOutputReqDto {
    private final Long id;
    @JsonProperty("username")
    private final String userName;
    private final String role;
    private final String email;

    public UserOutputReqDto(Long id, String userName, String role, String email) {
        this.id = id;
        this.userName = userName;
        this.role = role;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }
}