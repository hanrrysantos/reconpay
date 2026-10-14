import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

const root = fileURLToPath(new URL('./', import.meta.url))
const alias = { '@': root.replace(/\/$/, '') }

export default defineConfig({
  test: {
    projects: [
      {
        resolve: { alias },
        test: { name: 'node', environment: 'node', include: ['**/*.test.ts'], exclude: ['node_modules/**', '.next/**'] },
      },
      {
        resolve: { alias },
        test: {
          name: 'dom',
          environment: 'jsdom',
          include: ['**/*.test.tsx'],
          exclude: ['node_modules/**', '.next/**'],
          setupFiles: ['./vitest.setup.ts'],
        },
      },
    ],
  },
})
