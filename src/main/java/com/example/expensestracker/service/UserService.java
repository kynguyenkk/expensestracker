package com.example.expensestracker.service;

import com.example.expensestracker.exception.DataNotFoundException;
import com.example.expensestracker.model.dto.request.CategoryDTO;
import com.example.expensestracker.model.dto.request.ChangePasswordDTO;
import com.example.expensestracker.model.dto.request.UserDTO;
import com.example.expensestracker.model.dto.request.UserRegisterDTO;
import com.example.expensestracker.model.entity.CategoryEntity;
import com.example.expensestracker.model.entity.UserEntity;
import com.example.expensestracker.model.enums.CategoryType;
import com.example.expensestracker.model.enums.Gender;
import com.example.expensestracker.repositories.UserRepository;
import com.example.expensestracker.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    public UserEntity createUser(UserRegisterDTO userRegisterDTO) throws Exception {
        //register user
        String phoneNumber = userRegisterDTO.getPhoneNumber();
        String email = userRegisterDTO.getEmail();
        //kiểm tra xem số điện thoại đã tồn tại hay chưa
        if (userRepository.existsByPhoneNumber(phoneNumber) && userRepository.existsByEmail(email)) {
            throw new DataIntegrityViolationException("Phone number and Email already exists");
        }else if(userRepository.existsByEmail(email)) {
            throw new DataIntegrityViolationException("Email already exists");
        }else if(userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DataIntegrityViolationException("Phone number already exists");
        }
        //convert from userDTO -> userEntity
        UserEntity newUser = UserEntity.builder()
                .phoneNumber(userRegisterDTO.getPhoneNumber())
                .email(userRegisterDTO.getEmail())
                .password(passwordEncoder.encode(userRegisterDTO.getPassword()))
                .build();
        return userRepository.save(newUser);
    }
    @Override
    public UserEntity updateCategory( Long userId, UserDTO userDTO) throws Exception{
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy profile"));
        //kiểm tra xem số điện thoại đã tồn tại hay chưa
        if (userRepository.existsByPhoneNumber(userDTO.getPhoneNumber()) && userRepository.existsByEmail(userDTO.getEmail())) {
            throw new DataIntegrityViolationException("Phone number and Email already exists");
        }else if(userRepository.existsByEmail(userDTO.getEmail())) {
            throw new DataIntegrityViolationException("Email already exists");
        }else if(userRepository.existsByPhoneNumber(userDTO.getPhoneNumber())) {
            throw new DataIntegrityViolationException("Phone number already exists");
        }
        // Thực hiện cập nhật nếu danh mục không phải là mặc định
        user.setFullName(userDTO.getFullName());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setEmail(userDTO.getEmail());
        user.setGender(Gender.valueOf(userDTO.getGender()));
        user.setBirthDate(userDTO.getBirthDate());
        user.setAddress(userDTO.getAddress());
        userRepository.save(user);
        return user;
    }
    @Override
    public String login(String phoneNumber, String password) throws Exception {
        Optional<UserEntity> optionalUser = userRepository.findByPhoneNumber(phoneNumber);
        if (optionalUser.isEmpty()) {
            throw new  UsernameNotFoundException("Invalid phone number / password");
        }
        //return optionalUser.get();//muốn trả JWT token ?
        UserEntity existingUser = optionalUser.get();
        //check password
        if (!passwordEncoder.matches(password, existingUser.getPassword())) {
            throw new BadCredentialsException("Wrong phone number or password");
        }
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                phoneNumber, password,
                existingUser.getAuthorities()
        );
        //authenticate with Java Spring security
        authenticationManager.authenticate(authenticationToken);
        return jwtTokenUtil.generateToken(existingUser);
    }

    @Override
    public void changePassword(String username, ChangePasswordDTO changePasswordDTO) throws Exception {
        UserEntity user = userRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(changePasswordDTO.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        userRepository.save(user);
    }

}
