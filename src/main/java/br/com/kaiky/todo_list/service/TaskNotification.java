package br.com.kaiky.todo_list.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TaskNotification {

    private final RestClient restClient;
    private final String fromEmail;

    public TaskNotification(
            RestClient.Builder restClientBuilder,
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-email}") String fromEmail
    ) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.fromEmail = fromEmail;
    }

    public void sendTaskNotification(TaskNotificationEmail email) {
        ResendEmailRequest request = new ResendEmailRequest(
                "Liyz <" + fromEmail + ">",
                List.of(email.targetEmail()),
                "Task reminder: " + email.taskTitle(),
                buildHtml(email),
                buildText(email)
        );

        restClient
                .post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private String buildHtml(TaskNotificationEmail email) {
        return """
                <div style="font-family:Arial,sans-serif;line-height:1.5;color:#111827">
                    <h1 style="margin:0 0 16px;color:#7c3aed">Task reminder</h1>
                    <p>Hello, %s.</p>
                    <p>Your task is due now:</p>
                    <p><strong>%s</strong></p>
                    <p>%s</p>
                </div>
                """.formatted(
                escapeHtml(email.userName()),
                escapeHtml(email.taskTitle()),
                escapeHtml(email.taskDescription())
        );
    }

    private String buildText(TaskNotificationEmail email) {
        return """
                Hello, %s.

                Your task is due now:
                %s

                %s
                """.formatted(
                email.userName(),
                email.taskTitle(),
                email.taskDescription()
        );
    }

    private String escapeHtml(String value) {
        String safeValue = value == null ? "" : value;

        return safeValue
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }

    public record TaskNotificationEmail(
            String targetEmail,
            String userName,
            String taskTitle,
            String taskDescription
    ) {
    }

    private record ResendEmailRequest(
            String from,
            List<String> to,
            String subject,
            String html,
            String text
    ) {
    }
}
