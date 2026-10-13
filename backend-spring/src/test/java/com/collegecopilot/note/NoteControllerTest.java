package com.collegecopilot.note;

import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.note.dto.*;
import com.collegecopilot.security.JwtAuthenticationFilter;
import com.collegecopilot.security.JwtTokenProvider;
import com.collegecopilot.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NoteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NoteService noteService;

    @InjectMocks
    private NoteController noteController;

    private JwtTokenProvider tokenProvider;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        tokenA = tokenProvider.generateToken("u-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
        tokenB = tokenProvider.generateToken("u-2", "Bob Student", "bob@student.edu", "STUDENT", "std-2", null);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(noteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/notes returns 200 with notes list when authenticated")
    void getNotes_authenticated_returns200() throws Exception {
        NoteDto noteDto = new NoteDto("nt-1", "std-1", "BST", "CS 201", "Content", List.of("Trees"), "2026-03-30T10:00:00", "2026-03-30T10:00:00");
        when(noteService.getNotes(any(UserPrincipal.class))).thenReturn(new NoteListResponse(true, List.of(noteDto)));

        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("nt-1"))
                .andExpect(jsonPath("$.data[0].title").value("BST"))
                .andExpect(jsonPath("$.data[0].course").value("CS 201"));
    }

    @Test
    @DisplayName("GET /api/notes returns 401 when unauthenticated")
    void getNotes_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/notes returns 201 with created note when authenticated")
    void createNote_authenticated_returns201() throws Exception {
        NoteDto created = new NoteDto("nt-2", "std-1", "AVL Trees", "CS 201", "Content", List.of("AVL"), "2026-03-30T10:00:00", "2026-03-30T10:00:00");
        when(noteService.createNote(any(CreateNoteRequest.class), any(UserPrincipal.class)))
                .thenReturn(new NoteResponse(true, created));

        String requestJson = """
                {
                    "title": "AVL Trees",
                    "course": "CS 201",
                    "content": "Content",
                    "tags": ["AVL"]
                }
                """;

        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("nt-2"))
                .andExpect(jsonPath("$.data.title").value("AVL Trees"));
    }

    @Test
    @DisplayName("POST /api/notes returns 401 when unauthenticated")
    void createNote_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/notes returns 400 when title is missing")
    void createNote_invalidData_returns400() throws Exception {
        when(noteService.createNote(any(CreateNoteRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required (1-200 characters)"));

        String requestJson = """
                {
                    "title": "",
                    "content": "Valid content"
                }
                """;

        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Title is required (1-200 characters)"));
    }

    @Test
    @DisplayName("PUT /api/notes/{id} returns 200 when updated by owner")
    void updateNote_byOwner_returns200() throws Exception {
        NoteDto updated = new NoteDto("nt-1", "std-1", "Updated BST", "CS 201", "Updated content", List.of("Trees"), "2026-03-30T10:00:00", "2026-03-30T10:00:00");
        when(noteService.updateNote(eq("nt-1"), any(UpdateNoteRequest.class), any(UserPrincipal.class)))
                .thenReturn(new NoteResponse(true, updated));

        String updateJson = """
                {
                    "title": "Updated BST",
                    "content": "Updated content"
                }
                """;

        mockMvc.perform(put("/api/notes/nt-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated BST"));
    }

    @Test
    @DisplayName("PUT /api/notes/{id} returns 403 when updating another student's note (IDOR protection)")
    void updateNote_crossStudent_returns403() throws Exception {
        when(noteService.updateNote(eq("nt-1"), any(UpdateNoteRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this note"));

        mockMvc.perform(put("/api/notes/nt-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Hacked\"}")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this note"));
    }

    @Test
    @DisplayName("PUT /api/notes/{id} returns 404 when note is not found")
    void updateNote_notFound_returns404() throws Exception {
        when(noteService.updateNote(eq("missing"), any(UpdateNoteRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));

        mockMvc.perform(put("/api/notes/missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Title\"}")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/notes/{id} returns 200 when deleted by owner")
    void deleteNote_byOwner_returns200() throws Exception {
        when(noteService.deleteNote(eq("nt-1"), any(UserPrincipal.class)))
                .thenReturn(new NoteDeleteResponse(true, "Note deleted"));

        mockMvc.perform(delete("/api/notes/nt-1")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Note deleted"));
    }

    @Test
    @DisplayName("DELETE /api/notes/{id} returns 403 when deleting another student's note")
    void deleteNote_crossStudent_returns403() throws Exception {
        when(noteService.deleteNote(eq("nt-1"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this note"));

        mockMvc.perform(delete("/api/notes/nt-1")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/notes/{id} returns 404 when note not found")
    void deleteNote_notFound_returns404() throws Exception {
        when(noteService.deleteNote(eq("missing"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));

        mockMvc.perform(delete("/api/notes/missing")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/ai/summarize returns 200 with summary data")
    void summarizeNotes_authenticated_returns200() throws Exception {
        NoteSummaryDataDto summaryData = new NoteSummaryDataDto(
                "BST", "CS 201", 30, 1, "BST summary",
                List.of("• Invariant rule"),
                List.of(new FlashcardDto("Q1", "A1")),
                List.of(new QuizItemDto(1, "Question", List.of("O1", "O2", "O3", "O4"), 0, "Expl"))
        );
        when(noteService.summarizeNotes(any(NoteSummarizeRequest.class), any(UserPrincipal.class)))
                .thenReturn(new NoteSummarizeResponse(true, summaryData));

        String requestJson = """
                {
                    "title": "BST",
                    "course": "CS 201",
                    "content": "A binary search tree satisfies the BST invariant."
                }
                """;

        mockMvc.perform(post("/api/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("BST"))
                .andExpect(jsonPath("$.data.summary").value("BST summary"))
                .andExpect(jsonPath("$.data.flashcards[0].question").value("Q1"))
                .andExpect(jsonPath("$.data.quiz[0].id").value(1));
    }

    @Test
    @DisplayName("POST /api/ai/summarize returns 401 when unauthenticated")
    void summarizeNotes_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Valid text\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/ai/summarize returns 400 when content is blank")
    void summarizeNotes_blankContent_returns400() throws Exception {
        when(noteService.summarizeNotes(any(NoteSummarizeRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is required for AI summarization"));

        mockMvc.perform(post("/api/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"   \"}")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/ai/summarize returns 403 when referencing another student's noteId")
    void summarizeNotes_crossStudentNoteId_returns403() throws Exception {
        when(noteService.summarizeNotes(any(NoteSummarizeRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this note"));

        mockMvc.perform(post("/api/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noteId\": \"nt-1\"}")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
