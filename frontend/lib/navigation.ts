export const sections = {
  overview: "",
  exams: "exames",
  clinical: "evolucao",
  achievements: "conquistas",
  notifications: "notificacoes",
  profile: "perfil",
  "new-exam": "exames/novo",
} as const;
export type Screen = keyof typeof sections | "new-user" | "detail";
export function screenUrl(
  screen: Screen,
  userId?: number | null,
  examId?: number,
) {
  if (screen === "new-user") return "/cidadaos/novo";
  if (!userId) return "/";
  const base = `/cidadaos/${userId}`;
  if (screen === "detail") return `${base}/exames/${examId}`;
  return sections[screen] ? `${base}/${sections[screen]}` : base;
}
export function positiveId(value: string) {
  const id = Number(value);
  return /^[1-9]\d*$/.test(value) && Number.isSafeInteger(id) ? id : null;
}
