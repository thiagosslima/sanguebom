"use client";

import { useEffect, useMemo, useState } from "react";

type Page<T> = { content: T[]; totalPages: number; totalElements: number };
type Marker = { id: number; code: string; name: string; unit: string };
type Exam = { id: number; collectedAt: string; healthUnitName: string };
type Point = {
  examId: number;
  collectedAt: string;
  valueNumeric: number | null;
  valueText: string | null;
  unit: string;
  flag: string;
  matchedRule: {
    minValue: number | null;
    maxValue: number | null;
    description: string;
  } | null;
};
type Comparison = {
  exams: { examId: number; collectedAt: string }[];
  items: {
    itemCode: string;
    itemName: string;
    unit: string;
    values: {
      examId: number;
      valueNumeric: number | null;
      valueText: string | null;
      unit: string;
      flag: string;
    }[];
    variations: {
      fromExamId: number;
      toExamId: number;
      absoluteVariation: number | null;
      percentVariation: number | null;
    }[];
  }[];
};
const flags: Record<string, string> = {
  NORMAL: "Normal",
  OPTIMAL: "Ideal",
  LOW: "Baixo",
  HIGH: "Alto",
  VERY_HIGH: "Muito alto",
  ATTENTION: "Atenção",
  ALERTA: "Alerta",
  IN_ANALYSIS: "Em análise",
};
const date = (value: string) => new Date(value).toLocaleDateString("pt-BR");
const number = (value: number | null | undefined) =>
  value == null
    ? "—"
    : value.toLocaleString("pt-BR", { maximumFractionDigits: 2 });

async function get<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api/backend${path}`, { signal });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(
      body?.message ||
        body?.detail ||
        "Não foi possível carregar os dados. Tente novamente.",
    );
  }
  return response.json();
}

export default function Clinical({ userId }: { userId: number }) {
  const [markers, setMarkers] = useState<Marker[]>([]);
  const [exams, setExams] = useState<Exam[]>([]);
  const [marker, setMarker] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const [timeline, setTimeline] = useState<Page<Point> | null>(null);
  const [selected, setSelected] = useState<number[]>([]);
  const [comparison, setComparison] = useState<Comparison | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadingTimeline, setLoadingTimeline] = useState(false);
  const [loadingComparison, setLoadingComparison] = useState(false);
  const [error, setError] = useState("");
  const [timelineError, setTimelineError] = useState("");
  const [comparisonError, setComparisonError] = useState("");
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setSelected([]);
    setComparison(null);
    setTimeline(null);
    setPage(0);
    async function load() {
      try {
        const [items, first] = await Promise.all([
          get<Marker[]>("/api/examItems", controller.signal),
          get<Page<Exam>>(
            `/api/users/${userId}/exams?size=100`,
            controller.signal,
          ),
        ]);
        const all = [...first.content];
        for (let index = 1; index < first.totalPages; index++) {
          const next = await get<Page<Exam>>(
            `/api/users/${userId}/exams?size=100&page=${index}`,
            controller.signal,
          );
          all.push(...next.content);
        }
        if (controller.signal.aborted) return;
        setMarkers(items);
        setExams(all);
        setMarker((current) =>
          items.some((item) => item.code === current)
            ? current
            : items[0]?.code || "",
        );
      } catch (err) {
        if (!controller.signal.aborted)
          setError(
            err instanceof Error ? err.message : "Falha ao carregar exames.",
          );
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }
    void load();
    return () => controller.abort();
  }, [userId, revision]);

  useEffect(() => {
    if (!marker || loading) return;
    const controller = new AbortController();
    setTimeline(null);
    setTimelineError("");
    setLoadingTimeline(false);
    if (from && to && from > to) {
      setTimelineError(
        "A data inicial deve ser anterior ou igual à data final.",
      );
      return;
    }
    setLoadingTimeline(true);
    const query = new URLSearchParams({
      itemCode: marker,
      size: "20",
      page: String(page),
    });
    if (from) query.set("from", from);
    if (to) query.set("to", to);
    get<Page<Point>>(
      `/api/v1/doctor/patients/${userId}/timeline?${query}`,
      controller.signal,
    )
      .then(setTimeline)
      .catch((err) => {
        if (!controller.signal.aborted) setTimelineError(err.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoadingTimeline(false);
      });
    return () => controller.abort();
  }, [marker, from, to, page, userId, loading, revision]);

  useEffect(() => {
    setComparison(null);
    setComparisonError("");
    setLoadingComparison(false);
    if (selected.length < 2) return;
    const controller = new AbortController();
    setLoadingComparison(true);
    get<Comparison>(
      `/api/v1/doctor/patients/${userId}/exams/compare?examIds=${selected.join(",")}`,
      controller.signal,
    )
      .then(setComparison)
      .catch((err) => {
        if (!controller.signal.aborted) setComparisonError(err.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoadingComparison(false);
      });
    return () => controller.abort();
  }, [selected, userId]);

  const chart = useMemo(() => {
    const values = (timeline?.content || [])
      .filter((point) => point.valueNumeric != null)
      .sort((a, b) => Date.parse(a.collectedAt) - Date.parse(b.collectedAt));
    if (!values.length) return null;
    const units = new Set(values.map((point) => point.unit));
    if (units.size > 1) return null;
    const min = Math.min(...values.map((point) => point.valueNumeric!));
    const max = Math.max(...values.map((point) => point.valueNumeric!));
    const spread = max - min || Math.max(Math.abs(max) * 0.1, 1);
    const start = Date.parse(values[0].collectedAt);
    const duration = Date.parse(values[values.length - 1].collectedAt) - start;
    return {
      min,
      max,
      values: values.map((point) => ({
        ...point,
        x: duration
          ? 65 + ((Date.parse(point.collectedAt) - start) / duration) * 650
          : 390,
        y: 175 - ((point.valueNumeric! - min) / spread) * 120,
      })),
    };
  }, [timeline]);

  if (loading)
    return (
      <div className="panel" role="status">
        Carregando acompanhamento clínico…
      </div>
    );
  if (error)
    return (
      <div className="panel">
        <p className="error" role="alert">
          {error}
        </p>
        <button
          className="button secondary"
          onClick={() => setRevision((value) => value + 1)}
        >
          Tentar novamente
        </button>
      </div>
    );

  return (
    <div style={{ display: "grid", gap: 24 }}>
      <section className="panel" aria-labelledby="timeline-title">
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            gap: 16,
            flexWrap: "wrap",
          }}
        >
          <div>
            <h2 id="timeline-title">Sua saúde ao longo do tempo</h2>
            <p className="muted">
              Acompanhe cada marcador e entenda a evolução dos seus resultados.
            </p>
          </div>
          <span className="badge">Evolução clínica</span>
        </div>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
            gap: 16,
            margin: "24px 0",
          }}
        >
          <label className="field">
            Marcador
            <select
              value={marker}
              onChange={(event) => {
                setMarker(event.target.value);
                setPage(0);
              }}
            >
              {markers.map((item) => (
                <option key={item.id} value={item.code}>
                  {item.name} ({item.code})
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            De
            <input
              type="date"
              value={from}
              max={to || undefined}
              onChange={(event) => {
                setFrom(event.target.value);
                setPage(0);
              }}
            />
          </label>
          <label className="field">
            Até
            <input
              type="date"
              value={to}
              min={from || undefined}
              onChange={(event) => {
                setTo(event.target.value);
                setPage(0);
              }}
            />
          </label>
        </div>
        {timelineError && (
          <p className="error" role="alert">
            {timelineError}
          </p>
        )}
        {loadingTimeline && (
          <p role="status" className="muted">
            Consultando a evolução…
          </p>
        )}
        {!loadingTimeline &&
          !timelineError &&
          (!timeline || timeline.content.length === 0) && (
            <div className="empty">
              Nenhum resultado para este marcador no período. Escolha outro
              marcador ou amplie as datas.
            </div>
          )}
        {chart && (
          <figure style={{ margin: "16px 0" }}>
            <svg
              viewBox="0 0 780 230"
              role="img"
              aria-label={`Evolução de ${markers.find((item) => item.code === marker)?.name || marker}, em ${chart.values[0].unit || "unidades"}. Valores detalhados na tabela abaixo.`}
              style={{ display: "block", width: "100%", maxHeight: 300 }}
            >
              {[55, 115, 175].map((y) => (
                <line
                  key={y}
                  x1="65"
                  x2="715"
                  y1={y}
                  y2={y}
                  stroke="#e7edf0"
                  strokeDasharray="4 5"
                />
              ))}
              <text x="8" y="59" fontSize="12" fill="#617078">
                {number(chart.max)}
              </text>
              <text x="8" y="179" fontSize="12" fill="#617078">
                {number(chart.min)}
              </text>
              <polyline
                fill="none"
                stroke="#d55263"
                strokeWidth="3"
                strokeLinejoin="round"
                points={chart.values
                  .map((point) => `${point.x},${point.y}`)
                  .join(" ")}
              />
              {chart.values.map((point, index) => (
                <circle
                  key={`${point.examId}-${index}`}
                  cx={point.x}
                  cy={point.y}
                  r="5"
                  fill="#d55263"
                  stroke="white"
                  strokeWidth="2"
                >
                  <title>
                    {date(point.collectedAt)}: {number(point.valueNumeric)}{" "}
                    {point.unit} — {flags[point.flag] || point.flag}
                  </title>
                </circle>
              ))}
              <text x="65" y="213" fontSize="12" fill="#617078">
                {date(chart.values[0].collectedAt)}
              </text>
              <text
                x="715"
                y="213"
                textAnchor="end"
                fontSize="12"
                fill="#617078"
              >
                {date(chart.values[chart.values.length - 1].collectedAt)}
              </text>
            </svg>
            <figcaption className="muted">
              {chart.values[0].unit || "Valores numéricos"} · Resultados da
              página atual, em ordem cronológica.
            </figcaption>
          </figure>
        )}
        {timeline && timeline.content.length > 0 && (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Coleta</th>
                    <th>Resultado</th>
                    <th>Classificação</th>
                    <th>Regra aplicada</th>
                  </tr>
                </thead>
                <tbody>
                  {timeline.content.map((point, index) => (
                    <tr key={`${point.examId}-${index}`}>
                      <td>
                        {date(point.collectedAt)}
                        <small style={{ display: "block" }} className="muted">
                          Exame #{point.examId}
                        </small>
                      </td>
                      <td>
                        <strong>
                          {point.valueNumeric == null
                            ? point.valueText || "—"
                            : number(point.valueNumeric)}
                        </strong>{" "}
                        {point.unit}
                      </td>
                      <td>
                        <span className="badge">
                          {flags[point.flag] ||
                            point.flag ||
                            "Não classificado"}
                        </span>
                      </td>
                      <td>
                        {point.matchedRule?.description || "Não informada"}
                        {point.matchedRule && (
                          <small className="muted" style={{ display: "block" }}>
                            Limites: {number(point.matchedRule.minValue)} a{" "}
                            {number(point.matchedRule.maxValue)}
                          </small>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div
              style={{
                display: "flex",
                gap: 12,
                alignItems: "center",
                justifyContent: "flex-end",
                marginTop: 18,
              }}
            >
              <button
                className="button secondary"
                disabled={page === 0}
                onClick={() => setPage((value) => value - 1)}
              >
                Anterior
              </button>
              <span className="muted">
                {page + 1} de {timeline.totalPages}
              </span>
              <button
                className="button secondary"
                disabled={page + 1 >= timeline.totalPages}
                onClick={() => setPage((value) => value + 1)}
              >
                Próxima
              </button>
            </div>
          </>
        )}
      </section>
      <section className="panel" aria-labelledby="comparison-title">
        <h2 id="comparison-title">Compare seus exames</h2>
        <p className="muted">
          Selecione de dois a quatro exames para visualizar os resultados lado a
          lado.
        </p>
        {exams.length < 2 ? (
          <div className="empty">
            Você precisa de pelo menos dois exames para fazer uma comparação.
          </div>
        ) : (
          <fieldset style={{ border: 0, padding: 0, margin: "20px 0" }}>
            <legend className="muted">
              {selected.length} de 4 exames selecionados
            </legend>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
                gap: 10,
                maxHeight: 280,
                overflowY: "auto",
                padding: "12px 0",
              }}
            >
              {exams.map((exam) => (
                <label
                  key={exam.id}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 10,
                    padding: 14,
                    border: `1px solid ${selected.includes(exam.id) ? "#d55263" : "#e7edf0"}`,
                    borderRadius: 12,
                    cursor: "pointer",
                  }}
                >
                  <input
                    type="checkbox"
                    checked={selected.includes(exam.id)}
                    disabled={
                      !selected.includes(exam.id) && selected.length >= 4
                    }
                    onChange={(event) => {
                      setSelected((current) =>
                        event.target.checked
                          ? [...current, exam.id]
                          : current.filter((id) => id !== exam.id),
                      );
                    }}
                  />
                  <span>
                    <strong>{date(exam.collectedAt)}</strong>
                    <small className="muted" style={{ display: "block" }}>
                      {exam.healthUnitName || "Unidade não informada"} · #
                      {exam.id}
                    </small>
                  </span>
                </label>
              ))}
            </div>
          </fieldset>
        )}
        {loadingComparison && (
          <p role="status" className="muted">
            Comparando os resultados…
          </p>
        )}
        {comparisonError && (
          <p role="alert" className="error">
            {comparisonError}
          </p>
        )}
        {comparison && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Marcador</th>
                  {comparison.exams.map((exam) => (
                    <th key={exam.examId}>
                      {date(exam.collectedAt)}
                      <small className="muted" style={{ display: "block" }}>
                        Exame #{exam.examId}
                      </small>
                    </th>
                  ))}
                  <th>Variação entre coletas</th>
                </tr>
              </thead>
              <tbody>
                {comparison.items.map((item) => (
                  <tr key={item.itemCode}>
                    <td>
                      <strong>{item.itemName}</strong>
                      <small className="muted" style={{ display: "block" }}>
                        {item.itemCode}
                      </small>
                    </td>
                    {comparison.exams.map((exam) => {
                      const result = item.values.find(
                        (value) => value.examId === exam.examId,
                      );
                      return (
                        <td key={exam.examId}>
                          {result ? (
                            <>
                              {result.valueNumeric == null
                                ? result.valueText || "—"
                                : number(result.valueNumeric)}{" "}
                              {result.unit || item.unit}
                              <small
                                className="muted"
                                style={{ display: "block" }}
                              >
                                {flags[result.flag] || result.flag}
                              </small>
                            </>
                          ) : (
                            "Não medido"
                          )}
                        </td>
                      );
                    })}
                    <td>
                      {item.variations.length
                        ? item.variations.map((variation) => (
                            <small
                              key={`${variation.fromExamId}-${variation.toExamId}`}
                              style={{ display: "block", margin: "4px 0" }}
                            >
                              #{variation.fromExamId} → #{variation.toExamId}:{" "}
                              {number(variation.absoluteVariation)} {item.unit}
                              {variation.percentVariation != null
                                ? ` (${variation.percentVariation > 0 ? "+" : ""}${number(variation.percentVariation)}%)`
                                : ""}
                            </small>
                          ))
                        : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {comparison.items.length === 0 && (
              <p className="empty">
                Os exames selecionados ainda não têm resultados registrados.
              </p>
            )}
          </div>
        )}
      </section>
    </div>
  );
}
