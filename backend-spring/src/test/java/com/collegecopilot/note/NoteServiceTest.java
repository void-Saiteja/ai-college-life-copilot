package com.collegecopilot.note;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.note.dto.*;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.Student;
import com.collegecopilot.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private GeminiNoteSummarizerService summarizerService;

    private NoteService noteService;

    private UserPrincipal studentA;
    private UserPrincipal studentB;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(noteRepository, studentRepository, summarizerService);

        studentA = new UserPrincipal("u-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
        studentB = new UserPrincipal("u-2", "Bob Smith", "bob@student.edu", "STUDENT", "std-2", null);

        Student stdA = new Student();
        stdA.setId("std-1");
        stdA.setUserId("u-1");
        lenient().when(studentRepository.findById("std-1")).thenReturn(Optional.of(stdA));

        Student stdB = new Student();
        stdB.setId("std-2");
        stdB.setUserId("u-2");
        lenient().when(studentRepository.findById("std-2")).thenReturn(Optional.of(stdB));
    }

    @Test
    @DisplayName("getNotes returns notes for authenticated student")
    void getNotes_returnsStudentNotes() {
        Note n = new Note("nt-1", "std-1", "B-Trees", "CS 340", "B-Trees are balanced trees", "[\"Trees\"]");
        n.setCreatedAt(LocalDateTime.now());
        n.setUpdatedAt(LocalDateTime.now());
        when(noteRepository.findByStudentIdWithLegacy("std-1", true)).thenReturn(List.of(n));

        NoteListResponse response = noteService.getNotes(studentA);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("B-Trees", response.getData().get(0).getTitle());
        assertEquals(List.of("Trees"), response.getData().get(0).getTags());
    }

    @Test
    @DisplayName("createNote creates note with parsed tags and default course")
    void createNote_success() {
        CreateNoteRequest request = new CreateNoteRequest("Master Theorem", null, "T(n) = aT(n/b) + f(n)", List.of("Algorithms", "Math"));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteResponse response = noteService.createNote(request, studentA);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("Master Theorem", response.getData().getTitle());
        assertEquals("General", response.getData().getCourse());
        assertEquals(List.of("Algorithms", "Math"), response.getData().getTags());
        verify(noteRepository, times(1)).save(any(Note.class));
    }

    @Test
    @DisplayName("createNote with blank title throws 400 Bad Request")
    void createNote_blankTitle_throws400() {
        CreateNoteRequest request = new CreateNoteRequest("   ", "CS 201", "Content", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                noteService.createNote(request, studentA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Title is required"));
    }

    @Test
    @DisplayName("createNote with title exceeding 200 characters throws 400 Bad Request")
    void createNote_longTitle_throws400() {
        String longTitle = "A".repeat(201);
        CreateNoteRequest request = new CreateNoteRequest(longTitle, "CS 201", "Content", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                noteService.createNote(request, studentA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("createNote with blank content throws 400 Bad Request")
    void createNote_blankContent_throws400() {
        CreateNoteRequest request = new CreateNoteRequest("Valid Title", "CS 201", "   ", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                noteService.createNote(request, studentA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Content is required"));
    }

    @Test
    @DisplayName("createNote with content exceeding 50,000 characters throws 400 Bad Request")
    void createNote_exceedsMaxLength_throws400() {
        String hugeContent = "X".repeat(50001);
        CreateNoteRequest request = new CreateNoteRequest("Valid Title", "CS 201", hugeContent, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                noteService.createNote(request, studentA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("50,000 characters"));
    }

    @Test
    @DisplayName("createNote with course exceeding 60 characters throws 400 Bad Request")
    void createNote_longCourse_throws400() {
        String longCourse = "C".repeat(61);
        CreateNoteRequest request = new CreateNoteRequest("Valid Title", longCourse, "Valid content", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                noteService.createNote(request, studentA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("updateNote updates existing note owned by student")
    void updateNote_byOwner_success() {
        Note existing = new Note("nt-1", "std-1", "Old Title", "CS 101", "Old Content", "[]");
        when(noteRepository.findById("nt-1")).thenReturn(Optional.of(existing));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateNoteRequest req = new UpdateNoteRequest("New Title", "CS 201", "New Content", "tag1, tag2");
        NoteResponse res = noteService.updateNote("nt-1", req, studentA);

        assertTrue(res.isSuccess());
        assertEquals("New Title", res.getData().getTitle());
        assertEquals("CS 201", res.getData().getCourse());
        assertEquals(List.of("tag1", "tag2"), res.getData().getTags());
    }

    @Test
    @DisplayName("updateNote on another student's note throws 403 Forbidden")
    void updateNote_crossStudent_throws403() {
        Note existing = new Note("nt-1", "std-1", "Alex Note", "CS 101", "Secret content", "[]");
        when(noteRepository.findById("nt-1")).thenReturn(Optional.of(existing));

        UpdateNoteRequest req = new UpdateNoteRequest("Hacked Title", null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                noteService.updateNote("nt-1", req, studentB));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You do not own this note"));
    }

    @Test
    @DisplayName("updateNote for non-existent note throws 404 Not Found")
    void updateNote_notFound_throws404() {
        when(noteRepository.findById("missing-id")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                noteService.updateNote("missing-id", new UpdateNoteRequest("T", null, null, null), studentA));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("deleteNote deletes owned note")
    void deleteNote_byOwner_success() {
        Note existing = new Note("nt-1", "std-1", "Title", "CS 101", "Content", "[]");
        when(noteRepository.findById("nt-1")).thenReturn(Optional.of(existing));

        NoteDeleteResponse res = noteService.deleteNote("nt-1", studentA);

        assertTrue(res.isSuccess());
        assertEquals("Note deleted", res.getMessage());
        verify(noteRepository, times(1)).delete(existing);
    }

    @Test
    @DisplayName("deleteNote on another student's note throws 403 Forbidden")
    void deleteNote_crossStudent_throws403() {
        Note existing = new Note("nt-1", "std-1", "Title", "CS 101", "Content", "[]");
        when(noteRepository.findById("nt-1")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                noteService.deleteNote("nt-1", studentB));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You do not own this note"));
        verify(noteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteNote for non-existent note throws 404 Not Found")
    void deleteNote_notFound_throws404() {
        when(noteRepository.findById("missing-id")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                noteService.deleteNote("missing-id", studentA));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("summarizeNotes uses noteId content when noteId is supplied")
    void summarizeNotes_withNoteId_usesExistingNote() {
        Note existing = new Note("nt-1", "std-1", "DB Indexing", "CS 340", "B+ Trees leaf nodes are linked for range queries.", "[]");
        when(noteRepository.findById("nt-1")).thenReturn(Optional.of(existing));

        NoteSummaryDataDto summaryData = new NoteSummaryDataDto(
                "DB Indexing", "CS 340", 10, 1, "Summary", List.of("• Point 1"), List.of(), List.of()
        );
        when(summarizerService.generateSummary(eq("DB Indexing"), eq("CS 340"), eq(existing.getContent())))
                .thenReturn(summaryData);

        NoteSummarizeRequest req = new NoteSummarizeRequest(null, null, null, "nt-1");
        NoteSummarizeResponse res = noteService.summarizeNotes(req, studentA);

        assertTrue(res.isSuccess());
        assertEquals("DB Indexing", res.getData().getTitle());
        assertEquals("CS 340", res.getData().getCourse());
    }

    @Test
    @DisplayName("summarizeNotes with another student's noteId throws 403 Forbidden")
    void summarizeNotes_crossStudentNoteId_throws403() {
        Note existing = new Note("nt-1", "std-1", "Alex Note", "CS 101", "Private", "[]");
        when(noteRepository.findById("nt-1")).thenReturn(Optional.of(existing));

        NoteSummarizeRequest req = new NoteSummarizeRequest(null, null, null, "nt-1");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                noteService.summarizeNotes(req, studentB));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You do not own this note"));
    }

    @Test
    @DisplayName("summarizeNotes with blank content and no noteId throws 400 Bad Request")
    void summarizeNotes_blankContent_throws400() {
        NoteSummarizeRequest req = new NoteSummarizeRequest("Title", "CS 101", "   ", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                noteService.summarizeNotes(req, studentA));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Content is required"));
    }
}
