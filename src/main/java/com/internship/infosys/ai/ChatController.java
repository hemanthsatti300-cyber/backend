package com.internship.infosys.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(
    origins = "https://agent-6a7decacc5a0a1ca77--bespoke-begonia-5037cf.netlify.app"
)
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request) {

        return ResponseEntity.ok(
                chatService.chat(request)
        );
    }
}
