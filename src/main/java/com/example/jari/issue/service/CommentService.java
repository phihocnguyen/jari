package com.example.jari.issue.service;

import com.example.jari.issue.dto.CommentRequest;
import com.example.jari.issue.dto.CommentResponse;
import com.example.jari.issue.entity.Comment;
import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.mapper.IssueMapper;
import com.example.jari.issue.repository.CommentRepository;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.shared.exception.ForbiddenException;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final IssueMapper mapper;

    @Transactional(readOnly = true)
    public List<CommentResponse> list(UUID issueId) {
        return commentRepository.findByIssueIdAndDeletedAtIsNullOrderByCreatedAtAsc(issueId).stream()
            .map(mapper::toCommentResponse).toList();
    }

    @Transactional
    public CommentResponse create(UUID issueId, UUID authorId, CommentRequest req) {
        Issue issue = issueRepository.findById(issueId)
            .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("User", authorId));
        Comment comment = commentRepository.save(Comment.builder()
            .issue(issue).author(author).content(req.getContent()).build());
        return mapper.toCommentResponse(comment);
    }

    @Transactional
    public CommentResponse update(UUID commentId, UUID requesterId, CommentRequest req) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        if (!comment.getAuthor().getId().equals(requesterId)) throw new ForbiddenException();
        comment.setContent(req.getContent());
        return mapper.toCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public void delete(UUID commentId, UUID requesterId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        if (!comment.getAuthor().getId().equals(requesterId)) throw new ForbiddenException();
        comment.setDeletedAt(OffsetDateTime.now());
        commentRepository.save(comment);
    }
}
