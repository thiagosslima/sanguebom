package br.com.fiap.sanguebom.controller;

import br.com.fiap.sanguebom.config.swagger.docs.DoctorTimelineResourceDocs;
import br.com.fiap.sanguebom.model.doctor.ExamComparisonDTO;
import br.com.fiap.sanguebom.model.doctor.MarkerTimelinePointDTO;
import br.com.fiap.sanguebom.model.userexam.PageResponse;
import br.com.fiap.sanguebom.service.DoctorExamComparisonService;
import br.com.fiap.sanguebom.service.DoctorTimelineService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/doctor/patients/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
public class DoctorTimelineResource implements DoctorTimelineResourceDocs {

    private final DoctorTimelineService doctorTimelineService;
    private final DoctorExamComparisonService doctorExamComparisonService;

    public DoctorTimelineResource(final DoctorTimelineService doctorTimelineService,
            final DoctorExamComparisonService doctorExamComparisonService) {
        this.doctorTimelineService = doctorTimelineService;
        this.doctorExamComparisonService = doctorExamComparisonService;
    }

    @GetMapping("/timeline")
    @Override
    public ResponseEntity<PageResponse<MarkerTimelinePointDTO>> getTimeline(
            @PathVariable(name = "userId") final Long userId,
            @RequestParam(name = "itemCode") final String itemCode,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(name = "page", defaultValue = "0") final int page,
            @RequestParam(name = "size", defaultValue = "10") final int size) {
        return ResponseEntity.ok(doctorTimelineService.timeline(userId, itemCode, from, to, page, size));
    }

    @GetMapping("/exams/compare")
    @Override
    public ResponseEntity<ExamComparisonDTO> compareExams(
            @PathVariable(name = "userId") final Long userId,
            @RequestParam(name = "examIds") final List<Long> examIds) {
        return ResponseEntity.ok(doctorExamComparisonService.compare(userId, examIds));
    }
}
