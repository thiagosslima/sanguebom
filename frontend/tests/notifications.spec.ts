import { test, expect } from "@playwright/test";

test("sininho limpa ao abrir, persiste por cidadão e reaparece para novos eventos", async ({
  page,
}) => {
  const first = {
    id: 100,
    createdAt: "2026-09-19T10:00:00Z",
    title: "Aviso inicial",
    message: "Conteúdo de teste",
    status: "SENT",
    type: "REMINDER",
  };
  let notices = [first];
  await page.addInitScript(() => {
    class FakeSource extends EventTarget {
      constructor(url: string) {
        super();
        (
          window as unknown as { notificationStream: EventTarget }
        ).notificationStream = this;
        void url;
      }
      close() {}
    }
    Object.defineProperty(window, "EventSource", { value: FakeSource });
  });
  await page.route("**/api/backend/api/users/*/notifications?*", (route) =>
    route.fulfill({
      json: {
        content: notices,
        page: 0,
        size: 8,
        totalElements: notices.length,
        totalPages: 1,
      },
    }),
  );
  await page.goto("/cidadaos/1");
  const bell = page.getByRole("button", {
    name: "Ver notificações",
    exact: true,
  });
  const indicator = bell.locator("i");
  await expect(indicator).toBeVisible();
  await bell.click();
  await expect(
    page.getByRole("heading", { name: "Seu cuidado em tempo real" }),
  ).toBeVisible();
  await expect(indicator).toHaveCount(0);
  await expect(page.getByText("Aviso inicial", { exact: true })).toBeVisible();
  const saved = await page.evaluate(() =>
    JSON.parse(
      localStorage.getItem("sanguebom-notifications-seen:1") || "null",
    ),
  );
  expect(saved).toEqual({ id: 100, createdAt: first.createdAt });
  await page.getByRole("link", { name: "Visão geral", exact: true }).click();
  await expect(page).toHaveURL(/\/cidadaos\/1$/);
  await page.reload();
  await expect(
    page.getByText("Exames registrados", { exact: true }),
  ).toBeVisible();
  await expect(indicator).toHaveCount(0);
  const emit = async (notice: typeof first) =>
    page.evaluate((data) => {
      (
        window as unknown as { notificationStream: EventTarget }
      ).notificationStream.dispatchEvent(
        new MessageEvent("notification", { data: JSON.stringify(data) }),
      );
    }, notice);
  // Pending events may be replayed when SSE reconnects; an old event is not new.
  await emit(first);
  await expect(
    page.getByText("Exames registrados", { exact: true }),
  ).toBeVisible();
  await expect(indicator).toHaveCount(0);
  // A new notification can share the timestamp; the ID disambiguates it.
  const second = { ...first, id: 101, title: "Novo aviso" };
  notices = [second, first];
  await emit(second);
  await expect(indicator).toBeVisible();
  await page.getByRole("link", { name: /^Notificações/ }).click();
  await expect(page.getByText("Novo aviso", { exact: true })).toBeVisible();
  await expect(indicator).toHaveCount(0);
  const third = {
    ...first,
    id: 102,
    title: "Aviso recebido com a lista aberta",
  };
  notices = [third, second, first];
  await emit(third);
  await expect(page.getByText(third.title, { exact: true })).toBeVisible();
  await expect(indicator).toHaveCount(0);
  await page.goto("/cidadaos/2");
  await expect(indicator).toBeVisible();
  await page.goto("/cidadaos/1");
  await expect(
    page.getByText("Exames registrados", { exact: true }),
  ).toBeVisible();
  await expect(indicator).toHaveCount(0);
});

test("histórico vazio não apresenta indicador", async ({ page }) => {
  await page.route("**/api/backend/api/users/*/notifications?*", (route) =>
    route.fulfill({
      json: { content: [], page: 0, size: 8, totalElements: 0, totalPages: 0 },
    }),
  );
  await page.route("**/api/backend/api/users/*/notifications/stream", (route) =>
    route.fulfill({
      status: 200,
      contentType: "text/event-stream",
      body: "event: connected\ndata: {}\n\n",
    }),
  );
  await page.goto("/cidadaos/1");
  await expect(
    page.getByText("Exames registrados", { exact: true }),
  ).toBeVisible();
  await expect(
    page
      .getByRole("button", { name: "Ver notificações", exact: true })
      .locator("i"),
  ).toHaveCount(0);
});
