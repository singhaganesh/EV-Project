import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
    plugins: [react(), tailwindcss()],
    // sockjs-client references `global`, which doesn't exist in browser ESM.
    define: {
        global: 'globalThis',
    },
})
