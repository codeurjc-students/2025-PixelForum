package es.codeurjc.backend.dto.Topic;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;

import es.codeurjc.backend.model.Topic;

@Mapper(componentModel = "spring")
public interface TopicMapper {

    TopicDTO toDTO(Topic topic);

    List<TopicDTO> toDTOs(Collection<Topic> topics);

    Topic toDomain(TopicDTO topicDTO);
    
}