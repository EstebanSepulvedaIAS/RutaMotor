import { TestBed } from "@angular/core/testing";
import { of, throwError } from "rxjs";
import { HttpErrorResponse } from "@angular/common/http";
import { CatalogPageComponent } from "./catalog-page.component";
import { VehicleApiService } from "../../services/vehicle-api.service";
import { Vehicle } from "../../models/inventory";
describe("Catalog purchase intention", () => {
  const vehicle: Vehicle = {
    id: "00000000-0000-0000-0000-000000000001",
    vin: "RM000000000000001",
    brand: "Toyota",
    model: "Corolla",
    price: 85000000,
    currency: "COP",
    status: "AVAILABLE",
  };
  const api = { catalog: jest.fn(), reserve: jest.fn() };
  beforeEach(() => {
    sessionStorage.clear();
    jest.clearAllMocks();
    api.catalog.mockReturnValue(
      of({ items: [vehicle], page: 0, size: 25, total: 1 }),
    );
    TestBed.configureTestingModule({
      providers: [{ provide: VehicleApiService, useValue: api }],
    });
  });
  function page() {
    return TestBed.createComponent(CatalogPageComponent).componentInstance;
  }
  it("keeps reference and business data across an unknown outcome and retry", () => {
    api.reserve.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );
    const p = page();
    p.select(vehicle);
    p.submit("Buyer-01");
    const first = api.reserve.mock.calls[0][0];
    expect(p.result()).toBeNull();
    expect(p.locked()).toBe(true);
    expect(p.message()).toContain("sin confirmar");
    p.submit("Different-alias");
    expect(api.reserve.mock.calls[1][0]).toEqual(first);
    const restored = page();
    expect(restored.intent()?.request).toEqual(first);
  });
  it("shows durable rejection and refreshes availability", () => {
    api.reserve.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: {
              result: {
                outcome: "REJECTED",
                reason: "UNAVAILABLE",
                reference: "ref",
              },
            },
          }),
      ),
    );
    const p = page();
    p.select(vehicle);
    p.submit("Buyer-01");
    expect(p.result()?.outcome).toBe("REJECTED");
    expect(p.intent()).toBeNull();
    expect(api.catalog).toHaveBeenCalledTimes(2);
  });
  it("creates a new reference for a new intention after definitive success", () => {
    api.reserve.mockReturnValue(
      of({ outcome: "ACCEPTED", reference: "ref", appliedPrice: 85000000 }),
    );
    const p = page();
    p.select(vehicle);
    const previous = p.intent()!.request.reference;
    p.submit("Buyer-01");
    p.select(vehicle);
    expect(p.intent()!.request.reference).not.toBe(previous);
  });
  it("exposes a catalog error instead of an empty successful catalog", () => {
    api.catalog.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );
    const p = page();
    expect(p.loading()).toBe(false);
    expect(p.error()).toContain("No fue posible");
    expect(p.data()).toBeNull();
  });
});
