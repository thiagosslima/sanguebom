package br.com.fiap.sanguebom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sanguebom.notifications")
public record NotificationProperties(Sse sse, ExamGoal examGoal) {

    public NotificationProperties {
        sse = sse == null ? new Sse(null, null) : sse;
        examGoal = examGoal == null ? new ExamGoal(null) : examGoal;
    }

    public record Sse(Long timeoutMs, Long heartbeatMs) {

        public Sse {
            timeoutMs = timeoutMs == null ? 1_800_000L : timeoutMs;
            heartbeatMs = heartbeatMs == null ? 25_000L : heartbeatMs;
        }
    }

    public record ExamGoal(Integer pageSize) {

        public ExamGoal {
            pageSize = pageSize == null || pageSize < 1 ? 200 : pageSize;
        }
    }
}
