package es.codeurjc.backend.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.Topic;
import es.codeurjc.backend.model.User;
import jakarta.annotation.PostConstruct;


@Component
public class DatabaseInitializer {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    //@Autowired
    //private CommentService commentService;

    @Autowired
    private TopicService topicService;

    @PostConstruct
    public void init() throws IOException {
        
        User admin = new User("admin", "admin@host", "admin", "ADMIN", "USER");
        User usuario1 = new User("martin", "martin@gmail.com", "martin", "USER");
        User usuario2 = new User("robert","robert@gmail.com", "robert", "USER");
        User usuario3 = new User("daniel","daniel@gmail.com", "daniel", "USER");
        User usuario4 = new User("alvaro","alvaro@gmail.com", "alvaro", "USER");

        userService.save(admin);
        userService.save(usuario1);
        userService.save(usuario2);
        userService.save(usuario3);
        userService.save(usuario4);

        Topic topic1 = new Topic("GTA VI", "Videojuego de Rockstar Games");
        topicService.save(topic1);

        Post post1 = new Post("GTA VI Filtración Masiva", "Se han filtrado imágenes y vídeos del próximo Grand Theft Auto VI, mostrando nuevas ubicaciones, personajes y mecánicas de juego. Rockstar Games aún no ha comentado sobre la filtración.", 
            List.of(topic1));
        postService.save(post1);
    
    }   

}