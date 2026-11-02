import type { Config } from 'tailwindcss'

export default <Partial<Config>>{
  content: ['./app/**/*.{vue,ts}', './app/**/*.vue'],
  corePlugins: {
    preflight: false,
  },
}
