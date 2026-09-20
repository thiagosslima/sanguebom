import { test, expect } from "@playwright/test";

test("URLs das telas sobrevivem a recarregamento e histórico do navegador", async ({
  page,
}) => {
  await page.goto("/");
  await expect(page).toHaveURL(/\/cidadaos\/\d+$/);
  const base = page.url();
  const screens = [
    ["Meus exames", "exames", "Histórico de exames"],
    ["Evolução da saúde", "evolucao", "Sua saúde ao longo do tempo"],
    ["Minhas conquistas", "conquistas", "Seu cuidado merece reconhecimento"],
    ["Notificações", "notificacoes", "Seu cuidado em tempo real"],
    ["Meu perfil", "perfil", "Dados pessoais"],
  ];
  for (const [label, slug, heading] of screens) {
    await page.getByRole("link", { name: new RegExp(`^${label}`) }).click();
    await expect(page).toHaveURL(`${base}/${slug}`);
    await expect(
      page.getByRole("heading", { name: heading, exact: true }),
    ).toBeVisible();
    await page.reload();
    await expect(page).toHaveURL(`${base}/${slug}`);
    await expect(
      page.getByRole("heading", { name: heading, exact: true }),
    ).toBeVisible();
  }
  await page.goBack();
  await expect(page).toHaveURL(`${base}/notificacoes`);
  await expect(
    page.getByRole("heading", { name: "Seu cuidado em tempo real" }),
  ).toBeVisible();
  await page.goForward();
  await expect(page).toHaveURL(`${base}/perfil`);
  await expect(
    page.getByRole("heading", { name: "Dados pessoais" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Novo exame", exact: true }).click();
  await expect(page).toHaveURL(`${base}/exames/novo`);
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Informações do exame" }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Cadastrar cidadão", exact: true })
    .click();
  await expect(page).toHaveURL(/\/cidadaos\/novo$/);
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Comece seu acompanhamento" }),
  ).toBeVisible();
});

test("link de resultado funciona sem sessão prévia e tem prioridade sobre cidadão lembrado", async ({
  page,
  browser,
}) => {
  await page.goto("/");
  const result = page.getByRole("link", { name: "Ver resultado" }).first();
  await expect(result).toBeVisible();
  const href = await result.getAttribute("href");
  expect(href).toMatch(/^\/cidadaos\/\d+\/exames\/\d+$/);
  const owner = await page.getByLabel("Selecionar cidadão").inputValue();
  await result.click();
  await expect(
    page.getByText("Avaliação do exame", { exact: false }),
  ).toBeVisible();
  const url = page.url();
  const heading = await page
    .getByRole("heading", { name: /^Exame #/ })
    .textContent();
  await page.reload();
  await expect(
    page.getByRole("heading", { name: heading!, exact: true }),
  ).toBeVisible();
  const context = await browser.newContext();
  try {
    await context.addInitScript(() =>
      localStorage.setItem("sanguebom-user", "99999999"),
    );
    const shared = await context.newPage();
    await shared.goto(url);
    await expect(shared.getByLabel("Selecionar cidadão")).toHaveValue(owner);
    await expect(
      shared.getByRole("heading", { name: heading!, exact: true }),
    ).toBeVisible();
  } finally {
    await context.close();
  }
});

test("troca de cidadão é registrada no histórico", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveURL(/\/cidadaos\/\d+$/);
  const selector = page.getByLabel("Selecionar cidadão");
  const first = await selector.inputValue();
  const alternative = await selector
    .locator("option")
    .evaluateAll(
      (options, current) =>
        options
          .map((o) => (o as HTMLOptionElement).value)
          .find((v) => v && v !== current),
      first,
    );
  test.skip(!alternative, "Requer dois cidadãos para verificar a troca.");
  await selector.selectOption(alternative!);
  await expect(page).toHaveURL(new RegExp(`/cidadaos/${alternative}$`));
  await page.reload();
  await expect(selector).toHaveValue(alternative!);
  await page.goBack();
  await expect(page).toHaveURL(new RegExp(`/cidadaos/${first}$`));
  await expect(selector).toHaveValue(first);
});

test("endereços inválidos não abrem dados de outro cidadão", async ({
  page,
}) => {
  const response = await page.goto("/cidadaos/abc/exames");
  expect(response?.status()).toBe(404);
  await expect(
    page.getByRole("heading", { name: "Página não encontrada" }),
  ).toBeVisible();
  await page.goto("/cidadaos/99999999/exames");
  await expect(
    page.getByRole("heading", { name: "Cidadão não encontrado" }),
  ).toBeVisible();
  await expect(page).toHaveURL(/\/cidadaos\/99999999\/exames$/);
  await page.goto("/cidadaos/1/exames/99999999");
  await expect(page.locator("section.panel .error")).toBeVisible();
  await expect(page.getByRole("heading", { name: /^Exame #/ })).toHaveCount(0);
});
