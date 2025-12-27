# Hướng Dẫn Chạy Tests & Tạo Báo Cáo Excel

## 🎯 **Tổng Quan**

Bạn có **3 scripts độc lập** để chạy từng loại test riêng biệt:

1. **Automated Tests** (Maven/JUnit) - Không cần server
2. **Manual Security Tests** - Cần server đang chạy
3. **Penetration Tests** - Cần server đang chạy

Mỗi script tự động parse kết quả và tạo/update file Excel!

---

## 📋 **Scripts Có Sẵn**

### **1. Automated Tests (27+ tests)**
```bash
bash run_automated_tests.sh
```

**Thực hiện:**
- ✅ Chạy `./mvnw.cmd test`
- ✅ Parse Surefire XML reports
- ✅ Tạo `security_test_results.csv`

**Không cần:** Server đang chạy

---

### **2. Manual Security Tests (9+ tests)**
```bash
bash run_manual_tests.sh
```

**Thực hiện:**
- ✅ Check server (phải đang chạy)
- ✅ Chạy `scripts/manual_security_tests.sh`
- ✅ Lưu log: `manual_test_results.log`
- ✅ Parse log và update Excel

**Cần:** Server đang chạy trên `https://localhost:8443`

---

### **3. Penetration Tests (8+ tests)**
```bash
bash run_penetration_tests.sh
```

**Thực hiện:**
- ✅ Check server (phải đang chạy)
- ⚠️ Xác nhận (vì simulate attacks)
- ✅ Chạy `scripts/penetration_tests.sh all`
- ✅ Lưu log: `penetration_test_results.log`
- ✅ Parse log và update Excel

**Cần:** Server đang chạy trên `https://localhost:8443`

---

## 🚀 **Quy Trình Khuyến Nghị**

### **Option 1: Chỉ Automated Tests (Nhanh)**
```bash
# Chạy automated tests (đủ verify security)
bash run_automated_tests.sh

# Kết quả: security_test_results.csv (27+ tests)
```

---

### **Option 2: Tất Cả Tests (Đầy Đủ)**

**Bước 1: Automated Tests**
```bash
bash run_automated_tests.sh
# → security_test_results.csv (27+ tests)
```

**Bước 2: Start Server**
```bash
./mvnw.cmd spring-boot:run
# Chạy trong terminal riêng
```

**Bước 3: Manual Tests** (terminal mới)
```bash
bash run_manual_tests.sh
# → Update security_test_results.csv (thêm 9+ tests)
```

**Bước 4: Penetration Tests** (tùy chọn)
```bash
bash run_penetration_tests.sh
# → Update security_test_results.csv (thêm 8+ tests)
```

**Kết quả:** `security_test_results.csv` với 44+ tests!

---

## 📊 **File Excel Output**

**File:** `security_test_results.csv`

**Cấu trúc:**
| Test ID | Category | Test Name | Description | Actual Results | Status | Time | Timestamp |
|---------|----------|-----------|-------------|----------------|--------|------|-----------|
| TC-001 | Encryption | testDatabaseEncryption | Verify... | Test passed | PASS | 0.542s | ... |

**Mở trong Excel:**
```bash
# Windows
start security_test_results.csv

# Hoặc double-click file
```

---

## 🔧 **Parse Thủ Công (Nếu Cần)**

Nếu bạn đã chạy tests và chỉ muốn re-parse:

```bash
python3 parse_test_results.py
```

**Script Python sẽ parse:**
- `target/surefire-reports/TEST-*.xml` → Automated tests
- `manual_test_results.log` → Manual tests
- `penetration_test_results.log` → Penetration tests

---

## 📁 **Files Trong Project**

```
expensestracker/
├── run_automated_tests.sh        ← Script 1: Automated tests
├── run_manual_tests.sh            ← Script 2: Manual tests  
├── run_penetration_tests.sh       ← Script 3: Penetration tests
├── parse_test_results.py          ← Python parser (dùng bởi các scripts)
├── security_test_results.csv      ← Excel output (tạo tự động)
├── manual_test_results.log        ← Log manual tests
├── penetration_test_results.log   ← Log penetration tests
├── scripts/
│   ├── manual_security_tests.sh   ← Manual test script
│   └── penetration_tests.sh       ← Penetration test script
└── target/
    └── surefire-reports/          ← Automated test results (XML)
```

---

## ⚠️ **Lưu Ý**

### **1. Python Required**
Scripts cần Python 3 để parse results:
```bash
python3 --version
# Nếu chưa có: https://www.python.org/downloads/
```

### **2. Server Cho Manual/Penetration Tests**
```bash
# Start server (terminal riêng)
./mvnw.cmd spring-boot:run

# Verify server đang chạy
curl -k https://localhost:8443/api/users/login
```

### **3. Git Bash on Windows**
Chạy scripts trong **Git Bash**, không phải PowerShell:
```bash
# Mở Git Bash
cd /c/Users/kynguyen/IdeaProjects/expensestracker
bash run_automated_tests.sh
```

---

## 🎯 **Ví Dụ Sử Dụng**

### **Scenario 1: Quick Test Before Commit**
```bash
bash run_automated_tests.sh
# 1-2 phút → Verify security OK
```

### **Scenario 2: Full Test Before Deploy**
```bash
# Terminal 1: Start server
./mvnw.cmd spring-boot:run

# Terminal 2: Run all tests
bash run_automated_tests.sh
bash run_manual_tests.sh
bash run_penetration_tests.sh

# → Comprehensive Excel report!
```

### **Scenario 3: Re-generate Report**
```bash
# Nếu đã có logs, chỉ cần:
python3 parse_test_results.py
# → Re-parse và tạo lại Excel
```

---

## ✅ **Summary**

**3 Scripts Độc Lập:**
- `run_automated_tests.sh` - Không cần server (27+ tests)
- `run_manual_tests.sh` - Cần server (9+ tests)
- `run_penetration_tests.sh` - Cần server (8+ tests)

**1 Python Parser:**
- `parse_test_results.py` - Parse THỰC từ test results

**1 Excel Output:**
- `security_test_results.csv` - Kết quả thực tế, chính xác!

**Flexible!** Chạy riêng từng loại test theo nhu cầu! 🚀
