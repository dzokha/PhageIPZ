# PhageIPZ

mvn spring-boot:run

npm run dev

npm run build

mvn clean package -DskipTests

# 1. Tự động kéo mã nguồn, tự build React, tự đóng gói Jar vào Docker Image
docker build -t dzokha/phageipz:v1 .

# 2. Khởi chạy hệ thống PhageIPZ thế hệ mới
docker run -d \
  -p 2026:2026 \
  -v $(pwd)/my_soap_data:/app/data \
  --name phageipz_instance \
  dzokha/phageipz:v1



# Khởi tạo dự án React + Vite
npm create vite@latest frontend -- --template react

# Cài đặt Tailwind CSS cho Vite (Thay thế script CDN)
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# Cài đặt thư viện Icon, D3.js và thư viện gọi API
npm install react-icons d3 axios