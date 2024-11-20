package com.example.expensestracker.controller;

import com.example.expensestracker.model.dto.request.*;
import com.example.expensestracker.model.dto.response.ApiResponse;
import com.example.expensestracker.model.entity.UserEntity;
import com.example.expensestracker.service.IUserService;
import com.example.expensestracker.util.JwtTokenUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/users")
public class UserController {
    @Autowired
    private IUserService userService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping("/register")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRegisterDTO userRegisterDTO, BindingResult result) {
       try {
           if (result.hasErrors()) {
               List<String> errorsMessages = result.getFieldErrors()
                       .stream()
                       .map(FieldError::getDefaultMessage)
                       .toList();
               return ResponseEntity.badRequest().body(new ApiResponse("error", errorsMessages));
           }
           if (!userRegisterDTO.getPassword().equals(userRegisterDTO.getRetypePassword())) {
               return ResponseEntity.badRequest().body(new ApiResponse("error", "Passwords do not match"));
           }
           UserEntity user = userService.createUser(userRegisterDTO);//return ResponseEntity.ok("Register successfully");
           return ResponseEntity.ok(new ApiResponse("success", "Đăng ký thành công"));
       }
       catch (Exception ex){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse("error", ex.getMessage())); //rule 5
       }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordDTO changePasswordDTO) throws Exception {
        if (changePasswordDTO.getCurrentPassword().equals(changePasswordDTO.getNewPassword())) {
            return ResponseEntity.badRequest().body(new ApiResponse("error", "Mật khẩu mới không được trùng mật khẩu cũ"));
        }
        userService.changePassword(userDetails.getUsername(), changePasswordDTO);
        return ResponseEntity.ok(new ApiResponse("success", "Password updated successfully"));
    }

    @PutMapping("")
    public ResponseEntity<?> updateUser( @Valid @RequestBody UserDTO userDTO, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        try{
            // Lấy token từ header Authorization
            String token = authorizationHeader.substring(7); // Loại bỏ tiền tố "Bearer "
            // Trích xuất userId từ token
            Long userId = Long.valueOf(jwtTokenUtil.extractUserId(token));
            userService.updateCategory(userId,userDTO);
            return ResponseEntity.ok(new ApiResponse("success", "Update profile successfully"));
        }catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody UserLoginDTO userLoginDTO) {
        // Kiểm tra thông tin đăng nhập và sinh token
        try {
            String token = userService.login(userLoginDTO.getPhoneNumber(), userLoginDTO.getPassword());
            // Trả về token trong response
            return ResponseEntity.ok(new ApiResponse("success", token));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("error", e.getMessage()));
        }
    }
}
