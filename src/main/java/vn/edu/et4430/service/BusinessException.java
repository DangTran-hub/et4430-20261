package vn.edu.et4430.service;

/** Lỗi nghiệp vụ: thông điệp được hiển thị thẳng cho người dùng. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
