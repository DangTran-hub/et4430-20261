package vn.edu.et4430.exception;

/** Business failure safe to display to PDT. */
public class DraftException extends RuntimeException {
    public enum Code { NOT_FOUND, INVALID_TERM, DUPLICATE_PLAN, INVALID_DATES,
        INVALID_COURSES, NOT_EDITABLE, AMBIGUOUS_SOURCE, STORAGE_CONFLICT }

    private final Code code;

    public DraftException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public DraftException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Code getCode() { return code; }
}
