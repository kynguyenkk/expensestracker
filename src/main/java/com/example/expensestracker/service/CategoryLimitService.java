package com.example.expensestracker.service;

import com.example.expensestracker.exception.DataNotFoundException;
import com.example.expensestracker.model.dto.response.CategoryLimitResponse;
import com.example.expensestracker.model.entity.CategoryEntity;
import com.example.expensestracker.model.entity.CategoryLimitEntity;
import com.example.expensestracker.model.entity.UserEntity;
import com.example.expensestracker.repositories.CategoryLimitRepository;
import com.example.expensestracker.repositories.CategoryRepository;
import com.example.expensestracker.repositories.TransactionRepository;
import com.example.expensestracker.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryLimitService implements ICategoryLimitService {
    @Autowired
    private CategoryLimitRepository categoryLimitRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    public void autoCopyLimitsToNewMonth(Long userId) {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        // Kiểm tra nếu tháng hiện tại đã có dữ liệu giới hạn
        boolean currentMonthExists = categoryLimitRepository.existsByUserIdAndMonthAndYear(userId, currentMonth, currentYear);

        if (!currentMonthExists) {
            // Lấy giới hạn của tháng trước
            int previousMonth = (currentMonth == 1) ? 12 : currentMonth - 1;
            int previousYear = (currentMonth == 1) ? currentYear - 1 : currentYear;

            List<CategoryLimitEntity> previousLimits = categoryLimitRepository.findByUserIdAndMonthAndYear(userId, previousMonth, previousYear);

            // Nếu không có giới hạn tháng trước, tạo giá trị mặc định 0%
            if (previousLimits.isEmpty()) {
                List<CategoryLimitEntity> defaultLimits = categoryRepository.findAll().stream()
                        .map(category -> {
                            CategoryLimitEntity defaultLimit = new CategoryLimitEntity();
                            defaultLimit.setUser(userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
                            defaultLimit.setCategory(category);
                            defaultLimit.setMonth(currentMonth);
                            defaultLimit.setYear(currentYear);
                            defaultLimit.setPercentLimit(BigDecimal.ZERO);
                            return defaultLimit;
                        }).collect(Collectors.toList());
                categoryLimitRepository.saveAll(defaultLimits);
            } else {
                // Sao chép giới hạn từ tháng trước
                List<CategoryLimitEntity> newLimits = previousLimits.stream()
                        .map(limit -> {
                            CategoryLimitEntity newLimit = new CategoryLimitEntity();
                            newLimit.setUser(limit.getUser());
                            newLimit.setCategory(limit.getCategory());
                            newLimit.setMonth(currentMonth);
                            newLimit.setYear(currentYear);
                            newLimit.setPercentLimit(limit.getPercentLimit());
                            return newLimit;
                        }).collect(Collectors.toList());
                categoryLimitRepository.saveAll(newLimits);
            }
        }
    }
    @Scheduled(cron = "0 0 0 1 * ?")  // Chạy vào 00:00 ngày 1 mỗi tháng
    public void scheduleAutoCopyLimits() {
        List<Long> allUserIds = userRepository.findAllUserIds();  // Lấy danh sách tất cả userId
        allUserIds.forEach(userId -> autoCopyLimitsToNewMonth(userId));
    }
    @Override
    // Lưu giới hạn chi tiêu cho tháng hiện tại
    public void saveLimits(Long userId, List<CategoryLimitResponse> limits, int month, int year) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        List<CategoryLimitEntity> entities = limits.stream()
                .map(limit -> {
                    CategoryEntity existingCategory = categoryRepository
                            .findById(limit.getCategoryId())
                            .orElseThrow(() -> new DataNotFoundException("Cannot find category"));

                    // Kiểm tra xem giới hạn đã tồn tại hay chưa
                    CategoryLimitEntity existingLimit = categoryLimitRepository
                            .findByUserIdAndCategoryIdAndMonthAndYear(user.getUserId(), existingCategory.getCategoryId(), month, year);

                    if (existingLimit != null) {
                        // Nếu đã tồn tại, cập nhật giới hạn
                        existingLimit.setPercentLimit(limit.getPercentLimit());
                        return existingLimit;
                    } else {
                        // Nếu chưa có, tạo mới
                        CategoryLimitEntity newLimit = new CategoryLimitEntity();
                        newLimit.setUser(user);
                        newLimit.setCategory(existingCategory);
                        newLimit.setMonth(month);
                        newLimit.setYear(year);
                        newLimit.setPercentLimit(limit.getPercentLimit());
                        return newLimit;
                    }
                }).collect(Collectors.toList());

        // Lưu tất cả các thay đổi (cả mới và cập nhật)
        categoryLimitRepository.saveAll(entities);
    }

//    // Tính phần trăm còn lại của giới hạn chi tiêu
//    public List<CategoryLimitResponse> calculateRemainingPercent(Long userId) {
//        int currentMonth = LocalDate.now().getMonthValue();
//        int currentYear = LocalDate.now().getYear();
//
//        // Lấy đối tượng User từ userId
//        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
//
//        // Lấy giới hạn chi tiêu của tháng hiện tại
//        List<CategoryLimitEntity> currentLimits = categoryLimitRepository.findByUserIdAndMonthAndYear(userId, currentMonth, currentYear);
//        // Kiểm tra xem có giới hạn chi tiêu không
//        // Tính phần trăm còn lại cho mỗi danh mục chi tiêu
//        return currentLimits.stream()
//                .map(limit -> {
//                    // Lấy phần trăm chi tiêu đã sử dụng cho danh mục này
//                    BigDecimal spentPercent = getSpentPercent(userId, currentMonth, currentYear, limit.getCategory().getCategoryId());
//
//                    // Tính phần trăm còn lại
//                    BigDecimal remainingPercent = limit.getPercentLimit()
//                            .subtract(spentPercent); // Sử dụng phương thức subtract() thay vì phép trừ trực tiếp
//
//                    return new CategoryLimitResponse(
//                            limit.getCategory().getCategoryId(),
//                            limit.getPercentLimit(),
//                            remainingPercent.setScale(2, RoundingMode.HALF_UP) // Làm tròn đến 2 chữ số thập phân
//                    );
//                })
//                .collect(Collectors.toList());
//    }
//
//    // Giả lập hàm tính toán phần trăm đã chi tiêu, có thể thay đổi tùy vào cách tính chi tiêu của bạn
//    private BigDecimal getSpentPercent(Long userId, int month, int year, Long categoryId) {
//        // Ví dụ: giả sử chúng ta lấy tổng chi tiêu từ một bảng khác và tính toán phần trăm đã chi
//        BigDecimal totalSpent = new BigDecimal("5000");;  // Tổng chi tiêu
//        BigDecimal categoryLimit = new BigDecimal("10000");  // Giới hạn chi tiêu
//        // Tính phần trăm đã chi
//        BigDecimal percentSpent = totalSpent.multiply(new BigDecimal("100")).divide(categoryLimit, 2, RoundingMode.HALF_UP);
//        return percentSpent;  // Trả về phần trăm đã chi tiêu
//    }
}
