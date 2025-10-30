package es.codeurjc.backend.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import es.codeurjc.backend.service.CommentService;
import es.codeurjc.backend.service.PostService;
import es.codeurjc.backend.service.TopicService;
import es.codeurjc.backend.service.UserService;


@Controller
public class CommentWebController {

    @Autowired
    UserService userService;

    @Autowired
    PostService postService;

    @Autowired
    CommentService commentService;

    @Autowired
    TopicService topicService;


}