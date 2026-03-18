package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.exception.APIException;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    public User findByPhoneNumber(String mobileNumber) {
        Optional<User> user = userRepository.findByMobileNumber(mobileNumber);
        if (user.isEmpty()){
            return null;
        }
        return user.get();
    }

    public User findByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.orElse(null);
    }

    public User saveUser(User user) {
        // Handle empty email as null to avoid unique constraint violations
        if (user.getEmail() != null && user.getEmail().trim().isEmpty()) {
            user.setEmail(null);
        }

        // Check for existing mobile number
        if (user.getMobileNumber() != null && !user.getMobileNumber().isEmpty()) {
            Optional<User> existUser = userRepository.findByMobileNumber(user.getMobileNumber());
            
            if (existUser.isPresent()) {
                // If ID is not set or matches existing user, this is an update (Profile Completion)
                User existing = existUser.get();
                if (user.getId() == null || user.getId().equals(existing.getId())) {
                    existing.setName(user.getName());
                    existing.setEmail(user.getEmail());
                    if (user.getRole() != null) {
                        existing.setRole(user.getRole());
                    }
                    if (user.getPassword() != null) {
                        existing.setPassword(user.getPassword());
                    }
                    return userRepository.save(existing);
                } else {
                    throw new APIException("User already exists with this mobile number");
                }
            }
        }
        
        // Handle empty email for new user creation
        if (user.getEmail() != null) {
            Optional<User> existByEmail = userRepository.findByEmail(user.getEmail());
            if (existByEmail.isPresent()) {
                throw new APIException("Email already registered");
            }
        }

        return userRepository.save(user);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
