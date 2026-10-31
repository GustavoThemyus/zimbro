import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Empacota so o necessario para rodar: imagem final pequena, sem node_modules.
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  reactCompiler: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
