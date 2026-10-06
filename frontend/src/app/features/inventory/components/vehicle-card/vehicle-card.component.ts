import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from "@angular/core";
import { CurrencyPipe } from "@angular/common";
import { Vehicle } from "../../models/inventory";
@Component({
  selector: "app-vehicle-card",
  imports: [CurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<article class="vehicle-card">
    <div class="card-top">
      <span class="brand">{{ vehicle().brand }}</span
      ><span
        class="badge"
        [class.available]="vehicle().status === 'AVAILABLE'"
        >{{
          vehicle().status === "AVAILABLE"
            ? "Disponible"
            : vehicle().status === "RESERVED"
              ? "Separado"
              : "Vendido"
        }}</span
      >
    </div>
    <h2>{{ vehicle().model }}</h2>
    <p class="vin">VIN {{ vehicle().vin }}</p>
    <p class="unit">
      Unidad <span>{{ vehicle().id }}</span>
    </p>
    <div class="card-bottom">
      <strong>{{
        vehicle().price | currency: "COP" : "code" : "1.0-0"
      }}</strong>
      <button
        [disabled]="vehicle().status !== 'AVAILABLE' || disabled()"
        (click)="reserve.emit(vehicle())"
      >
        Separar unidad
      </button>
    </div>
  </article>`,
})
export class VehicleCardComponent {
  readonly vehicle = input.required<Vehicle>();
  readonly disabled = input(false);
  readonly reserve = output<Vehicle>();
}
