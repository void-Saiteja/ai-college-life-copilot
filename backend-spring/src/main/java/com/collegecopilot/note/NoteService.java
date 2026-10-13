package com.collegecopilot.note;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.note.dto.*;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.Student;
import com.collegecopilot.student.StudentRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final StudentRepository studentRepository;
    private final GeminiNoteSummarizerService summarizerService;
    private final ObjectMapper objectMapper;

    @Autowired
    public NoteService(NoteRepository noteRepository,
                       StudentRepository studentRepository,
                       GeminiNoteSummarizerService summarizerService) {
        this.noteRepository = noteRepository;
        this.studentRepository = studentRepository;
        this.summarizerService = summarizerService;
        this.objectMapper = JsonMapper.builder().build();
    }

    private String resolveStudentId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        String studentKey = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        if (studentKey == null || studentKey.isBlank()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        Optional<Student> direct = studentRepository.findById(studentKey);
        if (direct.isPresent()) {
            return direct.get().getId();
        }

        Optional<Student> byUser = studentRepository.findByUserId(studentKey);
        return byUser.map(Student::getId).orElse(studentKey);
    }

    public NoteListResponse getNotes(UserPrincipal principal) {
        String studentId = resolveStudentId(principal);
        boolean isStd1 = "std-1".equals(studentId);

        List<Note> notes = noteRepository.findByStudentIdWithLegacy(studentId, isStd1);
        List<NoteDto> dtoList = notes.stream().map(this::toDto).toList();

        return new NoteListResponse(true, dtoList);
    }

    @Transactional
    public NoteResponse createNote(CreateNoteRequest request, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note payload is required");
        }

        String title = request.getTitle();
        if (title == null || title.isBlank() || title.trim().length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required (1-200 characters)");
        }

        String content = request.getContent();
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is required");
        }
        if (content.length() > 50000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note content exceeds maximum allowable limit of 50,000 characters");
        }

        String course = request.getCourse();
        if (course != null && course.length() > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course code must be a string up to 60 characters");
        }

        List<String> tagsList = parseTags(request.getTags());
        String serializedTags = serializeTags(tagsList);

        String id = "nt-" + System.currentTimeMillis();
        Note note = new Note(
                id,
                studentId,
                title.trim(),
                course != null ? course.trim() : "General",
                content.trim(),
                serializedTags
        );
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        Note saved = noteRepository.save(note);
        return new NoteResponse(true, toDto(saved));
    }

    @Transactional
    public NoteResponse updateNote(String id, UpdateNoteRequest request, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));

        boolean isOwner = note.getStudentId().equals(studentId)
                || (note.getStudentId() == null && "std-1".equals(studentId));
        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this note");
        }

        if (request != null) {
            if (request.getTitle() != null) {
                if (request.getTitle().length() > 200) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must be up to 200 characters");
                }
                note.setTitle(request.getTitle().trim());
            }

            if (request.getContent() != null) {
                if (request.getContent().length() > 50000) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content exceeds 50,000 characters limit");
                }
                note.setContent(request.getContent().trim());
            }

            if (request.getCourse() != null) {
                if (request.getCourse().length() > 60) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course code must be a string up to 60 characters");
                }
                note.setCourse(request.getCourse().trim());
            }

            if (request.getTags() != null) {
                List<String> updatedTags = parseTags(request.getTags());
                note.setTags(serializeTags(updatedTags));
            }
        }

        note.setUpdatedAt(LocalDateTime.now());
        Note saved = noteRepository.save(note);
        return new NoteResponse(true, toDto(saved));
    }

    @Transactional
    public NoteDeleteResponse deleteNote(String id, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));

        boolean isOwner = note.getStudentId().equals(studentId)
                || (note.getStudentId() == null && "std-1".equals(studentId));
        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this note");
        }

        noteRepository.delete(note);
        return new NoteDeleteResponse(true, "Note deleted");
    }

    public NoteSummarizeResponse summarizeNotes(NoteSummarizeRequest request, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        String title = request != null ? request.getTitle() : null;
        String course = request != null ? request.getCourse() : null;
        String content = request != null ? request.getContent() : null;
        String noteId = request != null ? request.getNoteId() : null;

        if (noteId != null && !noteId.isBlank()) {
            Note note = noteRepository.findById(noteId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));

            boolean isOwner = note.getStudentId().equals(studentId)
                    || (note.getStudentId() == null && "std-1".equals(studentId));
            if (!isOwner) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this note");
            }

            if (content == null || content.isBlank()) content = note.getContent();
            if (title == null || title.isBlank()) title = note.getTitle();
            if (course == null || course.isBlank()) course = note.getCourse();
        }

        if (content == null || content.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is required for AI summarization");
        }
        if (content.length() > 50000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content exceeds 50,000 characters limit");
        }

        NoteSummaryDataDto summaryData = summarizerService.generateSummary(title, course, content);
        return new NoteSummarizeResponse(true, summaryData);
    }

    private List<String> parseTags(Object rawTags) {
        List<String> result = new ArrayList<>();
        if (rawTags == null) return result;

        if (rawTags instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    String str = item.toString().trim();
                    if (!str.isBlank()) {
                        result.add(str.length() > 30 ? str.substring(0, 30) : str);
                    }
                }
                if (result.size() >= 20) break;
            }
        } else if (rawTags instanceof String str && !str.isBlank()) {
            String[] parts = str.split(",");
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isBlank()) {
                    result.add(trimmed.length() > 30 ? trimmed.substring(0, 30) : trimmed);
                }
                if (result.size() >= 20) break;
            }
        }
        return result;
    }

    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> deserializeTags(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private NoteDto toDto(Note n) {
        List<String> tags = deserializeTags(n.getTags());
        String createdAtStr = n.getCreatedAt() != null
                ? n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String updatedAtStr = n.getUpdatedAt() != null
                ? n.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : createdAtStr;

        return new NoteDto(
                n.getId(),
                n.getStudentId(),
                n.getTitle(),
                n.getCourse(),
                n.getContent(),
                tags,
                createdAtStr,
                updatedAtStr
        );
    }
}
