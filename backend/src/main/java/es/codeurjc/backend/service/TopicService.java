package es.codeurjc.backend.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import es.codeurjc.backend.dto.topic.TopicDTO;
import es.codeurjc.backend.dto.topic.TopicMapper;
import es.codeurjc.backend.model.Topic;
import es.codeurjc.backend.repository.TopicRepository;
import jakarta.transaction.Transactional;

@Service
public class TopicService {

	private final TopicMapper mapper;
	private final TopicRepository topicRepository;

	public TopicService(TopicMapper mapper, TopicRepository topicRepository) {
		this.mapper = mapper;
		this.topicRepository = topicRepository;
	}
	
	public Optional<Topic> findById(long id) {
		return topicRepository.findById(id);
	}

	public boolean exist(long id) {
		return topicRepository.existsById(id);
	}

	public List<Topic> findAll() {
		return topicRepository.findAll();
	}

	public Topic save(Topic topic){
		return topicRepository.save(topic);
	}

    @Transactional 
    public void deleteById(Long id) {
        Optional<Topic> topicOptional = topicRepository.findById(id);
        if (topicOptional.isPresent()) {
            Topic topic = topicOptional.get();
			topicRepository.delete(topic);
        }
    }

	private TopicDTO toDTO (Topic topic) {
        return mapper.toDTO(topic);
    }

    private Topic toDomain (TopicDTO topicDTO) {
        return mapper.toDomain(topicDTO);
    }

    private List<TopicDTO> toDTOs(Collection<Topic> topics){
        return mapper.toDTOs(topics);
    }

	public Collection<TopicDTO> getTopics() {
		return toDTOs(topicRepository.findAll());
	}

	public TopicDTO getTopic(long id) {
		return toDTO(topicRepository.findById(id).orElseThrow());
	}

	public TopicDTO createTopic(TopicDTO topicDTO) {
		Topic topic = toDomain(topicDTO);
 		this.save(topic);
 		return toDTO(topic);
	}

}