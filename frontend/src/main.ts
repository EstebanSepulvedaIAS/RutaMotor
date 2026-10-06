import { bootstrapApplication } from "@angular/platform-browser";
import { provideHttpClient } from "@angular/common/http";
import { CatalogPageComponent } from "./app/features/inventory/pages/catalog/catalog-page.component";
bootstrapApplication(CatalogPageComponent, {
  providers: [provideHttpClient()],
}).catch(console.error);
