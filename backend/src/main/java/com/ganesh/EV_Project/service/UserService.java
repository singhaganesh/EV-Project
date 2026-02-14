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
        // Only check for existing mobile number if it's provided
        if (user.getMobileNumber() != null && !user.getMobileNumber().isEmpty()) {
            Optional<User> existUser = userRepository.findByMobileNumber(user.getMobileNumber());
            if (existUser.isPresent()){
                throw new APIException("User already exists with this mobile number");
            }
        }
        return userRepository.save(user);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
