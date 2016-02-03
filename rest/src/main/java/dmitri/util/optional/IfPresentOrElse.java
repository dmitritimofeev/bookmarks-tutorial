package dmitri.util.optional;

import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.http.ResponseEntity;

public class IfPresentOrElse {
	
	public static <T> ResponseEntity ifPresentOrElse(Optional<T> optional,
	        Consumer<? super T> action, 
	        Runnable emptyAction) {

	    if (optional.isPresent()) {
	        action.accept(optional.get());
	    } else {
	        emptyAction.run();
	    }
		return null;
	}
//	public static void main(String[] args) {
		//usage example
//		Optional<String> optional = Optional.ofNullable(null);
//		ifPresentOrElse(optional, s -> {
//		    System.out.println("Optional not null processed");
//		}, 
//		() -> System.out.println("Optional  null processed"));

//	}

}
