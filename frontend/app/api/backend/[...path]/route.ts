import { NextRequest } from "next/server";
export const dynamic = "force-dynamic";
const allowed = new Set([
  "appUsers",
  "healthProfiles",
  "healthUnits",
  "exams",
  "examItems",
  "achievements",
  "userAchievements",
  "users",
  "v1",
]);
async function proxy(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> },
) {
  const { path } = await context.params;
  if (
    path[0] !== "api" ||
    !allowed.has(path[1]) ||
    path.some((p) => p === ".." || p.includes("/") || p.includes("\\"))
  )
    return Response.json({ message: "Rota não encontrada." }, { status: 404 });
  const base = process.env.BACKEND_URL || "http://localhost:8080";
  const url = new URL(
    path.map(encodeURIComponent).join("/") + request.nextUrl.search,
    base.replace(/\/$/, "") + "/",
  );
  const streaming = path.at(-1) === "stream";
  try {
    const upstream = await fetch(url, {
      method: request.method,
      headers: {
        "Content-Type": "application/json",
        Accept: streaming ? "text/event-stream" : "application/json",
        "Accept-Language": "pt-BR",
      },
      body: ["GET", "HEAD"].includes(request.method)
        ? undefined
        : await request.text(),
      cache: "no-store",
      signal: streaming
        ? request.signal
        : AbortSignal.any([request.signal, AbortSignal.timeout(30000)]),
    });
    return new Response(upstream.body, {
      status: upstream.status,
      headers: {
        "Content-Type":
          upstream.headers.get("content-type") || "application/json",
        "Cache-Control": "no-store",
        ...(streaming ? { "X-Accel-Buffering": "no" } : {}),
      },
    });
  } catch {
    return Response.json(
      {
        message:
          "Não foi possível conectar à API. Verifique se o back-end está em execução e tente novamente.",
      },
      { status: 502 },
    );
  }
}
export { proxy as GET, proxy as POST, proxy as PUT };
