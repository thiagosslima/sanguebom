import { test, expect } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  const exams = Array.from({ length: 12 }, (_, index) => ({
    id: index + 1,
    collectedAt: "2026-09-18T12:00:00Z",
    healthUnitName: index === 11 ? "Unidade Especial" : "Unidade Central",
    status: "RELEASED",
  }));
  await page.route("**/api/backend/**", async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    let body: unknown = [];
    if (path.endsWith("/appUsers"))
      body = [{ id: 1, name: "Maria Silva", email: "maria@example.test" }];
    else if (path.endsWith("/appUsers/1"))
      body = {
        id: 1,
        name: "Maria Silva",
        email: "maria@example.test",
        birthDate: "1990-01-01",
      };
    else if (path.endsWith("/healthProfiles"))
      body = [{ id: 1, user: 1, sex: "F", examPeriodicity: "YEARLY" }];
    else if (path.endsWith("/exam-goal"))
      body = { dueDate: "2026-12-18", daysRemaining: 89, status: "UP_TO_DATE" };
    else if (path.endsWith("/exams/1"))
      body = {
        ...exams[0],
        riskLevel: "HIGH",
        riskScore: 5,
        explanation: "Avaliação de demonstração",
        items: [
          {
            itemCode: "TEST",
            itemName: "Marcador",
            valueNumeric: 3.5,
            unit: "mg/dL",
            flag: "LOW",
          },
        ],
      };
    else if (path.endsWith("/exams")) {
      const size = Number(url.searchParams.get("size") || 8),
        index = Number(url.searchParams.get("page") || 0);
      body = {
        content: exams.slice(index * size, (index + 1) * size),
        totalElements: exams.length,
        totalPages: Math.ceil(exams.length / size),
      };
    } else if (path.endsWith("/notifications"))
      body = { content: [], totalElements: 0, totalPages: 0 };
    else if (path.endsWith("/stream"))
      return route.fulfill({ contentType: "text/event-stream", body: "" });
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(body),
    });
  });
});

test("menu mobile exclui links ocultos e controla foco, Escape e retorno", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/cidadaos/1");
  await expect(
    page.getByText("Exames registrados", { exact: true }),
  ).toBeVisible();
  await page.keyboard.press("Tab");
  await page.keyboard.press("Tab");
  await expect(page.getByRole("button", { name: "Abrir menu" })).toBeFocused();
  await page.getByRole("button", { name: "Abrir menu" }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(
    page.getByRole("dialog").getByRole("button", { name: "Fechar menu" }),
  ).toBeFocused();
  await page.keyboard.press("Shift+Tab");
  await expect(page.getByRole("link", { name: "Meu perfil" })).toBeFocused();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Abrir menu" })).toBeFocused();
});

test("celular preserva meta, ação de resultado e largura da tela", async ({
  page,
}) => {
  for (const width of [320, 390, 768, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto("/cidadaos/1");
    await expect(
      page.getByText("Sua meta de cuidado", { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Novo exame", exact: true }),
    ).toHaveCSS("font-size", "16px");
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    if (width === 390 || width === 1440)
      await page.screenshot({
        path: `/tmp/sanguebom-improved-${width}.png`,
        fullPage: true,
      });
    if (width < 760) {
      const link = await page
        .getByRole("link", { name: "Ver resultado" })
        .first()
        .boundingBox();
      expect(link!.x).toBeGreaterThanOrEqual(0);
      expect(link!.x + link!.width).toBeLessThanOrEqual(width);
    }
  }
});

test("busca global encontra exame fora da primeira página e preserva filtros ao voltar", async ({
  page,
}) => {
  await page.goto("/cidadaos/1/exames");
  const search = page.getByRole("textbox", {
    name: "Buscar em todo o histórico",
  });
  await search.fill("Especial");
  await expect(page.getByText("Exame #12", { exact: true })).toBeVisible();
  await expect(page).toHaveURL(/q=Especial/);
  await page.reload();
  await expect(search).toHaveValue("Especial");
  await expect(page.getByText("Exame #12", { exact: true })).toBeVisible();
  await page.getByRole("link", { name: "Ver resultado" }).click();
  await expect(page).toHaveURL(/\/exames\/12$/);
  await page.goBack();
  await expect(search).toHaveValue("Especial");
  await expect(page.getByText("Exame #12", { exact: true })).toBeVisible();
});

test("resultado diferencia risco alto de marcador baixo e identifica cidadão na impressão", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/cidadaos/1/exames/1");
  await expect(page.getByText("Risco alto", { exact: true })).toBeVisible();
  await expect(
    page.getByText("Abaixo da referência", { exact: true }),
  ).not.toHaveClass(/green/);
  await expect(page.locator(".analysis-alert")).toBeVisible();
  await expect(page.getByText("3,5 mg/dL", { exact: true })).toBeVisible();
  const classification = await page
    .getByText("Abaixo da referência", { exact: true })
    .boundingBox();
  expect(classification!.x + classification!.width).toBeLessThanOrEqual(390);
  await page.emulateMedia({ media: "print" });
  await expect(
    page.getByText("Cidadão: Maria Silva · Cadastro #1"),
  ).toBeVisible();
});

test("evolução permite consultar pontos por teclado", async ({ page }) => {
  await page.route("**/api/backend/api/examItems", (route) =>
    route.fulfill({
      json: [{ id: 1, code: "TEST", name: "Marcador", unit: "mg/dL" }],
    }),
  );
  await page.route("**/timeline?*", (route) =>
    route.fulfill({
      json: {
        content: [
          {
            examId: 1,
            collectedAt: "2026-09-18T12:00:00Z",
            valueNumeric: 3.5,
            unit: "mg/dL",
            flag: "NORMAL",
            matchedRule: null,
          },
        ],
        totalPages: 1,
        totalElements: 1,
      },
    }),
  );
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/cidadaos/1/evolucao");
  const point = page.getByRole("button", { name: /18\/09\/2026: 3,5/ });
  await point.focus();
  await page.keyboard.press("Enter");
  await expect(page.locator(".chart-reading")).toContainText("3,5 mg/dL");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});

test("novo exame oferece acesso direto ao resultado salvo", async ({
  page,
}) => {
  await page.route("**/api/backend/api/v1/exam-items?*", (route) =>
    route.fulfill({
      json: {
        content: [{ id: 1, name: "Marcador", code: "TEST", unit: "mg/dL" }],
        totalPages: 1,
      },
    }),
  );
  await page.route("**/api/backend/api/healthUnits", (route) =>
    route.fulfill({ json: [{ id: 1, name: "Unidade Central" }] }),
  );
  await page.route("**/api/backend/api/exams", (route) =>
    route.fulfill({ status: 201, json: { examId: 1 } }),
  );
  await page.goto("/cidadaos/1/exames/novo");
  await page.getByLabel("Unidade de saúde").selectOption("1");
  await page.getByLabel("Marcador 1").selectOption("1");
  await page.getByLabel(/^Resultado/).fill("3.5");
  await page.getByRole("button", { name: "Salvar e analisar exame" }).click();
  await page.getByRole("button", { name: "Ver resultado do exame" }).click();
  await expect(page).toHaveURL(/\/exames\/1$/);
  await expect(page.getByText("Risco alto", { exact: true })).toBeVisible();
});

test("falha de exames não aparece como histórico vazio", async ({ page }) => {
  await page.route("**/api/backend/api/users/1/exams?*", (route) =>
    route.fulfill({
      status: 503,
      contentType: "application/json",
      body: JSON.stringify({ message: "Serviço indisponível" }),
    }),
  );
  await page.goto("/cidadaos/1");
  await expect(
    page.getByText("Não foi possível carregar os exames.", { exact: false }),
  ).toBeVisible();
  await expect(page.getByText("Seu histórico começa aqui")).toHaveCount(0);
  await expect(
    page.getByText("Sua meta de cuidado", { exact: true }),
  ).toBeVisible();
});

test("perfil protege alterações e mantém confirmação depois de salvar", async ({
  page,
}) => {
  await page.goto("/cidadaos/1/perfil");
  await page.getByLabel("Nome completo").fill("Maria Atualizada");
  page.once("dialog", (dialog) => dialog.dismiss());
  await page.getByRole("link", { name: "Meus exames", exact: true }).click();
  await expect(page).toHaveURL(/\/perfil$/);
  await page.getByRole("button", { name: "Salvar alterações" }).click();
  await expect(page.getByText("Seu perfil foi atualizado.")).toBeVisible();
  await page.getByRole("link", { name: "Meus exames", exact: true }).click();
  await expect(page).toHaveURL(/\/exames$/);
});
