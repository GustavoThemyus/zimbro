import type { MetadataRoute } from "next";

/**
 * O app e instalavel pela tela inicial, sem loja. Os atalhos (`shortcuts`)
 * aparecem no toque longo do icone no Android — e o substituto possivel do
 * widget, porque PWA nao tem widget de tela inicial em nenhum dos dois sistemas.
 */
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Zimbro",
    short_name: "Zimbro",
    description: "Quanto entrou, quanto saiu, quanto sobrou.",
    lang: "pt-BR",
    start_url: "/",
    display: "standalone",
    orientation: "portrait",
    background_color: "#faf9f7",
    theme_color: "#faf9f7",
    icons: [
      { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png" },
      { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png" },
      {
        src: "/icons/icon-maskable-512.png",
        sizes: "512x512",
        type: "image/png",
        purpose: "maskable",
      },
    ],
    shortcuts: [
      { name: "Registrar entrada", url: "/lancar?direcao=entrada" },
      { name: "Registrar saida", url: "/lancar?direcao=saida" },
      { name: "Fechamento do mes", url: "/mes" },
    ],
  };
}
