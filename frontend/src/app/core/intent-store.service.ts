import { Injectable } from "@angular/core";
import { PurchaseIntent } from "../features/inventory/models/inventory";
@Injectable({ providedIn: "root" })
export class IntentStore {
  private readonly key = "rutamotor.pending.v1";
  load(): PurchaseIntent | null {
    try {
      const p = JSON.parse(sessionStorage.getItem(this.key) ?? "null");
      return p?.request?.reference &&
        p?.request?.vehicleId === p?.vehicle?.id &&
        typeof p?.request?.buyerAlias === "string"
        ? p
        : null;
    } catch {
      return null;
    }
  }
  save(intent: PurchaseIntent) {
    try {
      sessionStorage.setItem(this.key, JSON.stringify(intent));
    } catch {
      /* in-memory retry remains available */
    }
  }
  clear() {
    try {
      sessionStorage.removeItem(this.key);
    } catch {
      /* storage may be unavailable */
    }
  }
}
