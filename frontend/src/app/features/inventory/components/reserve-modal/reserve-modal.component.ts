import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
  ElementRef,
  viewChild,
  afterNextRender,
  OnDestroy,
} from "@angular/core";
import { FormControl, ReactiveFormsModule, Validators } from "@angular/forms";
import { CurrencyPipe } from "@angular/common";
import { PurchaseIntent } from "../../models/inventory";
@Component({
  selector: "app-reserve-modal",
  imports: [ReactiveFormsModule, CurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<dialog
    #dialog
    aria-labelledby="reserve-title"
    (cancel)="cancel($event)"
  >
    <p class="eyebrow">SEPARACIÓN DE UNIDAD</p>
    <h2 id="reserve-title">
      {{ intent().vehicle.brand }} {{ intent().vehicle.model }}
    </h2>
    <p>
      {{ intent().vehicle.price | currency: "COP" : "code" : "1.0-0" }} · Precio
      del concesionario
    </p>
    <p class="reference">Referencia: {{ intent().request.reference }}</p>
    <form (submit)="$event.preventDefault(); send()">
      @if (!locked()) {
        <label for="buyer-alias">Alias ficticio del comprador</label>
        <input
          id="buyer-alias"
          autocomplete="off"
          maxlength="40"
          [formControl]="alias"
          aria-describedby="alias-help"
        />
        <p id="alias-help" class="muted">
          2–40 letras sin tildes, números, espacios, guiones o guiones bajos.
          Sin espacios al inicio o al final.
        </p>
        @if (alias.touched && alias.invalid) {
          <p class="error" role="alert">Ingrese un alias válido.</p>
        }
      } @else {
        <p>
          Comprador: <strong>{{ intent().request.buyerAlias }}</strong>
        </p>
      }
      @if (message()) {
        <p role="alert" class="notice warning">{{ message() }}</p>
      }
      <div class="actions">
        <button
          type="button"
          class="secondary"
          [disabled]="locked() || busy()"
          (click)="closed.emit()"
        >
          Cancelar
        </button>
        <button
          type="submit"
          [disabled]="busy() || (!locked() && alias.invalid)"
        >
          {{
            busy()
              ? "Confirmando…"
              : locked()
                ? "Reintentar misma solicitud"
                : "Confirmar separación"
          }}
        </button>
      </div>
    </form>
  </dialog>`,
})
export class ReserveModalComponent implements OnDestroy {
  readonly intent = input.required<PurchaseIntent>();
  readonly busy = input(false);
  readonly locked = input(false);
  readonly message = input("");
  readonly submitted = output<string>();
  readonly closed = output<void>();
  readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>("dialog");
  readonly alias = new FormControl("", {
    nonNullable: true,
    validators: [
      Validators.required,
      Validators.pattern(/^[A-Za-z0-9][A-Za-z0-9 _-]{1,39}$/),
      (c) => (c.value === c.value.trim() ? null : { trim: true }),
    ],
  });
  private readonly previousFocus = document.activeElement as HTMLElement | null;
  constructor() {
    afterNextRender(() => this.dialog().nativeElement.showModal());
  }
  send() {
    if (!this.busy() && (this.locked() || this.alias.valid))
      this.submitted.emit(
        this.locked() ? this.intent().request.buyerAlias : this.alias.value,
      );
  }
  cancel(event: Event) {
    event.preventDefault();
    if (!this.busy() && !this.locked()) this.closed.emit();
  }
  ngOnDestroy() {
    this.previousFocus?.focus();
  }
}
