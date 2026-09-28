package br.com.fiap.sanguebom.service.notification;

import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.Exam;
import br.com.fiap.sanguebom.model.entities.Notification;
import br.com.fiap.sanguebom.model.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.fiap.sanguebom.service.MessageService;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * CF-348 - liberar o exame cria a notificacao de resultado disponivel para aquele cidadao.
 */
@ExtendWith(MockitoExtension.class)
class ExamNotificationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long EXAM_ID = 10001L;

    @Mock
    private NotificationDispatcher dispatcher;

    private ExamNotificationService service;

    @BeforeEach
    void setUp() {
        service = new ExamNotificationService(dispatcher, new MessageService(realMessageSource()));
    }

    @Test
    @DisplayName("o aviso vai para o dono do exame, com o exame como chave do ciclo")
    void shouldNotifyExamOwner() {
        final Exam exam = exam();
        given(dispatcher.dispatch(any(AppUser.class), eq(NotificationType.EXAM_RESULT_AVAILABLE),
                anyString(), anyString(), anyString())).willReturn(Optional.of(new Notification()));

        service.notifyResultAvailable(exam);

        then(dispatcher).should().dispatch(eq(exam.getUser()),
                eq(NotificationType.EXAM_RESULT_AVAILABLE),
                eq("Resultado de exame disponível"),
                eq("O resultado do exame coletado em 15/08/2026 já está disponível para consulta no Sangue Bom."),
                eq("EXAM:10001"));
    }

    @Test
    @DisplayName("a chave do ciclo identifica o exame, entao o mesmo exame nao avisa duas vezes")
    void shouldUseExamAsCycleKey() {
        assertThat(ExamNotificationService.referenceKeyOf(EXAM_ID)).isEqualTo("EXAM:10001");
    }

    private static Exam exam() {
        final AppUser user = new AppUser();
        user.setId(USER_ID);
        final Exam exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setUser(user);
        exam.setCollectedAt(OffsetDateTime.of(2026, 8, 15, 8, 30, 0, 0, ZoneOffset.UTC));
        return exam;
    }

    private static ResourceBundleMessageSource realMessageSource() {
        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
}
