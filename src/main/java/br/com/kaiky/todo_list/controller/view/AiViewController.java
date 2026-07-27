package br.com.kaiky.todo_list.controller.view;

import br.com.kaiky.todo_list.dto.AiChatRequestDTO;
import br.com.kaiky.todo_list.service.AiService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AiViewController {
    private final AiService aiService;

    public AiViewController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public String chat (
            @Valid @ModelAttribute AiChatRequestDTO dto,
            Model model
    ) {
        model.addAttribute(
                "chat",
                aiService.chat(dto)
        );

        model.addAttribute(
                "aiRequest",
                new AiChatRequestDTO(null, "")
        );
        
        return "chat";

    }
}
