package br.com.fiap.sanguebom.model.exam;

import br.com.fiap.sanguebom.model.entities.AppUser;
import br.com.fiap.sanguebom.model.entities.ExamItem;
import br.com.fiap.sanguebom.model.entities.HealthUnit;

import java.util.Map;

public record ExamCreationContext(
        AppUser user,
        HealthUnit healthUnit,
        Map<Long, ExamItem> examItems
) {
}
