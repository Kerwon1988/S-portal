module.exports = {
  content: [
    "./src/main/resources/templates/**/*.html",
    "./src/main/frontend/js/**/*.js"
  ],
  theme: {
    extend: {
      colors: {
        bg: "var(--sp-bg)",
        surface: "var(--sp-surface)",
        "surface-alt": "var(--sp-surface-alt)",
        text: {
          DEFAULT: "var(--sp-text)",
          muted: "var(--sp-text-muted)"
        },
        "accent-text": "var(--sp-accent-text)",
        primary: "var(--sp-primary)",
        "on-primary": "var(--sp-on-primary)",
        border: "var(--sp-border)",
        focus: "var(--sp-focus)",
        danger: "var(--sp-danger)",
        success: "var(--sp-success)",
        warning: "var(--sp-warning)",
        info: "var(--sp-info)"
      },
      fontFamily: {
        brand: ["system-ui", "sans-serif"],
        heading: ["system-ui", "sans-serif"],
        body: ["system-ui", "sans-serif"]
      }
    }
  },
  plugins: []
};
