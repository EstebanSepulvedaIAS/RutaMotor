import { setupZoneTestEnv } from "jest-preset-angular/setup-env/zone";
import { randomUUID } from "node:crypto";
setupZoneTestEnv();
Object.defineProperty(globalThis.crypto, "randomUUID", { value: randomUUID });
