export interface Vehicle {
  id: string;
  vin: string;
  brand: string;
  model: string;
  price: number;
  currency: "COP";
  status: "AVAILABLE" | "RESERVED" | "SOLD";
}
export interface CatalogPage {
  items: Vehicle[];
  page: number;
  size: number;
  total: number;
}
export interface ReservationRequest {
  reference: string;
  vehicleId: string;
  buyerAlias: string;
}
export interface ReservationResult extends ReservationRequest {
  processedAt: string;
  outcome: "ACCEPTED" | "REJECTED";
  reason: "NOT_FOUND" | "UNAVAILABLE" | null;
  appliedPrice: number | null;
  currency: "COP";
}
export interface PurchaseIntent {
  vehicle: Vehicle;
  request: ReservationRequest;
}
