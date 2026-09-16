package br.com.fiap.sanguebom.service.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExamGoalNotificationJob {

    private final ExamGoalNotificationService examGoalNotificationService;

    public ExamGoalNotificationJob(final ExamGoalNotificationService examGoalNotificationService) {
        this.examGoalNotificationService = examGoalNotificationService;
    }

    @Scheduled(cron = "${sanguebom.notifications.exam-goal.cron:0 0 8 * * *}",
            zone = "${sanguebom.notifications.exam-goal.zone:America/Sao_Paulo}")
    public void run() {
        examGoalNotificationService.scanActiveUsers();
    }
}
