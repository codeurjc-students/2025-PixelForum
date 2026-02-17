package es.codeurjc.backend.controller.rest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.codeurjc.backend.model.Topic;
import es.codeurjc.backend.service.TopicService;

@RestController
@RequestMapping("/api/v1/topics")
public class TopicRestController {

    private final TopicService topicService;

    public TopicRestController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping("/")
    public List<Topic> getAllTopics() {
        return topicService.findAll();
    }
}