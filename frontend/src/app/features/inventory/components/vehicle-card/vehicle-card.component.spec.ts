import { TestBed } from "@angular/core/testing";
import { VehicleCardComponent } from "./vehicle-card.component";
describe("Vehicle card", () => {
  it("prevents a reserved physical unit from emitting a purchase intention", () => {
    const fixture = TestBed.createComponent(VehicleCardComponent);
    fixture.componentRef.setInput("vehicle", {
      id: "unit",
      vin: "RM000000000000001",
      brand: "Toyota",
      model: "Corolla",
      price: 85000000,
      currency: "COP",
      status: "RESERVED",
    });
    const reserve = jest.fn();
    fixture.componentInstance.reserve.subscribe(reserve);
    fixture.detectChanges();
    const button: HTMLButtonElement =
      fixture.nativeElement.querySelector("button");
    expect(button.disabled).toBe(true);
    button.click();
    expect(reserve).not.toHaveBeenCalled();
  });
});
