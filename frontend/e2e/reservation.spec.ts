import { test, expect } from "@playwright/test";
test("Angular to real service: reserve and refresh availability", async ({
  page,
}) => {
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: /La próxima ruta/ }),
  ).toBeVisible();
  await page.getByLabel("Filtrar por marca").selectOption("Toyota");
  await expect(page.getByText("Consultando unidades…")).not.toBeVisible();
  const card = page
    .locator("article")
    .filter({
      has: page.getByRole("button", {
        name: "Separar unidad",
        disabled: false,
      }),
    })
    .first();
  const vin = await card.locator(".vin").innerText();
  await card.getByRole("button", { name: "Separar unidad" }).click();
  await page.getByLabel("Alias ficticio del comprador").fill("E2E-buyer");
  await page.getByRole("button", { name: "Confirmar separación" }).click();
  await expect(
    page.getByText("Separación confirmada", { exact: true }),
  ).toBeVisible();
  await expect(page.getByText(/Precio aplicado:/)).toBeVisible();
  const updated = page.locator("article").filter({ hasText: vin });
  await expect(updated.getByText("Separado", { exact: true })).toBeVisible();
  await page.screenshot({
    path: "test-results/catalog-confirmation.png",
    fullPage: true,
  });
  await page.reload();
  await page.getByLabel("Filtrar por marca").selectOption("Toyota");
  await expect(
    page.locator("article").filter({ hasText: vin }).getByRole("button"),
  ).toBeDisabled();
});
test("lost response retains same reference and retries real result", async ({
  page,
}) => {
  await page.goto("/");
  await expect(page.getByText("Consultando unidades…")).not.toBeVisible();
  let original: unknown;
  await page.route("**/api/v1/reservations", async (route) => {
    original = route.request().postDataJSON();
    await route.fetch();
    await route.abort("failed");
  });
  await page
    .getByRole("button", { name: "Separar unidad", disabled: false })
    .first()
    .click();
  await page.getByLabel("Alias ficticio del comprador").fill("Lost-response");
  await page.getByRole("button", { name: "Confirmar separación" }).click();
  await expect(page.getByText(/Resultado sin confirmar por/)).toBeVisible();
  await page.unroute("**/api/v1/reservations");
  const retry = page.waitForRequest("**/api/v1/reservations");
  await page
    .getByRole("button", { name: "Reintentar misma solicitud" })
    .click();
  expect((await retry).postDataJSON()).toEqual(original);
  await expect(
    page.getByText("Separación confirmada", { exact: true }),
  ).toBeVisible();
});
