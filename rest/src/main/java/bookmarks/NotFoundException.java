package bookmarks;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Requested user id was not found")
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
