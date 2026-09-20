"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { screenUrl } from "../lib/navigation";
import { useFormFeedback } from "../hooks/use-form-feedback";

type Citizen = {
  id?: number;
  name: string;
  email: string;
  birthDate: string;
  cpfHash?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
};
type Health = {
  id?: number;
  user?: number;
  sex: string;
  heightCm: string;
  weightKg: string;
  riskFactors: string;
  examPeriodicity: string;
  createdAt?: string;
  updatedAt?: string;
};
type Item = { id: number; name: string; code: string; unit: string };
type Unit = { id: number; name: string };
const emptyCitizen: Citizen = { name: "", email: "", birthDate: "" };
const emptyHealth: Health = {
  sex: "",
  heightCm: "",
  weightKg: "",
  riskFactors: "",
  examPeriodicity: "YEARLY",
};

async function api<T>(
  path: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  const response = await fetch(`/api/backend/api/${path}`, {
    method,
    headers: { "Content-Type": "application/json" },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    cache: "no-store",
  });
  const data = await response.json().catch(() => null);
  if (!response.ok)
    throw new Error(
      data?.detail ||
        data?.message ||
        `Não foi possível concluir a operação (${response.status}). Tente novamente.`,
    );
  return data as T;
}
const message = (error: unknown) =>
  error instanceof Error
    ? error.message
    : "Não foi possível conectar ao servidor. Tente novamente.";
const today = () =>
  new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 10);

function riskFactorsText(value: string | null | undefined) {
  if (!value) return "";
  try {
    const parsed: unknown = JSON.parse(value);
    if (typeof parsed === "string") return parsed;
    if (
      Array.isArray(parsed) &&
      parsed.every((item) => typeof item === "string")
    )
      return parsed.join("\n");
  } catch {
    /* Preserve legacy text so it can be saved as valid JSON. */
  }
  return value;
}

function riskFactorsJson(value: string) {
  if (!value.trim()) return null;
  try {
    return JSON.stringify(JSON.parse(value));
  } catch {
    return JSON.stringify(
      value
        .split("\n")
        .map((line) => line.trim())
        .filter(Boolean),
    );
  }
}

function CitizenFields({
  value,
  onChange,
}: {
  value: Citizen;
  onChange: (v: Citizen) => void;
}) {
  return (
    <div className="grid-2">
      <label className="field">
        Nome completo
        <input
          required
          maxLength={150}
          autoComplete="name"
          value={value.name}
          onChange={(e) => onChange({ ...value, name: e.target.value })}
        />
      </label>
      <label className="field">
        E-mail
        <input
          required
          type="email"
          maxLength={255}
          autoComplete="email"
          value={value.email}
          onChange={(e) => onChange({ ...value, email: e.target.value })}
        />
      </label>
      <label className="field">
        Data de nascimento
        <input
          required
          type="date"
          min="1900-01-01"
          max={today()}
          value={value.birthDate}
          onChange={(e) => onChange({ ...value, birthDate: e.target.value })}
        />
      </label>
    </div>
  );
}

function HealthFields({
  value,
  onChange,
}: {
  value: Health;
  onChange: (v: Health) => void;
}) {
  return (
    <>
      <div className="grid-2">
        <label className="field">
          Sexo para referências laboratoriais
          <select
            required
            value={value.sex}
            onChange={(e) => onChange({ ...value, sex: e.target.value })}
          >
            <option value="">Selecione</option>
            <option value="F">Feminino</option>
            <option value="M">Masculino</option>
          </select>
        </label>
        <label className="field">
          Periodicidade dos exames
          <select
            required
            value={value.examPeriodicity}
            onChange={(e) =>
              onChange({ ...value, examPeriodicity: e.target.value })
            }
          >
            <option value="QUARTERLY">A cada 3 meses</option>
            <option value="SEMESTERLY">A cada 6 meses</option>
            <option value="YEARLY">Anual</option>
          </select>
        </label>
        <label className="field">
          Altura em cm (opcional)
          <input
            type="number"
            min="1"
            max="300"
            step="0.01"
            placeholder="170"
            value={value.heightCm}
            onChange={(e) => onChange({ ...value, heightCm: e.target.value })}
          />
        </label>
        <label className="field">
          Peso em kg (opcional)
          <input
            type="number"
            min="0.1"
            max="700"
            step="0.01"
            placeholder="70"
            value={value.weightKg}
            onChange={(e) => onChange({ ...value, weightKg: e.target.value })}
          />
        </label>
      </div>
      <label className="field">
        Fatores de risco (opcional)
        <textarea
          rows={3}
          placeholder="Informe uma condição ou informação relevante por linha"
          value={value.riskFactors}
          onChange={(e) => onChange({ ...value, riskFactors: e.target.value })}
        />
      </label>
    </>
  );
}

function healthBody(health: Health, user: number) {
  return {
    ...health,
    user,
    heightCm: health.heightCm || null,
    weightKg: health.weightKg || null,
    riskFactors: riskFactorsJson(health.riskFactors),
    createdAt: health.createdAt || new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
}

export function Profile({
  userId,
  onSaved,
}: {
  userId: number;
  onSaved: () => void;
}) {
  const [citizen, setCitizen] = useState<Citizen>(emptyCitizen);
  const [health, setHealth] = useState<Health>(emptyHealth);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const { formRef, markDirty, markSaved } = useFormFeedback(error);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    setSuccess("");
    setCitizen(emptyCitizen);
    Promise.all([
      api<Citizen>(`appUsers/${userId}`),
      api<Health[]>("healthProfiles"),
    ])
      .then(([user, profiles]) => {
        if (!active) return;
        setCitizen({
          ...user,
          name: user.name || "",
          email: user.email || "",
          birthDate: user.birthDate || "",
        });
        const profile = profiles.find((p) => p.user === userId);
        setHealth(
          profile
            ? {
                ...profile,
                sex:
                  profile.sex === "MALE"
                    ? "M"
                    : profile.sex === "FEMALE"
                      ? "F"
                      : profile.sex || "",
                heightCm: profile.heightCm || "",
                weightKg: profile.weightKg || "",
                riskFactors: riskFactorsText(profile.riskFactors),
                examPeriodicity: profile.examPeriodicity || "YEARLY",
              }
            : emptyHealth,
        );
      })
      .catch((e) => {
        if (active) setError(message(e));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [userId, retry]);
  async function save(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setSuccess("");
    let personalSaved = false;
    try {
      if (!citizen.name.trim()) throw new Error("Informe seu nome completo.");
      await api(`appUsers/${userId}`, "PUT", {
        ...citizen,
        name: citizen.name.trim(),
        email: citizen.email.trim(),
        updatedAt: new Date().toISOString(),
      });
      personalSaved = true;
      const existing = health.id
        ? health
        : (await api<Health[]>("healthProfiles")).find(
            (profile) => profile.user === userId,
          );
      const id = await api<number>(
        existing?.id ? `healthProfiles/${existing.id}` : "healthProfiles",
        existing?.id ? "PUT" : "POST",
        healthBody(
          {
            ...health,
            ...(existing
              ? { id: existing.id, createdAt: existing.createdAt }
              : {}),
          },
          userId,
        ),
      );
      setHealth((v) => ({ ...v, id }));
      setSuccess("Seu perfil foi atualizado.");
      markSaved();
      onSaved();
    } catch (e) {
      setError(
        `${personalSaved ? "Dados pessoais salvos. Não foi possível salvar o perfil de saúde; tente salvar novamente. " : ""}${message(e)}`,
      );
    } finally {
      setBusy(false);
    }
  }
  if (loading)
    return (
      <div className="panel" role="status">
        Carregando seu perfil…
      </div>
    );
  if (!citizen.id)
    return (
      <div className="panel">
        <p className="error" role="alert">
          {error}
        </p>
        <button
          className="button secondary"
          onClick={() => setRetry((v) => v + 1)}
        >
          Tentar novamente
        </button>
      </div>
    );
  return (
    <form ref={formRef} className="panel" onSubmit={save} onChange={markDirty}>
      <h2>Dados pessoais</h2>
      <p className="muted">
        Mantenha suas informações atualizadas para acompanhar sua saúde.
      </p>
      <fieldset disabled={busy} style={{ border: 0, padding: 0, margin: 0 }}>
        <CitizenFields value={citizen} onChange={setCitizen} />
        <h2>Perfil de saúde</h2>
        <HealthFields value={health} onChange={setHealth} />
        {error && (
          <p className="error" role="alert" tabIndex={-1}>
            {error}
          </p>
        )}
        {success && (
          <p className="success" role="status">
            {success}
          </p>
        )}
        <button className="button" type="submit">
          {busy ? "Salvando…" : "Salvar alterações"}
        </button>
      </fieldset>
    </form>
  );
}

export function NewUser({ onSaved }: { onSaved: (id: number) => void }) {
  const [citizen, setCitizen] = useState<Citizen>(emptyCitizen);
  const [health, setHealth] = useState<Health>(emptyHealth);
  const [cpf, setCpf] = useState("");
  const [createdId, setCreatedId] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const { formRef, markDirty, markSaved } = useFormFeedback(error);
  async function save(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    let id = createdId;
    try {
      if (!id) {
        if (!citizen.name.trim()) throw new Error("Informe seu nome completo.");
        const digits = cpf.replace(/\D/g, "");
        if (digits.length !== 11 || /^(\d)\1{10}$/.test(digits))
          throw new Error("Informe um CPF válido com 11 dígitos.");
        for (let count = 9; count <= 10; count++) {
          const sum = [...digits.slice(0, count)].reduce(
            (total, digit, index) =>
              total + Number(digit) * (count + 1 - index),
            0,
          );
          const check = ((sum * 10) % 11) % 10;
          if (check !== Number(digits[count]))
            throw new Error(
              "O CPF informado não é válido. Confira os dígitos.",
            );
        }
        if (!globalThis.crypto?.subtle)
          throw new Error(
            "Abra o site em localhost ou em uma conexão HTTPS para cadastrar o CPF com segurança.",
          );
        const digest = await crypto.subtle.digest(
          "SHA-256",
          new TextEncoder().encode(digits),
        );
        const cpfHash = Array.from(new Uint8Array(digest), (byte) =>
          byte.toString(16).padStart(2, "0"),
        ).join("");
        id = await api<number>("appUsers", "POST", {
          ...citizen,
          name: citizen.name.trim(),
          email: citizen.email.trim(),
          cpfHash,
          status: "ACTIVE",
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        });
        setCreatedId(id);
        setCpf("");
      }
      // Recover a profile if a preceding request succeeded but its response was lost.
      const profiles = await api<Health[]>("healthProfiles");
      const existing = profiles.find((p) => p.user === id);
      await api(
        existing ? `healthProfiles/${existing.id}` : "healthProfiles",
        existing ? "PUT" : "POST",
        healthBody(
          {
            ...health,
            ...(existing
              ? { id: existing.id, createdAt: existing.createdAt }
              : {}),
          },
          id,
        ),
      );
      markSaved();
      onSaved(id);
    } catch (e) {
      setError(
        `${id ? "Cidadão cadastrado. Falta concluir o perfil de saúde; clique em concluir novamente. " : ""}${message(e)}`,
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <form ref={formRef} className="panel" onSubmit={save} onChange={markDirty}>
      <h2>Comece seu acompanhamento</h2>
      <p className="muted">
        Cadastre seus dados e seu perfil de saúde para registrar exames.
      </p>
      <fieldset disabled={busy} style={{ border: 0, padding: 0, margin: 0 }}>
        <fieldset
          disabled={createdId !== null}
          style={{ border: 0, padding: 0, margin: 0 }}
        >
          <CitizenFields value={citizen} onChange={setCitizen} />
          <label className="field">
            CPF
            <input
              required={createdId === null}
              inputMode="numeric"
              autoComplete="off"
              maxLength={14}
              placeholder="000.000.000-00"
              aria-invalid={error.includes("CPF") || undefined}
              aria-describedby={
                error.includes("CPF") ? "citizen-error" : undefined
              }
              value={cpf}
              onChange={(e) => setCpf(e.target.value)}
            />
            <small className="muted">
              Usamos o CPF para identificar seu cadastro e evitar duplicidades.
              O número original não é enviado ao servidor.
            </small>
          </label>
        </fieldset>
        <h2>Seu perfil de saúde</h2>
        <HealthFields value={health} onChange={setHealth} />
        {error && (
          <p id="citizen-error" className="error" role="alert" tabIndex={-1}>
            {error}
          </p>
        )}
        <button className="button" type="submit">
          {busy
            ? "Salvando cadastro…"
            : createdId
              ? "Concluir perfil de saúde"
              : "Criar meu cadastro"}
        </button>
      </fieldset>
    </form>
  );
}

export function NewExam({
  userId,
  onSaved,
}: {
  userId: number;
  onSaved: (examId?: number) => void;
}) {
  const [items, setItems] = useState<Item[]>([]);
  const [units, setUnits] = useState<Unit[]>([]);
  const [unit, setUnit] = useState("");
  const [collectedAt, setCollectedAt] = useState(() =>
    new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16),
  );
  const [reference, setReference] = useState("");
  const [rows, setRows] = useState([{ item: "", value: "" }]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);
  const [savedExamId, setSavedExamId] = useState<number>();
  const { formRef, markDirty, markSaved } = useFormFeedback(error);
  const [retry, setRetry] = useState(0);
  const [ready, setReady] = useState(false);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    setReady(false);
    async function catalog() {
      const result: Item[] = [];
      for (let page = 0; ; page++) {
        const data = await api<{ content: Item[]; totalPages: number }>(
          `v1/exam-items?size=100&page=${page}`,
        );
        result.push(...data.content);
        if (page + 1 >= data.totalPages) return result;
      }
    }
    Promise.all([
      catalog(),
      api<Unit[]>("healthUnits"),
      api<Health[]>("healthProfiles"),
      api<Citizen>(`appUsers/${userId}`),
    ])
      .then(([catalogItems, healthUnits, profiles, citizen]) => {
        if (!active) return;
        setItems(catalogItems);
        setUnits(healthUnits);
        const profile = profiles.find((p) => p.user === userId);
        if (!profile?.sex || !citizen.birthDate)
          throw new Error(
            "Complete a data de nascimento e o sexo na página Meu perfil antes de registrar um exame.",
          );
        setReady(true);
      })
      .catch((e) => {
        if (active) setError(message(e));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [userId, retry]);
  async function save(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const collection = new Date(collectedAt);
      if (
        !Number.isFinite(collection.getTime()) ||
        collection.getTime() > Date.now()
      )
        throw new Error(
          "Informe uma data de coleta válida, que não esteja no futuro.",
        );
      if (new Set(rows.map((r) => r.item)).size !== rows.length)
        throw new Error("Cada marcador deve aparecer uma única vez no exame.");
      if (
        rows.some(
          (r) =>
            !r.item ||
            !r.value.trim() ||
            !Number.isFinite(Number(r.value)) ||
            Number(r.value) < 0,
        )
      )
        throw new Error(
          "Preencha todos os marcadores com resultados numéricos maiores ou iguais a zero.",
        );
      const result = await api<{ examId?: number; id?: number } | number>(
        "exams",
        "POST",
        {
          userId,
          healthUnitId: Number(unit),
          collectedAt: collection.toISOString(),
          externalReference: reference.trim() || null,
          analyzedItems: rows.map((row) => ({
            examItemId: Number(row.item),
            measuredValue: Number(row.value),
          })),
        },
      );
      setSavedExamId(
        typeof result === "number" ? result : (result?.examId ?? result?.id),
      );
      setDone(true);
      markSaved();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  if (loading)
    return (
      <div className="panel" role="status">
        Preparando o registro do exame…
      </div>
    );
  if (done)
    return (
      <div className="panel">
        <span className="badge">Tudo pronto</span>
        <h2>Exame registrado</h2>
        <p>
          Os resultados foram salvos e a análise está disponível no seu
          histórico.
        </p>
        <button className="button" onClick={() => onSaved(savedExamId)}>
          {savedExamId ? "Ver resultado do exame" : "Ver meus exames"}
        </button>
      </div>
    );
  if (!ready || !items.length || !units.length)
    return (
      <div className="panel">
        <h2>Registrar exame</h2>
        <p className="error" role="alert">
          {error ||
            "É necessário ter marcadores e unidades de saúde cadastrados na API para registrar exames."}
        </p>
        <button
          className="button secondary"
          onClick={() => setRetry((v) => v + 1)}
        >
          Verificar novamente
        </button>
        <Link className="button" href={screenUrl("profile", userId)}>
          Completar meu perfil
        </Link>
      </div>
    );
  return (
    <form ref={formRef} className="panel" onSubmit={save} onChange={markDirty}>
      <h2>Informações do exame</h2>
      <p className="muted">
        Transcreva os resultados do laudo e confira a unidade de cada marcador.
      </p>
      <fieldset disabled={busy} style={{ border: 0, padding: 0, margin: 0 }}>
        <div className="grid-2">
          <label className="field">
            Unidade de saúde
            <select
              required
              value={unit}
              onChange={(e) => setUnit(e.target.value)}
            >
              <option value="">Selecione a unidade</option>
              {units.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Data e horário da coleta
            <input
              required
              type="datetime-local"
              value={collectedAt}
              onChange={(e) => setCollectedAt(e.target.value)}
            />
          </label>
          <label className="field">
            Referência do laboratório (opcional)
            <input
              maxLength={100}
              placeholder="Código presente no laudo"
              value={reference}
              onChange={(e) => setReference(e.target.value)}
            />
          </label>
        </div>
        <h2>Resultados</h2>
        {rows.map((row, index) => (
          <div
            key={index}
            className="panel"
            style={{ padding: 16, marginBottom: 12 }}
          >
            <div className="grid-2">
              <label className="field">
                Marcador {index + 1}
                <select
                  required
                  value={row.item}
                  onChange={(e) =>
                    setRows((previous) =>
                      previous.map((r, i) =>
                        i === index ? { ...r, item: e.target.value } : r,
                      ),
                    )
                  }
                >
                  <option value="">Selecione o marcador</option>
                  {items.map((item) => (
                    <option
                      key={item.id}
                      value={item.id}
                      disabled={rows.some(
                        (r, i) => i !== index && r.item === String(item.id),
                      )}
                    >
                      {item.name} ({item.unit})
                    </option>
                  ))}
                </select>
              </label>
              <label className="field">
                Resultado{" "}
                {row.item
                  ? `(${items.find((i) => String(i.id) === row.item)?.unit || ""})`
                  : ""}
                <input
                  required
                  type="number"
                  min="0"
                  step="any"
                  placeholder="0,00"
                  value={row.value}
                  onChange={(e) =>
                    setRows((previous) =>
                      previous.map((r, i) =>
                        i === index ? { ...r, value: e.target.value } : r,
                      ),
                    )
                  }
                />
              </label>
            </div>
            {rows.length > 1 && (
              <button
                className="button secondary"
                type="button"
                aria-label={`Remover marcador ${index + 1}`}
                onClick={() =>
                  setRows((previous) => previous.filter((_, i) => i !== index))
                }
              >
                Remover marcador
              </button>
            )}
          </div>
        ))}
        <button
          className="button secondary"
          type="button"
          disabled={rows.length >= items.length}
          onClick={() => {
            markDirty();
            setRows((previous) => [...previous, { item: "", value: "" }]);
          }}
        >
          + Adicionar marcador
        </button>
        {error && (
          <p className="error" role="alert" tabIndex={-1}>
            {error}
          </p>
        )}
        <p className="muted">
          A análise automática auxilia o acompanhamento e deve ser avaliada por
          um profissional de saúde.
        </p>
        <button className="button" type="submit">
          {busy ? "Registrando e analisando…" : "Salvar e analisar exame"}
        </button>
      </fieldset>
    </form>
  );
}
