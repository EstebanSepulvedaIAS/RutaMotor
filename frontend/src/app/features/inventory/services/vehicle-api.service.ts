import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import {
  CatalogPage,
  ReservationRequest,
  ReservationResult,
} from "../models/inventory";
@Injectable({ providedIn: "root" })
export class VehicleApiService {
  private readonly http = inject(HttpClient);
  catalog(brand: string, page: number) {
    return this.http.get<CatalogPage>("/api/v1/vehicles", {
      params: { brand, page, size: 25 },
    });
  }
  reserve(request: ReservationRequest) {
    return this.http.post<ReservationResult>("/api/v1/reservations", request);
  }
}
