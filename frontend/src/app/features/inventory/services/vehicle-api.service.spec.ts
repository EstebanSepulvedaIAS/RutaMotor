import { TestBed } from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import {
  provideHttpClientTesting,
  HttpTestingController,
} from "@angular/common/http/testing";
import { VehicleApiService } from "./vehicle-api.service";
describe("VehicleApiService", () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }),
  );
  afterEach(() => TestBed.inject(HttpTestingController).verify());
  it("requests only a page and forwards the brand", () => {
    TestBed.inject(VehicleApiService).catalog("Mazda", 2).subscribe();
    const req = TestBed.inject(HttpTestingController).expectOne(
      (r) => r.url === "/api/v1/vehicles",
    );
    expect(req.request.params.get("size")).toBe("25");
    expect(req.request.params.get("page")).toBe("2");
    expect(req.request.params.get("brand")).toBe("Mazda");
    req.flush({ items: [], total: 0, page: 2, size: 25 });
  });
  it("sends reference and alias without a client-selected price", () => {
    const request = {
      reference: "ref",
      vehicleId: "unit",
      buyerAlias: "Demo-01",
    };
    TestBed.inject(VehicleApiService).reserve(request).subscribe();
    const req = TestBed.inject(HttpTestingController).expectOne(
      "/api/v1/reservations",
    );
    expect(req.request.body).toEqual(request);
    expect(req.request.body.price).toBeUndefined();
    req.flush({});
  });
});
