import { test, expect } from "@playwright/test";

test("painel, resultado, evolução, conquistas e perfil usam API real", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await expect(page.getByRole("heading", { name: /Olá/ })).toBeVisible();
  await expect(
    page.getByText("Exames registrados", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Ver resultado" }).first(),
  ).toBeVisible();
  await page.screenshot({
    path: "test-results/dashboard-desktop.png",
    fullPage: true,
  });
  await page.getByRole("link", { name: "Ver resultado" }).first().click();
  await expect(
    page.getByText("Avaliação do exame", { exact: false }),
  ).toBeVisible();
  await page
    .getByRole("link", { name: "Evolução da saúde", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Sua saúde ao longo do tempo" }),
  ).toBeVisible();
  await expect(page.locator("input[type=checkbox]").first()).toBeVisible();
  const boxes = page.locator("input[type=checkbox]");
  if ((await boxes.count()) >= 2) {
    await boxes.nth(0).check();
    await boxes.nth(1).check();
    await expect(
      page.getByText(/Variação entre coletas/).first(),
    ).toBeVisible();
  }
  await page
    .getByRole("link", { name: "Minhas conquistas", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Seu cuidado merece reconhecimento" }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Meu perfil", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "Salvar alterações" }),
  ).toBeVisible();
  await expect(page.getByLabel("Nome completo")).not.toBeEmpty();
  await page
    .getByRole("link", { name: /Notificações/ })
    .first()
    .click();
  await expect(
    page.getByRole("heading", { name: "Seu cuidado em tempo real" }),
  ).toBeVisible();
  expect(errors).toEqual([]);
});

test("mobile navega sem transbordamento e troca cidadão", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await expect(
    page.getByText("Exames registrados", { exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "test-results/dashboard-mobile.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Abrir menu" }).click();
  await page.getByRole("link", { name: "Meus exames", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Histórico de exames" }),
  ).toBeVisible();
  const select = page.getByLabel("Selecionar cidadão");
  const options = await select.locator("option").all();
  if (options.length > 2) {
    await select.selectOption((await options[2].getAttribute("value")) || "");
    await expect(
      page.getByText("Exames registrados", { exact: true }),
    ).toBeVisible();
  }
});

test("falha de conexão oferece recuperação", async ({ page }) => {
  await page.route("**/api/backend/api/appUsers", (r) =>
    r.fulfill({
      status: 502,
      contentType: "application/json",
      body: JSON.stringify({ message: "API indisponível para teste." }),
    }),
  );
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "Vamos restabelecer a conexão" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Tentar novamente" }),
  ).toBeVisible();
});

test("cadastro, perfil e exame completos em banco isolado", async ({
  page,
}) => {
  test.skip(
    process.env.E2E_WRITE !== "1",
    "Habilite apenas contra uma API com banco de testes isolado.",
  );
  const stamp = Date.now();
  let cpf = String(stamp).slice(-9);
  for (let n = 9; n < 11; n++) {
    let sum = 0;
    for (let i = 0; i < n; i++) sum += Number(cpf[i]) * (n + 1 - i);
    cpf += String(((sum * 10) % 11) % 10);
  }
  await page.goto("/");
  await expect(
    page.getByText("Exames registrados", { exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Cadastrar cidadão", exact: true })
    .click();
  await page.getByLabel("Nome completo").fill("Cidadão Teste Frontend");
  await page.getByLabel("E-mail").fill(`frontend.${stamp}@example.test`);
  await page.getByLabel("Data de nascimento").fill("1990-01-15");
  await page.getByLabel(/^CPF/).fill(cpf);
  await page
    .getByLabel("Sexo para referências laboratoriais")
    .selectOption("M");
  await page
    .getByLabel("Fatores de risco (opcional)")
    .fill("Acompanhamento de teste");
  await page.getByRole("button", { name: "Criar meu cadastro" }).click();
  await expect(
    page.getByRole("heading", { name: /Olá, Cidadão/ }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Seu histórico começa aqui" }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Meu perfil", exact: true }).click();
  await expect(page.getByLabel("Fatores de risco (opcional)")).toHaveValue(
    "Acompanhamento de teste",
  );
  await page.getByLabel("Altura em cm (opcional)").fill("175");
  await page.getByLabel("Periodicidade dos exames").selectOption("SEMESTERLY");
  const saved = page.waitForResponse(
    (r) =>
      r.url().includes("/healthProfiles/") &&
      r.request().method() === "PUT" &&
      r.ok(),
  );
  await page.getByRole("button", { name: "Salvar alterações" }).click();
  await saved;
  await page.getByRole("button", { name: "Novo exame", exact: true }).click();
  await page.getByLabel("Unidade de saúde").selectOption({ index: 1 });
  await page
    .getByLabel("Marcador 1")
    .selectOption({ label: "Glicemia em Jejum (mg/dL)" });
  await page.getByLabel(/^Resultado/).fill("90");
  await page.getByRole("button", { name: "Salvar e analisar exame" }).click();
  await expect(
    page.getByRole("heading", { name: "Exame registrado", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Ver meus exames" }).click();
  await expect(page.getByRole("link", { name: "Ver resultado" })).toBeVisible();
  await page.getByRole("link", { name: "Ver resultado" }).click();
  await expect(
    page.getByRole("cell", { name: "90 mg/dL", exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Meu perfil", exact: true }).click();
  await expect(page.getByLabel("Altura em cm (opcional)")).toHaveValue(
    "175.00",
  );
  await expect(page.getByLabel("Periodicidade dos exames")).toHaveValue(
    "SEMESTERLY",
  );
});
