package com.example.springweb.service;

import com.example.springweb.model.User;
import com.example.springweb.repository.UserRepository;
import com.example.springweb.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    // Get user by ID
    public User getUserById(Long id) {
        String cacheKey = "user_" + id;

        // 🔍 Kiểm tra Redis trước
        User cachedUser = (User) redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            System.out.println("Fetching from cache...");
            return cachedUser;
        }

        System.out.println("Fetching from database...");
        User user = userRepository.findById(id).orElse(null);

        if (user != null) {
            redisTemplate.opsForValue().set(cacheKey, user, 10, TimeUnit.MINUTES); // Lưu vào Redis 10 phút
        }

        return user;
    }

    // Create user
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // Delete User
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    // Get user and pagination
    public List<UserDTO> getUsers(String name, Integer page, Integer size) {
        int pageNumber = (page != null && page > 0) ? page - 1 : 0; 
        int pageSize = (size != null && size > 0) ? size : 10; 
        
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("id").descending());

        Page<User> usersPage;
        
        if (name != null && !name.isEmpty()) {
            usersPage = userRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            usersPage = userRepository.findAll(pageable);
        }

        List<UserDTO> userDTOs = usersPage.getContent()
            .stream()
            .map((User user) -> new UserDTO(user.getId(), user.getName(), user.getEmail())) 
            .collect(Collectors.toList());

        return userDTOs;
    }

    // Update user
    public User updateUser(Long id, User userDetails) {
        User existingUser = userRepository.findById(id).orElse(null);
        
        if (existingUser == null) {
            return null;
        }
        
        // Update user fields
        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());
        // Update other fields as needed
        
        // Save updated user
        User updatedUser = userRepository.save(existingUser);
        
        // Update cache
        String cacheKey = "user_" + id;
        redisTemplate.opsForValue().set(cacheKey, updatedUser, 10, TimeUnit.MINUTES);
        
        return updatedUser;
    }
    
    // Function for Redis
    public void saveToCache(String key, Object value) {
        redisTemplate.opsForValue().set(key, value, 10, TimeUnit.MINUTES); // Cache 10 phút
    }

    public Object getFromCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteFromCache(String key) {
        redisTemplate.delete(key);
    }
}