package bookmarks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


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

@RestController
@RequestMapping("/{userName}/bookmarks")
class BookmarkRestController {

    private final BookmarkRepository bookmarkRepository;
    private final AccountRepository accountRepository;
    static
    {
    System.out.println("In BookmarkRestController");
    }
    
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
    Bookmark readBookmark(@PathVariable Long bookmarkId) {
        return this.bookmarkRepository.findOne(bookmarkId);
    }

   @RequestMapping(method = RequestMethod.GET)
   Collection<Bookmark> readBookmarks(@PathVariable String userName) {
       return bookmarkRepository.findByAccountUsername(userName);
   }
    


    @Autowired
    BookmarkRestController(BookmarkRepository bookmarkRepository, AccountRepository accountRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.accountRepository = accountRepository;
    }
}