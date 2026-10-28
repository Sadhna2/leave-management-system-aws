/** Builds a flat static bundle so only compiled frontend files need uploading to S3. */
import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({plugins:[react()],build:{rollupOptions:{output:{
  entryFileNames:'app-[hash].js',chunkFileNames:'chunk-[hash].js',assetFileNames:'[name]-[hash][extname]'
}}}});
