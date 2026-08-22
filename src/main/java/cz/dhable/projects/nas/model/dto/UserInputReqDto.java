package cz.dhable.projects.nas.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import cz.dhable.projects.nas.model.entity.Role;

public class UserInputReqDto {
    @JsonProperty("username")
    private String userName;
    private Role role;
    private String password;
    private String email;

    public UserInputReqDto(String userName, String password, String email) {
        this.userName = userName;
        this.email = email;
        this.role = Role.USER;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public Role getRole() {
        return role;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }
}
