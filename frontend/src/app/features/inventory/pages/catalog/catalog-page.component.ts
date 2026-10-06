import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
} from "@angular/core";
import { CurrencyPipe } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Subject, catchError, of, switchMap, tap } from "rxjs";
import { VehicleApiService } from "../../services/vehicle-api.service";
import {
  CatalogPage,
  PurchaseIntent,
  ReservationResult,
  Vehicle,
} from "../../models/inventory";
import { VehicleCardComponent } from "../../components/vehicle-card/vehicle-card.component";
import { ReserveModalComponent } from "../../components/reserve-modal/reserve-modal.component";
import { IntentStore } from "../../../../core/intent-store.service";
@Component({
  selector: "app-root",
  imports: [CurrencyPipe, VehicleCardComponent, ReserveModalComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: "./catalog-page.component.html",
})
export class CatalogPageComponent {
  private readonly api = inject(VehicleApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly store = inject(IntentStore);
  private readonly refresh = new Subject<void>();
  readonly data = signal<CatalogPage | null>(null);
  readonly loading = signal(true);
  readonly error = signal("");
  readonly brand = signal("");
  readonly page = signal(0);
  readonly intent = signal<PurchaseIntent | null>(this.store.load());
  readonly busy = signal(false);
  readonly locked = signal(!!this.intent());
  readonly message = signal(
    this.intent()
      ? "Existe una solicitud sin confirmar. Reintente para recuperar su resultado."
      : "",
  );
  readonly result = signal<ReservationResult | null>(null);
  readonly notice = signal("");
  constructor() {
    this.refresh
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.error.set("");
        }),
        switchMap(() =>
          this.api.catalog(this.brand(), this.page()).pipe(
            catchError(() => {
              this.error.set(
                "No fue posible consultar el catálogo. Intente nuevamente.",
              );
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((data) => {
        this.data.set(data);
        this.loading.set(false);
      });
    this.reload();
  }
  reload() {
    this.refresh.next();
  }
  filter(value: string) {
    this.brand.set(value);
    this.page.set(0);
    this.reload();
  }
  navigate(delta: number) {
    this.page.update((p) => p + delta);
    this.reload();
  }
  select(vehicle: Vehicle) {
    this.result.set(null);
    this.notice.set("");
    this.message.set("");
    this.locked.set(false);
    this.intent.set({
      vehicle,
      request: {
        reference: crypto.randomUUID(),
        vehicleId: vehicle.id,
        buyerAlias: "",
      },
    });
  }
  close() {
    if (!this.locked() && !this.busy()) this.intent.set(null);
  }
  submit(alias: string) {
    const draft = this.intent();
    if (!draft || this.busy()) return;
    const current = this.locked()
      ? draft
      : { ...draft, request: { ...draft.request, buyerAlias: alias } };
    this.intent.set(current);
    this.store.save(current);
    this.locked.set(true);
    this.busy.set(true);
    this.message.set("");
    this.api
      .reserve(current.request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => this.finish(result),
        error: (error: HttpErrorResponse) => {
          if (
            (error.status === 409 || error.status === 404) &&
            error.error?.result
          ) {
            this.finish(error.error.result);
            return;
          }
          this.busy.set(false);
          if (error.status === 400 || error.status === 409) {
            this.notice.set(
              `${error.error?.detail ?? "Solicitud inválida."} Referencia: ${current.request.reference}`,
            );
            this.store.clear();
            this.intent.set(null);
            this.locked.set(false);
            this.reload();
          } else {
            this.message.set(
              "Resultado sin confirmar por un fallo de comunicación o del servicio. No asumimos aceptación ni rechazo. Reintente con esta misma referencia.",
            );
          }
        },
      });
  }
  private finish(result: ReservationResult) {
    this.result.set(result);
    this.busy.set(false);
    this.store.clear();
    this.intent.set(null);
    this.locked.set(false);
    this.reload();
  }
}
