import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

const frontendDir = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  build: {
    cssCodeSplit: false,
    emptyOutDir: true,
    lib: {
      cssFileName: "editor-island",
      entry: resolve(frontendDir, "src/main.ts"),
      fileName: () => "editor-island.js",
      formats: ["es"],
    },
    outDir: resolve(frontendDir, "../build/frontend-dist"),
  },
  server: {
    port: 5173,
    strictPort: true,
  },
});
