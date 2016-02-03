package bookmarks;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.VndErrors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application  extends SpringBootServletInitializer{
	 @Override
	    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
	        return application.sources(Application.class);
	    }

    @Bean
    CommandLineRunner init(AccountRepository accountRepository, BookmarkRepository bookmarkRepository) {
          return (stringArgsFromMain) ->
                Arrays.asList("jhoeller", "dsyer", "pwebb", "ogierke", "rwinch", "mfisher", "mpollack").forEach(a -> {
                	for(String arg:stringArgsFromMain){
                	System.out.println("arg: "+arg);
                	}
                    Account account = accountRepository.save(new Account(a, "password"));
                    Bookmark savedBookmark1 = bookmarkRepository.save(new Bookmark(account, "http://bookmark.com/1/" + a, "A description7"));
                    Bookmark savedBookmark2 = bookmarkRepository.save(new Bookmark(account, "http://bookmark.com/2/" + a, "A description8"));
                });
    }

    public static void main(String[] args) {
    String[] myargs = {"arg1","arg2"};
        SpringApplication.run(Application.class, myargs);
    }
}

class BookmarkResource extends ResourceSupport {

    private final Bookmark bookmark;

    public BookmarkResource(Bookmark bookmark) {
        String username = bookmark.account.username;
        this.bookmark = bookmark;
        this.add(new Link(bookmark.uri, "bookmark-uri"));
        this.add(linkTo(BookmarkRestController.class, username).withRel("bookmarks"));
        this.add(linkTo(methodOn(BookmarkRestController.class, username).readBookmark(username,bookmark.id)).withSelfRel());
    }

    public Bookmark getBookmark() {
        return bookmark;
    }
}

@RestController
@RequestMapping("/{userName}/bookmarks")
class BookmarkRestController {

    private final BookmarkRepository bookmarkRepository;
    private final AccountRepository accountRepository;
    
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<?> add(@PathVariable String userName, @RequestBody Bookmark userBookmark) {
    	Optional<Account> account = accountRepository.findByUsername(userName);
    	 ResponseEntity<Object> postedResponseEntity;
        if(account.isPresent()){
        	postedResponseEntity = account.map(
        
                (acctValue) -> {
                    Bookmark savedBookmark = bookmarkRepository.save(new Bookmark(acctValue, userBookmark.uri, userBookmark.description));

                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest().path("/{userName}")
                            .buildAndExpand(savedBookmark.id)
                            .toUri());
                    return new ResponseEntity<>(null, httpHeaders, HttpStatus.CREATED);
                })
//        	        //).orElseThrow(() -> new RuntimeException("could not find the user '" + userId + "'"));
//        	        ).orElseThrow(() -> new NotFoundException("could not find the user '" + userId + "'"));
     	              	
        		.get();
        	 
        }else{
        	postedResponseEntity = addNewUser(userName,userBookmark);
        };     
        return postedResponseEntity;
    }
    


    private ResponseEntity<Object> addNewUser(String userName, Bookmark userBookmark) {
    	 Account account = accountRepository.save(new Account(userName, "password"));
         Bookmark savedBookmark = bookmarkRepository.save(new Bookmark(account, userBookmark.uri, userBookmark.description));
         HttpHeaders httpHeaders = new HttpHeaders();
         httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest().path("/{userName}")
                 .buildAndExpand(savedBookmark.id)
                 .toUri());
         return new ResponseEntity<>(null, httpHeaders, HttpStatus.CREATED);

	}

	@RequestMapping(value = "/{bookmarkId}", method = RequestMethod.GET)
    BookmarkResource readBookmark(@PathVariable String userId,@PathVariable Long bookmarkId) {
        //return this.bookmarkRepository.findOne(bookmarkId);
		this.validateUser(userId);
        return new BookmarkResource(this.bookmarkRepository.findOne(bookmarkId));
    }

   @RequestMapping(method = RequestMethod.GET)
  // Collection<Bookmark> readBookmarks(@PathVariable String userName) {
   //    return bookmarkRepository.findByAccountUsername(userName);
   Resources<BookmarkResource> readBookmarks(@PathVariable String userId) {

       this.validateUser(userId);

       List<BookmarkResource> bookmarkResourceList = bookmarkRepository.findByAccountUsername(userId)
               .stream()
               .map(BookmarkResource::new)
               .collect(Collectors.toList());
       return new Resources<BookmarkResource>(bookmarkResourceList);
   }

   
    


    @Autowired
    BookmarkRestController(BookmarkRepository bookmarkRepository, AccountRepository accountRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.accountRepository = accountRepository;
    }


private void validateUser(String userId) {
    this.accountRepository.findByUsername(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
}
}

@ControllerAdvice
class BookmarkControllerAdvice {

@ResponseBody
@ExceptionHandler(UserNotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
VndErrors userNotFoundExceptionHandler(UserNotFoundException ex) {
    return new VndErrors("error", ex.getMessage());
}
}


class UserNotFoundException extends RuntimeException {

public UserNotFoundException(String userId) {
    super("could not find user '" + userId + "'.");
}
}