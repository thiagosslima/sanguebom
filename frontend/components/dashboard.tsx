"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { screenUrl, type Screen } from "../lib/navigation";
import { confirmNavigation } from "../hooks/use-form-feedback";
import {
  newerNotification,
  useNotificationIndicator,
  type NotificationHead,
  type NotificationMarker,
} from "../hooks/use-notification-indicator";
import {
  Activity,
  ArrowRight,
  Award,
  Bell,
  CalendarDays,
  Check,
  ChevronLeft,
  ChevronRight,
  Droplet,
  FileText,
  Heart,
  LayoutDashboard,
  Menu,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  Sparkles,
  Stethoscope,
  UserRound,
  Users,
  X,
} from "lucide-react";
import Clinical from "./clinical";
import { NewExam, NewUser, Profile } from "./manage";
type User = { id: number; name: string; email: string };
type Exam = {
  id: number;
  collectedAt: string;
  healthUnitName: string;
  status: string;
};
type Goal = {
  lastExamDate: string | null;
  dueDate: string | null;
  daysRemaining: number | null;
  periodicity: string;
  status: string;
};
type Detail = Exam & {
  riskScore: number | null;
  riskLevel: string | null;
  explanation: string;
  disclaimer: string;
  items: {
    itemCode: string;
    itemName: string;
    valueNumeric: number | null;
    valueText: string;
    unit: string;
    flag: string;
  }[];
};
type Notice = {
  id: number;
  title: string;
  message: string;
  createdAt: string;
  status: string;
};
type Achievement = {
  id: number;
  name: string;
  description: string;
  active: boolean;
};
type Earned = { user: number; achievement: number; earnedAt: string };
type Page<T> = { content: T[]; totalPages: number; totalElements: number };
const labels: Record<string, string> = {
  RELEASED: "Disponível",
  COMPLETED: "Concluído",
  COLLECTED: "Coletado",
  IN_ANALYSIS: "Em análise",
  NORMAL: "Normal",
  OPTIMAL: "Ótimo",
  LOW: "Baixo",
  HIGH: "Alto",
  VERY_HIGH: "Muito alto",
  ATTENTION: "Atenção",
  ALERTA: "Atenção",
  MODERATE: "Moderado",
  UP_TO_DATE: "Em dia",
  DUE_SOON: "Próximo do prazo",
  OVERDUE: "Prazo vencido",
  NO_HISTORY: "Primeiro passo",
  QUARTERLY: "Trimestral",
  SEMESTERLY: "Semestral",
  YEARLY: "Anual",
};
const date = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("pt-BR", {
        day: "2-digit",
        month: "short",
        year: "numeric",
      }).format(new Date(value.length === 10 ? value + "T12:00:00" : value))
    : "Ainda não disponível";
async function api<T>(path: string, signal?: AbortSignal): Promise<T> {
  const r = await fetch("/api/backend/api/" + path, {
    signal,
    cache: "no-store",
  });
  if (!r.ok) {
    const b = await r.json().catch(() => ({}));
    throw new Error(
      b.message ||
        b.detail ||
        "Não foi possível carregar os dados. Tente novamente.",
    );
  }
  return r.json();
}
function Badge({
  value,
  context = "status",
}: {
  value?: string | null;
  context?: "status" | "risk" | "marker";
}) {
  return (
    <span
      className={
        "badge " +
        (value &&
        [
          "NORMAL",
          "OPTIMAL",
          "RELEASED",
          "COMPLETED",
          "UP_TO_DATE",
          ...(context === "risk" ? ["LOW"] : []),
        ].includes(value)
          ? "green"
          : value && ["HIGH", "VERY_HIGH", "OVERDUE"].includes(value)
            ? "red"
            : "amber")
      }
    >
      {value
        ? context === "risk"
          ? `Risco ${labels[value]?.toLowerCase() || value}`
          : context === "marker" && value === "LOW"
            ? "Abaixo da referência"
            : context === "marker" && value === "HIGH"
              ? "Acima da referência"
              : labels[value] || value
        : "Não avaliado"}
    </span>
  );
}
const nav: { id: Screen; label: string; icon: typeof Activity }[] = [
  { id: "overview", label: "Visão geral", icon: LayoutDashboard },
  { id: "exams", label: "Meus exames", icon: FileText },
  { id: "clinical", label: "Evolução da saúde", icon: Activity },
  { id: "achievements", label: "Minhas conquistas", icon: Award },
  { id: "notifications", label: "Notificações", icon: Bell },
  { id: "profile", label: "Meu perfil", icon: UserRound },
];
export default function Dashboard({
  screen = "overview",
  routeUserId,
  routeExamId,
}: {
  screen?: Screen;
  routeUserId?: number;
  routeExamId?: number;
}) {
  const router = useRouter();
  const menuRef = useRef<HTMLElement>(null);
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const [smallScreen, setSmallScreen] = useState(false);
  const loadedOwner = useRef<number | null>(null);
  const [failures, setFailures] = useState<string[]>([]);
  const [allExams, setAllExams] = useState<Exam[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [unitFilter, setUnitFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [historyReady, setHistoryReady] = useState(false);
  const historyOwner = useRef<number | null>(null);
  const tab = screen;
  const detailId = routeExamId ?? null;
  const [users, setUsers] = useState<User[]>([]),
    [selectedUserId, setSelectedUserId] = useState<number | null>(null),
    [initial, setInitial] = useState(true),
    [initError, setInitError] = useState(""),
    [revision, setRevision] = useState(0),
    [mobile, setMobile] = useState(false);
  const [exams, setExams] = useState<Page<Exam>>({
      content: [],
      totalPages: 0,
      totalElements: 0,
    }),
    [goal, setGoal] = useState<Goal | null>(null),
    [notices, setNotices] = useState<Page<Notice>>({
      content: [],
      totalPages: 0,
      totalElements: 0,
    }),
    [achievements, setAchievements] = useState<Achievement[]>([]),
    [earned, setEarned] = useState<Earned[]>([]),
    [loading, setLoading] = useState(false),
    [error, setError] = useState(""),
    [page, setPage] = useState(0),
    [noticePage, setNoticePage] = useState(0),
    [query, setQuery] = useState(""),
    [detailState, setDetailState] = useState<{
      ownerId: number;
      value: Detail;
    } | null>(null),
    [detailError, setDetailError] = useState("");
  const userId = routeUserId ?? selectedUserId;
  useEffect(() => {
    const media = window.matchMedia("(max-width: 760px)");
    const update = () => {
      setSmallScreen(media.matches);
      if (!media.matches) setMobile(false);
    };
    update();
    media.addEventListener("change", update);
    return () => media.removeEventListener("change", update);
  }, []);
  useEffect(() => {
    if (!mobile || !smallScreen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    menuRef.current?.querySelector<HTMLElement>("button, a")?.focus();
    const keydown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        setMobile(false);
      }
      if (event.key !== "Tab") return;
      const elements = menuRef.current?.querySelectorAll<HTMLElement>(
        "a[href], button:not(:disabled)",
      );
      if (!elements?.length) return;
      const first = elements[0],
        last = elements[elements.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener("keydown", keydown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", keydown);
      menuButtonRef.current?.focus();
    };
  }, [mobile, smallScreen]);
  useEffect(() => {
    if (!userId || tab !== "exams") return;
    const controller = new AbortController();
    setHistoryLoading(true);
    setHistoryError("");
    if (historyOwner.current !== userId) setAllExams([]);
    void (async () => {
      try {
        const first = await api<Page<Exam>>(
          `users/${userId}/exams?size=100&page=0`,
          controller.signal,
        );
        const content = [...first.content];
        for (let index = 1; index < first.totalPages; index++) {
          const next = await api<Page<Exam>>(
            `users/${userId}/exams?size=100&page=${index}`,
            controller.signal,
          );
          content.push(...next.content);
        }
        if (!controller.signal.aborted) {
          historyOwner.current = userId;
          setAllExams(content);
        }
      } catch (e) {
        if (!controller.signal.aborted) setHistoryError((e as Error).message);
      } finally {
        if (!controller.signal.aborted) setHistoryLoading(false);
      }
    })();
    return () => controller.abort();
  }, [userId, tab, revision]);
  const [notificationHead, setNotificationHead] =
    useState<NotificationHead | null>(null);
  const { hasUnseen, markSeen } = useNotificationIndicator(
    userId,
    notificationHead,
    tab === "notifications" && !initial && !loading,
  );
  const detail = detailState?.ownerId === userId ? detailState.value : null;
  const loadUsers = useCallback(async (preferred?: number) => {
    setInitial(true);
    setInitError("");
    try {
      const data = await api<User[]>("appUsers");
      setUsers(data);
      setSelectedUserId((old) => {
        let remembered = 0;
        try {
          remembered = Number(localStorage.getItem("sanguebom-user"));
        } catch {}
        const saved = preferred || old || remembered;
        return data.some((u) => u.id === saved) ? saved : (data[0]?.id ?? null);
      });
    } catch (e) {
      setInitError((e as Error).message);
    } finally {
      setInitial(false);
    }
  }, []);
  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);
  useEffect(() => {
    if (userId && users.some((u) => u.id === userId)) {
      try {
        localStorage.setItem("sanguebom-user", String(userId));
      } catch {}
    }
  }, [userId, users]);
  useEffect(() => {
    if (
      !routeUserId &&
      tab === "overview" &&
      userId &&
      !initial &&
      !initError
    ) {
      router.replace(screenUrl("overview", userId));
    }
  }, [routeUserId, tab, userId, initial, initError, router]);
  useEffect(() => {
    if (!userId) return;
    const c = new AbortController();
    setLoading(true);
    setError("");
    if (loadedOwner.current !== userId) {
      setGoal(null);
      setExams({ content: [], totalPages: 0, totalElements: 0 });
      setNotices({ content: [], totalPages: 0, totalElements: 0 });
      setEarned([]);
      setFailures([]);
    }
    Promise.allSettled([
      api<Page<Exam>>(`users/${userId}/exams?page=0&size=8`, c.signal),
      api<Goal>(`users/${userId}/exam-goal`, c.signal),
      api<Page<Notice>>(
        `users/${userId}/notifications?page=${noticePage}&size=8`,
        c.signal,
      ),
      api<Achievement[]>("achievements", c.signal),
      api<Earned[]>("userAchievements", c.signal),
    ])
      .then(([a, b, d, e, f]) => {
        if (c.signal.aborted) return;
        loadedOwner.current = userId;
        setFailures(
          [a, b, d, e, f].flatMap((result, index) =>
            result.status === "rejected"
              ? [["exams", "goal", "notices", "achievements", "earned"][index]]
              : [],
          ),
        );
        if (a.status === "fulfilled") setExams(a.value);
        if (b.status === "fulfilled") setGoal(b.value);
        if (d.status === "fulfilled") {
          setNotices(d.value);
          if (noticePage === 0) {
            const latest = d.value.content.reduce<NotificationMarker | null>(
              (head, notice) => newerNotification(head, notice),
              null,
            );
            setNotificationHead((previous) => ({
              userId,
              latest: newerNotification(
                previous?.userId === userId ? previous.latest : null,
                latest,
              ),
            }));
          }
        }
        if (e.status === "fulfilled")
          setAchievements(e.value.filter((x) => x.active));
        if (f.status === "fulfilled")
          setEarned(f.value.filter((x) => x.user === userId));
        const failed = [a, b, d, e, f].find((x) => x.status === "rejected");
        if (failed?.status === "rejected")
          setError(
            failed.reason?.message ||
              "Alguns dados não puderam ser carregados.",
          );
      })
      .catch((e) => {
        if (!c.signal.aborted) setError(e.message);
      })
      .finally(() => {
        if (!c.signal.aborted) setLoading(false);
      });
    return () => c.abort();
  }, [userId, revision, noticePage]);
  useEffect(() => {
    if (!userId) return;
    const source = new EventSource(
      `/api/backend/api/users/${userId}/notifications/stream`,
    );
    source.addEventListener("notification", (event) => {
      try {
        const notice = JSON.parse(event.data) as NotificationMarker;
        setNotificationHead((previous) => ({
          userId,
          latest: newerNotification(
            previous?.userId === userId ? previous.latest : null,
            notice,
          ),
        }));
      } catch {
        /* Refresh the history even when an event cannot be decoded. */
      }
      setRevision((r) => r + 1);
    });
    return () => source.close();
  }, [userId]);
  useEffect(() => {
    if (!detailId || !userId) return;
    const c = new AbortController();
    setDetailState(null);
    setDetailError("");
    api<Detail>(`users/${userId}/exams/${detailId}`, c.signal)
      .then((value) => setDetailState({ ownerId: userId, value }))
      .catch((e) => {
        if (!c.signal.aborted) setDetailError(e.message);
      });
    return () => c.abort();
  }, [detailId, userId]);
  const user = users.find((u) => u.id === userId);
  useEffect(() => {
    setMobile(false);
    setNoticePage(0);
  }, [tab, userId, detailId]);
  useEffect(() => {
    if (tab !== "exams") {
      setHistoryReady(false);
      return;
    }
    const restore = () => {
      const params = new URLSearchParams(window.location.search);
      setQuery(params.get("q") || "");
      setFromDate(params.get("from") || "");
      setToDate(params.get("to") || "");
      setUnitFilter(params.get("unit") || "");
      setStatusFilter(params.get("status") || "");
      const requested = Number(params.get("page") || "1");
      setPage(
        Number.isSafeInteger(requested) && requested > 0 ? requested - 1 : 0,
      );
      setHistoryReady(true);
    };
    restore();
    window.addEventListener("popstate", restore);
    return () => window.removeEventListener("popstate", restore);
  }, [tab, userId]);
  useEffect(() => {
    if (!historyReady || tab !== "exams") return;
    const params = new URLSearchParams();
    if (query) params.set("q", query);
    if (fromDate) params.set("from", fromDate);
    if (toDate) params.set("to", toDate);
    if (unitFilter) params.set("unit", unitFilter);
    if (statusFilter) params.set("status", statusFilter);
    if (page) params.set("page", String(page + 1));
    const suffix = params.size ? `?${params}` : "";
    window.history.replaceState(
      window.history.state,
      "",
      `${window.location.pathname}${suffix}`,
    );
  }, [
    query,
    fromDate,
    toDate,
    unitFilter,
    statusFilter,
    page,
    historyReady,
    tab,
  ]);
  const go = (value: Screen, citizenId = userId) => {
    if (confirmNavigation()) router.push(screenUrl(value, citizenId));
  };
  const refresh = () => setRevision((r) => r + 1);
  const filteredExams = allExams.filter((e) => {
    const collected = new Date(e.collectedAt);
    const localDate = `${collected.getFullYear()}-${String(collected.getMonth() + 1).padStart(2, "0")}-${String(collected.getDate()).padStart(2, "0")}`;
    return (
      `${e.id} ${e.healthUnitName} ${date(e.collectedAt)}`
        .toLowerCase()
        .includes(query.toLowerCase()) &&
      (!fromDate || localDate >= fromDate) &&
      (!toDate || localDate <= toDate) &&
      (!unitFilter || e.healthUnitName === unitFilter) &&
      (!statusFilter || e.status === statusFilter)
    );
  });
  const historyPage = Math.min(
    page,
    Math.max(0, Math.ceil(filteredExams.length / 8) - 1),
  );
  const examTable = (compact = false) => (
    <>
      {(compact ? failures.includes("exams") : !!historyError) ? (
        <p role="alert">
          Não foi possível carregar os exames.{" "}
          <button className="text-button" onClick={refresh}>
            Tentar novamente
          </button>
        </p>
      ) : !compact && historyLoading && historyOwner.current !== userId ? (
        <p role="status">Carregando todo o histórico…</p>
      ) : (compact ? exams.content : allExams).length === 0 ? (
        <div className="empty">
          <FileText size={30} />
          <h3>Seu histórico começa aqui</h3>
          <p>Registre um exame para acompanhar seus resultados.</p>
          <button className="button" onClick={() => go("new-exam")}>
            Registrar primeiro exame
          </button>
        </div>
      ) : (
        <div className="table-wrap exam-history">
          <table>
            <thead>
              <tr>
                <th>Exame / coleta</th>
                <th>Unidade de saúde</th>
                <th>Status</th>
                <th>
                  <span className="sr-only">Ações</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {(compact
                ? exams.content.slice(0, 4)
                : filteredExams.slice(historyPage * 8, historyPage * 8 + 8)
              ).map((e) => (
                <tr key={e.id}>
                  <td>
                    <div className="exam-name">
                      <span className="small-icon">
                        <FileText size={19} />
                      </span>
                      <div>
                        <strong>Exame #{e.id}</strong>
                        <small>{date(e.collectedAt)}</small>
                      </div>
                    </div>
                  </td>
                  <td>{e.healthUnitName || "Não informada"}</td>
                  <td>
                    <Badge value={e.status} />
                  </td>
                  <td>
                    <Link
                      className="text-button"
                      href={screenUrl("detail", userId, e.id)}
                    >
                      Ver resultado <ArrowRight size={15} />
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!compact && !filteredExams.length && (
            <p className="empty">Nenhum exame encontrado com esses filtros.</p>
          )}
        </div>
      )}
    </>
  );
  return (
    <div className="app-shell">
      <a className="skip" href="#main">
        Pular para o conteúdo
      </a>
      <aside
        ref={menuRef}
        id="main-menu"
        inert={smallScreen && !mobile}
        role={smallScreen && mobile ? "dialog" : undefined}
        aria-modal={smallScreen && mobile ? true : undefined}
        aria-label="Menu principal"
        className={"sidebar " + (mobile ? "open" : "")}
      >
        <button
          className="icon-button menu-close"
          aria-label="Fechar menu"
          onClick={() => setMobile(false)}
        >
          <X size={22} />
        </button>
        <Link
          className="brand"
          href={screenUrl("overview", userId)}
          aria-label="SangueBom início"
        >
          <span className="brand-mark">
            <Droplet fill="currentColor" size={23} />
          </span>
          Sangue<span>Bom</span>
          <span className="brand-dot">.</span>
        </Link>
        <div className="workspace-label">SEU ESPAÇO DE CUIDADO</div>
        <nav aria-label="Menu principal">
          {nav.map((n) => (
            <Link
              key={n.id}
              className={"nav-item " + (tab === n.id ? "active" : "")}
              href={screenUrl(n.id, userId)}
              aria-current={tab === n.id ? "page" : undefined}
            >
              <n.icon size={19} />
              {n.label}
              {n.id === "notifications" && notices.totalElements > 0 && (
                <span
                  className="nav-count"
                  aria-label={`${notices.totalElements} avisos no histórico`}
                >
                  {notices.totalElements}
                  <span className="sr-only"> no histórico</span>
                </span>
              )}
            </Link>
          ))}
        </nav>
        <div className="sidebar-tip">
          <span className="tip-icon">
            <Heart size={21} />
          </span>
          <strong>
            Pequenos cuidados.
            <br />
            Grandes mudanças.
          </strong>
          <p>Manter seus exames em dia é um gesto de carinho com você.</p>
          <span>
            Conte com o SangueBom <Heart size={12} />
          </span>
        </div>
        <div className="sidebar-bottom">
          <span className="online-dot" /> Cuidar faz parte de você
        </div>
      </aside>
      {mobile && (
        <button
          className="overlay"
          aria-label="Fechar menu"
          onClick={() => setMobile(false)}
        />
      )}
      <div className="main-shell" inert={smallScreen && mobile}>
        <header className="topbar">
          <div className="topbar-left">
            <button
              className="icon-button mobile-toggle"
              aria-label="Abrir menu"
              ref={menuButtonRef}
              aria-expanded={mobile}
              aria-controls="main-menu"
              onClick={() => setMobile(!mobile)}
            >
              <Menu size={22} />
            </button>
            <span className="breadcrumb">
              Meu espaço <ChevronRight size={14} />{" "}
              <strong>
                {nav.find((n) => n.id === tab)?.label ||
                  (tab === "new-user"
                    ? "Novo cidadão"
                    : tab === "new-exam"
                      ? "Registrar exame"
                      : "Resultado do exame")}
              </strong>
            </span>
          </div>
          <div className="topbar-right">
            <button
              className="icon-button"
              aria-label="Ver notificações"
              onClick={() => {
                markSeen();
                go("notifications");
              }}
              aria-describedby={hasUnseen ? "new-notifications" : undefined}
            >
              <Bell size={20} />
              {hasUnseen && (
                <>
                  <i aria-hidden="true" />
                  <span id="new-notifications" className="sr-only">
                    Novas notificações
                  </span>
                </>
              )}
            </button>
            <div className="topbar-divider" />
            <span className="avatar">
              {user?.name
                ?.split(" ")
                .map((s) => s[0])
                .slice(0, 2)
                .join("") || "SB"}
            </span>
            <label className="user-select">
              <span>Consultar cidadão</span>
              <select
                aria-label="Selecionar cidadão"
                value={userId ?? ""}
                onChange={(e) => {
                  go("overview", Number(e.target.value));
                }}
              >
                <option value="" disabled>
                  Selecione um cidadão
                </option>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.name || `Cidadão #${u.id}`}
                  </option>
                ))}
              </select>
            </label>
            <button
              className="icon-button"
              title="Cadastrar cidadão"
              aria-label="Cadastrar cidadão"
              onClick={() => go("new-user")}
            >
              <Users size={19} />
            </button>
          </div>
        </header>
        <main id="main">
          <div className="page-heading">
            <div>
              <div className="eyebrow">SAÚDE QUE VOCÊ ACOMPANHA</div>
              <h1>
                {tab === "overview"
                  ? `Olá${user?.name ? ", " + user.name.split(" ")[0] : ""}! Vamos cuidar de você?`
                  : nav.find((n) => n.id === tab)?.label ||
                    (tab === "new-user"
                      ? "Um novo começo para sua saúde"
                      : tab === "new-exam"
                        ? "Registrar novo exame"
                        : "Seu resultado, com clareza")}
              </h1>
              <p>
                {tab === "overview"
                  ? "Cada cuidado conta. Veja como está a sua jornada de saúde."
                  : tab === "clinical"
                    ? "Acompanhe seus marcadores e compare resultados ao longo do tempo."
                    : tab === "profile"
                      ? "Mantenha suas informações e sua rotina de cuidados atualizadas."
                      : "Seu cuidado, organizado em um só lugar."}
              </p>
            </div>
            {userId && tab !== "new-user" && tab !== "new-exam" && (
              <button className="button" onClick={() => go("new-exam")}>
                <Plus size={17} /> Novo exame
              </button>
            )}
          </div>
          {initial ? (
            <div className="panel loading" role="status">
              <span className="spinner" /> Conectando ao seu espaço de saúde…
            </div>
          ) : initError ? (
            <div className="panel empty">
              <Activity size={36} />
              <h2>Vamos restabelecer a conexão</h2>
              <p role="alert">{initError}</p>
              <button className="button" onClick={() => void loadUsers()}>
                <RefreshCw size={16} /> Tentar novamente
              </button>
            </div>
          ) : routeUserId && !users.some((u) => u.id === routeUserId) ? (
            <section className="panel empty">
              <h2>Cidadão não encontrado</h2>
              <p>
                O cidadão deste endereço não está disponível. Selecione outro
                cidadão ou volte ao início.
              </p>
              <Link href="/" className="button">
                Voltar ao início
              </Link>
            </section>
          ) : tab === "new-user" ? (
            <NewUser
              onSaved={(id) => {
                go("overview", id);
                refresh();
              }}
            />
          ) : !userId ? (
            <div className="panel empty">
              <Users size={36} />
              <h2>Bem-vindo ao SangueBom</h2>
              <p>Cadastre seu primeiro cidadão para começar.</p>
              <button className="button" onClick={() => go("new-user")}>
                Cadastrar cidadão
              </button>
            </div>
          ) : (
            <>
              {error && (
                <div className="error" role="alert">
                  {error}{" "}
                  <button className="text-button" onClick={refresh}>
                    Tentar novamente
                  </button>
                </div>
              )}
              {tab === "new-exam" ? (
                <NewExam
                  key={userId}
                  userId={userId}
                  onSaved={(examId) => {
                    if (examId)
                      router.push(screenUrl("detail", userId, examId));
                    else go("exams");
                    refresh();
                  }}
                />
              ) : tab === "profile" ? (
                <Profile
                  key={userId}
                  userId={userId}
                  onSaved={() => {
                    void api<User[]>("appUsers")
                      .then(setUsers)
                      .catch(() => {});
                    refresh();
                  }}
                />
              ) : tab === "clinical" ? (
                <Clinical key={userId} userId={userId} />
              ) : tab === "detail" ? (
                <section className="panel">
                  <button className="text-button" onClick={() => go("exams")}>
                    <ChevronLeft size={16} /> Voltar aos exames
                  </button>
                  {detailError ? (
                    <p className="error">{detailError}</p>
                  ) : !detail || detail.id !== detailId ? (
                    <p className="loading" role="status">
                      Carregando resultado…
                    </p>
                  ) : (
                    <>
                      <div className="section-heading">
                        <div>
                          <p className="result-owner">
                            Cidadão: {user?.name} · Cadastro #{userId}
                          </p>
                          <h2>Exame #{detail.id}</h2>
                          <p>
                            {date(detail.collectedAt)} · {detail.healthUnitName}
                          </p>
                        </div>
                        <Badge value={detail.riskLevel} context="risk" />
                      </div>
                      <div
                        className={`analysis ${detail.riskLevel === "HIGH" || detail.riskLevel === "VERY_HIGH" ? "analysis-alert" : detail.riskLevel === "LOW" ? "" : "analysis-neutral"}`}
                      >
                        <Activity size={25} />
                        <div>
                          <strong>
                            Avaliação do exame{" "}
                            {detail.riskScore != null
                              ? `· Pontuação ${detail.riskScore}`
                              : ""}
                          </strong>
                          <p>
                            {detail.explanation ||
                              "Este exame ainda não tem uma avaliação disponível."}
                          </p>
                        </div>
                      </div>
                      <div className="table-wrap result-items">
                        <table>
                          <thead>
                            <tr>
                              <th>Marcador</th>
                              <th>Resultado</th>
                              <th>Classificação</th>
                            </tr>
                          </thead>
                          <tbody>
                            {detail.items.map((i) => (
                              <tr key={i.itemCode}>
                                <td data-label="Marcador">
                                  <strong>{i.itemName}</strong>
                                  <small>{i.itemCode}</small>
                                </td>
                                <td data-label="Resultado">
                                  {i.valueNumeric != null
                                    ? i.valueNumeric.toLocaleString("pt-BR")
                                    : i.valueText || "—"}{" "}
                                  {i.unit}
                                </td>
                                <td data-label="Classificação">
                                  <Badge value={i.flag} context="marker" />
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                      <p className="medical-note">
                        <Stethoscope size={18} />
                        {detail.disclaimer ||
                          "Esta avaliação não substitui uma consulta médica. Converse com um profissional de saúde."}
                      </p>
                      <button
                        className="button secondary"
                        onClick={() => window.print()}
                      >
                        Imprimir resultado
                      </button>
                    </>
                  )}
                </section>
              ) : loadedOwner.current !== userId ? (
                <div className="panel loading" role="status">
                  <span className="spinner" /> Atualizando seus dados…
                </div>
              ) : (
                <>
                  {tab === "overview" && (
                    <>
                      {exams.totalElements === 0 &&
                        !failures.includes("exams") && (
                          <section className="hero">
                            <div>
                              <span className="hero-label">
                                <span /> SUA SAÚDE EM PRIMEIRO LUGAR
                              </span>
                              <h2>
                                Cuidar de você é<br />
                                sempre um bom plano.
                              </h2>
                              <p>
                                Acompanhe seus resultados, conheça sua evolução
                                <br className="desktop-break" /> e dê o próximo
                                passo para uma vida mais saudável.
                              </p>
                              <button
                                className="hero-button"
                                onClick={() => go("exams")}
                              >
                                Acompanhar meus exames <ArrowRight size={17} />
                              </button>
                            </div>
                            <div className="hero-art" aria-hidden="true">
                              <div className="orbit orbit-one" />
                              <div className="orbit orbit-two" />
                              <div className="heart-orb">
                                <Heart
                                  size={94}
                                  strokeWidth={1.4}
                                  fill="currentColor"
                                />
                                <Activity className="heart-pulse" size={69} />
                              </div>
                              <span className="floating-card float-top">
                                <ShieldCheck size={24} />
                                <span>
                                  Mais cuidado
                                  <strong>Mais tranquilidade</strong>
                                </span>
                              </span>
                              <span className="floating-card float-bottom">
                                <span className="mini-check">
                                  <Check size={16} />
                                </span>
                                Sua saúde importa
                              </span>
                              <span className="sparkle">
                                <Sparkles size={26} />
                              </span>
                            </div>
                          </section>
                        )}
                      <div className="stats">
                        <div className="stat-card">
                          <span className="stat-icon pink">
                            <FileText size={22} />
                          </span>
                          <div>
                            <span>Exames registrados</span>
                            <strong>
                              {failures.includes("exams")
                                ? "Indisponível"
                                : exams.totalElements}
                              <small>na sua jornada</small>
                            </strong>
                          </div>
                        </div>
                        <div className="stat-card">
                          <span className="stat-icon teal">
                            <CalendarDays size={22} />
                          </span>
                          <div>
                            <span>Próximo exame</span>
                            <strong className="stat-date">
                              {failures.includes("goal")
                                ? "Indisponível"
                                : goal?.dueDate
                                  ? date(goal.dueDate)
                                  : "Vamos começar?"}
                              <small>
                                {failures.includes("goal")
                                  ? "Tente atualizar os dados"
                                  : goal
                                    ? labels[goal.status]
                                    : "Configure seu perfil"}
                              </small>
                            </strong>
                          </div>
                        </div>
                        <div className="stat-card">
                          <span className="stat-icon gold">
                            <Award size={23} />
                          </span>
                          <div>
                            <span>Conquistas desbloqueadas</span>
                            <strong>
                              {failures.includes("earned")
                                ? "Indisponível"
                                : earned.length}
                              <small>motivos para celebrar</small>
                            </strong>
                          </div>
                        </div>
                      </div>
                      <div className="dashboard-grid">
                        <section className="panel exams-panel">
                          <div className="section-heading">
                            <div>
                              <h2>Seus últimos exames</h2>
                              <p>
                                Um olhar sobre os seus cuidados mais recentes.
                              </p>
                            </div>
                            <button
                              className="text-button"
                              onClick={() => go("exams")}
                            >
                              Ver todos <ArrowRight size={15} />
                            </button>
                          </div>
                          {examTable(true)}
                        </section>
                        <section className="panel goal-panel">
                          {failures.includes("goal") ? (
                            <p role="alert">
                              Não foi possível carregar sua meta.{" "}
                              <button className="text-button" onClick={refresh}>
                                Tentar novamente
                              </button>
                            </p>
                          ) : (
                            <>
                              <div className="section-heading">
                                <h2>Sua meta de cuidado</h2>
                                <span className="small-icon teal">
                                  <CalendarDays size={20} />
                                </span>
                              </div>
                              <div className="goal-ring">
                                <Heart size={25} />
                                <strong>
                                  {goal?.daysRemaining != null
                                    ? Math.abs(goal.daysRemaining)
                                    : "—"}
                                </strong>
                                <span>
                                  {goal?.daysRemaining != null
                                    ? goal.daysRemaining < 0
                                      ? "dias após o prazo"
                                      : "dias para o próximo"
                                    : "sem histórico"}
                                </span>
                              </div>
                              <Badge value={goal?.status} />
                              <p>
                                {goal?.status === "NO_HISTORY"
                                  ? "O primeiro exame é o início de uma boa rotina."
                                  : goal?.status === "OVERDUE"
                                    ? "Que tal retomar sua rotina de exames?"
                                    : "Um pequeno lembrete para manter o cuidado em dia."}
                              </p>
                              <button
                                className="text-button"
                                onClick={() => go("profile")}
                              >
                                Ajustar minha periodicidade{" "}
                                <ArrowRight size={14} />
                              </button>
                            </>
                          )}
                        </section>
                      </div>
                      <section className="bottom-banner">
                        <span className="stat-icon gold">
                          <Award size={27} />
                        </span>
                        <div>
                          <h3>Cada passo merece uma conquista</h3>
                          <p>
                            Transforme sua rotina de cuidado em motivos para se
                            orgulhar.
                          </p>
                        </div>
                        <button
                          className="text-button"
                          onClick={() => go("achievements")}
                        >
                          Conhecer minhas conquistas <ArrowRight size={16} />
                        </button>
                      </section>
                    </>
                  )}
                  {tab === "exams" && (
                    <section className="panel">
                      <div className="section-heading">
                        <div>
                          <h2>Histórico de exames</h2>
                          <p>{exams.totalElements} exames registrados</p>
                        </div>
                        <label className="search">
                          <Search size={17} />
                          <input
                            placeholder="Buscar em todo o histórico"
                            aria-label="Buscar em todo o histórico"
                            value={query}
                            onChange={(e) => {
                              setQuery(e.target.value);
                              setPage(0);
                            }}
                          />
                        </label>
                      </div>
                      <div className="history-filters">
                        <label className="field">
                          De
                          <input
                            type="date"
                            value={fromDate}
                            max={toDate || undefined}
                            onChange={(e) => {
                              setFromDate(e.target.value);
                              setPage(0);
                            }}
                          />
                        </label>
                        <label className="field">
                          Até
                          <input
                            type="date"
                            value={toDate}
                            min={fromDate || undefined}
                            onChange={(e) => {
                              setToDate(e.target.value);
                              setPage(0);
                            }}
                          />
                        </label>
                        <label className="field">
                          Unidade
                          <select
                            value={unitFilter}
                            onChange={(e) => {
                              setUnitFilter(e.target.value);
                              setPage(0);
                            }}
                          >
                            <option value="">Todas as unidades</option>
                            {[
                              ...new Set(
                                allExams
                                  .map((e) => e.healthUnitName)
                                  .filter(Boolean),
                              ),
                            ].map((unit) => (
                              <option key={unit}>{unit}</option>
                            ))}
                          </select>
                        </label>
                        <label className="field">
                          Situação
                          <select
                            value={statusFilter}
                            onChange={(e) => {
                              setStatusFilter(e.target.value);
                              setPage(0);
                            }}
                          >
                            <option value="">Todas as situações</option>
                            {[...new Set(allExams.map((e) => e.status))].map(
                              (status) => (
                                <option key={status} value={status}>
                                  {labels[status] || status}
                                </option>
                              ),
                            )}
                          </select>
                        </label>
                      </div>
                      {(query ||
                        fromDate ||
                        toDate ||
                        unitFilter ||
                        statusFilter) && (
                        <button
                          className="text-button"
                          onClick={() => {
                            setQuery("");
                            setFromDate("");
                            setToDate("");
                            setUnitFilter("");
                            setStatusFilter("");
                            setPage(0);
                          }}
                        >
                          Limpar filtros
                        </button>
                      )}
                      {!historyLoading && !historyError && (
                        <p role="status">
                          {filteredExams.length} exames encontrados
                        </p>
                      )}
                      {examTable()}
                      <Pagination
                        page={historyPage}
                        pages={Math.ceil(filteredExams.length / 8)}
                        change={setPage}
                      />
                    </section>
                  )}
                  {tab === "achievements" && (
                    <>
                      {failures.some((key) =>
                        ["achievements", "earned"].includes(key),
                      ) ? (
                        <p className="error" role="alert">
                          Não foi possível carregar suas conquistas.{" "}
                          <button className="text-button" onClick={refresh}>
                            Tentar novamente
                          </button>
                        </p>
                      ) : (
                        <>
                          <div className="achievement-intro">
                            <Award size={32} />
                            <div>
                              <h2>Seu cuidado merece reconhecimento</h2>
                              <p>
                                {earned.length} de {achievements.length}{" "}
                                conquistas desbloqueadas. Continue a sua
                                jornada!
                              </p>
                            </div>
                          </div>
                          <div className="achievement-grid">
                            {achievements.map((a, i) => {
                              const won = earned.find(
                                (e) => e.achievement === a.id,
                              );
                              return (
                                <section
                                  className={
                                    "panel achievement " +
                                    (won ? "earned" : "locked")
                                  }
                                  key={a.id}
                                >
                                  <span className={"medal medal-" + i}>
                                    <Award size={45} />
                                  </span>
                                  <span className="eyebrow">
                                    {won
                                      ? "CONQUISTA DESBLOQUEADA"
                                      : "SEU PRÓXIMO DESAFIO"}
                                  </span>
                                  <h2>{a.name}</h2>
                                  <p>{a.description}</p>
                                  <span className="badge">
                                    {won
                                      ? `Conquistada em ${date(won.earnedAt)}`
                                      : "Continue cuidando de você"}
                                  </span>
                                </section>
                              );
                            })}
                          </div>
                          {!achievements.length && (
                            <div className="panel empty">
                              Nenhuma conquista disponível no momento.
                            </div>
                          )}
                        </>
                      )}
                    </>
                  )}
                  {tab === "notifications" && (
                    <section className="panel">
                      <div className="section-heading">
                        <div>
                          <h2>Seu cuidado em tempo real</h2>
                          <p>
                            Resultados e lembretes para acompanhar sua saúde.
                          </p>
                        </div>
                        <button
                          className="icon-button"
                          onClick={refresh}
                          aria-label="Atualizar notificações"
                        >
                          <RefreshCw size={19} />
                        </button>
                      </div>
                      {failures.includes("notices") ? (
                        <p role="alert">
                          Não foi possível carregar os avisos.{" "}
                          <button className="text-button" onClick={refresh}>
                            Tentar novamente
                          </button>
                        </p>
                      ) : notices.content.length ? (
                        notices.content.map((n) => (
                          <article className="notice" key={n.id}>
                            <span className="stat-icon pink">
                              <Bell size={21} />
                            </span>
                            <div>
                              <h3>{n.title}</h3>
                              <p>{n.message}</p>
                              <small>{date(n.createdAt)}</small>
                            </div>
                          </article>
                        ))
                      ) : (
                        <div className="empty">
                          <Bell size={30} />
                          <h3>Tudo tranquilo por aqui</h3>
                          <p>Seus próximos avisos aparecerão neste espaço.</p>
                        </div>
                      )}
                      <Pagination
                        page={noticePage}
                        pages={notices.totalPages}
                        change={setNoticePage}
                      />
                    </section>
                  )}
                </>
              )}
            </>
          )}
          <footer>
            <span>
              <Droplet size={14} /> SangueBom · Cuidado que acompanha você.
            </span>
            <span>
              Feito para uma vida mais saudável <Heart size={13} />
            </span>
          </footer>
        </main>
      </div>
    </div>
  );
}
function Pagination({
  page,
  pages,
  change,
}: {
  page: number;
  pages: number;
  change: (n: number) => void;
}) {
  return pages > 1 ? (
    <div className="pagination">
      <button
        className="button secondary"
        disabled={!page}
        onClick={() => change(page - 1)}
      >
        <ChevronLeft size={16} /> Anterior
      </button>
      <span>
        Página {page + 1} de {pages}
      </span>
      <button
        className="button secondary"
        disabled={page + 1 >= pages}
        onClick={() => change(page + 1)}
      >
        Próxima <ChevronRight size={16} />
      </button>
    </div>
  ) : null;
}
