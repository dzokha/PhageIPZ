# --- GIAI ĐOẠN 1: Build Frontend React ---
FROM node:20-alpine AS frontend-builder
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# --- GIAI ĐOẠN 2: Build Backend Java ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS backend-builder
WORKDIR /build
# Copy file cấu hình Maven
COPY pom.xml .
# Copy mã nguồn Java
COPY src ./src
# Copy kết quả build React từ Giai đoạn 1 vào thư mục static của Java
COPY --from=frontend-builder /build/src/main/resources/static ./src/main/resources/static
# Đóng gói file .jar sạch
RUN mvn clean package -DskipTests

# --- GIAI ĐOẠN 3: Môi trường chạy ứng dụng siêu gọn nhẹ ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cài đặt các công cụ sinh học cần thiết cho PhageIPZ tại đây (Ví dụ: fastp, spades...)
RUN apk update && apk add --no-cache bash curl

# Copy file .jar từ giai đoạn 2 sang
COPY --from=backend-builder /build/target/*.jar phageipz-app.jar

# Tạo các thư mục lưu trữ data cố định bên ngoài
RUN mkdir -p data/uploads data/reports data/raws data/db_files logs

EXPOSE 2026

ENTRYPOINT ["java", "-jar", "phageipz-app.jar"]