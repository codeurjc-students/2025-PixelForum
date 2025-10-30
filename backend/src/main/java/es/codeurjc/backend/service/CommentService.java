package es.codeurjc.backend.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.codeurjc.backend.dto.CommentDTO;
import es.codeurjc.backend.dto.CommentMapper;
import es.codeurjc.backend.model.Comment;
import es.codeurjc.backend.repository.CommentRepository;
import jakarta.transaction.Transactional;

@Service
public class CommentService {

	@Autowired
    private CommentMapper mapper;

    @Autowired
    private CommentRepository commentRepository;

	
	public Optional<Comment> findById(long id) {
		return commentRepository.findById(id);
	}

	public boolean exist(long id) {
		return commentRepository.existsById(id);
	}

	public List<Comment> findAll() {
		return commentRepository.findAll();
	}

	public Comment save(Comment comment){
		return commentRepository.save(comment);
	}

    @Transactional 
    public void deleteById(Long id) {
        Optional<Comment> commentOptional = commentRepository.findById(id);
        if (commentOptional.isPresent()) {
            Comment comment = commentOptional.get();
			commentRepository.delete(comment);
        }
    }

	private CommentDTO toDTO (Comment comment) {
        return mapper.toDTO(comment);
    }

    private Comment toDomain (CommentDTO commentDTO) {
        return mapper.toDomain(commentDTO);
    }

    private List<CommentDTO> toDTOs(Collection<Comment> comments){
        return mapper.toDTOs(comments);
    }

	public Collection<CommentDTO> getcomments() {
		return toDTOs(commentRepository.findAll());
	}

	public CommentDTO getcomment(long id) {
		return toDTO(commentRepository.findById(id).orElseThrow());
	}

	public CommentDTO createcomment(CommentDTO commentDTO) {
		Comment comment = toDomain(commentDTO);
 		this.save(comment);
 		return toDTO(comment);
	}

}