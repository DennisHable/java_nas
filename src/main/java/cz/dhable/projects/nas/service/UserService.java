package cz.dhable.projects.nas.service;

import cz.dhable.projects.nas.model.entity.User;
import cz.dhable.projects.nas.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findByUsername(String name) {
        Optional<User> user = userRepository.findByUsername(name);
        if (user.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username");
        return user.get();
    }

    public long countUsers() {
        return userRepository.count();
    }
}
