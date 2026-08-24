package cz.dhable.projects.nas.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import cz.dhable.projects.nas.model.entity.Role;

public class UserInputReqDto {
    @JsonProperty("username")
    private final String userName;
    private final Role role;
    private final String password;
    private final String email;
    private final Boolean rememberMe;

    public UserInputReqDto(String userName, String password, String email, Boolean rememberMe) {
        this.userName = userName;
        this.email = email;
        this.rememberMe = rememberMe;
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

    public Boolean isRememberMe() {
        return rememberMe;
    }
}
