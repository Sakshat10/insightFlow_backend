package com.insightflow.service;

import com.insightflow.dto.UpdateUserRequest;
import com.insightflow.dto.UserResponse;
import com.insightflow.entity.User;
import com.insightflow.exception.ForbiddenException;
import com.insightflow.exception.ResourceNotFoundException;
import com.insightflow.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getUserById(Integer id, User currentUser) {
        if (!currentUser.getId().equals(id)) {
            throw new ForbiddenException("You can only view your own profile");
        }
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(Integer id, UpdateUserRequest request, User currentUser) {
        if (!currentUser.getId().equals(id)) {
            throw new ForbiddenException("You can only update your own profile");
        }
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);
        log.info("User {} updated their profile", user.getUsername());
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Integer id, User currentUser) {
        if (!currentUser.getId().equals(id)) {
            throw new ForbiddenException("You can only delete your own account");
        }
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setIsDeleted(true);
        userRepository.save(user);
        log.info("User {} soft-deleted their account", user.getUsername());
    }
}
