import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react' // Sửa lại dòng này thành @vitejs/plugin-react
import tailwindcss from '@tailwindcss/vite' // Thêm dòng 1

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000, // Đổi cổng dev của React thành 3000 cho đẹp
    proxy: {
      // Bất kỳ request nào bắt đầu bằng /api sẽ được chuyển tiếp sang Spring Boot
      '/api': {
        target: 'http://localhost:2026',
        changeOrigin: true,
        secure: false,
      }
    }
  },
  build: {
    // KHI BUILD, VITE SẼ ĐẨY THẲNG SẢN PHẨM VÀO THƯ MỤC STATIC CỦA SPRING BOOT
    outDir: '../src/main/resources/static',
    emptyOutDir: true, // Xóa code cũ trong thư mục static trước khi ghi mới
  }
})