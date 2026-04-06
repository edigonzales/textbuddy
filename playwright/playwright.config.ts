import { defineConfig, devices } from "@playwright/test";
import { resolve } from "node:path";

export default defineConfig({
  testDir: "./tests",
  fullyParallel: false,
  reporter: "list",
  use: {
    baseURL: "http://127.0.0.1:4173",
    trace: "retain-on-failure",
  },
  webServer: {
    command:
      "./gradlew bootRun --args='--server.port=4173 --textbuddy.llm.mode=stub --textbuddy.languagetool.mode=stub --textbuddy.document.mode=stub'",
    cwd: resolve(__dirname, ".."),
    reuseExistingServer: false,
    timeout: 120_000,
    url: "http://127.0.0.1:4173",
  },
  projects: [
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
      },
    },
  ],
});
