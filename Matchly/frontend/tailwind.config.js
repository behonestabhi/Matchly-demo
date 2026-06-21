/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eef4ff',
          100: '#d9e6ff',
          200: '#bcd2ff',
          300: '#8eb4ff',
          400: '#598bff',
          500: '#3563f0',
          600: '#2147d6',
          700: '#1c39ac',
          800: '#1d3389',
          900: '#1d306d',
        },
      },
    },
  },
  plugins: [],
};
