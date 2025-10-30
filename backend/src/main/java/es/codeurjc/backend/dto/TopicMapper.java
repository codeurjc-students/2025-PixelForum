package es.codeurjc.backend.dto;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;

import es.codeurjc.backend.model.Topic;

@Mapper(componentModel = "spring")
public interface TopicMapper {

    TopicDTO toDTO(Topic Topic);

    List<TopicDTO> toDTOs(Collection<Topic> Topics);

    Topic toDomain(TopicDTO TopicDTO);

}