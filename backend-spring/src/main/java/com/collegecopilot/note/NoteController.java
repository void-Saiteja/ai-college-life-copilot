package com.collegecopilot.note;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.note.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class NoteController {

    private final NoteService noteService;

    @Autowired
    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        return principal;
    }

    // GET /api/notes
    @GetMapping("/api/notes")
    public ResponseEntity<NoteListResponse> getNotes() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(noteService.getNotes(principal));
    }

    // POST /api/notes
    @PostMapping("/api/notes")
    public ResponseEntity<NoteResponse> createNote(@RequestBody CreateNoteRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        NoteResponse response = noteService.createNote(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // PUT /api/notes/{id}
    @PutMapping("/api/notes/{id}")
    public ResponseEntity<NoteResponse> updateNote(@PathVariable("id") String id,
                                                   @RequestBody UpdateNoteRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(noteService.updateNote(id, request, principal));
    }

    // DELETE /api/notes/{id}
    @DeleteMapping("/api/notes/{id}")
    public ResponseEntity<NoteDeleteResponse> deleteNote(@PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(noteService.deleteNote(id, principal));
    }

    // POST /api/ai/summarize
    @PostMapping("/api/ai/summarize")
    public ResponseEntity<NoteSummarizeResponse> summarizeNotes(@RequestBody NoteSummarizeRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(noteService.summarizeNotes(request, principal));
    }
}
